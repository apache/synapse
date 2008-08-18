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

package org.apache.synapse.transport.testkit.client.axis2;

import java.io.File;

import javax.xml.namespace.QName;

import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
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
import org.apache.synapse.transport.testkit.TransportDescriptionFactory;
import org.apache.synapse.transport.testkit.client.ClientOptions;
import org.apache.synapse.transport.testkit.client.TestClient;
import org.apache.synapse.transport.testkit.listener.Channel;
import org.apache.synapse.transport.testkit.message.AxisMessage;

public class AxisTestClient implements TestClient {
    private static final Log log = LogFactory.getLog(AxisTestClient.class);
    
    private final TransportDescriptionFactory tdf;
    
    private Channel channel;
    private TransportOutDescription trpOutDesc;
    private ConfigurationContext cfgCtx;
    
    public AxisTestClient(TransportDescriptionFactory tdf) {
        this.tdf = tdf;
    }

    @SuppressWarnings("unused")
    private void setUp(Channel channel) throws Exception {
        this.channel = channel;
        
        cfgCtx =
            ConfigurationContextFactory.createConfigurationContextFromFileSystem(
                    new File("target/test_rep").getAbsolutePath());
        AxisConfiguration axisCfg = cfgCtx.getAxisConfiguration();

        trpOutDesc = tdf.createTransportOutDescription();
        axisCfg.addTransportOut(trpOutDesc);
        trpOutDesc.getSender().init(cfgCtx, trpOutDesc);
    }

    @SuppressWarnings("unused")
    private void tearDown() throws Exception {
        trpOutDesc.getSender().stop();
    }
    
    protected OperationClient createClient(ClientOptions options, AxisMessage message, QName operationQName) throws AxisFault {
        String endpointReference = options.getEndpointReference();
        log.info("Sending to " + endpointReference);
        
        Options axisOptions = new Options();
        axisOptions.setTo(channel.createEndpointReference(endpointReference));

        ServiceClient serviceClient = new ServiceClient(cfgCtx, null);
        serviceClient.setOptions(axisOptions);
        
        OperationClient mepClient = serviceClient.createClient(operationQName);
        MessageContext mc = new MessageContext();
        mc.setProperty(Constants.Configuration.MESSAGE_TYPE, message.getMessageType());
        mc.setEnvelope(message.getEnvelope());
        mc.setAttachmentMap(message.getAttachments());
        channel.setupRequestMessageContext(mc);
        setupRequestMessageContext(mc);
        mc.setProperty(Constants.Configuration.CHARACTER_SET_ENCODING, options.getCharset());
        mc.setServiceContext(serviceClient.getServiceContext());
        mepClient.addMessageContext(mc);
        
        return mepClient;
    }
    
    protected void setupRequestMessageContext(@SuppressWarnings("unused") MessageContext msgContext) throws AxisFault {
    }
}
