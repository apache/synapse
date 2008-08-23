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
import org.apache.synapse.transport.testkit.TestEnvironment;
import org.apache.synapse.transport.testkit.TransportDescriptionFactory;
import org.apache.synapse.transport.testkit.TransportTestSuite;
import org.apache.synapse.transport.testkit.client.AsyncTestClient;
import org.apache.synapse.transport.testkit.client.axis2.AxisAsyncTestClient;
import org.apache.synapse.transport.testkit.listener.AsyncChannel;
import org.apache.synapse.transport.testkit.listener.ContentTypeMode;
import org.apache.synapse.transport.testkit.message.MessageConverter;
import org.apache.synapse.transport.testkit.message.XMLMessage;
import org.apache.synapse.transport.testkit.server.axis2.AxisAsyncEndpointFactory;
import org.apache.synapse.transport.testkit.server.axis2.AxisServer;
import org.apache.synapse.transport.testkit.tests.misc.MinConcurrencyTest;

public class HttpCoreNIOListenerTest extends TestCase {
    public static TestSuite suite() {
        // TODO: temporary hack, remove later
        TestEnvironment env = new TestEnvironment() {};
        
        TransportTestSuite suite = new TransportTestSuite();
        
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
        
        AxisServer axisServer = new AxisServer();
        AxisAsyncEndpointFactory asyncEndpointFactory = new AxisAsyncEndpointFactory();
        HttpChannel channel = new HttpChannel();
        JavaNetClient javaNetClient = new JavaNetClient();
        List<AsyncTestClient<XMLMessage>> clients = new LinkedList<AsyncTestClient<XMLMessage>>();
        clients.add(adapt(javaNetClient, MessageConverter.XML_TO_BYTE));
        clients.add(adapt(new AxisAsyncTestClient(), MessageConverter.XML_TO_AXIS));
        for (AsyncTestClient<XMLMessage> client : clients) {
            suite.addSOAPTests(channel, client, asyncEndpointFactory, ContentTypeMode.TRANSPORT, env, axisServer, tdf);
            suite.addPOXTests(channel, client, asyncEndpointFactory, ContentTypeMode.TRANSPORT, env, axisServer, tdf);
        }
//        suite.addPOXTests(channel, new AxisRequestResponseMessageSender(), ContentTypeMode.TRANSPORT);
        suite.addSwATests(channel, javaNetClient, asyncEndpointFactory, env, axisServer, tdf);
        suite.addTextPlainTests(channel, adapt(javaNetClient, MessageConverter.STRING_TO_BYTE), adapt(asyncEndpointFactory, MessageConverter.AXIS_TO_STRING), ContentTypeMode.TRANSPORT, env, axisServer, tdf);
        suite.addBinaryTest(channel, javaNetClient, adapt(asyncEndpointFactory, MessageConverter.AXIS_TO_BYTE), ContentTypeMode.TRANSPORT, env, axisServer, tdf);
        suite.addRESTTests(channel, new JavaNetRESTClient(), asyncEndpointFactory, env, axisServer, tdf);
        suite.addTest(new MinConcurrencyTest(axisServer, new AsyncChannel[] { new HttpChannel(), new HttpChannel() }, 2, false, env, tdf));
        return suite;
    }
}
