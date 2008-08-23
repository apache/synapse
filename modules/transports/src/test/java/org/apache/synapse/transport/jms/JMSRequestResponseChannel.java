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

package org.apache.synapse.transport.jms;

import javax.jms.Destination;

import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisService;
import org.apache.synapse.transport.testkit.listener.RequestResponseChannel;
import org.apache.synapse.transport.testkit.name.Key;

public class JMSRequestResponseChannel extends JMSChannel implements RequestResponseChannel {
    private final String replyDestinationType;
    private String replyDestinationName;
    private Destination replyDestination;
    
    public JMSRequestResponseChannel(String name, String destinationType, String replyDestinationType) {
        super(name, destinationType);
        this.replyDestinationType = replyDestinationType;
    }
    
    public JMSRequestResponseChannel(String destinationType, String replyDestinationType) {
        this(null, destinationType, replyDestinationType);
    }
    
    @SuppressWarnings("unused")
    private void setUp(JMSTestEnvironment env) throws Exception {
        replyDestinationName = buildDestinationName("response", replyDestinationType);
        replyDestination = env.createDestination(replyDestinationType, replyDestinationName);
        env.getContext().bind(replyDestinationName, replyDestination);
    }

    @SuppressWarnings("unused")
    private void tearDown() throws Exception {
        env.getContext().unbind(replyDestinationName);
        replyDestinationName = null;
        replyDestination = null;
    }

    @Override
    public void setupService(AxisService service) throws Exception {
        super.setupService(service);
        service.addParameter(JMSConstants.REPLY_PARAM_TYPE, replyDestinationType);
        service.addParameter(JMSConstants.REPLY_PARAM, replyDestinationName);
    }

    @Override
    public void setupRequestMessageContext(MessageContext msgContext) {
        super.setupRequestMessageContext(msgContext);
//        msgContext.setProperty(JMSConstants.JMS_REPLY_TO, replyDestinationName);
    }

    @Override
    public EndpointReference getEndpointReference() throws Exception {
        String address = super.getEndpointReference().getAddress();
        return new EndpointReference(address + "&" + JMSConstants.REPLY_PARAM_TYPE + "=" + replyDestinationType + "&" + JMSConstants.REPLY_PARAM + "=" + replyDestinationName);
    }

    @Key("replyDestType")
    public String getReplyDestinationType() {
        return replyDestinationType;
    }

    public Destination getReplyDestination() {
        return replyDestination;
    }
}
