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

import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axiom.soap.impl.llom.soap11.SOAP11Factory;
import org.apache.axiom.soap.impl.llom.soap12.SOAP12Factory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.WSDL2Constants;
import org.apache.axis2.engine.MessageReceiver;
import org.apache.axis2.util.MessageContextBuilder;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.*;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.NHttpClientConnection;
import org.apache.http.nio.NHttpClientHandler;
import org.apache.http.nio.entity.ContentInputStream;
import org.apache.http.nio.util.*;
import org.apache.http.params.DefaultedHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.*;
import org.apache.synapse.transport.base.MetricsCollector;
import org.apache.synapse.transport.base.threads.WorkerPool;
import org.apache.synapse.transport.base.threads.WorkerPoolFactory;
import org.apache.synapse.transport.nhttp.util.SharedInputBuffer;

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
    /** the buffer allocator */
    private final ByteBufferAllocator allocator;

    /** the Axis2 configuration context */
    ConfigurationContext cfgCtx = null;
    /** the nhttp configuration */
    private NHttpConfiguration cfg = null;

    private WorkerPool workerPool = null;
    /** the metrics collector */
    private MetricsCollector metrics = null;

    public static final String OUTGOING_MESSAGE_CONTEXT = "synapse.axis2_message_context";
    public static final String AXIS2_HTTP_REQUEST = "synapse.axis2-http-request";

    public static final String REQUEST_SOURCE_BUFFER = "synapse.request-source-buffer";
    public static final String RESPONSE_SINK_BUFFER = "synapse.response-sink-buffer";

    private static final String CONTENT_TYPE = "Content-Type";

    /**
     * Create an instance of this client connection handler using the Axis2 configuration
     * context and Http protocol parameters given
     * @param cfgCtx the Axis2 configuration context
     * @param params the Http protocol parameters to adhere to
     */
    public ClientHandler(final ConfigurationContext cfgCtx, final HttpParams params,
        final MetricsCollector metrics) {
        
        super();
        this.cfgCtx = cfgCtx;
        this.params = params;
        this.httpProcessor = getHttpProcessor();
        this.connStrategy = new DefaultConnectionReuseStrategy();
        this.metrics = metrics;
        this.allocator = new HeapByteBufferAllocator();

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
            ContentOutputBuffer outputBuffer = new SharedOutputBuffer(cfg.getBufferSize(), conn, allocator);
            axis2Req.setOutputBuffer(outputBuffer);
            context.setAttribute(REQUEST_SOURCE_BUFFER, outputBuffer);            

            context.setAttribute(AXIS2_HTTP_REQUEST, axis2Req);
            context.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);
            context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, axis2Req.getHttpHost());
            context.setAttribute(OUTGOING_MESSAGE_CONTEXT, axis2Req.getMsgContext());

            HttpRequest request = axis2Req.getRequest();
            request.setParams(new DefaultedHttpParams(request.getParams(), this.params));
            this.httpProcessor.process(request, context);

            conn.submitRequest(request);
            context.setAttribute(ExecutionContext.HTTP_REQUEST, request);

            synchronized(axis2Req) {
                axis2Req.setReadyToStream(true);
                axis2Req.notifyAll();
            }

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

        if (log.isDebugEnabled() ) {
            log.debug("ClientHandler connected : " + conn);
        }

        try {
            HttpContext context = conn.getContext();
            Axis2HttpRequest axis2Req = (Axis2HttpRequest) attachment;
            ContentOutputBuffer outputBuffer = new SharedOutputBuffer(cfg.getBufferSize(), conn, allocator);
            axis2Req.setOutputBuffer(outputBuffer);
            context.setAttribute(REQUEST_SOURCE_BUFFER, outputBuffer);

            context.setAttribute(AXIS2_HTTP_REQUEST, axis2Req);
            context.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);
            context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, axis2Req.getHttpHost());
            context.setAttribute(OUTGOING_MESSAGE_CONTEXT, axis2Req.getMsgContext());

            HttpRequest request = axis2Req.getRequest();
            request.setParams(new DefaultedHttpParams(request.getParams(), this.params));
            this.httpProcessor.process(request, context);

            conn.submitRequest(request);
            context.setAttribute(ExecutionContext.HTTP_REQUEST, request);

            synchronized(axis2Req) {
                axis2Req.setReadyToStream(true);
                axis2Req.notifyAll();
            }

        } catch (IOException e) {
            handleException("I/O Error : " + e.getMessage(), e, conn);
        } catch (HttpException e) {
            handleException("Unexpected HTTP protocol error: " + e.getMessage(), e, conn);
        }
    }

    public void closed(final NHttpClientConnection conn) {
        ConnectionPool.forget(conn);
        checkAxisRequestComplete(conn, "Abnormal connection close", null);

        HttpContext context = conn.getContext();
        context.removeAttribute(RESPONSE_SINK_BUFFER);
        context.removeAttribute(REQUEST_SOURCE_BUFFER);        

        if (log.isTraceEnabled()) {
            log.trace("Connection closed");
        }
    }

    /**
     * Handle connection timeouts by shutting down the connections
     * @param conn the connection being processed
     */
    public void timeout(final NHttpClientConnection conn) {
        if (log.isDebugEnabled()) {
            log.debug("Connection Timeout");
        }
        if (metrics != null) {
            metrics.incrementTimeoutsSending();
        }
        checkAxisRequestComplete(conn, "Connection timeout", null);
        shutdownConnection(conn);
    }

    /**
     * Handle Http protocol violations encountered while reading from underlying channels
     * @param conn the connection being processed
     * @param e the exception encountered
     */
    public void exception(final NHttpClientConnection conn, final HttpException e) {
        log.error("HTTP protocol violation : " + e.getMessage());
    	checkAxisRequestComplete(conn, null, e);
        shutdownConnection(conn);
        if (metrics != null) {
            metrics.incrementFaultsSending();
        }
    }

    /**
     * Handle IO errors while reading or writing to underlying channels
     * @param conn the connection being processed
     * @param e the exception encountered
     */
    public void exception(final NHttpClientConnection conn, final IOException e) {
        log.error("I/O error : " + e.getMessage(), e);
    	checkAxisRequestComplete(conn, null, e);
        shutdownConnection(conn);
        if (metrics != null) {
            metrics.incrementFaultsSending();
        }
    }

    /**
     * check to see if http request-response has completed, if not completed yet,
     * notify an exception to the message-receiver
     *
     * @param conn the connection being checked for completion
     * @param errorMessage the text for an error message to be returned to the MR on failure
     * @param exceptionToRaise an Exception to be returned to the MR on failure
     */
    private void checkAxisRequestComplete(NHttpClientConnection conn,
        final String errorMessage, final Exception exceptionToRaise) {

        Axis2HttpRequest axis2Request = (Axis2HttpRequest)
                conn.getContext().getAttribute(AXIS2_HTTP_REQUEST);

        if (axis2Request != null && !axis2Request.isCompleted()) {

            axis2Request.setCompleted(true);
            if (errorMessage == null && exceptionToRaise == null) {
                return; // no need to continue
            }

            final MessageContext mc = axis2Request.getMsgContext();

            if (mc.getAxisOperation() != null &&
                    mc.getAxisOperation().getMessageReceiver() != null) {

                workerPool.execute( new Runnable() {
                    public void run() {
                        MessageReceiver mr = mc.getAxisOperation().getMessageReceiver();
                        try {
                            MessageContext nioFaultMessageContext = null;
                            if (errorMessage != null) {
                                nioFaultMessageContext = MessageContextBuilder.createFaultMessageContext(
                                    mc, new AxisFault(errorMessage));
                            } else if (exceptionToRaise != null) {
                                nioFaultMessageContext = MessageContextBuilder.createFaultMessageContext(
                                    /** this is not a mistake I do NOT want getMessage()*/
                                    mc, new AxisFault(exceptionToRaise.toString(), exceptionToRaise));
                            }

                            if (nioFaultMessageContext != null) {
                                nioFaultMessageContext.setProperty(
                                        NhttpConstants.SENDING_FAULT, Boolean.TRUE);
                                mr.receive(nioFaultMessageContext);
                            }

                        } catch (AxisFault af) {
                            log.error("Unable to report back failure to the message receiver", af);
                        }
                    }
                });
            }
        }
    }

    /**
     * Process ready input (i.e. response from remote server)
     * @param conn connection being processed
     * @param decoder the content decoder in use
     */
    public void inputReady(final NHttpClientConnection conn, final ContentDecoder decoder) {
        HttpContext context = conn.getContext();
        HttpResponse response = conn.getHttpResponse();
        ContentInputBuffer inBuf = (ContentInputBuffer) context.getAttribute(RESPONSE_SINK_BUFFER);

        try {
            int bytesRead = inBuf.consumeContent(decoder);
            if (metrics != null && bytesRead > 0) {
                metrics.incrementBytesReceived(bytesRead);
            }

            if (decoder.isCompleted()) {
                if (metrics != null) {
                    metrics.incrementMessagesReceived();
                }
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

        ContentOutputBuffer outBuf = (ContentOutputBuffer) context.getAttribute(REQUEST_SOURCE_BUFFER);

        try {
            int bytesWritten = outBuf.produceContent(encoder);
            if (metrics != null && bytesWritten > 0) {
                metrics.incrementBytesSent(bytesWritten);
            }

            if (encoder.isCompleted()) {
                if (metrics != null) {
                    metrics.incrementMessagesSent();
                }
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

        /*
         * responsed received means the whole request has been complete sent to server or
         * server doesn't need the left data of request
         */
    	checkAxisRequestComplete(conn, null, null);
        switch (response.getStatusLine().getStatusCode()) {
            case HttpStatus.SC_ACCEPTED : {
                if (log.isDebugEnabled()) {
                    log.debug("Received a 202 Accepted response");
                }

                // sometimes, some http clients sends an "\r\n" as the content body with a
                // HTTP 202 OK.. we will just get it into this temp buffer and ignore it..
                ContentInputBuffer inputBuffer = new SharedInputBuffer(8, conn, allocator);
                context.setAttribute(RESPONSE_SINK_BUFFER, inputBuffer);

                // create a dummy message with an empty SOAP envelope and a property
                // NhttpConstants.SC_ACCEPTED set to Boolean.TRUE to indicate this is a
                // placeholder message for the transport to send a HTTP 202 to the
                // client. Should / would be ignored by any transport other than
                // nhttp. For example, JMS would not send a reply message for one-way
                // operations.
                MessageContext outMsgCtx =
                        (MessageContext) context.getAttribute(OUTGOING_MESSAGE_CONTEXT);
                MessageReceiver mr = outMsgCtx.getAxisOperation().getMessageReceiver();

                // the following check is to support the dual channel invocation. Hence the
                // response will be sent as a new request to the client over a different channel
                // client sends back a 202 Accepted response to synapse and we need to neglect that 
                // 202 Accepted message
                if (!outMsgCtx.isPropertyTrue(NhttpConstants.IGNORE_SC_ACCEPTED)) {

                    try {
                        MessageContext responseMsgCtx = outMsgCtx.getOperationContext().
                                getMessageContext(WSDL2Constants.MESSAGE_LABEL_IN);
                        if (responseMsgCtx == null ||
                                outMsgCtx.getOptions().isUseSeparateListener()) {
                            // This means that we received a 202 accepted for an out-only ,
                            // for which we do not need a dummy message anyway
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
                        responseMsgCtx.setProperty(AddressingConstants.
                                DISABLE_ADDRESSING_FOR_OUT_MESSAGES, Boolean.TRUE);
                        responseMsgCtx.setProperty(NhttpConstants.SC_ACCEPTED, Boolean.TRUE);
                        mr.receive(responseMsgCtx);

                    } catch (org.apache.axis2.AxisFault af) {
                        log.debug("Unable to report back " +
                                "202 Accepted state to the message receiver");
                    }
                }

                return;
            }
            case HttpStatus.SC_INTERNAL_SERVER_ERROR : {
                Header contentType = response.getFirstHeader(CONTENT_TYPE);
                if (contentType != null) {

                    if ((contentType.getValue().indexOf(SOAP11Constants.SOAP_11_CONTENT_TYPE) >= 0)
                            || contentType.getValue().indexOf(
                            SOAP12Constants.SOAP_12_CONTENT_TYPE) >=0) {

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
            case HttpStatus.SC_CONTINUE : {

                if (log.isDebugEnabled()) {
                    log.debug("Received a 100 Continue response");
                }

                // according to the HTTP 1.1 specification HTTP status 100 continue implies that
                // the response will be followed, and the client should just ignore the 100 Continue
                // and wait for the response
                return;
            }
            case HttpStatus.SC_OK : {
                processResponse(conn, context, response);
                return;
            }
            default : {
                log.warn("Unexpected HTTP status code received : " +
                    response.getStatusLine().getStatusCode() + " :: " +
                    response.getStatusLine().getReasonPhrase());

                Header contentType = response.getFirstHeader(CONTENT_TYPE);
                if (contentType != null) {
                    if ((contentType.getValue().indexOf(SOAP11Constants.SOAP_11_CONTENT_TYPE) >= 0)
                            || contentType.getValue().indexOf(
                            SOAP12Constants.SOAP_12_CONTENT_TYPE) >=0) {

                        if (log.isDebugEnabled()) {
                            log.debug("Received an unexpected response with a SOAP payload");
                        }

                    } else if (contentType.getValue().indexOf("html") == -1) {
                        if (log.isDebugEnabled()) {
                            log.debug("Received an unexpected response with a POX/REST payload");
                        }
                    } else {
                        log.warn("Received an unexpected response - of content type : " +
                            contentType.getValue() + " and status code : " +
                            response.getStatusLine().getStatusCode() + " with reason : " +
                            response.getStatusLine().getReasonPhrase());
                    }
                } else {
                    log.warn("Received an unexpected response - of unknown content type " +
                        " with status code : " +
                        response.getStatusLine().getStatusCode() + " and reason : " +
                        response.getStatusLine().getReasonPhrase());
                }
                
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
    private void processResponse(final NHttpClientConnection conn, HttpContext context,
        HttpResponse response) {

        ContentInputBuffer inputBuffer = new SharedInputBuffer(cfg.getBufferSize(), conn, allocator);
        context.setAttribute(RESPONSE_SINK_BUFFER, inputBuffer);

        BasicHttpEntity entity = new BasicHttpEntity();
        if (response.getStatusLine().getProtocolVersion().greaterEquals(HttpVersion.HTTP_1_1)) {
            entity.setChunked(true);
        }
        response.setEntity(entity);
        context.setAttribute(ExecutionContext.HTTP_RESPONSE, response);

        workerPool.execute(
            new ClientWorker(cfgCtx, new ContentInputStream(inputBuffer), response,
                (MessageContext) context.getAttribute(OUTGOING_MESSAGE_CONTEXT)));

    }

    public void execute(Runnable task) {
        workerPool.execute(task);        
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

    public int getActiveCount() {
        return workerPool.getActiveCount();
    }

    public int getQueueSize() {
        return workerPool.getQueueSize();
    }
}