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

package org.apache.synapse.transport.nhttp.util;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.impl.llom.soap11.SOAP11Factory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.WSDL20DefaultValueHolder;
import org.apache.axis2.description.WSDL2Constants;
import org.apache.axis2.dispatchers.RequestURIBasedDispatcher;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.transport.http.util.URIEncoderDecoder;
import org.apache.http.Header;
import org.apache.synapse.transport.nhttp.NHttpConfiguration;
import org.apache.synapse.transport.nhttp.NhttpConstants;

import javax.xml.namespace.QName;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;

/**
 * This class provides a set of utility methods to manage the REST invocation calls
 * going out from the nhttp transport in the HTTP GET method
 */
public class RESTUtil {

    /**
     * This method will return the URI part for the GET HTTPRequest by converting
     * the SOAP infoset to the URL-encoded GET format
     *
     * @param messageContext - from which the SOAP infoset will be extracted to encode
     * @param address        - address of the actual service
     * @return uri       - ERI of the GET request
     * @throws AxisFault - if the SOAP infoset cannot be converted in to the GET URL-encoded format
     */
    public static String getURI(MessageContext messageContext, String address) throws AxisFault {

        OMElement firstElement;
        address = address.substring(address.indexOf("//") + 2);
        address = address.substring(address.indexOf("/"));
        String queryParameterSeparator = (String) messageContext
                .getProperty(WSDL2Constants.ATTR_WHTTP_QUERY_PARAMETER_SEPARATOR);
        // In case queryParameterSeparator is null we better use the default value

        if (queryParameterSeparator == null) {
            queryParameterSeparator = WSDL20DefaultValueHolder
                    .getDefaultValue(WSDL2Constants.ATTR_WHTTP_QUERY_PARAMETER_SEPARATOR);
        }

        firstElement = messageContext.getEnvelope().getBody().getFirstElement();
        String params = "";
        if (firstElement != null) {
            // first element corresponds to the operation name
            address = address + "/" + firstElement.getLocalName();
        } else {
            firstElement = messageContext.getEnvelope().getBody();
        }

        Iterator iter = firstElement.getChildElements();

        String legalCharacters = WSDL2Constants
                .LEGAL_CHARACTERS_IN_QUERY.replaceAll(queryParameterSeparator, "");
        StringBuffer buff = new StringBuffer(params);

        // iterate through the child elements and find the request parameters
        while (iter.hasNext()) {
            OMElement element = (OMElement) iter.next();
            try {
                buff.append(URIEncoderDecoder.quoteIllegal(element.getLocalName(),
                        legalCharacters)).append("=").append(URIEncoderDecoder.quoteIllegal(element.getText(),
                        legalCharacters)).append(queryParameterSeparator);
            } catch (UnsupportedEncodingException e) {
                throw new AxisFault("URI Encoding error : " + element.getLocalName()
                        + "=" + element.getText(), e);
            }
        }

        params = buff.toString();
        if (params.trim().length() != 0) {
            int index = address.indexOf("?");
            if (index == -1) {
                address = address + "?" + params.substring(0, params.length() - 1);
            } else if (index == address.length() - 1) {
                address = address + params.substring(0, params.length() - 1);

            } else {
                address = address
                        + queryParameterSeparator + params.substring(0, params.length() - 1);
            }
        }

        return address;
    }

    /**
     * Processes the HTTP GET / DELETE request and builds the SOAP info-set of the REST message
     *
     * @param msgContext        The MessageContext of the Request Message
     * @param out               The output stream of the response
     * @param requestURI        The URL that the request came to
     * @param contentTypeHeader The contentType header of the request
     * @param httpMethod        The http method of the request
     * @throws AxisFault - Thrown in case a fault occurs
     */
    public static void processGetAndDeleteRequest(MessageContext msgContext, OutputStream out,
                                                  String requestURI, Header contentTypeHeader,
                                                  String httpMethod) throws AxisFault {

        String contentType = getContentType(contentTypeHeader);

        prepareMessageContext(msgContext, requestURI, httpMethod, out, contentType);

        msgContext.setProperty(NhttpConstants.NO_ENTITY_BODY, Boolean.TRUE);

        org.apache.axis2.transport.http.util.RESTUtil.processURLRequest(msgContext, out,
                contentType);
    }

    /**
     * Processes the HTTP GET request and builds the SOAP info-set of the REST message
     *
     * @param msgContext The MessageContext of the Request Message
     * @param out        The output stream of the response
     * @param soapAction SoapAction of the request
     * @param requestURI The URL that the request came to
     * @throws AxisFault - Thrown in case a fault occurs
     */
    public static void processURLRequest(MessageContext msgContext, OutputStream out,
                                            String soapAction, String requestURI) throws AxisFault {

        if ((soapAction != null) && soapAction.startsWith("\"") && soapAction.endsWith("\"")) {
            soapAction = soapAction.substring(1, soapAction.length() - 1);
        }

        msgContext.setSoapAction(soapAction);
        msgContext.setTo(new EndpointReference(requestURI));
        msgContext.setProperty(MessageContext.TRANSPORT_OUT, out);
        msgContext.setServerSide(true);
        msgContext.setDoingREST(true);
        msgContext.setEnvelope(new SOAP11Factory().getDefaultEnvelope());
        msgContext.setProperty(NhttpConstants.NO_ENTITY_BODY, Boolean.TRUE);
        AxisEngine.receive(msgContext);
    }

    /**
     * Processes the HTTP POST request and builds the SOAP info-set of the REST message
     *
     * @param msgContext        The MessageContext of the Request Message
     * @param is                The  input stream of the request
     * @param os                The output stream of the response
     * @param requestURI        The URL that the request came to
     * @param contentTypeHeader The contentType header of the request
     * @throws AxisFault - Thrown in case a fault occurs
     */
    public static void processPOSTRequest(MessageContext msgContext, InputStream is,
                                          OutputStream os, String requestURI,
                                          Header contentTypeHeader) throws AxisFault {

        String contentType = getContentType(contentTypeHeader);

        prepareMessageContext(msgContext, requestURI, HTTPConstants.HTTP_METHOD_POST, os, contentType);

        org.apache.axis2.transport.http.util.RESTUtil.processXMLRequest(msgContext, is, os,
                contentType);
    }

    /**
     * Given the contentType HTTP header it extracts the content-type of the request
     *
     * @param contentTypeHeader content type HTTP header
     * @return content type value
     */
    private static String getContentType(Header contentTypeHeader) {

        String contentTypeStr = contentTypeHeader != null ? contentTypeHeader.getValue() : null;
        if (contentTypeStr != null) {
            int index = contentTypeStr.indexOf(';');
            if (index > 0) {
                contentTypeStr = contentTypeStr.substring(0, index);
            }
        }
        return contentTypeStr;
    }

    /**
     * prepare message context prior to call axis2 RestUtils
     *
     * @param msgContext  The MessageContext of the Request Message
     * @param requestURI  The URL that the request came to
     * @param httpMethod  The http method of the request
     * @param out         The output stream of the response
     * @param contentType The content type of the request
     * @throws AxisFault Thrown in case a fault occurs
     */
    private static void prepareMessageContext(MessageContext msgContext,
                                              String requestURI,
                                              String httpMethod,
                                              OutputStream out,
                                              String contentType) throws AxisFault {

        msgContext.setTo(new EndpointReference(requestURI));
        msgContext.setProperty(HTTPConstants.HTTP_METHOD, httpMethod);
        msgContext.setServerSide(true);
        msgContext.setDoingREST(true);
        msgContext.setProperty(MessageContext.TRANSPORT_OUT, out);
        msgContext.setProperty(NhttpConstants.REST_REQUEST_CONTENT_TYPE, contentType);
        // workaround to get REST working in the case of
        //  1) Based on the request URI , it is possible to find a service name and operation.
        // However, there is no actual service deployed in the synapse ( no  i.e proxy or other)
        //  e.g  http://localhost:8280/services/StudentService/students where there  is no proxy
        //  service with name StudentService.This is a senario where StudentService is in an external
        //  server and it is needed to call it from synapse using the main sequence
        //  2) request is to be injected into  the main sequence  .i.e. http://localhost:8280
        // This method does not cause any performance issue ...
        // Proper fix should be refractoring axis2 RestUtil in a proper way
        RequestURIBasedDispatcher requestDispatcher = new RequestURIBasedDispatcher();
        AxisService axisService = requestDispatcher.findService(msgContext);
        if (axisService == null) {
            String defaultSvcName = NHttpConfiguration.getInstance().getStringValue(
                    "nhttp.default.service", "__SynapseService");                 
            axisService = msgContext.getConfigurationContext()
                    .getAxisConfiguration().getService(defaultSvcName);
        }
        msgContext.setAxisService(axisService);
    }
}
