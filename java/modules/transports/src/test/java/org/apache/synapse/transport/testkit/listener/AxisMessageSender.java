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

package org.apache.synapse.transport.testkit.listener;

import java.io.File;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.OperationClient;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class AxisMessageSender extends AbstractMessageSender implements XMLMessageSender {
    private static final Log log = LogFactory.getLog(AxisMessageSender.class);
    
    private TransportOutDescription trpOutDesc;
    private ConfigurationContext cfgCtx;
    
    public AxisMessageSender() {
        super("axis");
    }

    @Override
    public void setUp(ListenerTestSetup setup) throws Exception {
        super.setUp(setup);
        cfgCtx =
            ConfigurationContextFactory.createConfigurationContextFromFileSystem(
                    new File("target/test_rep").getAbsolutePath());
        AxisConfiguration axisCfg = cfgCtx.getAxisConfiguration();

        trpOutDesc = setup.createTransportOutDescription();
        axisCfg.addTransportOut(trpOutDesc);
        trpOutDesc.getSender().init(cfgCtx, trpOutDesc);
    }

    public void sendMessage(ListenerTestSetup setup,
            String endpointReference, String contentType, String charset,
            OMElement message) throws Exception {
        
        Options options = new Options();
        options.setTo(new EndpointReference(endpointReference));

        ServiceClient serviceClient = new ServiceClient(cfgCtx, null);
        serviceClient.setOptions(options);
        
        OperationClient mepClient = serviceClient.createClient(ServiceClient.ANON_OUT_ONLY_OP);
        MessageContext mc = new MessageContext();
        SOAPEnvelope envelope;
        String messageType;
        if (message instanceof SOAPEnvelope) {
            envelope = (SOAPEnvelope)message;
            String ns = message.getNamespace().getNamespaceURI();
            if (ns.equals(SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI)) {
                messageType = SOAP12Constants.SOAP_12_CONTENT_TYPE;
            } else {
                messageType = SOAP11Constants.SOAP_11_CONTENT_TYPE;
            }
        } else {
            envelope = OMAbstractFactory.getSOAP11Factory().getDefaultEnvelope();
            envelope.getBody().addChild(message);
            messageType = "application/xml";
        }
        mc.setEnvelope(envelope);
        mc.setProperty(Constants.Configuration.MESSAGE_TYPE, messageType);
        mc.setProperty(Constants.Configuration.CHARACTER_SET_ENCODING, charset);
        mepClient.addMessageContext(mc);
        log.info("Sending to " + endpointReference);
        mepClient.execute(false);
    }

    @Override
    public void tearDown() throws Exception {
        trpOutDesc.getSender().stop();
    }
}
