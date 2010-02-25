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
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.synapse.transport.nhttp;

import org.apache.axiom.om.util.UUIDGenerator;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.builder.BuilderUtil;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.transport.RequestResponseTransport;
import org.apache.axis2.transport.base.MetricsCollector;
import org.apache.axis2.transport.http.HTTPTransportReceiver;
import org.apache.axis2.transport.http.HTTPTransportUtils;
import org.apache.axis2.util.JavaUtils;
import org.apache.axis2.util.MessageContextBuilder;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.*;
import org.apache.http.nio.NHttpServerConnection;
import org.apache.http.protocol.HTTP;
import org.apache.synapse.transport.nhttp.util.NhttpUtil;
import org.apache.synapse.transport.nhttp.util.RESTUtil;
import org.apache.ws.commons.schema.XmlSchema;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;

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
    /** is this https? */
    private boolean isHttps = false;
    /** the http request */
    private HttpRequest request = null;
    /** the http response message (which the this would be creating) */
    private HttpResponse response = null;
    /** the input stream to read the incoming message body */
    private InputStream is = null;
    /** the output stream to write the response message body */
    private OutputStream os = null;
    /** the metrics collector */
    private MetricsCollector metrics = null;
    
    private static final String SOAPACTION   = "SOAPAction";
    private static final String LOCATION     = "Location";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String TEXT_HTML    = "text/html";
    private static final String TEXT_XML     = "text/xml";
    /**
     * Save requesting user IP address for logging - even during response processing when
     * the connection may be closed
     */
    private String remoteAddress = null;

    /**
     * Create a new server side worker to process an incoming message and optionally begin creating
     * its output. This however does not force the processor to write a response back as the
     * traditional servlet service() method, but creates the background required to write the
     * response, if one would be created.
     * @param cfgCtx the Axis2 configuration context
     * @param conn the underlying http connection
     * @param isHttps whether https or not
     * @param metrics metrics for the transport
     * @param serverHandler the handler of the server side messages
     * @param request the http request received (might still be in the process of being streamed)
     * @param is the stream input stream to read the request body
     * @param response the response to be populated if applicable
     * @param os the output stream to write the response body if one is applicable
     */
    public ServerWorker(final ConfigurationContext cfgCtx, final NHttpServerConnection conn,
        final boolean isHttps,
        final MetricsCollector metrics,
        final ServerHandler serverHandler,
        final HttpRequest request, final InputStream is,
        final HttpResponse response, final OutputStream os) {

        this.cfgCtx = cfgCtx;
        this.conn = conn;
        this.isHttps = isHttps;
        this.metrics = metrics;
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
        msgContext.setMessageID(UUIDGenerator.getUUID());

        // There is a discrepency in what I thought, Axis2 spawns a new threads to
        // send a message if this is TRUE - and I want it to be the other way
        msgContext.setProperty(MessageContext.TRANSPORT_NON_BLOCKING, Boolean.FALSE);
        msgContext.setConfigurationContext(cfgCtx);
        if (isHttps) {
            msgContext.setTransportOut(cfgCtx.getAxisConfiguration()
                .getTransportOut(Constants.TRANSPORT_HTTPS));
            msgContext.setTransportIn(cfgCtx.getAxisConfiguration()
                .getTransportIn(Constants.TRANSPORT_HTTPS));
            msgContext.setIncomingTransportName(Constants.TRANSPORT_HTTPS);
        } else {
            msgContext.setTransportOut(cfgCtx.getAxisConfiguration()
                .getTransportOut(Constants.TRANSPORT_HTTP));
            msgContext.setTransportIn(cfgCtx.getAxisConfiguration()
                .getTransportIn(Constants.TRANSPORT_HTTP));
            msgContext.setIncomingTransportName(Constants.TRANSPORT_HTTP);
        }
        msgContext.setProperty(Constants.OUT_TRANSPORT_INFO, this);
        msgContext.setServiceGroupContextId(UUIDGenerator.getUUID());
        msgContext.setServerSide(true);
        msgContext.setProperty(
            Constants.Configuration.TRANSPORT_IN_URL, request.getRequestLine().getUri());

        // http transport header names are case insensitive 
        Map<String, String> headers = new TreeMap<String, String>(new Comparator<String>() {
            public int compare(String o1, String o2) {
                return o1.compareToIgnoreCase(o2);
            }
        });
        
        for (Header header : request.getAllHeaders()) {
            headers.put(header.getName(), header.getValue());
        }
        msgContext.setProperty(MessageContext.TRANSPORT_HEADERS, headers);

        // find the remote party IP address and set it to the message context
        if (conn instanceof HttpInetConnection) {
            HttpInetConnection inetConn = (HttpInetConnection) conn;
            InetAddress remoteAddr = inetConn.getRemoteAddress();
            if (remoteAddr != null) {
                msgContext.setProperty(
                        MessageContext.REMOTE_ADDR, remoteAddr.getHostAddress());
                msgContext.setProperty(
                        NhttpConstants.REMOTE_HOST, NhttpUtil.getHostName(remoteAddr));
                remoteAddress = remoteAddr.getHostAddress();
            }
        }

        msgContext.setProperty(RequestResponseTransport.TRANSPORT_CONTROL,
                new HttpCoreRequestResponseTransport(msgContext));

        msgContext.setProperty(ServerHandler.SERVER_CONNECTION_DEBUG,
            conn.getContext().getAttribute(ServerHandler.SERVER_CONNECTION_DEBUG));
        
        return msgContext;
    }

    /**
     * Process the incoming request
     */
    @SuppressWarnings({"unchecked"})
    public void run() {

        String method = request.getRequestLine().getMethod().toUpperCase();
        msgContext.setProperty(Constants.Configuration.HTTP_METHOD,
            request.getRequestLine().getMethod());

        if (NHttpConfiguration.getInstance().isHttpMethodDisabled(method)) {
            handleException("Unsupported method : " + method, null);
        }

        String uri = request.getRequestLine().getUri();
        String oriUri = uri;

        if (uri.indexOf(cfgCtx.getServicePath()) != -1) {
            // discard upto servicePath
            uri = uri.substring(uri.indexOf(cfgCtx.getServicePath()) +
                cfgCtx.getServicePath().length());
            // discard [proxy] service name if any
            int pos = uri.indexOf("/", 1);
            if (pos > 0) {
                uri = uri.substring(pos);
            } else {
                pos = uri.indexOf("?");
                if (pos != -1) {
                    uri = uri.substring(pos);
                } else {
                    uri = "";
                }
            }
        } else {
            // remove any absolute prefix if any
            int pos = uri.indexOf("://");
            if (pos != -1) {
                uri = uri.substring(pos + 3);
                pos = uri.indexOf("/");
                if (pos != -1) {
                    uri = uri.substring(pos + 1);
                }
            }
        }
        msgContext.setProperty(NhttpConstants.REST_URL_POSTFIX, uri);
        String servicePrefix = oriUri.substring(0, oriUri.indexOf(uri));
        if (servicePrefix.indexOf("://") == -1) {
            HttpInetConnection inetConn = (HttpInetConnection) conn;
            InetAddress localAddr = inetConn.getLocalAddress();
            if (localAddr != null) {
                servicePrefix = (isHttps ? "https://" : "http://") +
                        localAddr.getHostName() + ":" + inetConn.getLocalPort() + servicePrefix;
            }
        }
        msgContext.setProperty(NhttpConstants.SERVICE_PREFIX, servicePrefix);

        if ("GET".equals(method)) {
            processGet();
        } else if ("POST".equals(method)) {
            processEntityEnclosingMethod();
        } else if ("PUT".equals(method)) {
            processEntityEnclosingMethod();
        } else if ("HEAD".equals(method)) {
            processNonEntityEnclosingMethod();
        } else if ("OPTIONS".equals(method)) {
            processNonEntityEnclosingMethod();
        } else if ("DELETE".equals(method)) {
            processGetAndDelete("DELETE");
        } else if ("TRACE".equals(method)) {
            processNonEntityEnclosingMethod();
        } else {
            handleException("Unsupported method : " + method, null);
        }

        // here the RequestResponseTransport plays an important role when it comes to
        // dual channel invocation. This is becasue we need to ACK to the request once the request
        // is received to synapse. Otherwise we will not be able to support the single channel
        // invocation within the actual service and synapse for a dual channel request from the
        // client.
        if (isAckRequired()) {
            String respWritten = "";
            if (msgContext.getOperationContext() != null) {
                respWritten = (String) msgContext.getOperationContext().getProperty(
                        Constants.RESPONSE_WRITTEN);
            }
            boolean respWillFollow = !Constants.VALUE_TRUE.equals(respWritten)
                    && !"SKIP".equals(respWritten);
            boolean acked = (((RequestResponseTransport) msgContext.getProperty(
                    RequestResponseTransport.TRANSPORT_CONTROL)).getStatus()
                    == RequestResponseTransport.RequestResponseTransportStatus.ACKED);
            boolean forced = msgContext.isPropertyTrue(NhttpConstants.FORCE_SC_ACCEPTED);
            boolean nioAck = msgContext.isPropertyTrue("NIO-ACK-Requested", false);

            if (respWillFollow || acked || forced || nioAck) {

                if (!nioAck) {
                    if (log.isDebugEnabled()) {
                        log.debug("Sending 202 Accepted response for MessageID : " +
                                msgContext.getMessageID() +
                                " response written : " + respWritten +
                                " response will follow : " + respWillFollow +
                                " acked : " + acked + " forced ack : " + forced);
                    }
                    response.setStatusCode(HttpStatus.SC_ACCEPTED);
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Sending ACK response with status "
                                + msgContext.getProperty(NhttpConstants.HTTP_SC)
                                + ", for MessageID : " + msgContext.getMessageID());
                    }
                    response.setStatusCode(Integer.parseInt(
                            msgContext.getProperty(NhttpConstants.HTTP_SC).toString()));
                    Map<String, String> responseHeaders = (Map<String, String>)
                            msgContext.getProperty(MessageContext.TRANSPORT_HEADERS);
                    if (responseHeaders != null) {
                        for (String headerName : responseHeaders.keySet()) {
                            response.addHeader(headerName, responseHeaders.get(headerName));
                        }
                    }
                }

                if (metrics != null) {
                    metrics.incrementMessagesSent();
                }

                try {
                    serverHandler.commitResponse(conn, response);

                } catch (HttpException e) {
                    if (metrics != null) {
                        metrics.incrementFaultsSending();
                    }
                    handleException("Unexpected HTTP protocol error : " + e.getMessage(), e);
                } catch (ConnectionClosedException e) {
                    if (metrics != null) {
                        metrics.incrementFaultsSending();
                    }
                    log.warn("Connection closed by client (Connection closed)");
                } catch (IllegalStateException e) {
                    if (metrics != null) {
                        metrics.incrementFaultsSending();
                    }
                    log.warn("Connection closed by client (Buffer closed)");
                } catch (IOException e) {
                    if (metrics != null) {
                        metrics.incrementFaultsSending();
                    }
                    handleException("IO Error sending response message", e);
                } catch (Exception e) {
                    if (metrics != null) {
                        metrics.incrementFaultsSending();
                    }
                    handleException("General Error sending response message", e);
                }

                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException ignore) {}
                }

                // make sure that the output stream is flushed and closed properly
                try {
                    os.flush();
                    os.close();
                } catch (IOException ignore) {}
            }
        }
    }

    private boolean isAckRequired() {

        // This condition is a bit complex but cannot simplify any further.
        if (msgContext != null) {
            if (msgContext.getOperationContext() != null &&
                    (!msgContext.getOperationContext().getAxisOperation().isControlOperation() ||
                            msgContext.isPropertyTrue(NhttpConstants.FORCE_SC_ACCEPTED))) {

                return true;
            } else if (msgContext.isPropertyTrue("NIO-ACK-Requested", false)) {
                return true;
            }
        }

        return false;
    }

    /**
     *
     */
    private void processEntityEnclosingMethod() {

        try {
            Header contentType = request.getFirstHeader(HTTP.CONTENT_TYPE);
            String contentTypeStr = contentType != null ? contentType.getValue() : null;

            String charSetEncoding = BuilderUtil.getCharSetEncoding(contentTypeStr);
            msgContext.setProperty(
                    Constants.Configuration.CHARACTER_SET_ENCODING, charSetEncoding);

            Header soapAction  = request.getFirstHeader(SOAPACTION);

            HTTPTransportUtils.processHTTPPostRequest(
                msgContext, is,
                os,
                contentTypeStr,
                (soapAction != null  ? soapAction.getValue()  : null),
                request.getRequestLine().getUri());
        } catch (AxisFault e) {
            handleException("Error processing POST request ", e);
        }
    }

    /**
     * Process HEAD, DELETE, TRACE, OPTIONS
     */
    private void processNonEntityEnclosingMethod() {

        try {
            RESTUtil.processURLRequest(
                msgContext, os, null,
                request.getRequestLine().getUri());

        } catch (AxisFault e) {
            handleException("Error processing " + request.getRequestLine().getMethod() +
                " request for : " + request.getRequestLine().getUri(), e);
        }
    }

    /**
     *
     */
    private void processGet() {

        String uri = request.getRequestLine().getUri();

        String servicePath = cfgCtx.getServiceContextPath();
        if (!servicePath.startsWith("/")) {
            servicePath = "/" + servicePath;
        }

        String serviceName = null;
        if (uri.startsWith(servicePath)) {
            serviceName = uri.substring(servicePath.length());
            if (serviceName.startsWith("/")) {
                serviceName = serviceName.substring(1);
            }
            if (serviceName.indexOf("?") != -1) {
                serviceName = serviceName.substring(0, serviceName.indexOf("?"));
            }
        }

        if (serviceName != null) {
            int opnStart = serviceName.indexOf("/");
            if (opnStart != -1) {
                serviceName = serviceName.substring(0, opnStart);
            }
        }

        Map<String, String> parameters = new HashMap<String, String>();
        int pos = uri.indexOf("?");
        if (pos != -1) {
            msgContext.setTo(new EndpointReference(uri.substring(0, pos)));
            StringTokenizer st = new StringTokenizer(uri.substring(pos+1), "&");
            while (st.hasMoreTokens()) {
                String param = st.nextToken();
                pos = param.indexOf("=");
                if (pos != -1) {
                    parameters.put(param.substring(0, pos), param.substring(pos+1));
                } else {
                    parameters.put(param, null);
                }
            }
        } else {
            msgContext.setTo(new EndpointReference(uri));
        }

        if (uri.equals("/favicon.ico")) {
            response.setStatusCode(HttpStatus.SC_MOVED_PERMANENTLY);
            response.addHeader(LOCATION, "http://ws.apache.org/favicon.ico");
            serverHandler.commitResponseHideExceptions(conn,  response);

//        } else if (!uri.startsWith(servicePath)) {
//            response.setStatusCode(HttpStatus.SC_MOVED_PERMANENTLY);
//            response.addHeader(LOCATION, servicePath + "/");
//            serverHandler.commitResponseHideExceptions(conn, response);

        } else if (serviceName != null && parameters.containsKey("wsdl")) {
            AxisService service = cfgCtx.getAxisConfiguration().
                getServices().get(serviceName);
            if (service != null) {
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    String parameterValue = parameters.get("wsdl");
                    if (parameterValue == null) {
                        service.printWSDL(baos, getIpAddress());
                    } else {
                        // here the parameter value should be the wsdl file name
                        service.printUserWSDL(baos, parameterValue);
                    }
                    response.addHeader(CONTENT_TYPE, TEXT_XML);
                    serverHandler.commitResponseHideExceptions(conn, response);
                    os.write(baos.toByteArray());

                } catch (Exception e) {
                    handleBrowserException(
                        "Error generating ?wsdl output for service : " + serviceName, e);
                    return;
                }
            }

        } else if (serviceName != null && parameters.containsKey("wsdl2")) {
            AxisService service = cfgCtx.getAxisConfiguration().
                getServices().get(serviceName);
            if (service != null) {
                String parameterValue = (String) service.getParameterValue("serviceType");
                if ("proxy".equals(parameterValue) && !isWSDLProvidedForProxyService(service)) {
                    handleBrowserException("No WSDL was provided for the Service " + serviceName +
                            ". A WSDL cannot be generated.", null);
                    return;
                }
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    service.printWSDL2(baos, getIpAddress());
                    response.addHeader(CONTENT_TYPE, TEXT_XML);
                    serverHandler.commitResponseHideExceptions(conn, response);
                    os.write(baos.toByteArray());

                } catch (Exception e) {
                    handleBrowserException(
                        "Error generating ?wsdl2 output for service : " + serviceName, e);
                    return;
                }
            }

        } else if (serviceName != null && parameters.containsKey("xsd")) {
            if (parameters.get("xsd") == null || "".equals(parameters.get("xsd"))) {
                AxisService service = cfgCtx.getAxisConfiguration()
                    .getServices().get(serviceName);
                if (service != null) {
                    try {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        service.printSchema(baos);
                        response.addHeader(CONTENT_TYPE, TEXT_XML);
                        serverHandler.commitResponseHideExceptions(conn, response);
                        os.write(baos.toByteArray());

                    } catch (Exception e) {
                        handleBrowserException(
                            "Error generating ?xsd output for service : " + serviceName, e);
                        return;
                    }
                }

            } else {
                //cater for named xsds - check for the xsd name
                String schemaName = parameters.get("xsd");
                AxisService service = cfgCtx.getAxisConfiguration()
                    .getServices().get(serviceName);

                if (service != null) {
                    //run the population logic just to be sure
                    service.populateSchemaMappings();
                    //write out the correct schema
                    Map schemaTable = service.getSchemaMappingTable();
                    XmlSchema schema = (XmlSchema)schemaTable.get(schemaName);
                    if (schema == null) {
                        int dotIndex = schemaName.indexOf('.');
                        if (dotIndex > 0) {
                            String schemaKey = schemaName.substring(0,dotIndex);
                            schema = (XmlSchema) schemaTable.get(schemaKey);
                        }
                    }
                    //schema found - write it to the stream
                    if (schema != null) {
                        try {
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            schema.write(baos);
                            response.addHeader(CONTENT_TYPE, TEXT_XML);
                            serverHandler.commitResponseHideExceptions(conn, response);
                            os.write(baos.toByteArray());
                        } catch (Exception e) {
                            handleBrowserException(
                                "Error generating named ?xsd output for service : " + serviceName, e);
                            return;
                        }

                    } else {
                        // no schema available by that name  - send 404
                        response.setStatusCode(HttpStatus.SC_NOT_FOUND);
                    }
                }
            }

        }
        else if (serviceName != null && parameters.containsKey("info")) {
            AxisService service = cfgCtx.getAxisConfiguration().
                    getServices().get(serviceName);
            if (service != null) {
                String parameterValue = (String) service.getParameterValue("serviceType");
                if ("proxy".equals(parameterValue) && !isWSDLProvidedForProxyService(service)) {
                    handleBrowserException("No WSDL was provided for the Service " + serviceName +
                            ". A WSDL cannot be generated.", null);
                    return;
                }
                try {
                    byte[] bytes =
                        HTTPTransportReceiver.printServiceHTML(serviceName, cfgCtx).getBytes();
                    response.addHeader(CONTENT_TYPE, TEXT_HTML);
                    serverHandler.commitResponseHideExceptions(conn, response);
                    os.write(bytes);

                } catch (IOException e) {
                    handleBrowserException(
                        "Error generating service details page for : " + serviceName, e);
                    return;
                }
            } else {
                handleBrowserException("Invalid service : " + serviceName, null);
                return;
            }
        } else if (uri.startsWith(servicePath) &&
                (serviceName == null || serviceName.length() == 0)) {

            try {
                byte[] bytes = getServicesHTML().getBytes();
                response.addHeader(CONTENT_TYPE, TEXT_HTML);
                serverHandler.commitResponseHideExceptions(conn, response);
                os.write(bytes);

            } catch (IOException e) {
                handleBrowserException("Error generating services list", e);
            }

        } else {
            processGetAndDelete("GET");
            return;
        }

        // make sure that the output stream is flushed and closed properly
        try {
            os.flush();
            os.close();
        } catch (IOException ignore) {}
    }

    /**
     * Calls the RESTUtil to process GET and DELETE Request
     *
     * @param method HTTP method, either GET or DELETE
     */
    private void processGetAndDelete(String method) {
        try {
            RESTUtil.processGETRequest(
                    msgContext, os, request.getRequestLine().getUri(),
                    request.getFirstHeader(HTTP.CONTENT_TYPE));
            // do not let the output stream close (as by default below) since
            // we are serving this GET/DELETE request through the Synapse engine
        } catch (AxisFault axisFault) {
            handleException("Error processing " + method + " request for: " +
                    request.getRequestLine().getUri(), axisFault);
        }

    }

    private void handleBrowserException(String msg, Exception e) {

        if (e == null) {
            log.error(msg);
        } else {
            log.error(msg, e);
        }

        if (!response.containsHeader(HTTP.TRANSFER_ENCODING)) {
            response.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            response.setReasonPhrase(msg);
            response.addHeader(CONTENT_TYPE, TEXT_HTML);
            serverHandler.commitResponseHideExceptions(conn, response);
            try {
                os.write(msg.getBytes());
                os.close();
            } catch (IOException ignore) {}
        }
        
        if (conn != null) {
            try {
                conn.shutdown();
            } catch (IOException ignore) {}
        }
    }

    private void handleException(String msg, Exception e) {
        
        if (e == null) {
            log.error(msg);
        } else {
            log.error(msg, e);
        }

        if (e == null) {
            e = new Exception(msg);
        }

        try {
            MessageContext faultContext = MessageContextBuilder.createFaultMessageContext(
                    msgContext, e);
            AxisEngine.sendFault(faultContext);

        } catch (Exception ex) {
            response.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            response.addHeader(CONTENT_TYPE, TEXT_XML);
            serverHandler.commitResponseHideExceptions(conn, response);

            try {
                os.write(msg.getBytes());
                if (ex != null) {
                    os.write(ex.getMessage().getBytes());
                }
            } catch (IOException ignore) {}

            if (conn != null) {
                try {
                    conn.shutdown();
                } catch (IOException ignore) {}
            }
        }
    }

    private boolean isWSDLProvidedForProxyService(AxisService service) {
        boolean isWSDLProvided = false;
        if (service.getParameterValue(WSDLConstants.WSDL_4_J_DEFINITION) != null ||
                service.getParameterValue(WSDLConstants.WSDL_20_DESCRIPTION) != null) {
            isWSDLProvided = true;
        }
        return isWSDLProvided;
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

    public String getRemoteAddress() {
        return remoteAddress;
    }

    /**
     * Whatever this method returns as the IP is ignored by the actual http/s listener when
     * its getServiceEPR is invoked. This was originally copied from axis2
     *
     * @return Returns String.
     * @throws java.net.SocketException if the socket can not be accessed
     */
    private static String getIpAddress() throws SocketException {
        Enumeration e = NetworkInterface.getNetworkInterfaces();
        String address = "127.0.0.1";

        while (e.hasMoreElements()) {
            NetworkInterface netface = (NetworkInterface) e.nextElement();
            Enumeration addresses = netface.getInetAddresses();

            while (addresses.hasMoreElements()) {
                InetAddress ip = (InetAddress) addresses.nextElement();
                if (!ip.isLoopbackAddress() && isIP(ip.getHostAddress())) {
                    return ip.getHostAddress();
                }
            }
        }
        return address;
    }

    private static boolean isIP(String hostAddress) {
        return hostAddress.split("[.]").length == 4;
    }

    /**
     * Returns the HTML text for the list of services deployed.
     * This can be delegated to another Class as well
     * where it will handle more options of GET messages.
     *
     * @return the HTML to be displayed as a String
     */
    public String getServicesHTML() {

        Map services = cfgCtx.getAxisConfiguration().getServices();
        Hashtable erroneousServices = cfgCtx.getAxisConfiguration().getFaultyServices();
        boolean servicesFound = false;

        StringBuffer resultBuf = new StringBuffer();
        resultBuf.append("<html><head><title>Axis2: Services</title></head>" + "<body>");

        if ((services != null) && !services.isEmpty()) {

            servicesFound = true;
            resultBuf.append("<h2>" + "Deployed services" + "</h2>");

            for (Object service : services.values()) {

                AxisService axisService = (AxisService) service;
                if (axisService.getName().startsWith("__") || JavaUtils.isTrueExplicitly(
                        axisService.getParameter(NhttpConstants.HIDDEN_SERVICE_PARAM_NAME))) {
                    continue;    // skip private services
                }

                Iterator iterator = axisService.getOperations();
                resultBuf.append("<h3><a href=\"").append(axisService.getName()).append(
                        "?wsdl\">").append(axisService.getName()).append("</a></h3>");

                if (iterator.hasNext()) {
                    resultBuf.append("Available operations <ul>");

                    for (; iterator.hasNext();) {
                        AxisOperation axisOperation = (AxisOperation) iterator.next();
                        resultBuf.append("<li>").append(
                                axisOperation.getName().getLocalPart()).append("</li>");
                    }
                    resultBuf.append("</ul>");
                } else {
                    resultBuf.append("No operations specified for this service");
                }
            }
        }

        if ((erroneousServices != null) && !erroneousServices.isEmpty()) {
            servicesFound = true;
            resultBuf.append("<hr><h2><font color=\"blue\">Faulty Services</font></h2>");
            Enumeration faultyservices = erroneousServices.keys();

            while (faultyservices.hasMoreElements()) {
                String faultyserviceName = (String) faultyservices.nextElement();
                resultBuf.append("<h3><font color=\"blue\">").append(
                        faultyserviceName).append("</font></h3>");
            }
        }

        if (!servicesFound) {
            resultBuf.append("<h2>There are no services deployed</h2>");
        }

        resultBuf.append("</body></html>");
        return resultBuf.toString();
    }
}
