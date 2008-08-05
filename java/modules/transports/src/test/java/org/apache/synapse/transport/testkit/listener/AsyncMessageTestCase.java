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

import java.util.concurrent.TimeUnit;

import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.InOnlyAxisOperation;
import org.apache.axis2.engine.AxisConfiguration;

public abstract class AsyncMessageTestCase<C extends AsyncChannel<?>,M> extends ListenerTestCase<C,AsyncMessageSender<? super C,M>> {
    private final String charset;
    
    public AsyncMessageTestCase(C channel, AsyncMessageSender<? super C,M> sender, String name, ContentTypeMode contentTypeMode, String contentType, String charset) {
        super(channel, sender, name, contentTypeMode, contentType);
        this.charset = charset;
    }

    @Override
    protected void runTest() throws Throwable {
        // Set up a test service with a default operation backed by a mock message
        // receiver. The service is configured using the parameters specified by the
        // implementation.
        AxisService service = new AxisService("TestService");
        AxisOperation operation = new InOnlyAxisOperation(DefaultOperationDispatcher.DEFAULT_OPERATION_NAME);
        MockMessageReceiver messageReceiver = new MockMessageReceiver();
        operation.setMessageReceiver(messageReceiver);
        service.addOperation(operation);
        channel.setupService(service);
        if (contentTypeMode == ContentTypeMode.SERVICE) {
            channel.getSetup().setupContentType(service, contentType);
        }
        
        M message = prepareMessage();
        
        // Run the test.
        MessageData messageData;
        AxisConfiguration axisConfiguration = server.getAxisConfiguration();
        axisConfiguration.addService(service);
//        server.addErrorListener(messageReceiver);
        try {
            SenderOptions options = new SenderOptions(server.getEPR(service), charset);
//                    contentTypeMode == ContentTypeMode.TRANSPORT ? contentType : null);
            sender.sendMessage(channel, options, message);
            messageData = messageReceiver.waitForMessage(8, TimeUnit.SECONDS);
            if (messageData == null) {
                fail("Failed to get message");
            }
        }
        finally {
//            server.removeErrorListener(messageReceiver);
            axisConfiguration.removeService(service.getName());
        }
        
        checkMessageData(message, messageData);
    }
    
    protected abstract M prepareMessage() throws Exception;
    protected abstract void checkMessageData(M message, MessageData messageData) throws Exception;
}