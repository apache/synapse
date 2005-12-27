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

package org.apache.synapse.axis2;

import org.apache.axis2.AxisFault;
import org.apache.axis2.soap.SOAPEnvelope;
import org.apache.axis2.soap.SOAP12Constants;
import org.apache.axis2.soap.SOAPFactory;

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.TransportInDescription;

import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.om.OMAbstractFactory;
import org.apache.axis2.om.OMNamespace;

import org.apache.synapse.Constants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseMessage;
import org.apache.synapse.SynapseEnvironment;

import java.util.Iterator;


/**
 * This class helps the Axis2SynapseEnvironment implement the send method
 */
public class Axis2Sender {

    public static void sendOn(SynapseMessage smc, SynapseEnvironment se) {

        try {

            MessageContext messageContext = ((Axis2SynapseMessage) smc)
                    .getMessageContext();

            MessageContext outMsgContext = Axis2FlexibleMEPClient
                    .send(messageContext);

            // run all rules on response

            // todo: this logic need to be imporved with sendNow() and drop()
            smc.setResponse(true);
            ///////////////////////////////////////////////////////////////////
            // special treat for Module Engagement
            ConfigurationContext configContext = (ConfigurationContext) smc
                    .getProperty(
                            Constants.ADDRESSING_PROCESSED_CONFIGURATION_CONTEXT);
            if (configContext != null) {
                outMsgContext.setProperty(
                        Constants.ADDRESSING_PROCESSED_CONFIGURATION_CONTEXT,
                        configContext);
            }
            //////////////////////////////////////////////////////////////////

            outMsgContext.setServerSide(true);

            // deal with the fact that AddressingOutHandler has a bug if
            // there is no header at all.        
            // fixed in axis 0.9652 
            SOAPEnvelope envelope = outMsgContext.getEnvelope();
            // temporarty hack
            SOAPEnvelope newEnvelope;
            if (envelope.getHeader() == null) {
                SOAPFactory soapFactory;
                if (envelope.getNamespace().getName()
                        .equals(SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI)) {
                    soapFactory = OMAbstractFactory.getSOAP12Factory();
                    newEnvelope = soapFactory.getDefaultEnvelope();
                } else {
                    soapFactory = OMAbstractFactory.getSOAP11Factory();
                    newEnvelope = soapFactory.getDefaultEnvelope();

                }
                /**
                 * Need a big fix here. Axis2 folks should fix this
                 */
                //envelope.addChild(soapFactory.createSOAPHeader(envelope));
                //todo: bug in Axiom when another tree is declared and copy some elements from one tree to other
                //todo: the second tree doesn't serialize attribute aware namespaces properly
                //todo: as a temporartory hack this was taken into account
                Iterator iterator = envelope.getAllDeclaredNamespaces();
                while (iterator.hasNext()) {
                    OMNamespace namespace = (OMNamespace) iterator.next();
                    newEnvelope.declareNamespace(namespace);
                }
                newEnvelope.getBody()
                        .addChild(envelope.getBody().getFirstElement());
                outMsgContext.setEnvelope(newEnvelope);
            }

            Object os = messageContext
                    .getProperty(MessageContext.TRANSPORT_OUT);
            outMsgContext.setProperty(MessageContext.TRANSPORT_OUT, os);
            TransportInDescription ti = messageContext.getTransportIn();

            outMsgContext.setTransportIn(ti);
            se.injectMessage(new Axis2SynapseMessage(outMsgContext));

        } catch (Exception e) {
            throw new SynapseException(e);
        }
    }

    public static void sendBack(SynapseMessage smc) {
        MessageContext messageContext = ((Axis2SynapseMessage) smc)
                .getMessageContext();
        AxisEngine ae =
                new AxisEngine(messageContext.getConfigurationContext());
        try {
            if (messageContext.getEnvelope().getHeader() == null) {
                messageContext.getEnvelope().getBody().insertSiblingBefore(
                        OMAbstractFactory.getSOAP11Factory()
                                .getDefaultEnvelope()
                                .getHeader());

            }
            System.out.println(messageContext.getEnvelope());

            messageContext
                    .setProperty(Constants.ISRESPONSE_PROPERTY, new Boolean(
                            true));
            // check for addressing is alredy engaged for this message.
            // if engage we should use the address enable Configuraion context.
            ConfigurationContext configContext = (ConfigurationContext) smc
                    .getProperty(
                            Constants.ADDRESSING_PROCESSED_CONFIGURATION_CONTEXT);
            if (configContext != null) {
                messageContext.setConfigurationContext(configContext);
            }

            ae.send(messageContext);
        } catch (AxisFault e) {
            throw new SynapseException(e);

        }

    }

}
