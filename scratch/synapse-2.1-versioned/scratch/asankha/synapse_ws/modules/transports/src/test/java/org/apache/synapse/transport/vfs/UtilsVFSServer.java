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

import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.synapse.transport.UtilsTransportServer;

import java.util.ArrayList;
import java.util.List;
import java.io.File;

/**
 * A VFS enabled Axis2 server implementation for unit testing
 */
public class UtilsVFSServer extends UtilsTransportServer {

    public void start() throws Exception {

        TransportOutDescription trpOutDesc =
            new TransportOutDescription(VFSTransportListener.TRANSPORT_NAME);
        trpOutDesc.setSender(new VFSTransportSender());

        TransportInDescription trpInDesc =
            new TransportInDescription(VFSTransportListener.TRANSPORT_NAME);
        trpInDesc.setReceiver(new VFSTransportListener());
        super.start(trpInDesc, trpOutDesc);

        // create a temp directory for us to poll for the sample service
        makeCleanPath("./target/vfs1/req");
        makeCleanPath("./target/vfs1/res");

        makeCleanPath("./target/vfs2/req");
        makeCleanPath("./target/vfs2/res");

        // Service1 - polls target/vfs1/req/request.xml, and writes the response to
        // target/vfs1/res folder and deletes request on success. Polls every 2 secs
        List parameters = new ArrayList();
        parameters.add(new Parameter("transport.vfs.FileURI",
            "vfs:file://" + new File(".").getAbsolutePath() + File.separator + "target/vfs1/req"));
        parameters.add(new Parameter("transport.vfs.FileNamePattern", "request.xml"));
        parameters.add(new Parameter("transport.vfs.ReplyFileURI",
            "vfs:file://" + new File(".").getAbsolutePath() + File.separator + "target/vfs1/res"));
        parameters.add(new Parameter("transport.vfs.ContentType", "text/xml"));
        parameters.add(new Parameter("transport.PollInterval", "2"));

        parameters.add(new Parameter("transport.vfs.ActionAfterProcess", "DELETE"));
        deployEchoService("Service1", parameters);

        // Service2 - polls target/vfs2/req/requests.jar, and writes the response to
        // target/vfs/res folder and deletes request on success. Polls every 2 secs
        parameters = new ArrayList();
        parameters.add(new Parameter("transport.vfs.FileURI",
            "vfs:file://" + new File(".").getAbsolutePath() + File.separator + "target/vfs2/req/requests.jar"));
        //parameters.add(new Parameter("transport.vfs.FileNamePattern", "request.xml"));
        parameters.add(new Parameter("transport.vfs.ReplyFileURI",
            "vfs:file://" + new File(".").getAbsolutePath() + File.separator + "target/vfs2/res"));
        parameters.add(new Parameter("transport.vfs.ContentType", "text/xml"));
        parameters.add(new Parameter("transport.PollInterval", "2"));

        parameters.add(new Parameter("transport.vfs.ActionAfterProcess", "DELETE"));
        deployEchoService("Service2", parameters);
    }

}
