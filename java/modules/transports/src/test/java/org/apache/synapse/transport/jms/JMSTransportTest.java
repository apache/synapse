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

import junit.framework.TestCase;
import junit.framework.TestSuite;

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

public class JMSTransportTest extends TestCase {
    public static TestSuite suite() throws Exception {
        TransportTestSuite suite = new TransportTestSuite(JMSTransportTest.class);
        
        // SwA doesn't make sense with text messages
        suite.addExclude("(&(test=AsyncSwA)(client=TextMessage))");
        // SYNAPSE-304:
        suite.addExclude("(&(test=AsyncTextPlain)(client=BytesMessage))");
        
        TransportTestSuiteBuilder builder = new TransportTestSuiteBuilder(suite);

        TransportDescriptionFactory tdf = new JMSTransportDescriptionFactory();
        JMSTestEnvironment[] environments = new JMSTestEnvironment[] { new QpidTestEnvironment(), new ActiveMQTestEnvironment() };
        
        for (JMSTestEnvironment env : environments) {
            builder.addEnvironment(env, tdf);
        }
        
        builder.addAsyncChannel(new JMSAsyncChannel(JMSConstants.DESTINATION_TYPE_QUEUE, ContentTypeMode.TRANSPORT));
        builder.addAsyncChannel(new JMSAsyncChannel(JMSConstants.DESTINATION_TYPE_TOPIC, ContentTypeMode.TRANSPORT));
        
        builder.addAxisAsyncTestClient(new AxisAsyncTestClient());
        builder.addAxisAsyncTestClient(new AxisAsyncTestClient(new JMSAxisTestClientSetup(JMSConstants.JMS_BYTE_MESSAGE)));
        builder.addAxisAsyncTestClient(new AxisAsyncTestClient(new JMSAxisTestClientSetup(JMSConstants.JMS_TEXT_MESSAGE)));
        builder.addByteArrayAsyncTestClient(new JMSBytesMessageClient());
        builder.addStringAsyncTestClient(new JMSTextMessageClient());
        
        builder.addAxisAsyncEndpoint(new AxisAsyncEndpoint());
        
        builder.addRequestResponseChannel(new JMSRequestResponseChannel(JMSConstants.DESTINATION_TYPE_QUEUE, JMSConstants.DESTINATION_TYPE_QUEUE, ContentTypeMode.TRANSPORT));
        
        builder.addAxisRequestResponseTestClient(new AxisRequestResponseTestClient());
        
        builder.addEchoEndpoint(new MockEchoEndpoint());
        builder.addEchoEndpoint(new AxisEchoEndpoint());
        
        for (JMSTestEnvironment env : new JMSTestEnvironment[] { new QpidTestEnvironment(), new ActiveMQTestEnvironment() }) {
            suite.addTest(new MinConcurrencyTest(AxisServer.INSTANCE, new AsyncChannel[] {
                    new JMSAsyncChannel("endpoint1", JMSConstants.DESTINATION_TYPE_QUEUE, ContentTypeMode.TRANSPORT),
                    new JMSAsyncChannel("endpoint2", JMSConstants.DESTINATION_TYPE_QUEUE, ContentTypeMode.TRANSPORT) },
                    2, false, env, tdf));
        }
        
        
        builder.build();
        
        return suite;
    }
}
