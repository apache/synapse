package org.apache.synapse.spi.injection;

import junit.framework.TestCase;
import org.apache.axis2.transport.http.SimpleHTTPServer;
import org.apache.axis2.transport.tcp.TCPServer;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.client.Options;
import org.apache.axis2.om.OMElement;
import org.apache.synapse.util.Axis2EvnSetup;
/*
* Copyright 2004,2005 The Apache Software Foundation.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*
*/

public class HTTPToTCPMessageMediationTest extends TestCase {
    private SimpleHTTPServer synapseServer;
    private TCPServer axis2TcpServer;
    private EndpointReference targetEpr = new EndpointReference(
            "http://127.0.0.1:5043/axis2/services/anonymous");

    public void setUp() throws Exception {
        synapseServer =
                new SimpleHTTPServer("target/synapse-repository-httptcp",
                        5043);
        /**
         * axis2TcpServer is the one who holds the actual service
         */
        axis2TcpServer =
                new TCPServer(8090, "target/synapse-repository-sendonAxis2Tcp");
        synapseServer.start();
        axis2TcpServer.start();
    }

    protected void tearDown() throws Exception {
        synapseServer.stop();
        axis2TcpServer.stop();
    }

    public void testSendProcessor() throws Exception {
        ServiceClient serviceClient = new ServiceClient(
                Axis2EvnSetup.getConfigurationContextFromFileSystem(
                        "target/synapse-repository-httptcp"), null);
        Options options = new Options();
        options.setTo(targetEpr);
        serviceClient.setOptions(options);
        OMElement response = serviceClient.sendReceive(Axis2EvnSetup.payload());
        assertEquals("Synapse Testing String_Response", response.getText());

    }

}
