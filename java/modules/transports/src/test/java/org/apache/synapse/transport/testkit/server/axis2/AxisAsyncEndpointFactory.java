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
import org.apache.synapse.transport.testkit.listener.AsyncChannel;
import org.apache.synapse.transport.testkit.message.AxisMessage;
import org.apache.synapse.transport.testkit.server.AsyncEndpoint;
import org.apache.synapse.transport.testkit.server.AsyncEndpointFactory;

public class AxisAsyncEndpointFactory implements AsyncEndpointFactory<AxisMessage> {
    private AxisServer server;
    private AsyncChannel channel;
    
    @SuppressWarnings("unused")
    private void setUp(AxisServer server, AsyncChannel channel) {
        this.server = server;
        this.channel = channel;
    }
    
    public AsyncEndpoint<AxisMessage> createAsyncEndpoint(String contentType) throws Exception {
        AxisOperation operation = new InOnlyAxisOperation(DefaultOperationDispatcher.DEFAULT_OPERATION_NAME);
        MockMessageReceiver messageReceiver = new MockMessageReceiver();
        operation.setMessageReceiver(messageReceiver);
        AxisService service = server.deployService(channel, operation, contentType);
//        server.addErrorListener(messageReceiver);
        return new AsyncEndpointImpl(server, service, messageReceiver);
    }
}
