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
package org.apache.synapse.transport.nhttp;

import org.apache.http.nio.NHttpClientHandler;
import org.apache.http.nio.NHttpClientConnection;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpParamsLinker;
import org.apache.http.*;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.protocol.*;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.synapse.transport.nhttp.util.PipeImpl;
import org.apache.synapse.transport.nhttp.util.WorkerPool;
import org.apache.synapse.transport.nhttp.util.WorkerPoolFactory;
import org.apache.axis2.description.WSDL2Constants;
import org.apache.axis2.engine.MessageReceiver;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.impl.llom.soap12.SOAP12Factory;
import org.apache.axiom.soap.impl.llom.soap11.SOAP11Factory;

import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.io.IOException;

/**
 * The client connection handler. An instance of this class is used by each IOReactor, to
 * process every connection. Hence this class should not store any data related to a single
 * connection - as this is being shared.
 */
public class ClientHandler implements NHttpClientHandler {

    private static final Log log = LogFactory.getLog(ClientHandler.class);

    /** the HTTP protocol parameters to adhere to for outgoing messages */
    private final HttpParams params;
    /** the HttpProcessor for response messages received */
    private final HttpProcessor httpProcessor;
    /** the connection re-use strategy */
    private final ConnectionReuseStrategy connStrategy;

    /** the Axis2 configuration context */
    ConfigurationContext cfgCtx = null;
    /** the nhttp configuration */
    private NHttpConfiguration cfg = null;

    private WorkerPool workerPool = null;

    private static final String REQUEST_BUFFER = "request-buffer";
    private static final String RESPONSE_BUFFER = "response-buffer";
    private static final String OUTGOING_MESSAGE_CONTEXT = "axis2_message_context";
    private static final String REQUEST_SOURCE_CHANNEL = "request-source-channel";
    private static final String RESPONSE_SINK_CHANNEL = "request-sink-channel";

    private static final String CONTENT_TYPE = "Content-Type";

    /**
     * Create an instance of this client connection handler using the Axis2 configuration
     * context and Http protocol parameters given
     * @param cfgCtx the Axis2 configuration context
     * @param params the Http protocol parameters to adhere to
     */
    public ClientHandler(final ConfigurationContext cfgCtx, final HttpParams params) {
        super();
        this.cfgCtx = cfgCtx;
        this.params = params;
        this.httpProcessor = getHttpProcessor();
        this.connStrategy = new DefaultConnectionReuseStrategy();

        this.cfg = NHttpConfiguration.getInstance();
        workerPool = WorkerPoolFactory.getWorkerPool(
            cfg.getClientCoreThreads(),
            cfg.getClientMaxThreads(),
            cfg.getClientKeepalive(),
            cfg.getClientQueueLen(),
            "Client Worker thread group", "HttpClientWorker");
    }

    public void requestReady(final NHttpClientConnection conn) {
        // The connection is ready for submission of a new request
    }

    /**
     * Submit a new request over an already established connection, which has been 
     * 'kept alive'
     * @param conn the connection to use to send the request, which has been kept open
     * @param axis2Req the new request
     */
    public void submitRequest(final NHttpClientConnection conn, Axis2HttpRequest axis2Req) {

        try {
            HttpContext context = conn.getContext();

            context.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);
            context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, axis2Req.getHttpHost());

            context.setAttribute(OUTGOING_MESSAGE_CONTEXT, axis2Req.getMsgContext());
            context.setAttribute(REQUEST_SOURCE_CHANNEL, axis2Req.getSourceChannel());

            HttpRequest request = axis2Req.getRequest();
            HttpParamsLinker.link(request, this.params);
            this.httpProcessor.process(request, context);

            conn.submitRequest(request);
            context.setAttribute(ExecutionContext.HTTP_REQUEST, request);

        } catch (IOException e) {
            handleException("I/O Error : " + e.getMessage(), e, conn);
        } catch (HttpException e) {
            handleException("Unexpected HTTP protocol error: " + e.getMessage(), e, conn);
        }
    }

    /**
     * Invoked when the destination is connected
     * @param conn the connection being processed
     * @param attachment the attachment set previously
     */
    public void connected(final NHttpClientConnection conn, final Object attachment) {
        try {
            HttpContext context = conn.getContext();
            Axis2HttpRequest axis2Req = (Axis2HttpRequest) attachment;

            context.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);
            context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, axis2Req.getHttpHost());

            // allocate temporary buffers to process this request
            context.setAttribute(REQUEST_BUFFER, ByteBuffer.allocate(cfg.getBufferZise()));
            context.setAttribute(RESPONSE_BUFFER, ByteBuffer.allocate(cfg.getBufferZise()));

            context.setAttribute(OUTGOING_MESSAGE_CONTEXT, axis2Req.getMsgContext());
            context.setAttribute(REQUEST_SOURCE_CHANNEL, axis2Req.getSourceChannel());

            HttpRequest request = axis2Req.getRequest();
            HttpParamsLinker.link(request, this.params);
            this.httpProcessor.process(request, context);

            conn.submitRequest(request);
            context.setAttribute(ExecutionContext.HTTP_REQUEST, request);

        } catch (IOException e) {
            handleException("I/O Error : " + e.getMessage(), e, conn);
        } catch (HttpException e) {
            handleException("Unexpected HTTP protocol error: " + e.getMessage(), e, conn);
        }
    }

    public void closed(final NHttpClientConnection conn) {
        log.trace("Connection closed");
    }

    /**
     * Handle connection timeouts by shutting down the connections
     * @param conn the connection being processed
     */
    public void timeout(final NHttpClientConnection conn) {
        if (log.isDebugEnabled()) {
            log.debug("Connection Timeout");
        }
        shutdownConnection(conn);
    }

    /**
     * Handle Http protocol violations encountered while reading from underlying channels
     * @param conn the connection being processed
     * @param e the exception encountered
     */
    public void exception(final NHttpClientConnection conn, final HttpException e) {
        log.error("HTTP protocol violation : " + e.getMessage());
        shutdownConnection(conn);
    }

    /**
     * Handle IO errors while reading or writing to underlying channels
     * @param conn the connection being processed
     * @param e the exception encountered
     */
    public void exception(final NHttpClientConnection conn, final IOException e) {
        log.error("I/O error : " + e.getMessage(), e);
        shutdownConnection(conn);
    }

    /**
     * Process ready input (i.e. response from remote server)
     * @param conn connection being processed
     * @param decoder the content decoder in use
     */
    public void inputReady(final NHttpClientConnection conn, final ContentDecoder decoder) {
        HttpContext context = conn.getContext();
        HttpResponse response = conn.getHttpResponse();
        WritableByteChannel sink = (WritableByteChannel) context.getAttribute(RESPONSE_SINK_CHANNEL);
        ByteBuffer inbuf = (ByteBuffer) context.getAttribute(REQUEST_BUFFER);

        try {
            while (decoder.read(inbuf) > 0) {
                inbuf.flip();
                sink.write(inbuf);
                inbuf.compact();
            }

            if (decoder.isCompleted()) {
                if (sink != null) sink.close();
                if (!connStrategy.keepAlive(response, context)) {
                    conn.close();
                } else {
                    ConnectionPool.release(conn);
                }
            }

        } catch (IOException e) {
            handleException("I/O Error : " + e.getMessage(), e, conn);
        }
    }

    /**
     * Process ready output (i.e. write request to remote server)
     * @param conn the connection being processed
     * @param encoder the encoder in use
     */
    public void outputReady(final NHttpClientConnection conn, final ContentEncoder encoder) {
        HttpContext context = conn.getContext();

        ReadableByteChannel source = (ReadableByteChannel) context.getAttribute(REQUEST_SOURCE_CHANNEL);
        ByteBuffer outbuf = (ByteBuffer) context.getAttribute(RESPONSE_BUFFER);

        try {
            int bytesRead = source.read(outbuf);
            if (bytesRead == -1) {
                encoder.complete();
            } else {
                outbuf.flip();
                encoder.write(outbuf);
                outbuf.compact();
            }

            if (encoder.isCompleted()) {
                source.close();
            }

        } catch (IOException e) {
            handleException("I/O Error : " + e.getMessage(), e, conn);
        }
    }

    /**
     * Process a response received for the request sent out
     * @param conn the connection being processed
     */
    public void responseReceived(final NHttpClientConnection conn) {
        HttpContext context = conn.getContext();
        HttpResponse response = conn.getHttpResponse();

        switch (response.getStatusLine().getStatusCode()) {
            case HttpStatus.SC_ACCEPTED : {
                if (log.isDebugEnabled()) {
                    log.debug("Received a 202 Accepted response");
                }

                // create a dummy message with an empty SOAP envelope and a property
                // NhttpConstants.SC_ACCEPTED set to Boolean.TRUE to indicate this is a
                // placeholder message for the transport to send a HTTP 202 to the
                // client. Should / would be ignored by any transport other than
                // nhttp. For example, JMS would not send a reply message for one-way
                // operations.
                MessageContext outMsgCtx =
                    (MessageContext) context.getAttribute(OUTGOING_MESSAGE_CONTEXT);
                MessageReceiver mr = outMsgCtx.getAxisOperation().getMessageReceiver();

                try {
                    MessageContext responseMsgCtx = outMsgCtx.getOperationContext().
                        getMessageContext(WSDL2Constants.MESSAGE_LABEL_IN);
                    if (responseMsgCtx == null) {
                        // to support Sandesha.. however, this means that we received a 202 accepted
                        // for an out-only , for which we do not need a dummy message anyway
                        return;
                    }
                    responseMsgCtx.setServerSide(true);
                    responseMsgCtx.setDoingREST(outMsgCtx.isDoingREST());
                    responseMsgCtx.setProperty(MessageContext.TRANSPORT_IN,
                        outMsgCtx.getProperty(MessageContext.TRANSPORT_IN));
                    responseMsgCtx.setTransportIn(outMsgCtx.getTransportIn());
                    responseMsgCtx.setTransportOut(outMsgCtx.getTransportOut());

                    responseMsgCtx.setAxisMessage(outMsgCtx.getAxisOperation().
                        getMessage(WSDLConstants.MESSAGE_LABEL_IN_VALUE));
                    responseMsgCtx.setOperationContext(outMsgCtx.getOperationContext());
                    responseMsgCtx.setConfigurationContext(outMsgCtx.getConfigurationContext());
                    responseMsgCtx.setTo(null);

                    if (!outMsgCtx.isDoingREST() && !outMsgCtx.isSOAP11()) {
                        responseMsgCtx.setEnvelope(new SOAP12Factory().getDefaultEnvelope());
                    } else {
                        responseMsgCtx.setEnvelope(new SOAP11Factory().getDefaultEnvelope());
                    }
                    responseMsgCtx.setProperty(AddressingConstants.DISABLE_ADDRESSING_FOR_OUT_MESSAGES, Boolean.TRUE);
                    responseMsgCtx.setProperty(NhttpConstants.SC_ACCEPTED, Boolean.TRUE);
                    mr.receive(responseMsgCtx);

                } catch (org.apache.axis2.AxisFault af) {
                    log.error("Unable to report back 202 Accepted state to the message receiver", af);
                }

                return;
            }
            case HttpStatus.SC_INTERNAL_SERVER_ERROR : {
                Header contentType = response.getFirstHeader(CONTENT_TYPE);
                if (contentType != null) {
                    if ((contentType.getValue().indexOf(SOAP11Constants.SOAP_11_CONTENT_TYPE) >= 0) ||
                     contentType.getValue().indexOf(SOAP12Constants.SOAP_12_CONTENT_TYPE) >=0) {
                        if (log.isDebugEnabled()) {
                            log.debug("Received an internal server error with a SOAP payload");
                        }
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("Received an internal server error with a POX/REST payload");
                        }
                    }
                    processResponse(conn, context, response);
                    return;
                }
                log.error("Received an internal server error : " +
                    response.getStatusLine().getReasonPhrase());
                return;
            }
            case HttpStatus.SC_OK : {
                processResponse(conn, context, response);
            }
        }
    }

    /**
     * Perform processing of the received response though Axis2
     * @param conn
     * @param context
     * @param response
     */
    private void processResponse(final NHttpClientConnection conn, HttpContext context, HttpResponse response) {

        try {
            PipeImpl responsePipe = new PipeImpl();
            context.setAttribute(RESPONSE_SINK_CHANNEL, responsePipe.sink());

            BasicHttpEntity entity = new BasicHttpEntity();
            if (response.getStatusLine().getProtocolVersion().greaterEquals(HttpVersion.HTTP_1_1)) {
                entity.setChunked(true);
            }
            response.setEntity(entity);
            context.setAttribute(ExecutionContext.HTTP_RESPONSE, response);

            workerPool.execute(
                new ClientWorker(cfgCtx, Channels.newInputStream(responsePipe.source()), response,
                    (MessageContext) context.getAttribute(OUTGOING_MESSAGE_CONTEXT)));

        } catch (IOException e) {
            handleException("I/O Error : " + e.getMessage(), e, conn);
        }
    }

    // ----------- utility methods -----------

    private void handleException(String msg, Exception e, NHttpClientConnection conn) {
        log.error(msg, e);
        if (conn != null) {
            shutdownConnection(conn);
        }
    }

    /**
     * Shutdown the connection ignoring any IO errors during the process
     * @param conn the connection to be shutdown
     */
    private void shutdownConnection(final HttpConnection conn) {
        try {
            conn.shutdown();
        } catch (IOException ignore) {}
    }

    /**
     * Return the HttpProcessor for requests
     * @return the HttpProcessor that processes requests
     */
    private HttpProcessor getHttpProcessor() {
        BasicHttpProcessor httpProcessor = new BasicHttpProcessor();
        httpProcessor.addInterceptor(new RequestContent());
        httpProcessor.addInterceptor(new RequestTargetHost());
        httpProcessor.addInterceptor(new RequestConnControl());
        httpProcessor.addInterceptor(new RequestUserAgent());
        httpProcessor.addInterceptor(new RequestExpectContinue());
        return httpProcessor;
    }

}
