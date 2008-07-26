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

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.synapse.transport.testkit.listener.AxisMessageSender;
import org.apache.synapse.transport.testkit.listener.ContentTypeMode;
import org.apache.synapse.transport.testkit.listener.ListenerTestSetup;
import org.apache.synapse.transport.testkit.listener.ListenerTestSuite;
import org.apache.synapse.transport.testkit.listener.BinaryPayloadSender;
import org.apache.synapse.transport.testkit.listener.XMLMessageSender;

/**
 * TransportListenerTestTemplate implementation for the VFS transport.
 */
public class VFSTransportListenerTest extends TestCase {
    public static class TestStrategyImpl extends ListenerTestSetup {
        private final File requestFile = new File("target/vfs3/req/in").getAbsoluteFile();
        
        @Override
        public TransportInDescription createTransportInDescription() {
            TransportInDescription trpInDesc =
                new TransportInDescription(VFSTransportListener.TRANSPORT_NAME);
            trpInDesc.setReceiver(new VFSTransportListener());
            return trpInDesc;
        }
        
        @Override
        public TransportOutDescription createTransportOutDescription() throws Exception {
            TransportOutDescription trpOutDesc =
                new TransportOutDescription(VFSTransportSender.TRANSPORT_NAME);
            trpOutDesc.setSender(new VFSTransportSender());
            return trpOutDesc;
        }

        @Override
        public void beforeStartup() throws Exception {
            requestFile.getParentFile().mkdirs();
            requestFile.delete();
        }
        
        @Override
        public void setupService(AxisService service) throws Exception {
            service.addParameter("transport.vfs.FileURI", "vfs:" + requestFile.toURL());
            service.addParameter("transport.PollInterval", "1");
            service.addParameter("transport.vfs.ActionAfterProcess", "DELETE");
        }

        @Override
        public void setupContentType(AxisService service, String contentType) throws Exception {
            service.addParameter("transport.vfs.ContentType", contentType);
        }
    }
    
    private static class MessageSenderImpl extends BinaryPayloadSender {
        @Override
        public void sendMessage(ListenerTestSetup setup, String endpointReference, String contentType, byte[] content) throws Exception {
            OutputStream out = new FileOutputStream("target/vfs3/req/in");
            out.write(content);
            out.close();
        }
    }
    
    public static TestSuite suite() {
        // TODO: the VFS listener doesn't like reuseServer == true...
        ListenerTestSuite suite = new ListenerTestSuite(false);
        ListenerTestSetup setup = new TestStrategyImpl();
        BinaryPayloadSender vfsSender = new MessageSenderImpl();
        for (XMLMessageSender sender : new XMLMessageSender[] { vfsSender, new AxisMessageSender() }) {
            suite.addSOAPTests(setup, sender, ContentTypeMode.SERVICE);
            suite.addPOXTests(setup, sender, ContentTypeMode.SERVICE);
            // Since VFS has no Content-Type header, SwA is not supported.
        }
        suite.addTextPlainTests(setup, vfsSender, ContentTypeMode.SERVICE);
        suite.addBinaryTest(setup, vfsSender, ContentTypeMode.SERVICE);
        return suite;
    }
}
