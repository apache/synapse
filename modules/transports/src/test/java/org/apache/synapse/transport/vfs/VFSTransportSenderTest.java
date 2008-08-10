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

import org.apache.synapse.transport.testkit.listener.AxisAsyncMessageSender;
import org.apache.synapse.transport.testkit.listener.ContentTypeMode;
import org.apache.synapse.transport.testkit.listener.ListenerTestSuite;
import org.apache.synapse.transport.testkit.message.ByteArrayMessage;
import org.apache.synapse.transport.testkit.message.MessageConverter;
import org.apache.synapse.transport.testkit.server.AsyncEndpoint;
import org.apache.synapse.transport.testkit.server.AsyncEndpointFactory;
import org.apache.synapse.transport.testkit.server.DummyServer;

public class VFSTransportSenderTest extends TestCase {
    public static TestSuite suite() {
        // TODO: ListenerTestSuite is inappropriate!
        ListenerTestSuite suite = new ListenerTestSuite();
        
        VFSTestSetup setup = new VFSTestSetup();
        
        AsyncEndpointFactory<VFSFileChannel,ByteArrayMessage> endpointFactory =
            new AsyncEndpointFactory<VFSFileChannel,ByteArrayMessage>() {
            
            public AsyncEndpoint<ByteArrayMessage> createAsyncEndpoint(
                    VFSFileChannel channel, String contentType)
                    throws Exception {
                
                return new VFSMockAsyncEndpoint(channel, contentType);
            }
        };
        
        VFSFileChannel channel = new VFSFileChannel(new DummyServer<VFSTestSetup>(setup), new File("target/vfs3/req/in").getAbsoluteFile());
        AxisAsyncMessageSender sender = new AxisAsyncMessageSender();
        
        suite.addBinaryTest(channel, adapt(sender, MessageConverter.BINARY_WRAPPER), endpointFactory, ContentTypeMode.SERVICE);
        suite.addTextPlainTests(channel, adapt(sender, MessageConverter.TEXT_WRAPPER), adapt(endpointFactory, MessageConverter.BYTE_TO_STRING), ContentTypeMode.SERVICE);
        
        return suite;
    }
}
