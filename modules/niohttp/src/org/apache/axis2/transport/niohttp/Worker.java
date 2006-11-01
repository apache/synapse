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
package org.apache.axis2.transport.niohttp;

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.transport.niohttp.impl.*;
import org.apache.axis2.transport.http.HTTPTransportReceiver;
import org.apache.axis2.transport.http.HTTPTransportUtils;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.AxisFault;
import org.apache.axis2.engine.AxisEngine;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.net.SocketException;
import java.io.OutputStreamWriter;
import java.io.IOException;

public class Worker implements Runnable {

    private static final Log log = LogFactory.getLog(Worker.class);

    private MessageContext msgContext = null;
    private ConfigurationContext cfgCtx = null;
    private HttpRequest request = null;
    private String contextPath = null;
    private String servicePath = null;
    private static final String SOAPACTION = "SOAPAction";

    Worker(ConfigurationContext cfgCtx, MessageContext msgContext, HttpRequest request) {
        this.cfgCtx = cfgCtx;
        this.msgContext = msgContext;
        this.request = request;
        contextPath = "/" + cfgCtx.getContextRoot() + "/";
        servicePath = cfgCtx.getServiceContextPath();
    }

    public void run() {

        HttpResponse response = request.createResponse();

        if (Constants.GET.equals(request.getMethod())) {
            processGet(response);

        } else if (Constants.POST.equals(request.getMethod())) {
            response.addHeader(Constants.TRANSFER_ENCODING, Constants.CHUNKED);
            response.addHeader(Constants.CONTENT_TYPE, "text/xml; charset=utf-8");
            processPost(response);
            response.commit();

        } else {
            handleException("Unsupported method : " + request.getMethod(), null, response);
        }

        //response.commit();
    }

    private void processGet(HttpResponse response) {

        String uri = request.getPath();
        String serviceName = uri.substring(uri.lastIndexOf("/") + 1);
        int qPos = serviceName.indexOf("?");
        if (qPos != -1) {
            serviceName = serviceName.substring(0, qPos);
        }

        Map parameters = new HashMap();
        Iterator iter = request.getParameterNames();
        while (iter.hasNext()) {
            String name = (String) iter.next();
            parameters.put(name, request.getParameter(name));
        }

        if (uri.equals("/favicon.ico")) {
            response.setStatus(ResponseStatus.MOVED_PERMANENTLY, "Redirect");
            response.addHeader(Constants.LOCATION, "http://ws.apache.org/favicon.ico");

        } else if (!uri.startsWith(contextPath)) {
            response.setStatus(ResponseStatus.MOVED_PERMANENTLY, "Redirect");
            response.addHeader(Constants.LOCATION, contextPath);

        } else if (parameters.containsKey("wsdl")) {
            AxisService service = (AxisService) cfgCtx.getAxisConfiguration().
                getServices().get(serviceName);
            if (service != null) {
                try {
                    service.printWSDL(response.getOutputStream(),
                        Util.getIpAddress(), servicePath);
                    response.addHeader(Constants.CONTENT_TYPE, Constants.TEXT_HTML);
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
            AxisService service = (AxisService) cfgCtx.getAxisConfiguration().
                getServices().get(serviceName);
            if (service != null) {
                try {
                    service.printWSDL2(response.getOutputStream(),
                        Util.getIpAddress(), servicePath);
                    response.addHeader(Constants.CONTENT_TYPE, Constants.TEXT_HTML);
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
                AxisService service = (AxisService) cfgCtx.getAxisConfiguration()
                    .getServices().get(serviceName);
                if (service != null) {
                    try {
                        service.printSchema(response.getOutputStream());
                        response.addHeader(Constants.CONTENT_TYPE, Constants.TEXT_HTML);
                        response.setStatus(ResponseStatus.OK);

                    } catch (AxisFault axisFault) {
                        handleException("Error writing ?xsd output to client", axisFault, response);
                        return;
                    }
                }

            } else {
                //cater for named xsds - check for the xsd name
                String schemaName = (String) parameters.get("xsd");
                AxisService service = (AxisService) cfgCtx.getAxisConfiguration()
                    .getServices().get(serviceName);

                if (service != null) {
                    //run the population logic just to be sure
                    service.populateSchemaMappings();
                    //write out the correct schema
                    Map schemaTable = service.getSchemaMappingTable();
                    final XmlSchema schema = (XmlSchema) schemaTable.get(schemaName);
                    //schema found - write it to the stream
                    if (schema != null) {
                        schema.write(response.getOutputStream());
                        response.addHeader(Constants.CONTENT_TYPE, Constants.TEXT_HTML);
                        response.setStatus(ResponseStatus.OK);
                    } else {
                        // no schema available by that name  - send 404
                        response.setStatus(ResponseStatus.NOT_FOUND, "Schema Not Found");
                    }
                }
            }

        } else if (parameters.isEmpty()) {

            // request is for a service over GET without params, send service HTML
            if (!(uri.endsWith(contextPath) || uri.endsWith(contextPath + "/"))) {

                OutputStreamWriter out = new OutputStreamWriter(
                    response.getOutputStream());
                try {
                    out.write(
                        HTTPTransportReceiver.printServiceHTML(
                            serviceName, cfgCtx));
                    out.close();
                    response.addHeader(Constants.CONTENT_TYPE, Constants.TEXT_HTML);
                    response.setStatus(ResponseStatus.OK);

                } catch (IOException e) {
                    handleException("Error writing service HTML to client", e, response);
                    return;
                }
            } else {
                processAxisGet(response, parameters);
            }
        }

        response.commit();
    }

    public void processPost(HttpResponse response) {

        try {
            HTTPTransportUtils.processHTTPPostRequest(
                msgContext,
                request.getInputStream(),
                response.getOutputStream(),
                request.getHeader(Constants.CONTENT_TYPE),
                request.getHeader(SOAPACTION),
                request.getPath());
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
                request.getPath(),
                cfgCtx,
                parameters);

            if (!processed) {
                OutputStreamWriter out = new OutputStreamWriter(
                    response.getOutputStream());
                try {
                    out.write(HTTPTransportReceiver.getServicesHTML(cfgCtx));
                    out.close();
                    response.addHeader(Constants.CONTENT_TYPE, Constants.TEXT_HTML);
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
        }
    }
}
