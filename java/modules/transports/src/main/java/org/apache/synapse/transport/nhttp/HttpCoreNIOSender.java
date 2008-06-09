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

import org.apache.axiom.om.OMOutputFormat;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.addressing.AddressingHelper;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.engine.MessageReceiver;
import org.apache.axis2.handlers.AbstractHandler;
import org.apache.axis2.transport.MessageFormatter;
import org.apache.axis2.transport.OutTransportInfo;
import org.apache.axis2.transport.TransportSender;
import org.apache.axis2.util.MessageContextBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.SSLIOSessionHandler;
import org.apache.http.nio.NHttpClientConnection;
import org.apache.http.nio.NHttpClientHandler;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.IOReactorExceptionHandler;
import org.apache.http.nio.reactor.SessionRequest;
import org.apache.http.nio.reactor.SessionRequestCallback;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.synapse.transport.base.BaseConstants;
import org.apache.synapse.transport.base.ManagementSupport;
import org.apache.synapse.transport.base.MetricsCollector;
import org.apache.synapse.transport.base.TransportView;
import org.apache.synapse.transport.nhttp.util.MessageFormatterDecoratorFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * NIO transport sender for Axis2 based on HttpCore and NIO extensions
 */
public class HttpCoreNIOSender extends AbstractHandler implements TransportSender, ManagementSupport {

    private static final Log log = LogFactory.getLog(HttpCoreNIOSender.class);

    /** The Axis2 configuration context */
    private ConfigurationContext cfgCtx;
    /** The IOReactor */
    private DefaultConnectingIOReactor ioReactor = null;
    /** The client handler */
    private ClientHandler handler = null;
    /** The session request callback that calls back to the message receiver with errors */
    private final SessionRequestCallback sessionRequestCallback = getSessionRequestCallback();
    /** The SSL Context to be used */
    private SSLContext sslContext = null;
    /** The SSL session handler that manages hostname verification etc */
    private SSLIOSessionHandler sslIOSessionHandler = null;
    /** Metrics collector for the sender */
    private MetricsCollector metrics = new MetricsCollector();
    /** state of the listener */
    private int state = BaseConstants.STOPPED;

    /**
     * Initialize the transport sender, and execute reactor in new seperate thread
     * @param cfgCtx the Axis2 configuration context
     * @param transportOut the description of the http/s transport from Axis2 configuration
     * @throws AxisFault thrown on an error
     */
    public void init(ConfigurationContext cfgCtx, TransportOutDescription transportOut) throws AxisFault {
        this.cfgCtx = cfgCtx;

        // is this an SSL Sender?
        sslContext = getSSLContext(transportOut);
        sslIOSessionHandler = getSSLIOSessionHandler(transportOut);

        // start the Sender in a new seperate thread
        Thread t = new Thread(new Runnable() {
            public void run() {
                executeClientEngine();
            }
        }, "HttpCoreNIOSender");
        t.start();
        log.info((sslContext == null ? "HTTP" : "HTTPS") + " Sender starting");

        // register with JMX
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        String jmxAgentName = System.getProperty("jmx.agent.name");
        if (jmxAgentName == null || "".equals(jmxAgentName)) {
            jmxAgentName = "org.apache.synapse";
        }
        String name;
        try {
            name = jmxAgentName + ":Type=Transport,ConnectorName=" +
                "nio-http" + (sslContext == null ? "" : "s") + "-sender";
            TransportView tBean = new TransportView(null, this);
            registerMBean(mbs, tBean, name);
        } catch (Exception e) {
            log.warn("Error registering the non-blocking http" +
                (sslContext == null ? "" : "s") + " transport sender for JMX management", e);
        }

    }

    /**
     * Configure and start the IOReactor
     */
    private void executeClientEngine() {

        HttpParams params = getClientParameters();
        try {
            ioReactor = new DefaultConnectingIOReactor(
                NHttpConfiguration.getInstance().getClientIOWorkers(), params);
            ioReactor.setExceptionHandler(new IOReactorExceptionHandler() {
                public boolean handle(IOException ioException) {
                    log.warn("System may be unstable: IOReactor encountered a checked exception : " +
                        ioException.getMessage(), ioException);
                    return true;
                }

                public boolean handle(RuntimeException runtimeException) {
                    log.warn("System may be unstable: IOReactor encountered a runtime exception : " +
                        runtimeException.getMessage(), runtimeException);
                    return true;
                }
            });
        } catch (IOException e) {
            log.error("Error starting the IOReactor", e);
        }

        handler = new ClientHandler(cfgCtx, params, metrics);
        IOEventDispatch ioEventDispatch = getEventDispatch(
            handler, sslContext, sslIOSessionHandler, params);

        state = BaseConstants.STARTED;
        try {
            ioReactor.execute(ioEventDispatch);
        } catch (InterruptedIOException ex) {
            log.fatal("Reactor Interrupted");
        } catch (IOException e) {
            log.fatal("Encountered an I/O error: " + e.getMessage(), e);
        }
        log.info((sslContext == null ? "HTTP" : "HTTPS") + " Sender Shutdown");
    }

    /**
     * Return the IOEventDispatch implementation to be used. This is overridden by the
     * SSL sender
     * @param handler
     * @param sslContext
     * @param params
     * @return
     */
    protected IOEventDispatch getEventDispatch(
        NHttpClientHandler handler, SSLContext sslContext,
        SSLIOSessionHandler sslIOSessionHandler, HttpParams params) {
        return new PlainClientIOEventDispatch(handler, params);
    }

    /**
     * Always return null, as this implementation does not support outgoing SSL
     * @param transportOut
     * @return null
     * @throws AxisFault
     */
    protected SSLContext getSSLContext(TransportOutDescription transportOut) throws AxisFault {
        return null;
    }

    /**
     * Create the SSL IO Session handler to be used by this listener
     * @param transportOut
     * @return always null
     */
    protected SSLIOSessionHandler getSSLIOSessionHandler(TransportOutDescription transportOut)
        throws AxisFault {
        return null;
    }

    /**
     * get HTTP protocol parameters to which the sender must adhere to
     * @return the applicable HTTP protocol parameters
     */
    private HttpParams getClientParameters() {
        NHttpConfiguration cfg = NHttpConfiguration.getInstance();
        HttpParams params = new BasicHttpParams();
        params
            .setIntParameter(HttpConnectionParams.SO_TIMEOUT,
                cfg.getProperty(HttpConnectionParams.SO_TIMEOUT, 60000))
            .setIntParameter(HttpConnectionParams.CONNECTION_TIMEOUT,
                cfg.getProperty(HttpConnectionParams.CONNECTION_TIMEOUT, 10000))
            .setIntParameter(HttpConnectionParams.SOCKET_BUFFER_SIZE,
                cfg.getProperty(HttpConnectionParams.SOCKET_BUFFER_SIZE, 8 * 1024))
            .setBooleanParameter(HttpConnectionParams.STALE_CONNECTION_CHECK,
                cfg.getProperty(HttpConnectionParams.STALE_CONNECTION_CHECK, 0) == 1)
            .setBooleanParameter(HttpConnectionParams.TCP_NODELAY,
                cfg.getProperty(HttpConnectionParams.TCP_NODELAY, 1) == 1)
            .setParameter(HttpProtocolParams.USER_AGENT, "Synapse-HttpComponents-NIO");
        return params;
    }

    /**
     * transport sender invocation from Axis2 core
     * @param msgContext message to be sent
     * @return the invocation response (always InvocationResponse.CONTINUE)
     * @throws AxisFault on error
     */
    public InvocationResponse invoke(MessageContext msgContext) throws AxisFault {

        // remove unwanted HTTP headers (if any from the current message)
        removeUnwantedHeaders(msgContext);

        if (AddressingHelper.isReplyRedirected(msgContext)
                && !msgContext.getReplyTo().hasNoneAddress()) {

            msgContext.setProperty(NhttpConstants.IGNORE_SC_ACCEPTED, Constants.VALUE_TRUE);
        }

        EndpointReference epr = NhttpUtils.getDestinationEPR(msgContext);
        if (epr != null) {
            if (!AddressingConstants.Final.WSA_NONE_URI.equals(epr.getAddress())) {
                sendAsyncRequest(epr, msgContext);
            } else {
                handleException("Cannot send message to " + AddressingConstants.Final.WSA_NONE_URI);
            }
        } else {

            if (msgContext.getProperty(Constants.OUT_TRANSPORT_INFO) != null) {
                if (msgContext.getProperty(Constants.OUT_TRANSPORT_INFO) instanceof ServerWorker) {
                    sendAsyncResponse(msgContext);
                } else {
                    sendUsingOutputStream(msgContext);
                }
            } else {
                handleException("No valid destination EPR or OutputStream to send message");
            }
        }

        if (msgContext.getOperationContext() != null) {
            msgContext.getOperationContext().setProperty(
                Constants.RESPONSE_WRITTEN, Constants.VALUE_TRUE);
        }

        return InvocationResponse.CONTINUE;
    }

    /**
     * Remove unwanted headers from the http response of outgoing request. These are headers which
     * should be dictated by the transport and not the user. We remove these as these may get
     * copied from the request messages
     * @param msgContext the Axis2 Message context from which these headers should be removed
     */
    private void removeUnwantedHeaders(MessageContext msgContext) {
        Map headers = (Map) msgContext.getProperty(MessageContext.TRANSPORT_HEADERS);

        if (headers == null || headers.isEmpty()) {
            return;
        }

        Iterator iter = headers.keySet().iterator();
        while (iter.hasNext()) {
            String headerName = (String) iter.next();
            if (HTTP.CONN_DIRECTIVE.equalsIgnoreCase(headerName) ||
                HTTP.TRANSFER_ENCODING.equalsIgnoreCase(headerName) ||
                HTTP.DATE_HEADER.equalsIgnoreCase(headerName) ||
                HTTP.SERVER_HEADER.equalsIgnoreCase(headerName) ||
                HTTP.CONTENT_TYPE.equalsIgnoreCase(headerName) ||
                HTTP.CONTENT_LEN.equalsIgnoreCase(headerName) ||
                HTTP.USER_AGENT.equalsIgnoreCase(headerName)) {
                iter.remove();
            }
        }
    }

    /**
     * Send the request message asynchronously to the given EPR
     * @param epr the destination EPR for the message
     * @param msgContext the message being sent
     * @throws AxisFault on error
     */
    private void sendAsyncRequest(EndpointReference epr, MessageContext msgContext) throws AxisFault {
        try {
            URL url = new URL(epr.getAddress());
            int port = url.getPort();
            if (port == -1) {
                // use default
                if ("http".equals(url.getProtocol())) {
                    port = 80;
                } else if ("https".equals(url.getProtocol())) {
                    port = 443;
                }
            }
            HttpHost httpHost = new HttpHost(url.getHost(), port, url.getProtocol());

            Axis2HttpRequest axis2Req = new Axis2HttpRequest(epr, httpHost, msgContext);

            NHttpClientConnection conn = ConnectionPool.getConnection(url.getHost(), port);

            if (conn == null) {
                ioReactor.connect(new InetSocketAddress(url.getHost(), port),
                    null, axis2Req, sessionRequestCallback);
                if (log.isDebugEnabled()) {
                    log.debug("A new connection established to : " + url.getHost() + ":" + port);
                }
            } else {
                ((ClientHandler) handler).submitRequest(conn, axis2Req);
                if (log.isDebugEnabled()) {
                    log.debug("An existing connection reused to : " + url.getHost() + ":" + port);
                }
            }

            axis2Req.streamMessageContents();

        } catch (MalformedURLException e) {
            handleException("Malformed destination EPR : " + epr.getAddress(), e);
        } catch (IOException e) {
            handleException("IO Error while submiting request message for sending", e);
        }
    }

    /**
     * Send the passed in response message, asynchronously
     * @param msgContext the message context to be sent
     * @throws AxisFault on error
     */
    private void sendAsyncResponse(MessageContext msgContext) throws AxisFault {

        // remove unwanted HTTP headers (if any from the current message)
        removeUnwantedHeaders(msgContext);
        
        ServerWorker worker = (ServerWorker) msgContext.getProperty(Constants.OUT_TRANSPORT_INFO);
        HttpResponse response = worker.getResponse();

        OMOutputFormat format = NhttpUtils.getOMOutputFormat(msgContext);
        MessageFormatter messageFormatter =
                MessageFormatterDecoratorFactory.createMessageFormatterDecorator(msgContext);
        response.setHeader(
            HTTP.CONTENT_TYPE,
            messageFormatter.getContentType(msgContext, format, msgContext.getSoapAction()));

        // return http 500 when a SOAP fault is returned
        if (msgContext.getEnvelope().getBody().hasFault() || msgContext.isProcessingFault()) {
            response.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }

        // if this is a dummy message to handle http 202 case with non-blocking IO
        // set the status code to 202 and the message body to an empty byte array (see below)
        if (msgContext.isPropertyTrue(NhttpConstants.SC_ACCEPTED)) {
            response.setStatusCode(HttpStatus.SC_ACCEPTED);
        } else {
            Object statusCode = msgContext.getProperty(NhttpConstants.HTTP_SC);
            if (statusCode != null) {
                try {
                    response.setStatusCode(Integer.parseInt(
                            msgContext.getProperty(NhttpConstants.HTTP_SC).toString()));
                } catch (NumberFormatException e) {
                    log.warn("Unable to set the HTTP Status Code from " +
                            "the property HHTP_SC with value : " + statusCode);
                }
            }
        }

        // set any transport headers
        Map transportHeaders = (Map) msgContext.getProperty(MessageContext.TRANSPORT_HEADERS);
        if (transportHeaders != null && !transportHeaders.values().isEmpty()) {
            Iterator iter = transportHeaders.keySet().iterator();
            while (iter.hasNext()) {
                Object header = iter.next();
                Object value = transportHeaders.get(header);
                if (value != null && header instanceof String && value instanceof String) {
                    response.setHeader((String) header, (String) value);
                }
            }
        }
        worker.getServiceHandler().commitResponse(worker.getConn(), response);

        OutputStream out = worker.getOutputStream();
        try {
            if (msgContext.isPropertyTrue(NhttpConstants.SC_ACCEPTED)) {
                // see comment above on the reasoning
                out.write(new byte[0]);
            } else {
                messageFormatter.writeTo(msgContext, format, out, false);
            }
            out.close();
        } catch (IOException e) {
            handleException("IO Error sending response message", e);
        }

        try {
            worker.getIs().close();
        } catch (IOException ignore) {}        
    }

    private void sendUsingOutputStream(MessageContext msgContext) throws AxisFault {

        OMOutputFormat format = NhttpUtils.getOMOutputFormat(msgContext);
        MessageFormatter messageFormatter =
                MessageFormatterDecoratorFactory.createMessageFormatterDecorator(msgContext);
        OutputStream out = (OutputStream) msgContext.getProperty(MessageContext.TRANSPORT_OUT);

        if (msgContext.isServerSide()) {
            OutTransportInfo transportInfo =
                (OutTransportInfo) msgContext.getProperty(Constants.OUT_TRANSPORT_INFO);

            if (transportInfo != null) {
                transportInfo.setContentType(
                messageFormatter.getContentType(msgContext, format, msgContext.getSoapAction()));
            } else {
                throw new AxisFault(Constants.OUT_TRANSPORT_INFO + " has not been set");
            }
        }

        try {
            messageFormatter.writeTo(msgContext, format, out, false);
            out.close();
        } catch (IOException e) {
            handleException("IO Error sending response message", e);
        }
    }


    public void cleanup(MessageContext msgContext) throws AxisFault {
        // do nothing
    }

    public void stop() {
        if (state != BaseConstants.STARTED) return;
        try {
            ioReactor.shutdown();
            state = BaseConstants.STOPPED;
        } catch (IOException e) {
            log.warn("Error shutting down IOReactor", e);
        }
    }

    /**
     * Return a SessionRequestCallback which gets notified of a connection failure
     * or an error during a send operation. This method finds the corresponding
     * Axis2 message context for the outgoing request, and find the message receiver
     * and sends a fault message back to the message receiver that is marked as
     * related to the outgoing request
     * @return a Session request callback
     */
    private SessionRequestCallback getSessionRequestCallback() {
        return new SessionRequestCallback() {
            public void completed(SessionRequest request) {
            }

            public void failed(SessionRequest request) {
                handleError(request, false);
            }

            public void timeout(SessionRequest request) {
                handleError(request, true);
                request.cancel();
            }

            public void cancelled(SessionRequest sessionRequest) {

            }

            private void handleError(SessionRequest request, boolean isTimeout) {
                if (request.getAttachment() != null &&
                    request.getAttachment() instanceof Axis2HttpRequest) {

                    Axis2HttpRequest axis2Request = (Axis2HttpRequest) request.getAttachment();
                    if (!axis2Request.isCompleted()) {

                        axis2Request.setCompleted(true);
                        MessageContext mc = axis2Request.getMsgContext();
                        final MessageReceiver mr = mc.getAxisOperation().getMessageReceiver();

                        if (mr != null) {
                            try {
                                // this fault is NOT caused by the endpoint while processing. so we have to
                                // inform that this is a sending error (e.g. endpoint failure) and handle it
                                // differently at the message receiver.

                                AxisFault axisFault;
                                if (isTimeout) {
                                    // In case of a timeout there is no exception
                                    axisFault = new AxisFault("The connection timed out");
                                } else {
                                    Exception exception = request.getException();
                                    /** this is not a mistake I do NOT want getMessage()*/
                                    axisFault = new AxisFault(exception.toString(), exception);
                                }
                                final MessageContext nioFaultMessageContext =
                                    MessageContextBuilder.createFaultMessageContext(mc, axisFault);
                                nioFaultMessageContext.setProperty(NhttpConstants.SENDING_FAULT, Boolean.TRUE);

                                handler.execute(new Runnable() {
                                    public void run() {
                                        try {
                                            mr.receive(nioFaultMessageContext);
                                        } catch (AxisFault af) {
                                            log.error("Error processing fault message context", af);
                                        }
                                    }
                                });

                            } catch (AxisFault af) {
                                log.error("Unable to report back failure to the message receiver", af);
                            }
                        }
                    }
                }
            }
        };
    }

    // -------------- utility methods -------------
    private void handleException(String msg, Exception e) throws AxisFault {
        log.error(msg, e);
        throw new AxisFault(msg, e);
    }

    private void handleException(String msg) throws AxisFault {
        log.error(msg);
        throw new AxisFault(msg);
    }

    public void pause() throws AxisFault {
        if (state != BaseConstants.STARTED) return;
        state = BaseConstants.PAUSED;
        log.info("Sender paused");
    }

    public void resume() throws AxisFault {
        if (state != BaseConstants.PAUSED) return;
        state = BaseConstants.STARTED;
        log.info("Sender resumed");
    }

    public void maintenenceShutdown(long millis) throws AxisFault {
        if (state != BaseConstants.STARTED) return;
        try {
            long start = System.currentTimeMillis();
            ioReactor.shutdown(millis);
            state = BaseConstants.STOPPED;
            log.info("Sender shutdown in : " + (System.currentTimeMillis() - start) / 1000 + "s");
        } catch (IOException e) {
            handleException("Error shutting down the IOReactor for maintenence", e);
        }
    }

    /**
     * Returns the number of active threads processing messages
     * @return number of active threads processing messages
     */    
    public int getActiveThreadCount() {
        return handler.getActiveCount();
    }

    /**
     * Returns the number of requestes queued in the thread pool
     * @return queue size
     */
    public int getQueueSize() {
        return handler.getQueueSize();
    }

    // -- jmx/management methods--
    public long getMessagesReceived() {
        if (metrics != null) {
            return metrics.getMessagesReceived();
        }
        return -1;
    }

    public long getFaultsReceiving() {
        if (metrics != null) {
            return metrics.getFaultsReceiving();
        }
        return -1;
    }

    public long getBytesReceived() {
        if (metrics != null) {
            return metrics.getBytesReceived();
        }
        return -1;
    }

    public long getMessagesSent() {
        if (metrics != null) {
            return metrics.getMessagesSent();
        }
        return -1;
    }

    public long getFaultsSending() {
        if (metrics != null) {
            return metrics.getFaultsSending();
        }
        return -1;
    }

    public long getBytesSent() {
        if (metrics != null) {
            return metrics.getBytesSent();
        }
        return -1;
    }

    private void registerMBean(MBeanServer mbs, Object mbeanInstance, String objectName) {
        try {
            ObjectName name = new ObjectName(objectName);
            Set set = mbs.queryNames(name, null);
            if (set != null && set.isEmpty()) {
                mbs.registerMBean(mbeanInstance, name);
            } else {
                mbs.unregisterMBean(name);
                mbs.registerMBean(mbeanInstance, name);
            }
        } catch (Exception e) {
            log.warn("Error registering a MBean with objectname ' " + objectName +
                " ' for JMX management", e);
        }
    }
}
