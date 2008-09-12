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

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.synapse.transport.testkit.TransportTestSuite;
import org.apache.synapse.transport.testkit.TransportTestSuiteBuilder;
import org.apache.synapse.transport.testkit.client.axis2.AxisAsyncTestClient;
import org.apache.synapse.transport.testkit.server.axis2.AxisAsyncEndpoint;
import org.apache.synapse.transport.testkit.server.axis2.AxisEchoEndpoint;
import org.apache.synapse.transport.testkit.server.axis2.AxisServer;
import org.apache.synapse.transport.testkit.tests.misc.MinConcurrencyTest;

public class MailTransportListenerTest extends TestCase {
    public static TestSuite suite() throws Exception {
        TransportTestSuite suite = new TransportTestSuite(MailTransportListenerTest.class, false);
        
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
        
        TransportTestSuiteBuilder builder = new TransportTestSuiteBuilder(suite);
        
        GreenMailTestEnvironment env = new GreenMailTestEnvironment();
        
        builder.addEnvironment(env);
        
        MailChannel channel = new MailChannel();
        
        builder.addAsyncChannel(channel);
        
        builder.addAxisAsyncTestClient(new AxisAsyncTestClient(), new MailAxisTestClientSetup(MailConstants.TRANSPORT_FORMAT_TEXT));
        builder.addAxisAsyncTestClient(new AxisAsyncTestClient(), new MailAxisTestClientSetup(MailConstants.TRANSPORT_FORMAT_MP));
        builder.addByteArrayAsyncTestClient(new MailAsyncClient(new FlatLayout()));
        builder.addByteArrayAsyncTestClient(new MailAsyncClient(new MultipartLayout()));
        
        builder.addAxisAsyncEndpoint(new AxisAsyncEndpoint());
        
        builder.addRequestResponseChannel(channel);
        
        builder.addByteArrayRequestResponseTestClient(new MailRequestResponseClient(new FlatLayout()));
        builder.addByteArrayRequestResponseTestClient(new MailRequestResponseClient(new MultipartLayout()));
        
        builder.addEchoEndpoint(new AxisEchoEndpoint());
        
        builder.build();
        
        suite.addTest(new MinConcurrencyTest(AxisServer.INSTANCE, new MailChannel[] { new MailChannel(), new MailChannel() }, 2, true, env));
        return suite;
    }
}
