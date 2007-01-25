/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.axis2.transport.nhttp;

import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.transport.http.HTTPTransportUtils;
import org.apache.axis2.util.UUIDGenerator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.nio.NHttpServerConnection;
import org.apache.http.protocol.HTTP;

import javax.xml.namespace.QName;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Processes an incoming request through Axis2. An instance of this class would be created to
 * process each unique request
 */
public class ServerWorker implements Runnable {

    private static final Log log = LogFactory.getLog(ServerWorker.class);

    /** the incoming message to be processed */
    private MessageContext msgContext = null;
    /** the Axis2 configuration context */
    private ConfigurationContext cfgCtx = null;
    /** the message handler to be used */
    private ServerHandler serverHandler = null;
    /** the underlying http connection */
    private NHttpServerConnection conn = null;
    /** the http request */
    private HttpRequest request = null;
    /** the http response message (which the this would be creating) */
    private HttpResponse response = null;
    /** the input stream to read the incoming message body */
    private InputStream is = null;
    /** the output stream to write the response message body */
    private OutputStream os = null;
    private static final String SOAPACTION = "SOAPAction";

    /**
     * Create a new server side worker to process an incoming message and optionally begin creating
     * its output. This however does not force the processor to write a response back as the
     * traditional servlet service() method, but creates the background required to write the
     * response, if one would be created.
     * @param cfgCtx the Axis2 configuration context
     * @param conn the underlying http connection
     * @param serverHandler the handler of the server side messages
     * @param request the http request received (might still be in the process of being streamed)
     * @param is the stream input stream to read the request body
     * @param response the response to be populated if applicable
     * @param os the output stream to write the response body if one is applicable
     */
    public ServerWorker(final ConfigurationContext cfgCtx, final NHttpServerConnection conn,
        final ServerHandler serverHandler,
        final HttpRequest request, final InputStream is,
        final HttpResponse response, final OutputStream os) {

        this.cfgCtx = cfgCtx;
        this.conn = conn;
        this.serverHandler = serverHandler;
        this.request = request;
        this.response = response;
        this.is = is;
        this.os = os;
        this.msgContext = createMessageContext(request);
    }

    /**
     * Create an Axis2 message context for the given http request. The request may be in the
     * process of being streamed
     * @param request the http request to be used to create the corresponding Axis2 message context
     * @return the Axis2 message context created
     */
    private MessageContext createMessageContext(HttpRequest request) {

        MessageContext msgContext = new MessageContext();
        msgContext.setProperty(MessageContext.TRANSPORT_NON_BLOCKING, Boolean.TRUE);
        msgContext.setConfigurationContext(cfgCtx);
        msgContext.setIncomingTransportName(Constants.TRANSPORT_HTTP);
        msgContext.setProperty(Constants.OUT_TRANSPORT_INFO, this);
        msgContext.setServiceGroupContextId(UUIDGenerator.getUUID());
        msgContext.setServerSide(true);
        msgContext.setProperty(
            Constants.Configuration.TRANSPORT_IN_URL, request.getRequestLine().getUri());

        Map headers = new HashMap();
        Header[] headerArr = request.getAllHeaders();
        for (int i = 0; i < headerArr.length; i++) {
            headers.put(headerArr[i].getName(), headerArr[i].getValue());
        }
        msgContext.setProperty(MessageContext.TRANSPORT_HEADERS, headers);

        try {
            msgContext.setTransportOut(cfgCtx.getAxisConfiguration()
                .getTransportOut(new QName(Constants.TRANSPORT_HTTP)));
            msgContext.setTransportIn(cfgCtx.getAxisConfiguration()
                .getTransportIn(new QName(Constants.TRANSPORT_HTTP)));
        } catch (AxisFault af) {
            handleException("Unable to get out/in http transport configurations from Axis2", af);
            return null;
        }
        
        return msgContext;
    }

    /**
     * Process the incoming request
     */
    public void run() {

        String method = request.getRequestLine().getMethod().toUpperCase();
        if ("GET".equals(method)) {
            //processGet(response);
        } else if ("POST".equals(method)) {
            processPost();
        } else {
            handleException("Unsupported method : " + method, null);
        }
    }

    /**
     *
     */
    private void processPost() {

        try {
            HTTPTransportUtils.processHTTPPostRequest(
                msgContext, is,
                os,
                (request.getFirstHeader(HTTP.CONTENT_TYPE) != null ?
                    request.getFirstHeader(HTTP.CONTENT_TYPE).getValue() : null),
                (request.getFirstHeader(SOAPACTION) != null ?
                    request.getFirstHeader(SOAPACTION).getValue() : null),
                request.getRequestLine().getUri());
        } catch (AxisFault e) {
            handleException("Error processing POST request ", e);
        }
    }

    private void handleException(String msg, Exception e) {

        log.error(msg, e);
        if (conn != null) {
            try {
                conn.shutdown();
            } catch (IOException ignore) {}
        }
        /*try {
            AxisEngine engine = new AxisEngine(cfgCtx);
            msgContext.setProperty(MessageContext.TRANSPORT_OUT, response.getOutputStream());
            msgContext.setProperty(org.apache.axis2.Constants.OUT_TRANSPORT_INFO, response.getOutputStream());
            MessageContext faultContext = engine.createFaultMessageContext(msgContext, e);
            engine.sendFault(faultContext);

        } catch (Exception ex) {
            response.addHeader(Constants.CONTENT_TYPE, Constants.TEXT_PLAIN);
            OutputStreamWriter out = new OutputStreamWriter(
                response.getOutputStream());
            try {
                out.write(ex.getMessage());
                out.close();
            } catch (IOException ee) {
            }

        } finally {
            response.setStatus(ResponseStatus.INTERNAL_SERVER_ERROR);
            response.commit();
        }*/
    }


    public HttpResponse getResponse() {
        return response;
    }

    public OutputStream getOutputStream() {
        return os;
    }

    public InputStream getIs() {
        return is;
    }

    public ServerHandler getServiceHandler() {
        return serverHandler;
    }

    public NHttpServerConnection getConn() {
        return conn;
    }
}
