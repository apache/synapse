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
import org.apache.synapse.transport.testkit.message.MessageDecoder;
import org.apache.synapse.transport.testkit.message.MessageEncoder;
import org.apache.synapse.transport.testkit.message.XMLMessage;
import org.apache.synapse.transport.testkit.server.axis2.AxisAsyncEndpoint;
import org.apache.synapse.transport.testkit.server.axis2.AxisEchoEndpoint;
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
        
        AxisAsyncEndpoint asyncEndpoint = new AxisAsyncEndpoint();
        MailChannel channel = new MailChannel();
        suite.addPOXTests(channel, adapt(new MailRequestResponseClient(new FlatLayout()), MessageEncoder.XML_TO_BYTE, MessageDecoder.BYTE_TO_XML), new AxisEchoEndpoint(), env);
        List<MailAsyncClient> clients = new LinkedList<MailAsyncClient>();
        clients.add(new MailAsyncClient(new FlatLayout()));
        clients.add(new MailAsyncClient(new MultipartLayout()));
        for (MailAsyncClient client : clients) {
            AsyncTestClient<XMLMessage> xmlClient = adapt(client, MessageEncoder.XML_TO_BYTE);
            suite.addSOAPTests(channel, xmlClient, adapt(new AxisAsyncEndpoint(), MessageDecoder.AXIS_TO_XML), env);
            suite.addPOXTests(channel, xmlClient, adapt(new AxisAsyncEndpoint(), MessageDecoder.AXIS_TO_XML), env);
            suite.addSwATests(channel, xmlClient, adapt(asyncEndpoint, MessageDecoder.AXIS_TO_XML), env);
            suite.addTextPlainTests(channel, adapt(client, MessageEncoder.STRING_TO_BYTE), AdapterUtils.adapt(asyncEndpoint, MessageDecoder.AXIS_TO_STRING), env);
            suite.addBinaryTest(channel, client, adapt(asyncEndpoint, MessageDecoder.AXIS_TO_BYTE), env);
        }
        AxisAsyncTestClient axisClient = new AxisAsyncTestClient();
        suite.addSOAPTests(channel, adapt(axisClient, MessageEncoder.XML_TO_AXIS), adapt(new AxisAsyncEndpoint(), MessageDecoder.AXIS_TO_XML), env);
        suite.addPOXTests(channel, adapt(axisClient, MessageEncoder.XML_TO_AXIS), adapt(new AxisAsyncEndpoint(), MessageDecoder.AXIS_TO_XML), env);
        suite.addTextPlainTests(channel, adapt(axisClient, MessageEncoder.TEXT_WRAPPER), AdapterUtils.adapt(asyncEndpoint, MessageDecoder.AXIS_TO_STRING), env);
        suite.addBinaryTest(channel, adapt(axisClient, MessageEncoder.BINARY_WRAPPER), adapt(asyncEndpoint, MessageDecoder.AXIS_TO_BYTE), env);
        suite.addTest(new MinConcurrencyTest(AxisServer.INSTANCE, new MailChannel[] { new MailChannel(), new MailChannel() }, 2, true, env));
        return suite;
    }
}
