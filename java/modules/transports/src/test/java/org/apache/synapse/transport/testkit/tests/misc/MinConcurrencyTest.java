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

package org.apache.synapse.transport.testkit.tests.misc;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.mail.internet.ContentType;
import javax.xml.namespace.QName;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.InOnlyAxisOperation;
import org.apache.axis2.engine.MessageReceiver;
import org.apache.synapse.transport.testkit.axis2.client.AxisAsyncTestClient;
import org.apache.synapse.transport.testkit.axis2.client.AxisTestClientContext;
import org.apache.synapse.transport.testkit.axis2.endpoint.AxisEndpoint;
import org.apache.synapse.transport.testkit.axis2.endpoint.AxisServer;
import org.apache.synapse.transport.testkit.channel.AsyncChannel;
import org.apache.synapse.transport.testkit.client.ClientOptions;
import org.apache.synapse.transport.testkit.message.AxisMessage;
import org.apache.synapse.transport.testkit.name.Name;
import org.apache.synapse.transport.testkit.tests.TestResourceSet;
import org.apache.synapse.transport.testkit.tests.TransportTestCase;

/**
 * Generic test case to check whether a transport listener processes messages with the expected
 * level of concurrency. This test case is used to verify that the listener is able to
 * process messages simultaneously.
 * <p>
 * The test case deploys a given number of services and sends a configurable number of messages
 * to each of these services. The services are configured with a custom message receiver that
 * blocks until the expected level of concurrency (given by the number of endpoints times the
 * number of messages) is reached. If after some timeout the concurrency level is not reached,
 * the test fails.
 */
@Name("MinConcurrency")
public class MinConcurrencyTest extends TransportTestCase {
    private final AsyncChannel[] channels;
    private final int messages;
    private final boolean preloadMessages;
    
    public MinConcurrencyTest(AxisServer server, AsyncChannel[] channels, int messages,
            boolean preloadMessages, Object... resources) {
        super(resources);
        addResource(server);
        addResource(AxisTestClientContext.INSTANCE);
        this.channels = channels;
        this.messages = messages;
        this.preloadMessages = preloadMessages;
    }
    
    @Override
    protected void runTest() throws Throwable {
        int endpointCount = channels.length;
        int expectedConcurrency = endpointCount * messages;
        
        final CountDownLatch shutdownLatch = new CountDownLatch(1);
        final CountDownLatch concurrencyReachedLatch = new CountDownLatch(expectedConcurrency);
        
        final MessageReceiver messageReceiver = new MessageReceiver() {
            public void receive(MessageContext msgContext) throws AxisFault {
                concurrencyReachedLatch.countDown();
                try {
                    shutdownLatch.await();
                } catch (InterruptedException ex) {
                }
            }
        };
        
        TestResourceSet[] clientResourceSets = new TestResourceSet[endpointCount];
        TestResourceSet[] endpointResourceSets = new TestResourceSet[endpointCount];
        try {
            for (int i=0; i<endpointCount; i++) {
                TestResourceSet clientResourceSet = new TestResourceSet(getResourceSet());
                AsyncChannel channel = channels[i];
                clientResourceSet.addResource(channel);
                AxisAsyncTestClient client = new AxisAsyncTestClient(false);
                clientResourceSet.addResource(client);
                clientResourceSet.setUp();
                clientResourceSets[i] = clientResourceSet;
                
                TestResourceSet endpointResourceSet = new TestResourceSet(clientResourceSet);
                endpointResourceSet.addResource(new AxisEndpoint() {
                    @Override
                    protected AxisOperation createOperation() {
                        AxisOperation operation = new InOnlyAxisOperation(new QName("in"));
                        operation.setMessageReceiver(messageReceiver);
                        return operation;
                    }
                });
                
                if (!preloadMessages) {
                    endpointResourceSet.setUp();
                    endpointResourceSets[i] = endpointResourceSet;
                }
                for (int j=0; j<messages; j++) {
                    ClientOptions options = new ClientOptions(new ContentType(SOAP11Constants.SOAP_11_CONTENT_TYPE), "UTF-8");
                    AxisMessage message = new AxisMessage();
                    message.setMessageType(SOAP11Constants.SOAP_11_CONTENT_TYPE);
                    SOAPFactory factory = OMAbstractFactory.getSOAP11Factory();
                    SOAPEnvelope envelope = factory.getDefaultEnvelope();
                    message.setEnvelope(envelope);
                    client.sendMessage(options, new ContentType(message.getMessageType()), message);
                }
                if (preloadMessages) {
                    endpointResourceSet.setUp();
                    endpointResourceSets[i] = endpointResourceSet;
                }
            }
        
            if (!concurrencyReachedLatch.await(5, TimeUnit.SECONDS)) {
                fail("Concurrency reached is " + (expectedConcurrency -
                        concurrencyReachedLatch.getCount()) + ", but expected " +
                        expectedConcurrency);
            }
        } finally {
            shutdownLatch.countDown();
            for (int i=0; i<endpointCount; i++) {
                if (endpointResourceSets[i] != null) {
                    endpointResourceSets[i].tearDown();
                }
                if (clientResourceSets[i] != null) {
                    clientResourceSets[i].tearDown();
                }
            }
        }
    }
}
