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

package org.apache.synapse.transport.nhttp;

import static org.apache.synapse.transport.testkit.AdapterUtils.adapt;

import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.transport.http.CommonsHTTPTransportSender;
import org.apache.axis2.transport.http.SimpleHTTPServer;
import org.apache.synapse.transport.testkit.SimpleTransportDescriptionFactory;
import org.apache.synapse.transport.testkit.TransportDescriptionFactory;
import org.apache.synapse.transport.testkit.TransportTestSuite;
import org.apache.synapse.transport.testkit.client.AsyncTestClient;
import org.apache.synapse.transport.testkit.client.axis2.AxisAsyncTestClient;
import org.apache.synapse.transport.testkit.listener.AsyncChannel;
import org.apache.synapse.transport.testkit.message.MessageDecoder;
import org.apache.synapse.transport.testkit.message.MessageEncoder;
import org.apache.synapse.transport.testkit.message.XMLMessage;
import org.apache.synapse.transport.testkit.server.axis2.AxisAsyncEndpoint;
import org.apache.synapse.transport.testkit.server.axis2.AxisServer;
import org.apache.synapse.transport.testkit.tests.misc.MinConcurrencyTest;

public class HttpCoreNIOListenerTest extends TestCase {
    public static TestSuite suite() throws Exception {
        TransportTestSuite suite = new TransportTestSuite();
        
        suite.addExclude("(!(|(client=axis)(endpoint=axis)))");
        
        TransportDescriptionFactory tdfNIO =
            new SimpleTransportDescriptionFactory("http", HttpCoreNIOListener.class, 
                                                  HttpCoreNIOSender.class);
        TransportDescriptionFactory tdfSimple =
            new SimpleTransportDescriptionFactory("http", SimpleHTTPServer.class, 
                                                  CommonsHTTPTransportSender.class) {

            @Override
            public TransportInDescription createTransportInDescription() throws Exception {
                TransportInDescription desc = super.createTransportInDescription();
                desc.addParameter(new Parameter(SimpleHTTPServer.PARAM_PORT, "8888"));
                return desc;
            }
        };
        
        // Change to tdfSimple if you want to check the behavior of Axis' blocking HTTP transport 
        TransportDescriptionFactory tdf = tdfNIO;
        
        AxisAsyncEndpoint asyncEndpoint = new AxisAsyncEndpoint();
        JettyServer jettyServer = new JettyServer();
        JettyAsyncEndpoint jettyAsyncEndpoint = new JettyAsyncEndpoint();
//        AxisEchoEndpointFactory echoEndpointFactory = new AxisEchoEndpointFactory();
        HttpChannel channel = new HttpChannel();
        JavaNetClient javaNetClient = new JavaNetClient();
        List<AsyncTestClient<XMLMessage>> clients = new LinkedList<AsyncTestClient<XMLMessage>>();
        clients.add(adapt(javaNetClient, MessageEncoder.XML_TO_BYTE));
        clients.add(adapt(new AxisAsyncTestClient(new HttpAxisTestClientSetup(false)), MessageEncoder.XML_TO_AXIS));
        clients.add(adapt(new AxisAsyncTestClient(new HttpAxisTestClientSetup(true)), MessageEncoder.XML_TO_AXIS));
        for (AsyncTestClient<XMLMessage> client : clients) {
            suite.addSOAPTests(channel, client, adapt(asyncEndpoint, MessageDecoder.AXIS_TO_XML), tdf);
            suite.addPOXTests(channel, client, adapt(asyncEndpoint, MessageDecoder.AXIS_TO_XML), tdf);
            suite.addSOAPTests(channel, client, adapt(jettyAsyncEndpoint, MessageDecoder.BYTE_TO_XML), jettyServer, tdf);
            suite.addPOXTests(channel, client, adapt(jettyAsyncEndpoint, MessageDecoder.BYTE_TO_XML), jettyServer, tdf);
        }
//        suite.addPOXTests(channel, adapt(new AxisRequestResponseTestClient(), MessageConverter.XML_TO_AXIS, MessageConverter.AXIS_TO_XML), echoEndpointFactory, env, axisServer, tdf);
        suite.addSwATests(channel, javaNetClient, asyncEndpoint, tdf);
        suite.addTextPlainTests(channel, adapt(javaNetClient, MessageEncoder.STRING_TO_BYTE), adapt(asyncEndpoint, MessageDecoder.AXIS_TO_STRING), tdf);
        suite.addBinaryTest(channel, javaNetClient, adapt(asyncEndpoint, MessageDecoder.AXIS_TO_BYTE), tdf);
        suite.addRESTTests(channel, new JavaNetRESTClient(), asyncEndpoint, tdf);
        suite.addTest(new MinConcurrencyTest(AxisServer.INSTANCE, new AsyncChannel[] { new HttpChannel(), new HttpChannel() }, 2, false, tdf));
        return suite;
    }
}
