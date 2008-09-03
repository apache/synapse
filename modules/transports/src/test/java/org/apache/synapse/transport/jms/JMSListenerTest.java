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
import org.apache.synapse.transport.testkit.listener.MessageTestData;
import org.apache.synapse.transport.testkit.message.MessageDecoder;
import org.apache.synapse.transport.testkit.message.MessageEncoder;
import org.apache.synapse.transport.testkit.message.XMLMessage;
import org.apache.synapse.transport.testkit.server.axis2.AxisAsyncEndpoint;
import org.apache.synapse.transport.testkit.server.axis2.AxisEchoEndpoint;
import org.apache.synapse.transport.testkit.server.axis2.AxisServer;
import org.apache.synapse.transport.testkit.server.axis2.ContentTypeServiceConfigurator;
import org.apache.synapse.transport.testkit.tests.misc.MinConcurrencyTest;

public class JMSListenerTest extends TestCase {
    public static TestSuite suite() {
        TransportTestSuite suite = new TransportTestSuite();
        TransportDescriptionFactory tdf = new JMSTransportDescriptionFactory();
        AxisAsyncEndpoint asyncEndpoint = new AxisAsyncEndpoint();
        AxisEchoEndpoint echoEndpoint = new AxisEchoEndpoint();
        JMSBytesMessageClient bytesMessageClient = new JMSBytesMessageClient();
        JMSTextMessageClient textMessageClient = new JMSTextMessageClient();
        ContentTypeServiceConfigurator cfgtr = new ContentTypeServiceConfigurator("transport.jms.ContentType");
        List<AsyncTestClient<XMLMessage>> clients = new LinkedList<AsyncTestClient<XMLMessage>>();
        clients.add(adapt(bytesMessageClient, MessageEncoder.XML_TO_BYTE));
        clients.add(adapt(textMessageClient, MessageEncoder.XML_TO_STRING));
        clients.add(adapt(new AxisAsyncTestClient(), MessageEncoder.XML_TO_AXIS));
        clients.add(adapt(new AxisAsyncTestClient(new JMSAxisTestClientSetup(JMSConstants.JMS_BYTE_MESSAGE)), MessageEncoder.XML_TO_AXIS));
        clients.add(adapt(new AxisAsyncTestClient(new JMSAxisTestClientSetup(JMSConstants.JMS_TEXT_MESSAGE)), MessageEncoder.XML_TO_AXIS));
        for (JMSTestEnvironment env : new JMSTestEnvironment[] { new QpidTestEnvironment(), new ActiveMQTestEnvironment() }) {
            suite.addPOXTests(new JMSRequestResponseChannel(JMSConstants.DESTINATION_TYPE_QUEUE, JMSConstants.DESTINATION_TYPE_QUEUE, ContentTypeMode.TRANSPORT), adapt(new AxisRequestResponseTestClient(), MessageEncoder.XML_TO_AXIS, MessageDecoder.AXIS_TO_XML), echoEndpoint, env, tdf);
            suite.addPOXTests(new JMSRequestResponseChannel(JMSConstants.DESTINATION_TYPE_QUEUE, JMSConstants.DESTINATION_TYPE_QUEUE, ContentTypeMode.TRANSPORT), adapt(new AxisRequestResponseTestClient(), MessageEncoder.XML_TO_AXIS, MessageDecoder.AXIS_TO_XML), new MockEchoEndpoint(), env, tdf);
            for (String destinationType : new String[] { JMSConstants.DESTINATION_TYPE_QUEUE, JMSConstants.DESTINATION_TYPE_TOPIC }) {
                for (ContentTypeMode contentTypeMode : ContentTypeMode.values()) {
                    JMSAsyncChannel channel = new JMSAsyncChannel(destinationType, contentTypeMode);
                    for (AsyncTestClient<XMLMessage> client : clients) {
                        if (contentTypeMode == ContentTypeMode.TRANSPORT) {
                            suite.addSOAPTests(channel, client, adapt(new AxisAsyncEndpoint(), MessageDecoder.AXIS_TO_XML), env, tdf);
                            suite.addPOXTests(channel, client, adapt(new AxisAsyncEndpoint(), MessageDecoder.AXIS_TO_XML), env, tdf);
                        } else {
                            // If no content type header is used, SwA can't be used and the JMS transport
                            // always uses the default charset encoding
                            suite.addSOAP11Test(channel, client, adapt(new AxisAsyncEndpoint(), MessageDecoder.AXIS_TO_XML), new MessageTestData(null, TransportTestSuite.testString,
                                    MessageContext.DEFAULT_CHAR_SET_ENCODING), env, tdf, cfgtr);
                            suite.addSOAP12Test(channel, client, adapt(new AxisAsyncEndpoint(), MessageDecoder.AXIS_TO_XML), new MessageTestData(null, TransportTestSuite.testString,
                                    MessageContext.DEFAULT_CHAR_SET_ENCODING), env, tdf, cfgtr);
                            suite.addPOXTest(channel, client, adapt(new AxisAsyncEndpoint(), MessageDecoder.AXIS_TO_XML), new MessageTestData(null, TransportTestSuite.testString,
                                    MessageContext.DEFAULT_CHAR_SET_ENCODING), env, tdf, cfgtr);
                        }
                    }
                    if (contentTypeMode == ContentTypeMode.TRANSPORT) {
                        suite.addSwATests(channel, adapt(bytesMessageClient, MessageEncoder.XML_TO_BYTE), adapt(asyncEndpoint, MessageDecoder.AXIS_TO_XML), env, tdf);
                        suite.addBinaryTest(channel, bytesMessageClient, adapt(asyncEndpoint, MessageDecoder.AXIS_TO_BYTE), env, tdf);
                    } else {
                        suite.addBinaryTest(channel, bytesMessageClient, adapt(asyncEndpoint, MessageDecoder.AXIS_TO_BYTE), env, tdf, cfgtr);
                    }
                    // TODO: these tests are temporarily disabled because of SYNAPSE-304
                    // addTextPlainTests(strategy, suite);
                }
            }
            suite.addTest(new MinConcurrencyTest(AxisServer.INSTANCE, new AsyncChannel[] {
                    new JMSAsyncChannel("endpoint1", JMSConstants.DESTINATION_TYPE_QUEUE, ContentTypeMode.TRANSPORT),
                    new JMSAsyncChannel("endpoint2", JMSConstants.DESTINATION_TYPE_QUEUE, ContentTypeMode.TRANSPORT) },
                    2, false, env, tdf));
        }
        return suite;
    }
}
