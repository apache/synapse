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

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.transport.http.CommonsHTTPTransportSender;
import org.apache.axis2.transport.http.SimpleHTTPServer;
import org.apache.synapse.transport.testkit.SimpleTransportDescriptionFactory;
import org.apache.synapse.transport.testkit.TransportDescriptionFactory;
import org.apache.synapse.transport.testkit.TransportTestSuite;
import org.apache.synapse.transport.testkit.TransportTestSuiteBuilder;
import org.apache.synapse.transport.testkit.client.axis2.AxisAsyncTestClient;
import org.apache.synapse.transport.testkit.client.axis2.AxisRequestResponseTestClient;
import org.apache.synapse.transport.testkit.listener.AsyncChannel;
import org.apache.synapse.transport.testkit.server.axis2.AxisAsyncEndpoint;
import org.apache.synapse.transport.testkit.server.axis2.AxisEchoEndpoint;
import org.apache.synapse.transport.testkit.server.axis2.AxisServer;
import org.apache.synapse.transport.testkit.tests.misc.MinConcurrencyTest;

public class HttpCoreNIOListenerTest extends TestCase {
    public static TestSuite suite() throws Exception {
        TransportTestSuite suite = new TransportTestSuite(HttpCoreNIOListenerTest.class);
        
        // These tests don't work because of a problem similar to SYNAPSE-418
        suite.addExclude("(test=EchoXML)");
        
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
        
        TransportTestSuiteBuilder builder = new TransportTestSuiteBuilder(suite);
        
        builder.addEnvironment(tdf);
        
        HttpChannel channel = new HttpChannel();
        
        builder.addAsyncChannel(channel);
        
        builder.addByteArrayAsyncTestClient(new JavaNetClient());
        builder.addAxisAsyncTestClient(new AxisAsyncTestClient(), new HttpAxisTestClientSetup(false));
        builder.addAxisAsyncTestClient(new AxisAsyncTestClient(), new HttpAxisTestClientSetup(true));
        builder.addRESTAsyncTestClient(new JavaNetRESTClient());
        
        builder.addAxisAsyncEndpoint(new AxisAsyncEndpoint());
        builder.addByteArrayAsyncEndpoint(new JettyAsyncEndpoint());
        
        builder.addRequestResponseChannel(channel);
        
        builder.addAxisRequestResponseTestClient(new AxisRequestResponseTestClient());
        
        builder.addEchoEndpoint(new AxisEchoEndpoint());
        
        builder.build();
        
        suite.addTest(new MinConcurrencyTest(AxisServer.INSTANCE, new AsyncChannel[] { new HttpChannel(), new HttpChannel() }, 2, false, tdf));
        return suite;
    }
}
