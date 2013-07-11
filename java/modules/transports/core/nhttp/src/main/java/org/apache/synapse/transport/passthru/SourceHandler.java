/*
 *  Copyright 2013 Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.synapse.transport.passthru;

import org.apache.http.nio.*;
import org.apache.http.*;
import org.apache.http.protocol.*;
import org.apache.http.params.DefaultedHttpParams;
import org.apache.http.message.BasicHttpResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.transport.passthru.config.SourceConfiguration;
import org.apache.synapse.transport.passthru.jmx.PassThroughTransportMetricsCollector;

import java.io.IOException;

/**
 * This is the class where transport interacts with the client. This class
 * receives events for a particular connection. These events give information
 * about the message and its various states.
 */
public class SourceHandler implements NHttpServiceHandler {
    private static Log log = LogFactory.getLog(SourceHandler.class);

    private final SourceConfiguration sourceConfiguration;

    private PassThroughTransportMetricsCollector metrics = null;

    public SourceHandler(SourceConfiguration sourceConfiguration) {
        this.sourceConfiguration = sourceConfiguration;
        this.metrics = sourceConfiguration.getMetrics();
    }

    public void connected(NHttpServerConnection conn) {
        // we have to have these two operations in order
        sourceConfiguration.getSourceConnections().addConnection(conn);
        SourceContext.create(conn, ProtocolState.REQUEST_READY, sourceConfiguration);

        metrics.connected();
    }

    public void requestReceived(NHttpServerConnection conn) {
        try {
            if (!SourceContext.assertState(conn, ProtocolState.REQUEST_READY)) {
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

            sourceConfiguration.getWorkerPool().execute(
                    new ServerWorker(request, sourceConfiguration));
        } catch (HttpException e) {
            log.error(e.getMessage(), e);

            informReaderError(conn);

            SourceContext.updateState(conn, ProtocolState.CLOSED);
            sourceConfiguration.getSourceConnections().shutDownConnection(conn);
        } catch (IOException e) {
            logIOException(e);

            informReaderError(conn);

            SourceContext.updateState(conn, ProtocolState.CLOSED);
            sourceConfiguration.getSourceConnections().shutDownConnection(conn);
        }
    }

    public void inputReady(NHttpServerConnection conn,
                           ContentDecoder decoder) {
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
            sourceConfiguration.getSourceConnections().shutDownConnection(conn);
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
            sourceConfiguration.getSourceConnections().shutDownConnection(conn);
        } catch (HttpException e) {
            log.error(e.getMessage(), e);

            informWriterError(conn);

            SourceContext.updateState(conn, ProtocolState.CLOSING);
            sourceConfiguration.getSourceConnections().shutDownConnection(conn);
        }
    }

    public void outputReady(NHttpServerConnection conn,
                            ContentEncoder encoder) {
        try {
            ProtocolState protocolState = SourceContext.getState(conn);
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
            metrics.incrementBytesSent(bytesSent);
        } catch (IOException e) {
            logIOException(e);

            informWriterError(conn);

            SourceContext.updateState(conn, ProtocolState.CLOSING);
            sourceConfiguration.getSourceConnections().shutDownConnection(conn);
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
        sourceConfiguration.getSourceConnections().shutDownConnection(conn);
    }

    private void logIOException(IOException e) {
        // this check feels like crazy! But weird things happened, when load testing.
        if (e == null) {
            return;
        }
        if (e instanceof ConnectionClosedException || (e.getMessage() != null &&
                e.getMessage().toLowerCase().contains("connection reset by peer") ||
                e.getMessage().toLowerCase().contains("forcibly closed"))) {
            if (log.isDebugEnabled()) {
                log.debug("I/O error (Probably the keepalive connection " +
                        "was closed):" + e.getMessage());
            }
        } else if (e.getMessage() != null) {
            String msg = e.getMessage().toLowerCase();
            if (msg.indexOf("broken") != -1) {
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
        try {
            if (conn.isResponseSubmitted()) {
                sourceConfiguration.getSourceConnections().shutDownConnection(conn);
                return;
            }
            HttpContext httpContext = conn.getContext();

            HttpResponse response = new BasicHttpResponse(
                    HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST, "Bad request");
            response.setParams(
                    new DefaultedHttpParams(sourceConfiguration.getHttpParameters(),
                            response.getParams()));
            response.addHeader(HTTP.CONN_DIRECTIVE, HTTP.CONN_CLOSE);

            // Pre-process HTTP request
            httpContext.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);
            httpContext.setAttribute(ExecutionContext.HTTP_REQUEST, null);
            httpContext.setAttribute(ExecutionContext.HTTP_RESPONSE, response);

            sourceConfiguration.getHttpProcessor().process(response, httpContext);

            conn.submitResponse(response);            
            SourceContext.updateState(conn, ProtocolState.CLOSED);
            conn.close();
        } catch (Exception e1) {
            log.error(e.getMessage(), e);
            SourceContext.updateState(conn, ProtocolState.CLOSED);
            sourceConfiguration.getSourceConnections().shutDownConnection(conn);
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
        sourceConfiguration.getSourceConnections().shutDownConnection(conn);
    }

    public void closed(NHttpServerConnection conn) {
        ProtocolState state = SourceContext.getState(conn);

        if (state == ProtocolState.REQUEST_READY || state == ProtocolState.RESPONSE_DONE) {
            if (log.isDebugEnabled()) {
                log.debug("Keep-Alive connection was closed: " + conn);
            }
        } else if (state == ProtocolState.REQUEST_BODY ||
                state == ProtocolState.REQUEST_HEAD) {
            informReaderError(conn);
            log.warn("Connection closed while reading the request: " + conn);
        } else if (state == ProtocolState.RESPONSE_BODY ||
                state == ProtocolState.RESPONSE_HEAD) {
            informWriterError(conn);
            log.warn("Connection closed while writing the response: " + conn);
        } else if (state == ProtocolState.REQUEST_DONE) {
            log.warn("Connection closed by the client after request is read: " + conn);
        }

        metrics.disconnected();

        SourceContext.updateState(conn, ProtocolState.CLOSED);
        sourceConfiguration.getSourceConnections().shutDownConnection(conn);
    }

    private void handleInvalidState(NHttpServerConnection conn, String action) {
        log.warn(action + " while the handler is in an inconsistent state " +
                SourceContext.getState(conn));
        SourceContext.updateState(conn, ProtocolState.CLOSED);
        sourceConfiguration.getSourceConnections().shutDownConnection(conn);
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
}
