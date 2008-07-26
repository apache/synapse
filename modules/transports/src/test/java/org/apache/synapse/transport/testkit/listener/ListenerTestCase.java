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

public abstract class ListenerTestCase extends TestCase {
    private final ListenerTestSetup setup;
    private final ContentTypeMode contentTypeMode;
    private final String contentType;
    
    private ListenerTestServer server;
    private boolean manageServer = true;
    
    public ListenerTestCase(ListenerTestSetup setup, String baseName, ContentTypeMode contentTypeMode, String contentType) {
        super(setup.getTestName(baseName) + "_" + contentTypeMode);
        this.setup = setup;
        this.contentTypeMode = contentTypeMode;
        this.contentType = contentType;
    }
    
    public ListenerTestSetup getSetup() {
        return setup;
    }
    
    public void setServer(ListenerTestServer server){
        this.server = server;
        manageServer = false;
    }
    
    @Override
    protected void setUp() throws Exception {
        if (manageServer) {
            server = new ListenerTestServer(setup);
            setup.beforeStartup();
            server.start();
        }
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
        setup.setupService(service);
        if (contentTypeMode == ContentTypeMode.SERVICE) {
            setup.setupContentType(service, contentType);
        }
        
        // Run the test.
        MessageData messageData;
        AxisConfiguration axisConfiguration = server.getAxisConfiguration();
        axisConfiguration.addService(service);
        try {
            sendMessage(server.getEPR(service),
                    contentTypeMode == ContentTypeMode.TRANSPORT ? contentType : null);
            messageData = messageReceiver.waitForMessage(8, TimeUnit.SECONDS);
            if (messageData == null) {
                fail("Failed to get message");
            }
        }
        finally {
            axisConfiguration.removeService(service.getName());
        }
        
        checkMessageData(messageData);
    }
    
    @Override
    protected void tearDown() throws Exception {
        if (manageServer) {
            server.stop();
            Thread.sleep(100); // TODO: this is required for the NIO transport; check whether this is a bug
            server = null;
        }
    }

    protected abstract void sendMessage(String endpointReference, String contentType) throws Exception;
    protected abstract void checkMessageData(MessageData messageData) throws Exception;
}