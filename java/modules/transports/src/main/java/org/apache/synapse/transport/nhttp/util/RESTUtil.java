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
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.impl.llom.soap11.SOAP11Factory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.util.Utils;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.i18n.Messages;
import org.apache.axis2.builder.BuilderUtil;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.*;
import org.apache.axis2.dispatchers.HTTPLocationBasedDispatcher;
import org.apache.axis2.dispatchers.RequestURIBasedDispatcher;
import org.apache.axis2.dispatchers.RequestURIOperationDispatcher;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.engine.Handler;
import org.apache.axis2.transport.TransportUtils;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.transport.http.util.URIEncoderDecoder;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;

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
     * Processes the HTTP GET request and builds the SOAP info-set of the REST message
     *
     * @param msgContext The MessageContext of the Request Message
     * @param out The output stream of the response
     * @param soapAction SoapAction of the request
     * @param requestURI The URL that the request came to
     * @param configurationContext The Axis Configuration Context
     * @param requestParameters The parameters of the request message
     * @return boolean indication whether the operation was succesfull
     * @throws AxisFault - Thrown in case a fault occurs
     */
    public static boolean processURLRequest(MessageContext msgContext, OutputStream out,
                                            String soapAction, String requestURI, ConfigurationContext configurationContext,
                                            Map requestParameters) throws AxisFault {

        if ((soapAction != null) && soapAction.startsWith("\"") && soapAction.endsWith("\"")) {
            soapAction = soapAction.substring(1, soapAction.length() - 1);
        }

        msgContext.setSoapAction(soapAction);
        msgContext.setTo(new EndpointReference(requestURI));
        msgContext.setProperty(MessageContext.TRANSPORT_OUT, out);
        msgContext.setServerSide(true);
        SOAPEnvelope envelope = createEnvelopeFromGetRequest(
                requestURI, requestParameters, configurationContext);

        if (envelope == null) {
            return false;
        } else {
            msgContext.setDoingREST(true);
            msgContext.setEnvelope(envelope);
            AxisEngine.receive(msgContext);
            return true;
        }
    }

    /**
     * Creates the {@link SOAPEnvelope} from the GET URL request. REST message building inside
     * synapse will be handled in this manner
     *
     * @param requestUrl GET URL of the request
     * @param map query parameters of the GET request
     * @param configCtx axis configuration context
     * @return created SOAPEnvelope or null if cannot be processed
     * @throws AxisFault if the service represented by the GET request URL cannot be found
     */
    public static SOAPEnvelope createEnvelopeFromGetRequest(String requestUrl, Map map,
                                                            ConfigurationContext configCtx) throws AxisFault {

        String[] values = Utils.parseRequestURLForServiceAndOperation(
                requestUrl, configCtx.getServiceContextPath());

        if (values == null) {
            return new SOAP11Factory().getDefaultEnvelope();
        }

        if ((values[1] != null) && (values[0] != null)) {
            String srvice = values[0];
            AxisService service = configCtx.getAxisConfiguration().getService(srvice);
            if (service == null) {
                throw new AxisFault("service not found: " + srvice);
            }
            String operation = values[1];
            SOAPFactory soapFactory = new SOAP11Factory();
            SOAPEnvelope envelope = soapFactory.getDefaultEnvelope();
            OMNamespace omNs = soapFactory.createOMNamespace(service.getSchemaTargetNamespace(),
                    service.getSchemaTargetNamespacePrefix());
            soapFactory.createOMNamespace(service.getSchemaTargetNamespace(),
                    service.getSchemaTargetNamespacePrefix());
            OMElement opElement;
            if (operation.length() == 0) {
                opElement = envelope.getBody();
            } else {
                opElement = soapFactory.createOMElement(operation, omNs);
                envelope.getBody().addChild(opElement);
            }

            for (Object o : map.keySet()) {
                String name = (String) o;
                String value = (String) map.get(name);
                OMElement omEle = soapFactory.createOMElement(name, omNs);

                omEle.setText(value);
                opElement.addChild(omEle);
            }
            return envelope;
        } else {
            return null;
        }
    }
}
