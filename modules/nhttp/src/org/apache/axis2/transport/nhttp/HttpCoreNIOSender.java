/*
* Copyright 2004,2005 The Apache Software Foundation.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.axis2.transport.nhttp;

import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.handlers.AbstractHandler;
import org.apache.axis2.transport.TransportSender;
import org.apache.axis2.transport.OutTransportInfo;
import org.apache.axiom.om.OMOutputFormat;
import org.apache.http.nio.NHttpClientHandler;
import org.apache.http.nio.NHttpClientConnection;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.SessionRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpHost;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.DefaultClientIOEventDispatch;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.stream.XMLStreamException;
import javax.net.ssl.SSLContext;
import java.io.OutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Iterator;

/**
 * NIO transport sender for Axis2 based on HttpCore and NIO extensions
 */
public class HttpCoreNIOSender extends AbstractHandler implements TransportSender {

    private static final Log log = LogFactory.getLog(HttpCoreNIOSender.class);

    /** The Axis2 configuration context */
    private ConfigurationContext cfgCtx;
    /** The IOReactor */
    private ConnectingIOReactor ioReactor = null;
    /** The client handler */
    private NHttpClientHandler handler = null;
    /** The SSL Context to be used */
    SSLContext sslContext = null;

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

        // start the Sender in a new seperate thread
        Thread t = new Thread(new Runnable() {
            public void run() {
                executeClientEngine();
            }
        }, "HttpCoreNIOSender");
        t.start();
        log.info("Sender started");
    }

    /**
     * Configure and start the IOReactor
     */
    private void executeClientEngine() {

        HttpParams params = getClientParameters();
        try {
            ioReactor = new DefaultConnectingIOReactor(2, params);
        } catch (IOException e) {
            log.error("Error starting the IOReactor", e);
        }

        handler = new ClientHandler(cfgCtx, params);
        IOEventDispatch ioEventDispatch = getEventDispatch(handler, sslContext, params);

        try {
            ioReactor.execute(ioEventDispatch);
        } catch (InterruptedIOException ex) {
            log.fatal("Reactor Interrupted");
        } catch (IOException e) {
            log.fatal("Encountered an I/O error: " + e.getMessage(), e);
        }
        log.info("Sender Shutdown");
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
        NHttpClientHandler handler, SSLContext sslContext, HttpParams params) {
        return new DefaultClientIOEventDispatch(handler, params);
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
     * get HTTP protocol parameters to which the sender must adhere to
     * @return the applicable HTTP protocol parameters
     */
    private HttpParams getClientParameters() {
        HttpParams params = new BasicHttpParams();
        params
            .setIntParameter(HttpConnectionParams.SO_TIMEOUT, 30000)
            .setIntParameter(HttpConnectionParams.CONNECTION_TIMEOUT, 10000)
            .setIntParameter(HttpConnectionParams.SOCKET_BUFFER_SIZE, 8 * 1024)
            .setBooleanParameter(HttpConnectionParams.STALE_CONNECTION_CHECK, false)
            .setBooleanParameter(HttpConnectionParams.TCP_NODELAY, true)
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

        EndpointReference epr = Util.getDestinationEPR(msgContext);
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
                SessionRequest req = ioReactor.connect(
                    new InetSocketAddress(url.getHost(), port), null, axis2Req);
                log.debug("A new connection established");
            } else {
                ((ClientHandler) handler).submitRequest(conn, axis2Req);
                log.debug("An existing connection reused");
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

        ServerWorker worker = (ServerWorker) msgContext.getProperty(Constants.OUT_TRANSPORT_INFO);
        HttpResponse response = worker.getResponse();

        OMOutputFormat format = Util.getOMOutputFormat(msgContext);

        response.setHeader(HTTP.CONTENT_TYPE, Util.getContentType(msgContext) + "; charset=" + format.getCharSetEncoding());

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
        format.setDoOptimize(msgContext.isDoingMTOM());
        try {
            (msgContext.isDoingREST() ?
                msgContext.getEnvelope().getBody().getFirstElement() : msgContext.getEnvelope())
                .serializeAndConsume(out, format);
            out.close();
        } catch (XMLStreamException e) {
            handleException("Error serializing response message", e);
        } catch (IOException e) {
            handleException("IO Error sending response message", e);
        }

        try {
            worker.getIs().close();
        } catch (IOException ignore) {}        
    }

    private void sendUsingOutputStream(MessageContext msgContext) throws AxisFault {
        OMOutputFormat format = Util.getOMOutputFormat(msgContext);
        OutputStream out =
            (OutputStream) msgContext
                .getProperty(MessageContext.TRANSPORT_OUT);

        if (msgContext.isServerSide()) {
            OutTransportInfo transportInfo =
                (OutTransportInfo) msgContext.getProperty(Constants.OUT_TRANSPORT_INFO);

            if (transportInfo != null) {
                String encoding = Util.getContentType(msgContext) + "; charset=" + format.getCharSetEncoding();
                transportInfo.setContentType(encoding);
            } else {
                throw new AxisFault(Constants.OUT_TRANSPORT_INFO + " has not been set");
            }
        }

        try {
            (msgContext.isDoingREST() ?
                msgContext.getEnvelope().getBody().getFirstElement() : msgContext.getEnvelope())
                .serializeAndConsume(out, format);
        } catch (XMLStreamException e) {
            handleException("Error serializing response message", e);
        }
    }


    public void cleanup(MessageContext msgContext) throws AxisFault {
        // do nothing
    }

    public void stop() {
        try {
            ioReactor.shutdown();
            log.info("Sender shut down");
        } catch (IOException e) {
            log.warn("Error shutting down IOReactor", e);
        }
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
}
