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
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.WSDL20DefaultValueHolder;
import org.apache.axis2.description.WSDL2Constants;
import org.apache.axis2.transport.http.util.URIEncoderDecoder;

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
        } else {
            throw new AxisFault("Cannot convert SOAP infoset to a GET URL-encoded format");
        }

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
}
