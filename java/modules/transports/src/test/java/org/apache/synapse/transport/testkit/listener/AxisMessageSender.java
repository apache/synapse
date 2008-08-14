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

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
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
import org.apache.synapse.transport.testkit.TestEnvironment;
import org.apache.synapse.transport.testkit.message.XMLMessageType;

public class AxisMessageSender<C extends Channel<?>> extends AbstractMessageSender<TestEnvironment,C> {
    private static final Log log = LogFactory.getLog(AxisMessageSender.class);
    
    private C channel;
    private TransportOutDescription trpOutDesc;
    private ConfigurationContext cfgCtx;
    
    @Override
    public void setUp(TestEnvironment env, C channel) throws Exception {
        super.setUp(env, channel);
        this.channel = channel;
        
        cfgCtx =
            ConfigurationContextFactory.createConfigurationContextFromFileSystem(
                    new File("target/test_rep").getAbsolutePath());
        AxisConfiguration axisCfg = cfgCtx.getAxisConfiguration();

        trpOutDesc = channel.createTransportOutDescription();
        axisCfg.addTransportOut(trpOutDesc);
        trpOutDesc.getSender().init(cfgCtx, trpOutDesc);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        trpOutDesc.getSender().stop();
    }
    
    protected OperationClient createClient(String endpointReference, QName operationQName, XMLMessageType xmlMessageType, OMElement payload, String charset) throws AxisFault {
        log.info("Sending to " + endpointReference);
        
        Options options = new Options();
        options.setTo(channel.createEndpointReference(endpointReference));

        ServiceClient serviceClient = new ServiceClient(cfgCtx, null);
        serviceClient.setOptions(options);
        
        OperationClient mepClient = serviceClient.createClient(operationQName);
        MessageContext mc = xmlMessageType.createMessageContext(payload);
        channel.setupRequestMessageContext(mc);
        setupRequestMessageContext(mc);
        mc.setProperty(Constants.Configuration.CHARACTER_SET_ENCODING, charset);
        mc.setServiceContext(serviceClient.getServiceContext());
        mepClient.addMessageContext(mc);
        
        return mepClient;
    }
    
    protected void setupRequestMessageContext(MessageContext msgContext) throws AxisFault {
    }
}
