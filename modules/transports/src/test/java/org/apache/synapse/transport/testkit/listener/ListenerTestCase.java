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

import junit.framework.TestCase;

import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.InOnlyAxisOperation;
import org.apache.axis2.engine.AxisConfiguration;

public abstract class ListenerTestCase<S extends MessageSender> extends TestCase {
    private final String name;
    private final Channel<?> channel;
    private final S sender;
    private final ContentTypeMode contentTypeMode;
    private final String contentType;
    
    private ListenerTestServer server;
    private boolean manageServer = true;
    
    public ListenerTestCase(Channel<?> channel, S sender, String name, ContentTypeMode contentTypeMode, String contentType) {
        this.channel = channel;
        this.sender = sender;
        this.name = name;
        this.contentTypeMode = contentTypeMode;
        this.contentType = contentType;
    }
    
    @Override
    public String getName() {
        String testName = super.getName();
        if (testName == null) {
            NameBuilder nameBuilder = new NameBuilder();
            nameBuilder.addComponent("test", name);
            channel.buildName(nameBuilder);
            buildName(nameBuilder);
            nameBuilder.addComponent("contentTypeMode", contentTypeMode.toString().toLowerCase());
            testName = nameBuilder.toString();
            setName(testName);
        }
        return testName;
    }

    protected void buildName(NameBuilder name) {
        sender.buildName(name);
    }
    
    public Channel<?> getChannel() {
        return channel;
    }

    public ListenerTestSetup getSetup() {
        return channel.getSetup();
    }
    
    public void setServer(ListenerTestServer server){
        this.server = server;
        manageServer = false;
    }
    
    @Override
    protected void setUp() throws Exception {
        if (manageServer) {
            server = new ListenerTestServer(channel);
            server.start();
        }
        sender.setUp(channel);
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
        
        // Run the test.
        MessageData messageData;
        AxisConfiguration axisConfiguration = server.getAxisConfiguration();
        axisConfiguration.addService(service);
//        server.addErrorListener(messageReceiver);
        try {
            sendMessage(sender, server.getEPR(service),
                    contentTypeMode == ContentTypeMode.TRANSPORT ? contentType : null);
            messageData = messageReceiver.waitForMessage(8, TimeUnit.SECONDS);
            if (messageData == null) {
                fail("Failed to get message");
            }
        }
        finally {
//            server.removeErrorListener(messageReceiver);
            axisConfiguration.removeService(service.getName());
        }
        
        checkMessageData(messageData);
    }
    
    @Override
    protected void tearDown() throws Exception {
        sender.tearDown();
        if (manageServer) {
            server.stop();
            server = null;
        }
    }

    protected abstract void sendMessage(S sender, String endpointReference, String contentType) throws Exception;
    protected abstract void checkMessageData(MessageData messageData) throws Exception;
}