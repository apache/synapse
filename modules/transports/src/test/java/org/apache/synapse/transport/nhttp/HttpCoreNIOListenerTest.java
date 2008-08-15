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
import org.apache.synapse.transport.testkit.listener.AsyncMessageSender;
import org.apache.synapse.transport.testkit.listener.AxisAsyncMessageSender;
import org.apache.synapse.transport.testkit.listener.ContentTypeMode;
import org.apache.synapse.transport.testkit.message.MessageConverter;
import org.apache.synapse.transport.testkit.message.XMLMessage;
import org.apache.synapse.transport.testkit.server.axis2.AxisServer;

public class HttpCoreNIOListenerTest extends TestCase {
    public static TestSuite suite() {
        TransportTestSuite<TestEnvironment> suite = new TransportTestSuite<TestEnvironment>();
        
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
        
        AxisServer<TestEnvironment> axisServer = new AxisServer<TestEnvironment>(tdf);
        HttpChannel channel = new HttpChannel();
        JavaNetSender javaNetSender = new JavaNetSender();
        List<AsyncMessageSender<TestEnvironment,? super HttpChannel,XMLMessage>> senders = new LinkedList<AsyncMessageSender<TestEnvironment,? super HttpChannel,XMLMessage>>();
        senders.add(adapt(javaNetSender, MessageConverter.XML_TO_BYTE));
        senders.add(new AxisAsyncMessageSender(tdf));
        for (AsyncMessageSender<TestEnvironment,? super HttpChannel,XMLMessage> sender : senders) {
            suite.addSOAPTests(null, channel, sender, axisServer, ContentTypeMode.TRANSPORT);
            suite.addPOXTests(null, channel, sender, axisServer, ContentTypeMode.TRANSPORT);
        }
//        suite.addPOXTests(channel, new AxisRequestResponseMessageSender(), ContentTypeMode.TRANSPORT);
        suite.addSwATests(null, channel, javaNetSender, axisServer);
        suite.addTextPlainTests(null, channel, adapt(javaNetSender, MessageConverter.STRING_TO_BYTE), adapt(axisServer, MessageConverter.AXIS_TO_STRING), ContentTypeMode.TRANSPORT);
        suite.addBinaryTest(null, channel, javaNetSender, adapt(axisServer, MessageConverter.AXIS_TO_BYTE), ContentTypeMode.TRANSPORT);
        suite.addRESTTests(null, channel, new JavaNetRESTSender(), axisServer);
        return suite;
    }
}
