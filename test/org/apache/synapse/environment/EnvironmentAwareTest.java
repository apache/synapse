package org.apache.synapse.environment;

import org.apache.axis2.transport.http.SimpleHTTPServer;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;

import org.apache.axis2.client.ServiceClient;
import org.apache.synapse.util.Axis2EvnSetup;
import junit.framework.TestCase;

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

public class EnvironmentAwareTest extends TestCase {
    private SimpleHTTPServer synapseServer;
    private EndpointReference targetEpr = new EndpointReference(
            "http://127.0.0.1:5043/axis2/services/anonymous");

    public void setUp() throws Exception {
        synapseServer = new SimpleHTTPServer(
                "target/synapse-repository-environmentaware",
                5043);
        synapseServer.start();
    }

    protected void tearDown() throws Exception {
        synapseServer.stop();
    }

    public void testSendProcessor() throws Exception {
        // this test case throws exceptions if fail
        // exceptions are propergated from Synapes Server
        ServiceClient serviceClient = new ServiceClient(
                Axis2EvnSetup.createConfigurationContextFromFileSystem(
                        "target/synapse-repository-environmentaware"), null);
        Options options = new Options();
        options.setTo(targetEpr);
        serviceClient.setOptions(options);
        serviceClient.fireAndForget(Axis2EvnSetup.payloadNamedAdddressing());

    }

}
