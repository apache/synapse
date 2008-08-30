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
import org.apache.synapse.transport.testkit.message.MessageDecoder;
import org.apache.synapse.transport.testkit.message.MessageEncoder;
import org.apache.synapse.transport.testkit.message.XMLMessage;
import org.apache.synapse.transport.testkit.server.axis2.AxisAsyncEndpoint;
import org.apache.synapse.transport.testkit.server.axis2.ContentTypeServiceConfigurator;
import org.apache.synapse.transport.testkit.tests.async.LargeSOAPAsyncMessageTestCase;

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
        VFSTestEnvironment env = new VFSTestEnvironment(new File("target/vfs3"));
        AxisAsyncEndpoint asyncEndpoint = new AxisAsyncEndpoint();
        VFSFileChannel channel = new VFSFileChannel("req/in");
        VFSClient vfsClient = new VFSClient();
        List<AsyncTestClient<XMLMessage>> clients = new LinkedList<AsyncTestClient<XMLMessage>>();
        clients.add(adapt(vfsClient, MessageEncoder.XML_TO_BYTE));
        clients.add(adapt(new AxisAsyncTestClient(), MessageEncoder.XML_TO_AXIS));
        ContentTypeServiceConfigurator cfgtr = new ContentTypeServiceConfigurator("transport.vfs.ContentType");
        for (AsyncTestClient<XMLMessage> client : clients) {
            suite.addSOAPTests(channel, client, adapt(new AxisAsyncEndpoint(), MessageDecoder.AXIS_TO_XML), env, tdf, cfgtr);
            suite.addPOXTests(channel, client, adapt(new AxisAsyncEndpoint(), MessageDecoder.AXIS_TO_XML), env, tdf, cfgtr);
            // Since VFS has no Content-Type header, SwA is not supported.
        }
        suite.addTextPlainTests(channel, adapt(vfsClient, MessageEncoder.STRING_TO_BYTE), adapt(asyncEndpoint, MessageDecoder.AXIS_TO_STRING), env, tdf, cfgtr);
        suite.addBinaryTest(channel, vfsClient, adapt(asyncEndpoint, MessageDecoder.AXIS_TO_BYTE), env, tdf, cfgtr);
        // Regression test for SYNAPSE-423:
        suite.addTest(new LargeSOAPAsyncMessageTestCase(channel, adapt(vfsClient, MessageEncoder.XML_TO_BYTE), adapt(new AxisAsyncEndpoint(), MessageDecoder.AXIS_TO_XML), env, tdf, cfgtr));
//        suite.addTest(new MinConcurrencyTest(server, new AsyncChannel[] { new VFSFileChannel("req/in1"), new VFSFileChannel("req/in2") }, 1, true, env, tdf));
        return suite;
    }
}
