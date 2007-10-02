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

package org.apache.synapse.core.axis2;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axis2.AxisFault;

import java.util.Iterator;

public class SOAPUtils {

    /**
     * Converts the SOAP version of the message context.  Creates a new envelope of the given SOAP
     * version, copy headers and bodies from the old envelope and sets the new envelope to the same
     * message context.
     *
     * @param axisOutMsgCtx  messageContext where version conversion is done
     * @param soapVersionURI either org.apache.axis2.namespace.Constants.URI_SOAP12_ENV or
     *                       org.apache.axis2.namespace.Constants.URI_SOAP11_ENV
     * @throws AxisFault
     */
    public static void convertSoapVersion(org.apache.axis2.context.MessageContext axisOutMsgCtx,
        String soapVersionURI) throws AxisFault {

        // create a new envelope of the given version
        SOAPFactory soapFactory = null;
        if (org.apache.axis2.namespace.Constants.URI_SOAP12_ENV.equals(soapVersionURI)) {
            soapFactory = OMAbstractFactory.getSOAP12Factory();
        } else if (org.apache.axis2.namespace.Constants.URI_SOAP11_ENV.equals(soapVersionURI)) {
            soapFactory = OMAbstractFactory.getSOAP11Factory();
        } else {
            throw new RuntimeException("Invalid soapVersionURI");
        }
        SOAPEnvelope newEnvelope = soapFactory.getDefaultEnvelope();

        // get the existing envelope
        SOAPEnvelope oldEnvelope = axisOutMsgCtx.getEnvelope();

        // move all headers
        if (oldEnvelope.getHeader() != null) {
            Iterator itr = oldEnvelope.getHeader().getChildren();
            while (itr.hasNext()) {
                newEnvelope.getHeader().addChild(((OMNode) itr.next()));
            }
        }

        // move all bodies
        if (oldEnvelope.getBody() != null) {
            Iterator itr = oldEnvelope.getBody().getChildren();
            while (itr.hasNext()) {
                newEnvelope.getBody().addChild(((OMNode) itr.next()));
            }
        }

        // set new envelope to message context; old envelope go to garbage
        axisOutMsgCtx.setEnvelope(newEnvelope);
    }

}