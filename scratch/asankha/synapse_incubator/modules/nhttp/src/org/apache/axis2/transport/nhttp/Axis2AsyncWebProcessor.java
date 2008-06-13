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
package org.apache.axis2.transport.nhttp;

import org.safehaus.asyncweb.http.*;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.context.SessionContext;
import org.apache.axis2.Constants;
import org.apache.axis2.AxisFault;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.transport.http.HTTPTransportUtils;
import org.apache.axis2.transport.http.HTTPTransportReceiver;
import org.apache.axis2.transport.http.server.SessionManager;
import org.apache.axis2.util.UUIDGenerator;
import org.apache.axis2.util.threadpool.DefaultThreadFactory;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.description.AxisService;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.namespace.QName;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.net.SocketException;
import java.net.NetworkInterface;
import java.net.InetAddress;

import edu.emory.mathcs.backport.java.util.concurrent.*;

public class Axis2AsyncWebProcessor implements HttpService {

    private static final Log log = LogFactory.getLog(Axis2AsyncWebProcessor.class);

    private static final String TEXT_PLAIN = "text/plain";
    private static final String CONTENT_TYPE = "ContentType";

    private static final int WORKERS_MAX_THREADS = 40;
    private static final long WORKER_KEEP_ALIVE = 100L;
    private static final TimeUnit TIME_UNIT = TimeUnit.SECONDS;
    private Executor workerPool = null;
    private SessionManager sessionManager = new SessionManager();
    private int port = 8080;

    private ConfigurationContext configurationContext = null;

    Axis2AsyncWebProcessor(int port) {
        this.port = port;
        // create thread pool of workers
        workerPool = new ThreadPoolExecutor(
            1,
            WORKERS_MAX_THREADS, WORKER_KEEP_ALIVE, TIME_UNIT,
            new LinkedBlockingQueue(),
            new DefaultThreadFactory(
                    new ThreadGroup("HTTP Worker thread group"),
                    "HTTPWorker"));
    }

    public void setConfigurationContext(ConfigurationContext configurationContext) {
        this.configurationContext = configurationContext;
    }

    public void handleRequest(HttpRequest request) {

        log.debug("@@@@ Got new Async HTTP request for: " +
            request.getRequestURI() + " on port : " + port);

        MessageContext msgContext = new MessageContext();
        msgContext.setIncomingTransportName(Constants.TRANSPORT_HTTP);
        try {
            TransportOutDescription transportOut = configurationContext.getAxisConfiguration()
                .getTransportOut(new QName(Constants.TRANSPORT_HTTP));
            TransportInDescription transportIn = configurationContext.getAxisConfiguration()
                .getTransportIn(new QName(Constants.TRANSPORT_HTTP));

            msgContext.setConfigurationContext(configurationContext);

            String sessionKey = request.getSession(true).getId();
            if (configurationContext.getAxisConfiguration().isManageTransportSession()) {
                SessionContext sessionContext = sessionManager.getSessionContext(sessionKey);
                msgContext.setSessionContext(sessionContext);
            }

            msgContext.setTransportIn(transportIn);
            msgContext.setTransportOut(transportOut);
            msgContext.setServiceGroupContextId(UUIDGenerator.getUUID());
            msgContext.setServerSide(true);
            msgContext.setProperty(Constants.Configuration.TRANSPORT_IN_URL, request.getRequestURI());

            // set the transport Headers
            Map headerMap = new HashMap();
            for (Iterator it = request.getHeaderNames(); it.hasNext(); ) {
                String headerName = (String) it.next();
                headerMap.put(headerName, request.getHeader(headerName));
            }
            msgContext.setProperty(MessageContext.TRANSPORT_HEADERS, headerMap);
            msgContext.setProperty(Constants.OUT_TRANSPORT_INFO, request);

            workerPool.execute(new Worker(msgContext, request));

        } catch (AxisFault e) {
            HttpResponse response = request.createHttpResponse();

            try {
                AxisEngine engine = new AxisEngine(configurationContext);
                msgContext.setProperty(MessageContext.TRANSPORT_OUT, response.getOutputStream());
                msgContext.setProperty(Constants.OUT_TRANSPORT_INFO, response.getOutputStream());

                MessageContext faultContext = engine.createFaultMessageContext(msgContext, e);
                engine.sendFault(faultContext);

                response.setStatus(ResponseStatus.INTERNAL_SERVER_ERROR);

            } catch (Exception ex) {
                response.setStatus(ResponseStatus.INTERNAL_SERVER_ERROR);
                response.setHeader(CONTENT_TYPE, TEXT_PLAIN);
                OutputStreamWriter out = new OutputStreamWriter(
                    response.getOutputStream());
                try {
                    out.write(ex.getMessage());
                    out.close();
                } catch (IOException ee) {}
            }
            request.commitResponse(response);
            return;
        }
    }

    public void start() {
    }

    public void stop() {
    }

    class Worker implements Runnable {

        private MessageContext msgContext = null;
        private HttpRequest request = null;
        private String contextPath = null;
        private String servicePath = null;
        private static final String SOAPACTION = "SOAPAction";
        private static final String TEXT_HTML = "text/html";
        private static final String CONTENT_TYPE = Axis2AsyncWebProcessor.CONTENT_TYPE;
        private static final String TRANSFER_ENCODING = "Transfer-Encoding";
        private static final String CHUNKED = "chunked";
        private static final String LOCATION = "Location";

        Worker(MessageContext msgContext, HttpRequest request) {
            this.msgContext = msgContext;
            this.request = request;
            contextPath = configurationContext.getContextRoot() + "/";
            servicePath = configurationContext.getServiceContextPath();
        }

        public void run() {

            // TODO handle chunking and correct http versions
            HttpResponse response = request.createHttpResponse();

            if (HttpMethod.GET.equals(request.getMethod())) {
                processGet(response);

            } else if (HttpMethod.POST.equals(request.getMethod())) {
                processPost(response);

                /*// Finalize response
                OperationContext operationContext = msgContext.getOperationContext();
                Object contextWritten = null;
                if (operationContext != null) {
                    contextWritten = operationContext.getProperty(Constants.RESPONSE_WRITTEN);
                }

                if (!(contextWritten != null && "SKIP".equals(contextWritten))) {

                    if ((contextWritten != null) && Constants.VALUE_TRUE.equals(contextWritten)) {
                        response.setStatus(ResponseStatus.OK);
                    } else {
                        response.setStatus(ResponseStatus.ACCEPTED);
                    }
                    request.commitResponse(response);
                }*/

            } else {
                handleException("Unsupported method : " + request.getMethod(), null, response);
            }
        }

        private void processGet(HttpResponse response) {

            String uri = request.getRequestURI();
            String serviceName = uri.substring(uri.lastIndexOf("/") + 1);

            Map parameters = new HashMap();
            Iterator iter = request.getParameterNames();
            while (iter.hasNext()) {
                String name = (String) iter.next();
                parameters.put(name, request.getParameter(name));
            }

            if (uri.equals("/favicon.ico")) {
                response.setStatus(ResponseStatus.MOVED_PERMANENTLY);
                response.addHeader(LOCATION, "http://ws.apache.org/favicon.ico");

            } else if (!uri.startsWith(contextPath)) {
                response.setStatus(ResponseStatus.MOVED_PERMANENTLY);
                response.addHeader(LOCATION, contextPath);

            } else if (parameters.containsKey("wsdl")) {
                AxisService service = (AxisService) configurationContext.getAxisConfiguration().
                    getServices().get(serviceName);
                if (service != null) {
                    try {
                        service.printWSDL(response.getOutputStream(),
                            getIpAddress(), servicePath);
                        response.setHeader(CONTENT_TYPE, TEXT_HTML);
                        response.setStatus(ResponseStatus.OK);

                    } catch (AxisFault e) {
                        handleException("Axis2 fault writing ?wsdl output", e, response);
                        return;
                    } catch (SocketException e) {
                        handleException("Error getting ip address for ?wsdl output", e, response);
                        return;
                    }
                }

            } else if (parameters.containsKey("wsdl2")) {
                AxisService service = (AxisService) configurationContext.getAxisConfiguration().
                    getServices().get(serviceName);
                if (service != null) {
                    try {
                        service.printWSDL2(response.getOutputStream(),
                            getIpAddress(), servicePath);
                        response.setHeader(CONTENT_TYPE, TEXT_HTML);
                        response.setStatus(ResponseStatus.OK);

                    } catch (AxisFault e) {
                        handleException("Axis2 fault writing ?wsdl2 output", e, response);
                        return;
                    } catch (SocketException e) {
                        handleException("Error getting ip address for ?wsdl2 output", e, response);
                        return;
                    }
                }

            } else if (parameters.containsKey("xsd")) {
                if (parameters.get("xsd") == null || "".equals(parameters.get("xsd"))) {
                    AxisService service = (AxisService) configurationContext.getAxisConfiguration()
                        .getServices().get(serviceName);
                    if (service != null) {
                        try {
                            service.printSchema(response.getOutputStream());
                            response.setHeader(CONTENT_TYPE, TEXT_HTML);
                            response.setStatus(ResponseStatus.OK);

                        } catch (AxisFault axisFault) {
                            handleException("Error writing ?xsd output to client", axisFault, response);
                            return;
                        }
                    }

                } else {
                    //cater for named xsds - check for the xsd name
                    String schemaName = (String) parameters.get("xsd");
                    AxisService service = (AxisService) configurationContext.getAxisConfiguration()
                        .getServices().get(serviceName);

                    if (service != null) {
                        //run the population logic just to be sure
                        service.populateSchemaMappings();
                        //write out the correct schema
                        Map schemaTable = service.getSchemaMappingTable();
                        final XmlSchema schema = (XmlSchema)schemaTable.get(schemaName);
                        //schema found - write it to the stream
                        if (schema != null) {
                            schema.write(response.getOutputStream());
                            response.setHeader(CONTENT_TYPE, TEXT_HTML);
                            response.setStatus(ResponseStatus.OK);
                        } else {
                            // no schema available by that name  - send 404
                            response.setStatus(ResponseStatus.NOT_FOUND, "Schema Not Found");
                        }
                    }
                }

            } else if (parameters.isEmpty()) {

                // request is for a service over GET without params, send service HTML
                if (!(uri.endsWith(contextPath) || uri.endsWith(contextPath+"/"))) {

                    OutputStreamWriter out = new OutputStreamWriter(
                        response.getOutputStream());
                    try {
                        out.write(
                            HTTPTransportReceiver.printServiceHTML(
                                serviceName, configurationContext));
                        out.close();
                        response.setHeader(CONTENT_TYPE, TEXT_HTML);
                        response.setStatus(ResponseStatus.OK);

                    } catch (IOException e) {
                        handleException("Error writing service HTML to client", e, response);
                        return;
                    }
                } else {
                    processAxisGet(response, parameters);
                }
            }

            request.commitResponse(response);
        }

        public void processPost(HttpResponse response) {

            try {
                HTTPTransportUtils.processHTTPPostRequest(
                    msgContext,
                    request.getInputStream(),
                    response.getOutputStream(),
                    request.getHeader(CONTENT_TYPE),
                    request.getHeader(SOAPACTION),
                    request.getRequestURI());
            } catch (AxisFault e) {
                handleException("Error processing POST request ", e, response);
            }
        }

        private void processAxisGet(HttpResponse response, Map parameters) {
            try {
                // deal with GET request
                boolean processed = HTTPTransportUtils.processHTTPGetRequest(
                    msgContext,
                    response.getOutputStream(),
                    request.getHeader(SOAPACTION),
                    request.getRequestURI(),
                    configurationContext,
                    parameters);

                if (!processed) {
                    OutputStreamWriter out = new OutputStreamWriter(
                        response.getOutputStream());
                    try {
                        out.write(HTTPTransportReceiver.getServicesHTML(configurationContext));
                        out.flush();
                        response.setHeader(CONTENT_TYPE, TEXT_HTML);
                        response.setStatus(ResponseStatus.OK);

                    } catch (IOException e) {
                        handleException("Error writing ? output to client", e, response);
                    }
                }
            } catch (AxisFault e) {
                handleException("Axis fault while serving GET request", e, response);
            }
        }

        private void handleException(String msg, Exception e, HttpResponse response) {
            log.error(msg, e);

            try {
                AxisEngine engine = new AxisEngine(configurationContext);
                msgContext.setProperty(MessageContext.TRANSPORT_OUT, response.getOutputStream());
                msgContext.setProperty(Constants.OUT_TRANSPORT_INFO, response.getOutputStream());
                MessageContext faultContext = engine.createFaultMessageContext(msgContext, e);
                engine.sendFault(faultContext);

            } catch (Exception ex) {
                response.setHeader(CONTENT_TYPE, TEXT_PLAIN);
                OutputStreamWriter out = new OutputStreamWriter(
                    response.getOutputStream());
                try {
                    out.write(ex.getMessage());
                    out.close();
                } catch (IOException ee) {}

            } finally {
                response.setStatus(ResponseStatus.INTERNAL_SERVER_ERROR);
                request.commitResponse(response);
            }
        }
    }

    /**
     * Copied from transport.http of Axis2
     *
     * Returns the ip address to be used for the replyto epr
     * CAUTION:
     * This will go through all the available network interfaces and will try to return an ip address.
     * First this will try to get the first IP which is not loopback address (127.0.0.1). If none is found
     * then this will return this will return 127.0.0.1.
     * This will <b>not<b> consider IPv6 addresses.
     * <p/>
     * TODO:
     * - Improve this logic to genaralize it a bit more
     * - Obtain the ip to be used here from the Call API
     *
     * @return Returns String.
     * @throws SocketException
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
}
