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

package org.apache.synapse.transport.mail;

import static org.apache.synapse.transport.testkit.AdapterUtils.adapt;

import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.synapse.transport.testkit.AdapterUtils;
import org.apache.synapse.transport.testkit.TransportTestSuite;
import org.apache.synapse.transport.testkit.client.AsyncTestClient;
import org.apache.synapse.transport.testkit.client.axis2.AxisAsyncTestClient;
import org.apache.synapse.transport.testkit.listener.ContentTypeMode;
import org.apache.synapse.transport.testkit.message.MessageConverter;
import org.apache.synapse.transport.testkit.message.XMLMessage;
import org.apache.synapse.transport.testkit.server.axis2.AxisAsyncEndpointFactory;
import org.apache.synapse.transport.testkit.server.axis2.AxisEchoEndpointFactory;
import org.apache.synapse.transport.testkit.server.axis2.AxisServer;
import org.apache.synapse.transport.testkit.tests.misc.MinConcurrencyTest;

public class MailTransportListenerTest extends TestCase {
    public static TestSuite suite() throws Exception {
        TransportTestSuite suite = new TransportTestSuite(false);
        
        // TODO: these test don't work; need more analysis why this is so
        suite.addExclude("(&(messageType=SOAP12)(data=Latin1))");
        suite.addExclude("(&(messageType=POX)(data=Latin1))");
        suite.addExclude("(&(layout=multipart)(data=Latin1))");
        suite.addExclude("(&(layout=multipart)(messageType=POX))");
        suite.addExclude("(test=AsyncSwA)");
        suite.addExclude("(test=AsyncBinary)");
        suite.addExclude("(&(test=AsyncTextPlain)(!(data=ASCII)))");
        // SYNAPSE-434
        suite.addExclude("(test=MinConcurrency)");
        
        MailTestEnvironment env = new GreenMailTestEnvironment();
        
        AxisServer axisServer = new AxisServer();
        AxisAsyncEndpointFactory asyncEndpointFactory = new AxisAsyncEndpointFactory();
        MailChannel channel = new MailChannel();
        suite.addPOXTests(channel, adapt(new MailRequestResponseClient(new FlatLayout()), MessageConverter.XML_TO_BYTE, MessageConverter.BYTE_TO_XML), new AxisEchoEndpointFactory(), ContentTypeMode.TRANSPORT, env, axisServer);
        List<MailAsyncClient> clients = new LinkedList<MailAsyncClient>();
        clients.add(new MailAsyncClient(new FlatLayout()));
        clients.add(new MailAsyncClient(new MultipartLayout()));
        for (MailAsyncClient client : clients) {
            AsyncTestClient<XMLMessage> xmlClient = adapt(client, MessageConverter.XML_TO_BYTE);
            suite.addSOAPTests(channel, xmlClient, asyncEndpointFactory, ContentTypeMode.TRANSPORT, env, axisServer);
            suite.addPOXTests(channel, xmlClient, asyncEndpointFactory, ContentTypeMode.TRANSPORT, env, axisServer);
            suite.addSwATests(channel, client, asyncEndpointFactory, env, axisServer);
            suite.addTextPlainTests(channel, adapt(client, MessageConverter.STRING_TO_BYTE), AdapterUtils.adapt(asyncEndpointFactory, MessageConverter.AXIS_TO_STRING), ContentTypeMode.TRANSPORT, env, axisServer);
            suite.addBinaryTest(channel, client, adapt(asyncEndpointFactory, MessageConverter.AXIS_TO_BYTE), ContentTypeMode.TRANSPORT, env, axisServer);
        }
        AxisAsyncTestClient axisClient = new AxisAsyncTestClient();
        suite.addSOAPTests(channel, adapt(axisClient, MessageConverter.XML_TO_AXIS), asyncEndpointFactory, ContentTypeMode.TRANSPORT, env, axisServer);
        suite.addPOXTests(channel, adapt(axisClient, MessageConverter.XML_TO_AXIS), asyncEndpointFactory, ContentTypeMode.TRANSPORT, env, axisServer);
        suite.addTextPlainTests(channel, adapt(axisClient, MessageConverter.TEXT_WRAPPER), AdapterUtils.adapt(asyncEndpointFactory, MessageConverter.AXIS_TO_STRING), ContentTypeMode.TRANSPORT, env, axisServer);
        suite.addBinaryTest(channel, adapt(axisClient, MessageConverter.BINARY_WRAPPER), adapt(asyncEndpointFactory, MessageConverter.AXIS_TO_BYTE), ContentTypeMode.TRANSPORT, env, axisServer);
        suite.addTest(new MinConcurrencyTest(axisServer, new MailChannel[] { new MailChannel(), new MailChannel() }, 2, true, env));
        return suite;
    }
}
