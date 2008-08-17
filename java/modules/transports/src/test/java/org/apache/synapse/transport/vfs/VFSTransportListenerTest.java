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
import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.synapse.transport.testkit.SimpleTransportDescriptionFactory;
import org.apache.synapse.transport.testkit.TransportDescriptionFactory;
import org.apache.synapse.transport.testkit.TransportTestSuite;
import org.apache.synapse.transport.testkit.client.AsyncTestClient;
import org.apache.synapse.transport.testkit.client.axis2.AxisAsyncTestClient;
import org.apache.synapse.transport.testkit.listener.ContentTypeMode;
import org.apache.synapse.transport.testkit.message.MessageConverter;
import org.apache.synapse.transport.testkit.message.XMLMessage;
import org.apache.synapse.transport.testkit.server.axis2.AxisAsyncEndpointFactory;
import org.apache.synapse.transport.testkit.server.axis2.AxisServer;

/**
 * TransportListenerTestTemplate implementation for the VFS transport.
 */
public class VFSTransportListenerTest extends TestCase {
    public static TestSuite suite() {
        // TODO: the VFS listener doesn't like reuseServer == true...
        TransportTestSuite suite = new TransportTestSuite(false);
        TransportDescriptionFactory tdf =
            new SimpleTransportDescriptionFactory("vfs", VFSTransportListener.class,
                    VFSTransportSender.class);
        VFSTestEnvironment env = new VFSTestEnvironment();
        AxisServer server = new AxisServer(tdf);
        AxisAsyncEndpointFactory asyncEndpointFactory = new AxisAsyncEndpointFactory(server);
        VFSFileChannel channel = new VFSFileChannel(new File("target/vfs3/req/in").getAbsoluteFile());
        VFSClient vfsClient = new VFSClient();
        List<AsyncTestClient<XMLMessage>> clients = new LinkedList<AsyncTestClient<XMLMessage>>();
        clients.add(adapt(vfsClient, MessageConverter.XML_TO_BYTE));
        clients.add(new AxisAsyncTestClient(tdf));
        for (AsyncTestClient<XMLMessage> client : clients) {
            suite.addSOAPTests(env, channel, client, asyncEndpointFactory, ContentTypeMode.SERVICE);
            suite.addPOXTests(env, channel, client, asyncEndpointFactory, ContentTypeMode.SERVICE);
            // Since VFS has no Content-Type header, SwA is not supported.
        }
        suite.addTextPlainTests(env, channel, adapt(vfsClient, MessageConverter.STRING_TO_BYTE), adapt(asyncEndpointFactory, MessageConverter.AXIS_TO_STRING), ContentTypeMode.SERVICE);
        suite.addBinaryTest(env, channel, vfsClient, adapt(asyncEndpointFactory, MessageConverter.AXIS_TO_BYTE), ContentTypeMode.SERVICE);
        return suite;
    }
}
