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

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.synapse.transport.testkit.SimpleTransportDescriptionFactory;
import org.apache.synapse.transport.testkit.TransportDescriptionFactory;
import org.apache.synapse.transport.testkit.TransportTestSuite;
import org.apache.synapse.transport.testkit.TransportTestSuiteBuilder;
import org.apache.synapse.transport.testkit.client.axis2.AxisAsyncTestClient;
import org.apache.synapse.transport.testkit.server.axis2.AxisAsyncEndpoint;
import org.apache.synapse.transport.testkit.server.axis2.ContentTypeServiceConfigurator;

/**
 * TransportListenerTestTemplate implementation for the VFS transport.
 */
public class VFSTransportTest extends TestCase {
    public static TestSuite suite() throws Exception {
        // TODO: the VFS listener doesn't like reuseResources == true...
        TransportTestSuite suite = new TransportTestSuite(VFSTransportTest.class, false);
        
        // Since VFS has no Content-Type header, SwA is not supported.
        suite.addExclude("(test=AsyncSwA)");
        
        TransportDescriptionFactory tdf =
            new SimpleTransportDescriptionFactory("vfs", VFSTransportListener.class,
                    VFSTransportSender.class);
        
        TransportTestSuiteBuilder builder = new TransportTestSuiteBuilder(suite);
        
        builder.addEnvironment(new VFSTestEnvironment(new File("target/vfs3")), tdf);
        
        builder.addAsyncChannel(new VFSFileChannel("req/in"));
        
        builder.addAxisAsyncTestClient(new AxisAsyncTestClient());
        builder.addByteArrayAsyncTestClient(new VFSClient());
        
        builder.addAxisAsyncEndpoint(new AxisAsyncEndpoint(), new ContentTypeServiceConfigurator("transport.vfs.ContentType"));
        builder.addByteArrayAsyncEndpoint(new VFSMockAsyncEndpoint());
        
        builder.build();
        
//        suite.addTest(new MinConcurrencyTest(server, new AsyncChannel[] { new VFSFileChannel("req/in1"), new VFSFileChannel("req/in2") }, 1, true, env, tdf));
        return suite;
    }
}
