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

package org.apache.synapse.transport.testkit.message;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.context.MessageContext;

public interface XMLMessageType {
    class SOAP implements XMLMessageType {
        private final SOAPFactory factory;
        private final String messageType;

        public SOAP(SOAPFactory factory, String messageType) {
            this.factory = factory;
            this.messageType = messageType;
        }

        public OMFactory getOMFactory() {
            return factory;
        }
        
        public SOAPEnvelope getMessage(OMElement payload) {
            SOAPEnvelope envelope = factory.createSOAPEnvelope();
            SOAPBody body = factory.createSOAPBody();
            body.addChild(payload);
            envelope.addChild(body);
            return envelope;
        }

        public MessageContext createMessageContext(OMElement payload) throws AxisFault {
            MessageContext mc = new MessageContext();
            mc.setEnvelope(getMessage(payload));
            mc.setProperty(Constants.Configuration.MESSAGE_TYPE, messageType);
            return mc;
        }
    }
    
    XMLMessageType SOAP11 = new SOAP(OMAbstractFactory.getSOAP11Factory(), SOAP11Constants.SOAP_11_CONTENT_TYPE);
    XMLMessageType SOAP12 = new SOAP(OMAbstractFactory.getSOAP12Factory(), SOAP12Constants.SOAP_12_CONTENT_TYPE);
    
    XMLMessageType POX = new XMLMessageType() {
        private final OMFactory factory = OMAbstractFactory.getOMFactory();

        public OMFactory getOMFactory() {
            return factory;
        }
        
        public OMElement getMessage(OMElement payload) {
            return payload;
        }

        public MessageContext createMessageContext(OMElement payload) throws AxisFault {
            MessageContext mc = new MessageContext();
            SOAPEnvelope envelope = OMAbstractFactory.getSOAP11Factory().getDefaultEnvelope();
            envelope.getBody().addChild(payload);
            mc.setEnvelope(envelope);
            mc.setProperty(Constants.Configuration.MESSAGE_TYPE, "application/xml");
            return mc;
        }
    };
    
    OMFactory getOMFactory();
    OMElement getMessage(OMElement payload);
    MessageContext createMessageContext(OMElement payload) throws AxisFault;
}
