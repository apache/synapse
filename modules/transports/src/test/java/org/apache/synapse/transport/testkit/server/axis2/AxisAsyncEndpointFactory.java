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

import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.InOnlyAxisOperation;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.synapse.transport.testkit.TestEnvironment;
import org.apache.synapse.transport.testkit.listener.AsyncChannel;
import org.apache.synapse.transport.testkit.message.AxisMessage;
import org.apache.synapse.transport.testkit.server.AsyncEndpoint;
import org.apache.synapse.transport.testkit.server.AsyncEndpointFactory;

public class AxisAsyncEndpointFactory implements AsyncEndpointFactory<AxisMessage> {
    private TestEnvironment env;
    private AxisServer server;
    private AsyncChannel channel;
    
    @SuppressWarnings("unused")
    private void setUp(TestEnvironment env, AxisServer server, AsyncChannel channel) {
        this.env = env;
        this.server = server;
        this.channel = channel;
    }
    
    public AsyncEndpoint<AxisMessage> createAsyncEndpoint(String contentType) throws Exception {
        // Set up a test service with a default operation backed by a mock message
        // receiver. The service is configured using the parameters specified by the
        // implementation.
        AxisService service = new AxisService("TestService");
        AxisOperation operation = new InOnlyAxisOperation(DefaultOperationDispatcher.DEFAULT_OPERATION_NAME);
        MockMessageReceiver messageReceiver = new MockMessageReceiver();
        operation.setMessageReceiver(messageReceiver);
        service.addOperation(operation);
        channel.setupService(service);
        if (contentType != null) {
            env.setupContentType(service, contentType);
        }
        AxisConfiguration axisConfiguration = server.getAxisConfiguration();
        axisConfiguration.addService(service);
//        server.addErrorListener(messageReceiver);
        return new AsyncEndpointImpl(server, service, messageReceiver);
    }
}
