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

package org.apache.synapse.transport.testkit.server.axis2;

import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.InOutAxisOperation;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.receivers.AbstractInOutMessageReceiver;
import org.apache.synapse.transport.testkit.TestEnvironment;
import org.apache.synapse.transport.testkit.listener.RequestResponseChannel;
import org.apache.synapse.transport.testkit.server.Endpoint;
import org.apache.synapse.transport.testkit.server.EndpointFactory;

public class AxisEchoEndpointFactory implements EndpointFactory {
    private TestEnvironment env;
    private AxisServer server;
    private RequestResponseChannel channel;
    
    @SuppressWarnings("unused")
    private void setUp(TestEnvironment env, AxisServer server, RequestResponseChannel channel) {
        this.env = env;
        this.server = server;
        this.channel = channel;
    }

    public Endpoint createEchoEndpoint(String contentType) throws Exception {
        AxisService service = new AxisService("EchoService");
        AxisOperation operation = new InOutAxisOperation(DefaultOperationDispatcher.DEFAULT_OPERATION_NAME);
        operation.setMessageReceiver(new AbstractInOutMessageReceiver() {
            @Override
            public void invokeBusinessLogic(MessageContext inMessage, MessageContext outMessage) throws AxisFault {
                System.out.println(inMessage.getProperty(Constants.OUT_TRANSPORT_INFO));
                System.out.println(inMessage.getEnvelope());
                outMessage.setEnvelope(inMessage.getEnvelope());
            }
        });
        service.addOperation(operation);
        channel.setupService(service);
        if (contentType != null) {
            env.setupContentType(service, contentType);
        }
        
        AxisConfiguration axisConfiguration = server.getAxisConfiguration();
        axisConfiguration.addService(service);
        return new EndpointImpl(server, service);
    }
}
