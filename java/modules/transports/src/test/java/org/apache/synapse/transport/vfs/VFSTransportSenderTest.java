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

package org.apache.synapse.transport.vfs;

import static org.apache.synapse.transport.testkit.AdapterUtils.adapt;

import java.io.File;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.synapse.transport.testkit.SimpleTransportDescriptionFactory;
import org.apache.synapse.transport.testkit.TransportDescriptionFactory;
import org.apache.synapse.transport.testkit.TransportTestSuite;
import org.apache.synapse.transport.testkit.client.axis2.AxisAsyncTestClient;
import org.apache.synapse.transport.testkit.listener.ContentTypeMode;
import org.apache.synapse.transport.testkit.message.ByteArrayMessage;
import org.apache.synapse.transport.testkit.message.MessageConverter;
import org.apache.synapse.transport.testkit.server.AsyncEndpoint;
import org.apache.synapse.transport.testkit.server.AsyncEndpointFactory;
import org.apache.synapse.transport.testkit.server.Server;

public class VFSTransportSenderTest extends TestCase {
    public static TestSuite suite() {
        TransportTestSuite<VFSTestEnvironment> suite = new TransportTestSuite<VFSTestEnvironment>();
        
        VFSTestEnvironment env = new VFSTestEnvironment();
        TransportDescriptionFactory tdf =
            new SimpleTransportDescriptionFactory("vfs", VFSTransportListener.class,
                    VFSTransportSender.class);
        
        AsyncEndpointFactory<VFSTestEnvironment,VFSFileChannel,ByteArrayMessage> endpointFactory =
            new AsyncEndpointFactory<VFSTestEnvironment,VFSFileChannel,ByteArrayMessage>() {

            public Server<VFSTestEnvironment> getServer() {
                return null;
            }
            
            public AsyncEndpoint<ByteArrayMessage> createAsyncEndpoint(
                    VFSTestEnvironment env, VFSFileChannel channel, String contentType)
                    throws Exception {
                
                return new VFSMockAsyncEndpoint(channel, contentType);
            }
        };
        
        VFSFileChannel channel = new VFSFileChannel(new File("target/vfs3/req/in").getAbsoluteFile());
        AxisAsyncTestClient client = new AxisAsyncTestClient(tdf);
        
        suite.addBinaryTest(env, channel, adapt(client, MessageConverter.BINARY_WRAPPER), endpointFactory, ContentTypeMode.SERVICE);
        suite.addTextPlainTests(env, channel, adapt(client, MessageConverter.TEXT_WRAPPER), adapt(endpointFactory, MessageConverter.BYTE_TO_STRING), ContentTypeMode.SERVICE);
        
        return suite;
    }
}
