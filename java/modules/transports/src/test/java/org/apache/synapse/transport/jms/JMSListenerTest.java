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

import static org.apache.synapse.transport.testkit.AdapterUtils.adapt;

import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.axis2.context.MessageContext;
import org.apache.synapse.transport.testkit.TransportDescriptionFactory;
import org.apache.synapse.transport.testkit.TransportTestSuite;
import org.apache.synapse.transport.testkit.client.AsyncTestClient;
import org.apache.synapse.transport.testkit.client.axis2.AxisAsyncTestClient;
import org.apache.synapse.transport.testkit.client.axis2.AxisRequestResponseTestClient;
import org.apache.synapse.transport.testkit.listener.AsyncChannel;
import org.apache.synapse.transport.testkit.listener.ContentTypeMode;
import org.apache.synapse.transport.testkit.listener.MessageTestData;
import org.apache.synapse.transport.testkit.message.MessageConverter;
import org.apache.synapse.transport.testkit.message.XMLMessage;
import org.apache.synapse.transport.testkit.server.axis2.AxisAsyncEndpointFactory;
import org.apache.synapse.transport.testkit.server.axis2.AxisEchoEndpointFactory;
import org.apache.synapse.transport.testkit.server.axis2.AxisServer;
import org.apache.synapse.transport.testkit.tests.misc.MinConcurrencyTest;

public class JMSListenerTest extends TestCase {
    public static TestSuite suite() {
        TransportTestSuite suite = new TransportTestSuite();
        TransportDescriptionFactory tdf = new JMSTransportDescriptionFactory();
        AxisServer server = new AxisServer();
        AxisAsyncEndpointFactory asyncEndpointFactory = new AxisAsyncEndpointFactory();
        AxisEchoEndpointFactory echoEndpointFactory = new AxisEchoEndpointFactory();
        JMSBytesMessageClient bytesMessageClient = new JMSBytesMessageClient();
        JMSTextMessageClient textMessageClient = new JMSTextMessageClient();
        List<AsyncTestClient<XMLMessage>> clients = new LinkedList<AsyncTestClient<XMLMessage>>();
        clients.add(adapt(bytesMessageClient, MessageConverter.XML_TO_BYTE));
        clients.add(adapt(textMessageClient, MessageConverter.XML_TO_STRING));
        clients.add(adapt(new AxisAsyncTestClient(), MessageConverter.XML_TO_AXIS));
        clients.add(adapt(new JMSAxisAsyncClient(JMSConstants.JMS_BYTE_MESSAGE), MessageConverter.XML_TO_AXIS));
        clients.add(adapt(new JMSAxisAsyncClient(JMSConstants.JMS_TEXT_MESSAGE), MessageConverter.XML_TO_AXIS));
        for (JMSTestEnvironment env : new JMSTestEnvironment[] { new QpidTestEnvironment(), new ActiveMQTestEnvironment() }) {
            suite.addPOXTests(new JMSRequestResponseChannel(JMSConstants.DESTINATION_TYPE_QUEUE, JMSConstants.DESTINATION_TYPE_QUEUE), adapt(new AxisRequestResponseTestClient(), MessageConverter.XML_TO_AXIS, MessageConverter.AXIS_TO_XML), echoEndpointFactory, ContentTypeMode.TRANSPORT, env, server, tdf);
            suite.addPOXTests(new JMSRequestResponseChannel(JMSConstants.DESTINATION_TYPE_QUEUE, JMSConstants.DESTINATION_TYPE_QUEUE), adapt(new AxisRequestResponseTestClient(), MessageConverter.XML_TO_AXIS, MessageConverter.AXIS_TO_XML), new MockEchoEndpointFactory(), ContentTypeMode.TRANSPORT, env, tdf);
            for (String destinationType : new String[] { JMSConstants.DESTINATION_TYPE_QUEUE, JMSConstants.DESTINATION_TYPE_TOPIC }) {
                JMSAsyncChannel channel = new JMSAsyncChannel(destinationType);
                for (ContentTypeMode contentTypeMode : ContentTypeMode.values()) {
                    for (AsyncTestClient<XMLMessage> client : clients) {
                        if (contentTypeMode == ContentTypeMode.TRANSPORT) {
                            suite.addSOAPTests(channel, client, asyncEndpointFactory, contentTypeMode, env, server, tdf);
                            suite.addPOXTests(channel, client, asyncEndpointFactory, contentTypeMode, env, server, tdf);
                        } else {
                            // If no content type header is used, SwA can't be used and the JMS transport
                            // always uses the default charset encoding
                            suite.addSOAP11Test(channel, client, asyncEndpointFactory, contentTypeMode, new MessageTestData(null, TransportTestSuite.testString,
                                    MessageContext.DEFAULT_CHAR_SET_ENCODING), env, server, tdf);
                            suite.addSOAP12Test(channel, client, asyncEndpointFactory, contentTypeMode, new MessageTestData(null, TransportTestSuite.testString,
                                    MessageContext.DEFAULT_CHAR_SET_ENCODING), env, server, tdf);
                            suite.addPOXTest(channel, client, asyncEndpointFactory, contentTypeMode, new MessageTestData(null, TransportTestSuite.testString,
                                    MessageContext.DEFAULT_CHAR_SET_ENCODING), env, server, tdf);
                        }
                    }
                    if (contentTypeMode == ContentTypeMode.TRANSPORT) {
                        suite.addSwATests(channel, bytesMessageClient, asyncEndpointFactory, env, server, tdf);
                    }
                    // TODO: these tests are temporarily disabled because of SYNAPSE-304
                    // addTextPlainTests(strategy, suite);
                    suite.addBinaryTest(channel, bytesMessageClient, adapt(asyncEndpointFactory, MessageConverter.AXIS_TO_BYTE), contentTypeMode, env, server, tdf);
                }
            }
            suite.addTest(new MinConcurrencyTest(server, new AsyncChannel[] {
                    new JMSAsyncChannel("endpoint1", JMSConstants.DESTINATION_TYPE_QUEUE),
                    new JMSAsyncChannel("endpoint2", JMSConstants.DESTINATION_TYPE_QUEUE) },
                    2, false, env, tdf));
        }
        return suite;
    }
}
