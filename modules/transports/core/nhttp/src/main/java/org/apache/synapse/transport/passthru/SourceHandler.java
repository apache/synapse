/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.transport.passthru;

import org.apache.http.nio.*;
import org.apache.http.nio.util.ContentOutputBuffer;
import org.apache.http.nio.util.HeapByteBufferAllocator;
import org.apache.http.*;
import org.apache.http.nio.util.SimpleOutputBuffer;
import org.apache.http.protocol.*;
import org.apache.http.message.BasicHttpResponse;
import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.transport.passthru.config.SourceConfiguration;
import org.apache.synapse.transport.passthru.jmx.LatencyView;
import org.apache.synapse.transport.passthru.jmx.PassThroughTransportMetricsCollector;
import org.apache.synapse.transport.passthru.util.PassThroughTransportUtils;

import java.io.IOException;

/**
 * This is the class where transport interacts with the client. This class
 * receives events for a particular connection. These events give information
 * about the message and its various states.
 */
public class SourceHandler implements NHttpServerEventHandler {

    private static final Log log = LogFactory.getLog(SourceHandler.class);

    private final SourceConfiguration sourceConfiguration;

    private PassThroughTransportMetricsCollector metrics = null;
    
    private LatencyView latencyView = null;
    
    private LatencyView s2sLatencyView = null;

    public SourceHandler(SourceConfiguration sourceConfiguration) {
        this.sourceConfiguration = sourceConfiguration;
        this.metrics = sourceConfiguration.getMetrics();
        
		try {
			if (!sourceConfiguration.isSsl()) {
				this.latencyView = new LatencyView(sourceConfiguration.isSsl());
			} else {
				this.s2sLatencyView = new LatencyView(sourceConfiguration.isSsl());
			}
		} catch (AxisFault e) {
			log.error("Error while initializing latency view calculators", e);
		}
    }

    public void connected(NHttpServerConnection conn) {
        // we have to have these two operations in order
        sourceConfiguration.getSourceConnections().addConnection(conn);
        SourceContext.create(conn, ProtocolState.REQUEST_READY, sourceConfiguration);
        metrics.connected();
    }

    public void requestReceived(NHttpServerConnection conn) {
        try {
            conn.getContext().setAttribute(PassThroughConstants.REQ_ARRIVAL_TIME,
                    System.currentTimeMillis());
        	 
            if (!SourceContext.assertState(conn, ProtocolState.REQUEST_READY) &&
                    !SourceContext.assertState(conn, ProtocolState.GET_REQUEST_COMPLETE)) {
                handleInvalidState(conn, "Request received");
                return;
            }
            // we have received a message over this connection. So we must inform the pool
            sourceConfiguration.getSourceConnections().useConnection(conn);

            // at this point we have read the HTTP Headers
            SourceContext.updateState(conn, ProtocolState.REQUEST_HEAD);
            SourceRequest request = new SourceRequest(
                    sourceConfiguration, conn.getHttpRequest(), conn);
            SourceContext.setRequest(conn, request);
            request.start(conn);
            metrics.incrementMessagesReceived();

            String method = request.getRequest() != null ?
                    request.getRequest().getRequestLine().getMethod().toUpperCase() : "";
			if ("GET".equals(method)) {
				HttpContext context = request.getConnection().getContext();
				ContentOutputBuffer outputBuffer = new SimpleOutputBuffer(8192,
                        HeapByteBufferAllocator.INSTANCE);
				context.setAttribute(PassThroughConstants.PASS_THROUGH_RESPONSE_SOURCE_BUFFER,
                        outputBuffer);
			} 

            sourceConfiguration.getWorkerPool().execute(
                    new ServerWorker(request, sourceConfiguration));

        } catch (HttpException e) {
            log.error("HTTP exception while processing request", e);
            informReaderError(conn);
            SourceContext.updateState(conn, ProtocolState.CLOSED);
            sourceConfiguration.getSourceConnections().shutDownConnection(conn, true);
        } catch (IOException e) {
            logIOException(e);
            informReaderError(conn);
            SourceContext.updateState(conn, ProtocolState.CLOSED);
            sourceConfiguration.getSourceConnections().shutDownConnection(conn, true);
        }
    }

    public void inputReady(NHttpServerConnection conn, ContentDecoder decoder) {
        try {
            ProtocolState protocolState = SourceContext.getState(conn);

            if (protocolState != ProtocolState.REQUEST_HEAD
                    && protocolState != ProtocolState.REQUEST_BODY) {
                handleInvalidState(conn, "Request message body data received");
                return;
            }

            SourceContext.updateState(conn, ProtocolState.REQUEST_BODY);
            SourceRequest request = SourceContext.getRequest(conn);
            int readBytes = request.read(conn, decoder);
            if (readBytes > 0) {
                metrics.incrementBytesReceived(readBytes);
            }

        } catch (IOException e) {
            logIOException(e);
            informReaderError(conn);
            SourceContext.updateState(conn, ProtocolState.CLOSED);
            sourceConfiguration.getSourceConnections().shutDownConnection(conn, true);
        }
    }

    public void responseReady(NHttpServerConnection conn) {
        try {
            ProtocolState protocolState = SourceContext.getState(conn);
            if (protocolState.compareTo(ProtocolState.REQUEST_DONE) < 0) {                
                return;
            }

            if (protocolState.compareTo(ProtocolState.CLOSING) >= 0) {
                return;
            }

            if (protocolState != ProtocolState.REQUEST_DONE) {
                handleInvalidState(conn, "Writing a response");
                return;
            }

            // because the duplex nature of http core we can reach hear without a actual response
            SourceResponse response = SourceContext.getResponse(conn);
            if (response != null) {
                response.start(conn);
                metrics.incrementMessagesSent();
            }

        } catch (IOException e) {
            logIOException(e);
            informWriterError(conn);
            SourceContext.updateState(conn, ProtocolState.CLOSING);
            sourceConfiguration.getSourceConnections().shutDownConnection(conn, true);
        } catch (HttpException e) {
            log.error(e.getMessage(), e);
            informWriterError(conn);
            SourceContext.updateState(conn, ProtocolState.CLOSING);
            sourceConfiguration.getSourceConnections().shutDownConnection(conn, true);
        }
    }

    public void outputReady(NHttpServerConnection conn, ContentEncoder encoder) {
        try {
            ProtocolState protocolState = SourceContext.getState(conn);

            // special case to handle WSDLs
            if (protocolState == ProtocolState.GET_REQUEST_COMPLETE) {
                SimpleOutputBuffer outBuf = (SimpleOutputBuffer) conn.getContext().getAttribute(
                        PassThroughConstants.PASS_THROUGH_RESPONSE_SOURCE_BUFFER);
                synchronized (conn.getContext()) {
                    // SimpleOutputBuffer is not thread safe
                    // Explicit synchronization required
                    int bytesWritten = outBuf.produceContent(encoder);
                    if (metrics != null && bytesWritten > 0) {
                        metrics.incrementBytesSent(bytesWritten);
                    }

                    conn.requestInput();
                    if (!outBuf.hasData() && encoder.isCompleted()) {
                        // We are done - At this point the entire response payload has been
                        // written out to the SimpleOutputBuffer
                        PassThroughTransportUtils.finishUsingSourceConnection(conn.getHttpResponse(),
                                conn, sourceConfiguration.getSourceConnections());
                    }
                }
                return;
            }

            if (protocolState != ProtocolState.RESPONSE_HEAD
                    && protocolState != ProtocolState.RESPONSE_BODY) {
                log.warn("Illegal incoming connection state: "
                        + protocolState + " . Possibly two send backs " +
                        "are happening for the same request");

                handleInvalidState(conn, "Trying to write response body");
                return;
            }

            SourceContext.updateState(conn, ProtocolState.RESPONSE_BODY);
            SourceResponse response = SourceContext.getResponse(conn);

            int bytesSent = response.write(conn, encoder);
            
			if (encoder.isCompleted()) {
				HttpContext context = conn.getContext();
				if (context.getAttribute(PassThroughConstants.REQ_ARRIVAL_TIME) != null &&
				    context.getAttribute(PassThroughConstants.REQ_DEPARTURE_TIME) != null &&
				    context.getAttribute(PassThroughConstants.RES_HEADER_ARRIVAL_TIME) != null) {

					if (latencyView != null) {
						latencyView.notifyTimes((Long) context.getAttribute(PassThroughConstants.REQ_ARRIVAL_TIME),
						                        (Long) context.getAttribute(PassThroughConstants.REQ_DEPARTURE_TIME),
						                        (Long) context.getAttribute(PassThroughConstants.RES_HEADER_ARRIVAL_TIME),
						                        System.currentTimeMillis());
					} else if (s2sLatencyView != null) {
						s2sLatencyView.notifyTimes((Long) context.getAttribute(PassThroughConstants.REQ_ARRIVAL_TIME),
						                           (Long) context.getAttribute(PassThroughConstants.REQ_DEPARTURE_TIME),
						                           (Long) context.getAttribute(PassThroughConstants.RES_HEADER_ARRIVAL_TIME),
						                           System.currentTimeMillis());
					}
				}

				context.removeAttribute(PassThroughConstants.REQ_ARRIVAL_TIME);
				context.removeAttribute(PassThroughConstants.REQ_DEPARTURE_TIME);
				context.removeAttribute(PassThroughConstants.RES_HEADER_ARRIVAL_TIME);
			}
            
            metrics.incrementBytesSent(bytesSent);
        } catch (IOException e) {
            logIOException(e);
            informWriterError(conn);
            SourceContext.updateState(conn, ProtocolState.CLOSING);
            sourceConfiguration.getSourceConnections().shutDownConnection(conn, true);
        } 
    }

    public void endOfInput(NHttpServerConnection conn) throws IOException {
        ProtocolState state = SourceContext.getState(conn);
        boolean isError = false;

        if (state == ProtocolState.REQUEST_READY || state == ProtocolState.RESPONSE_DONE) {
            if (log.isDebugEnabled()) {
                log.debug("Keep-Alive connection was closed by the client: " + conn);
            }
        } else if (state == ProtocolState.REQUEST_BODY || state == ProtocolState.REQUEST_HEAD) {
            isError = true;
            informReaderError(conn);
            log.warn("Connection closed by the client while reading the request: " + conn);
        } else if (state == ProtocolState.RESPONSE_BODY || state == ProtocolState.RESPONSE_HEAD) {
            isError = true;
            informWriterError(conn);
            log.warn("Connection closed by the client end while writing the response: " + conn);
        } else if (state == ProtocolState.REQUEST_DONE) {
            isError = true;
            log.warn("Connection closed by the client after request is read: " + conn);
        }

        SourceContext.updateState(conn, ProtocolState.CLOSED);
        sourceConfiguration.getSourceConnections().shutDownConnection(conn, isError);
    }

    public void exception(NHttpServerConnection conn, Exception e) {
        if (e instanceof HttpException) {
            exception(conn, (HttpException) e);
        } else if (e instanceof IOException) {
            exception(conn, (IOException) e);
        } else {
            log.error("Unexpected exception encountered in SourceHandler", e);
            metrics.incrementFaultsReceiving();

            ProtocolState state = SourceContext.getState(conn);
            if (state == ProtocolState.REQUEST_BODY ||
                    state == ProtocolState.REQUEST_HEAD) {
                informReaderError(conn);
            } else if (state == ProtocolState.RESPONSE_BODY ||
                    state == ProtocolState.RESPONSE_HEAD) {
                informWriterError(conn);
            } else if (state == ProtocolState.REQUEST_DONE) {
                informWriterError(conn);
            } else if (state == ProtocolState.RESPONSE_DONE) {
                informWriterError(conn);
            }

            SourceContext.updateState(conn, ProtocolState.CLOSED);
            sourceConfiguration.getSourceConnections().shutDownConnection(conn, true);
        }
    }

    public void exception(NHttpServerConnection conn, IOException e) {
        logIOException(e);

        metrics.incrementFaultsReceiving();

        ProtocolState state = SourceContext.getState(conn);
        if (state == ProtocolState.REQUEST_BODY ||
                state == ProtocolState.REQUEST_HEAD) {
            informReaderError(conn);
        } else if (state == ProtocolState.RESPONSE_BODY ||
                state == ProtocolState.RESPONSE_HEAD) {
            informWriterError(conn);
        } else if (state == ProtocolState.REQUEST_DONE) {
            informWriterError(conn);
        } else if (state == ProtocolState.RESPONSE_DONE) {
            informWriterError(conn);
        }
        
        SourceContext.updateState(conn, ProtocolState.CLOSED);
        sourceConfiguration.getSourceConnections().shutDownConnection(conn, true);
    }

    private void logIOException(IOException e) {
        // this check feels like crazy! But weird things happened, when load testing.
        if (e == null) {
            return;
        }
        if (e instanceof ConnectionClosedException || (e.getMessage() != null && (
                e.getMessage().toLowerCase().contains("connection reset by peer") ||
                e.getMessage().toLowerCase().contains("forcibly closed")))) {
            if (log.isDebugEnabled()) {
                log.debug("I/O error (Probably a keep-alive connection was closed):" + e.getMessage());
            }
        } else if (e.getMessage() != null) {
            String msg = e.getMessage().toLowerCase();
            if (msg.contains("broken")) {
                log.warn("I/O error (Probably the connection " +
                        "was closed by the remote party):" + e.getMessage());
            } else {
                log.error("I/O error: " + e.getMessage(), e);
            }

            metrics.incrementFaultsReceiving();
        } else {
            log.error("Unexpected I/O error: " + e.getClass().getName(), e);
            metrics.incrementFaultsReceiving();
        }
    }

    public void exception(NHttpServerConnection conn, HttpException e) {
        if (log.isDebugEnabled()) {
            log.debug("HTTP protocol error encountered in SourceHandler", e);
        }

        if (conn.isResponseSubmitted()) {
            sourceConfiguration.getSourceConnections().shutDownConnection(conn, true);
            return;
        }
        HttpContext httpContext = conn.getContext();

        HttpResponse response = new BasicHttpResponse(
                HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST, "Bad request");
        response.addHeader(HTTP.CONN_DIRECTIVE, HTTP.CONN_CLOSE);

        // Pre-process HTTP request
        httpContext.setAttribute(HttpCoreContext.HTTP_CONNECTION, conn);
        httpContext.setAttribute(HttpCoreContext.HTTP_REQUEST, null);
        httpContext.setAttribute(HttpCoreContext.HTTP_RESPONSE, response);

        try {
            sourceConfiguration.getHttpProcessor().process(response, httpContext);
            conn.submitResponse(response);
        } catch (Exception ex) {
            log.error("Error while handling HttpException", ex);
        } finally {
            SourceContext.updateState(conn, ProtocolState.CLOSED);
            sourceConfiguration.getSourceConnections().shutDownConnection(conn, true);
        }
    }

    public void timeout(NHttpServerConnection conn) {
        ProtocolState state = SourceContext.getState(conn);

        if (state == ProtocolState.REQUEST_READY || state == ProtocolState.RESPONSE_DONE) {
            if (log.isDebugEnabled()) {
                log.debug("Keep-Alive connection was time out: " + conn);
            }
        } else if (state == ProtocolState.REQUEST_BODY ||
                state == ProtocolState.REQUEST_HEAD) {

            metrics.incrementTimeoutsReceiving();

            informReaderError(conn);
            log.warn("Connection time out while reading the request: " + conn);
        } else if (state == ProtocolState.RESPONSE_BODY ||
                state == ProtocolState.RESPONSE_HEAD) {
            informWriterError(conn);
            log.warn("Connection time out while writing the response: " + conn);
        } else if (state == ProtocolState.REQUEST_DONE){
            log.warn("Connection time out after request is read: " + conn);
        }

        SourceContext.updateState(conn, ProtocolState.CLOSED);
        sourceConfiguration.getSourceConnections().shutDownConnection(conn, true);
    }

    public void closed(NHttpServerConnection conn) {
        ProtocolState state = SourceContext.getState(conn);
        boolean isFault = false;

        if (state == ProtocolState.REQUEST_READY || state == ProtocolState.RESPONSE_DONE) {
            if (log.isDebugEnabled()) {
                log.debug("Keep-Alive connection was closed: " + conn);
            }
        } else if (state == ProtocolState.REQUEST_BODY || state == ProtocolState.REQUEST_HEAD) {
            isFault = true;
            informReaderError(conn);
            log.warn("Connection closed while reading the request: " + conn);
        } else if (state == ProtocolState.RESPONSE_BODY || state == ProtocolState.RESPONSE_HEAD) {
            isFault = true;
            informWriterError(conn);
            log.warn("Connection closed while writing the response: " + conn);
        } else if (state == ProtocolState.REQUEST_DONE) {
            isFault = true;
            log.warn("Connection closed after request is read: " + conn);
        }

        metrics.disconnected();
        if (state != ProtocolState.CLOSED) {
            SourceContext.updateState(conn, ProtocolState.CLOSED);
            sourceConfiguration.getSourceConnections().shutDownConnection(conn, isFault);
        }
    }

    private void handleInvalidState(NHttpServerConnection conn, String action) {
        log.warn(action + " while the handler is in an inconsistent state " +
                SourceContext.getState(conn));
        SourceContext.updateState(conn, ProtocolState.CLOSED);
        sourceConfiguration.getSourceConnections().shutDownConnection(conn, true);
    }

    private void informReaderError(NHttpServerConnection conn) {
        Pipe reader = SourceContext.get(conn).getReader();

        metrics.incrementFaultsReceiving();

        if (reader != null) {
            reader.producerError();
        }
    }

    private void informWriterError(NHttpServerConnection conn) {
        Pipe writer = SourceContext.get(conn).getWriter();

        metrics.incrementFaultsSending();

        if (writer != null) {
            writer.consumerError();
        }
    }
    
    // ----------- utility methods -----------

    private void handleException(String msg, Exception e, NHttpServerConnection conn) {
        String errorMessage;
        if (conn != null) {
            errorMessage = "[" + conn + "] " + msg;
        } else {
            errorMessage = msg;
        }
        log.error(errorMessage, e);
    }
    
    
    /**
     * Commit the response to the connection. Processes the response through the configured
     * HttpProcessor and submits it to be sent out. This method hides any exceptions and is targeted
     * for non critical (i.e. browser requests etc) requests, which are not core messages
     * @param conn the connection being processed
     * @param response the response to commit over the connection
     */
    public void commitResponseHideExceptions(
            final NHttpServerConnection conn, final HttpResponse response) {
        try {
            conn.suspendInput();
            sourceConfiguration.getHttpProcessor().process(response, conn.getContext());
            conn.submitResponse(response);
        } catch (HttpException e) {
            handleException("Unexpected HTTP protocol error : " + e.getMessage(), e, conn);
        } catch (IOException e) {
            handleException("IO error submitting response : " + e.getMessage(), e, conn);
        }
    }

}
