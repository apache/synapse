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
import org.apache.synapse.transport.testkit.message.MessageDecoder;
import org.apache.synapse.transport.testkit.message.MessageEncoder;
import org.apache.synapse.transport.testkit.server.AsyncEndpoint;

public class VFSTransportSenderTest extends TestCase {
    public static TestSuite suite() {
        TransportTestSuite suite = new TransportTestSuite(VFSTransportSenderTest.class, false);
        
        VFSTestEnvironment env = new VFSTestEnvironment(new File("target/vfs4"));
        TransportDescriptionFactory tdf =
            new SimpleTransportDescriptionFactory("vfs", VFSTransportListener.class,
                    VFSTransportSender.class);
        
        AsyncEndpoint<byte[]> endpoint = new VFSMockAsyncEndpoint();
        
        VFSFileChannel channel = new VFSFileChannel("req/in");
        AxisAsyncTestClient client = new AxisAsyncTestClient();
        
        suite.addBinaryTest(channel, adapt(client, MessageEncoder.BINARY_WRAPPER), endpoint, env, tdf);
        suite.addTextPlainTests(channel, adapt(client, MessageEncoder.TEXT_WRAPPER), adapt(endpoint, MessageDecoder.BYTE_TO_STRING), env, tdf);
        
        return suite;
    }
}
