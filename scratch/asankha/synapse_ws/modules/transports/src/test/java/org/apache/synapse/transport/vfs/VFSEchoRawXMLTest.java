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

import org.apache.axis2.Constants;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.synapse.transport.AbstractTransportTest;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.VFS;

import java.io.File;

public class VFSEchoRawXMLTest extends AbstractTransportTest {

    public VFSEchoRawXMLTest() {
        server = new UtilsVFSServer();
    }

    public void testXMLFileInDirectory() throws Exception {

        Options options = new Options();
        options.setTo(
            new EndpointReference("vfs:file:" + new File(".").getAbsolutePath() +
                File.separator + "target/vfs1/req/request.xml"));
        options.setAction(Constants.AXIS2_NAMESPACE_URI + "/echoOMElementNoResponse");

        ServiceClient sender = new ServiceClient(getClientCfgCtx(), null);
        sender.setOptions(options);
        sender.fireAndForget(createPayload());

        Thread.yield();
        Thread.sleep(1000 * 5);

        File req = new File("./target/vfs1/req/request.xml");
        if (req.exists()) {
            fail("Request file still exists. Not processed by service? : " + req.getPath());
        }

        File res = new File("target/vfs1/res/response.xml");
        if (!res.exists()) {
            fail("Response file not created : " + res.getPath());
        }
    }

    /*public void testXMLFilesInJAR() throws Exception {

        Options options = new Options();
        options.setTo(
            new EndpointReference("vfs:jar:" + new File(".").getAbsolutePath() +
                File.separator + "target/vfs2/req/requests.jar!request.xml"));
        options.setAction(Constants.AXIS2_NAMESPACE_URI + "/echoOMElementNoResponse");

        ServiceClient sender = new ServiceClient(getClientCfgCtx(), null);
        sender.setOptions(options);
        sender.fireAndForget(createPayload());

        Thread.yield();
        Thread.sleep(1000 * 5 * 60);

        File req = new File("./target/vfs2/req/requests.jar");
        if (req.exists()) {
            fail("Request file still exists. Not processed by service? : " + req.getPath());
        }

        File res = new File("target/vfs2/res/response.xml");
        if (!res.exists()) {
            fail("Response file not created : " + res.getPath());
        }
    }*/

    /*public void testVFS() throws Exception {
        FileSystemManager fsManager = VFS.getManager();
        FileObject jarFile = fsManager.createFileSystem(
            fsManager.resolveFile("jar:/tmp/aJarFile.jar"));
        System.out.println("dc");
    }*/

    /**
     * Create a axis2 configuration context that 'knows' about the VFS transport
     * @return
     * @throws Exception
     */
    public ConfigurationContext getClientCfgCtx() throws Exception {
        AxisConfiguration axisCfg = new AxisConfiguration();
        TransportOutDescription trpOutDesc = new TransportOutDescription("vfs");
        VFSTransportSender trpSender = new VFSTransportSender();
        trpOutDesc.setSender(trpSender);
        axisCfg.addTransportOut(trpOutDesc);
        ConfigurationContext cfgCtx = new ConfigurationContext(axisCfg);

        trpSender.init(cfgCtx, trpOutDesc);
        return cfgCtx;
    }
}
