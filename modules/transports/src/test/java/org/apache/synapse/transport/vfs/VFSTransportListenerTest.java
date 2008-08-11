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
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.synapse.transport.testkit.TransportTestSuite;
import org.apache.synapse.transport.testkit.listener.AbstractMessageSender;
import org.apache.synapse.transport.testkit.listener.AsyncMessageSender;
import org.apache.synapse.transport.testkit.listener.AxisAsyncMessageSender;
import org.apache.synapse.transport.testkit.listener.ContentTypeMode;
import org.apache.synapse.transport.testkit.listener.SenderOptions;
import org.apache.synapse.transport.testkit.message.ByteArrayMessage;
import org.apache.synapse.transport.testkit.message.MessageConverter;
import org.apache.synapse.transport.testkit.message.XMLMessage;
import org.apache.synapse.transport.testkit.server.axis2.AxisServer;

/**
 * TransportListenerTestTemplate implementation for the VFS transport.
 */
public class VFSTransportListenerTest extends TestCase {
    private static class MessageSenderImpl extends AbstractMessageSender<VFSFileChannel> implements AsyncMessageSender<VFSFileChannel,ByteArrayMessage> {
        public void sendMessage(VFSFileChannel channel, SenderOptions options, ByteArrayMessage message) throws Exception {
            OutputStream out = new FileOutputStream(channel.getRequestFile());
            out.write(message.getContent());
            out.close();
        }
    }
    
    public static TestSuite suite() {
        // TODO: the VFS listener doesn't like reuseServer == true...
        TransportTestSuite<VFSTestEnvironment> suite = new TransportTestSuite<VFSTestEnvironment>(false);
        VFSTestEnvironment env = new VFSTestEnvironment();
        AxisServer<VFSTestEnvironment> server = new AxisServer<VFSTestEnvironment>(env);
        VFSFileChannel channel = new VFSFileChannel(new File("target/vfs3/req/in").getAbsoluteFile());
        MessageSenderImpl vfsSender = new MessageSenderImpl();
        List<AsyncMessageSender<? super VFSFileChannel,XMLMessage>> senders = new LinkedList<AsyncMessageSender<? super VFSFileChannel,XMLMessage>>();
        senders.add(adapt(vfsSender, MessageConverter.XML_TO_BYTE));
        senders.add(new AxisAsyncMessageSender());
        for (AsyncMessageSender<? super VFSFileChannel,XMLMessage> sender : senders) {
            suite.addSOAPTests(env, channel, sender, server, ContentTypeMode.SERVICE);
            suite.addPOXTests(env, channel, sender, server, ContentTypeMode.SERVICE);
            // Since VFS has no Content-Type header, SwA is not supported.
        }
        suite.addTextPlainTests(env, channel, adapt(vfsSender, MessageConverter.STRING_TO_BYTE), adapt(server, MessageConverter.AXIS_TO_STRING), ContentTypeMode.SERVICE);
        suite.addBinaryTest(env, channel, vfsSender, adapt(server, MessageConverter.AXIS_TO_BYTE), ContentTypeMode.SERVICE);
        return suite;
    }
}
