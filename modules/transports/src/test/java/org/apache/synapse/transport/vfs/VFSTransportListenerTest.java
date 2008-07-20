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
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestSuite;

import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportInDescription;
import org.apache.synapse.transport.TransportListenerTestTemplate;

/**
 * TransportListenerTestTemplate implementation for the VFS transport.
 */
public class VFSTransportListenerTest extends TransportListenerTestTemplate {
    public static class TestStrategyImpl extends TestStrategy {
        private final File requestFile = new File("target/vfs3/req/in").getAbsoluteFile();
        
        @Override
        protected TransportInDescription createTransportInDescription() {
            TransportInDescription trpInDesc =
                new TransportInDescription(VFSTransportListener.TRANSPORT_NAME);
            trpInDesc.setReceiver(new VFSTransportListener());
            return trpInDesc;
        }
        
        @Override
        protected void beforeStartup() throws Exception {
            requestFile.getParentFile().mkdirs();
            requestFile.delete();
        }
        
        @Override
        protected List<Parameter> getServiceParameters(String contentType) throws Exception {
            List<Parameter> parameters = new ArrayList<Parameter>();
            parameters.add(new Parameter("transport.vfs.FileURI", "vfs:" + requestFile.toURL()));
            parameters.add(new Parameter("transport.vfs.ContentType", contentType));
            parameters.add(new Parameter("transport.PollInterval", "1"));
            parameters.add(new Parameter("transport.vfs.ActionAfterProcess", "DELETE"));
            return parameters;
        }
        
        @Override
        protected void sendMessage(String endpointReference, String contentType, byte[] content) throws Exception {
            OutputStream out = new FileOutputStream("target/vfs3/req/in");
            out.write(content);
            out.close();
        }
    }
    
    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        TestStrategy strategy = new TestStrategyImpl();
        addSOAP11Tests(strategy, suite);
        addPOXTests(strategy, suite);
        // Since VFS has no Content-Type header, SwA is not supported.
        addTextPlainTests(strategy, suite);
        addBinaryTest(strategy, suite);
        return suite;
    }
}
