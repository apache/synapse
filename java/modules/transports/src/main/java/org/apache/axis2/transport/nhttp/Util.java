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

import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.Constants;
import org.apache.axis2.transport.http.HTTPTransportUtils;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axiom.om.OMOutputFormat;

public class Util {

    /**
     * Get the EPR for the message passed in
     * @param msgContext the message context
     * @return the destination EPR
     */
    public static EndpointReference getDestinationEPR(MessageContext msgContext) {

        // Trasnport URL can be different from the WSA-To
        String transportURL = (String) msgContext.getProperty(
            Constants.Configuration.TRANSPORT_URL);

        if (transportURL != null) {
            return new EndpointReference(transportURL);
        } else if (
            (msgContext.getTo() != null) &&
                !AddressingConstants.Submission.WSA_ANONYMOUS_URL.equals(
                    msgContext.getTo().getAddress()) &&
                !AddressingConstants.Final.WSA_ANONYMOUS_URL.equals(
                    msgContext.getTo().getAddress())) {
            return msgContext.getTo();
        }
        return null;
    }

    /**
     * Retirn the OMOutputFormat to be used for the message context passed in
     * @param msgContext the message context
     * @return the OMOutputFormat to be used
     */
    public static OMOutputFormat getOMOutputFormat(MessageContext msgContext) {

        OMOutputFormat format = new OMOutputFormat();
        msgContext.setDoingMTOM(HTTPTransportUtils.doWriteMTOM(msgContext));
        msgContext.setDoingSwA(HTTPTransportUtils.doWriteSwA(msgContext));
        msgContext.setDoingREST(HTTPTransportUtils.isDoingREST(msgContext));        
        format.setSOAP11(msgContext.isSOAP11());
        format.setDoOptimize(msgContext.isDoingMTOM());
        format.setDoingSWA(msgContext.isDoingSwA());

        format.setCharSetEncoding(HTTPTransportUtils.getCharSetEncoding(msgContext));
        Object mimeBoundaryProperty = msgContext.getProperty(Constants.Configuration.MIME_BOUNDARY);
        if (mimeBoundaryProperty != null) {
            format.setMimeBoundary((String) mimeBoundaryProperty);
        }

        return format;
    }

    /**
     * Get the content type for the message passed in
     * @param msgContext the message
     * @return content type of the message
     */
    public static String getContentType(MessageContext msgContext) {
        Object contentTypeObject = msgContext.getProperty(Constants.Configuration.CONTENT_TYPE);
        if (contentTypeObject != null) {
            return (String) contentTypeObject;
        } else if (msgContext.isDoingREST()) {
            return HTTPConstants.MEDIA_TYPE_APPLICATION_XML;
        } else {
            return getOMOutputFormat(msgContext).getContentType();
        }
    }

}
