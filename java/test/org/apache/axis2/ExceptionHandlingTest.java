package org.apache.axis2;

import junit.framework.TestCase;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.om.OMElement;
import org.apache.axis2.transport.http.SimpleHTTPServer;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.axis2.Axis2SynapseMessage;
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

public class ExceptionHandlingTest extends TestCase {
    private MessageContext msgCtx;
    private SynapseEnvironment env;
    private OMElement config;
    private SimpleHTTPServer targetServer;
    private String synapsexml =
            "<synapse xmlns=\"http://ws.apache.org/ns/synapse\">\n" +
                    "<header type=\"to\" value=\"http://localhost:5043/axis2/services/npe\"/>\n" +
                    "<header type=\"action\" value=\"urn:synapse/sendon-fault\"/>\n" +
                    "<stage name=\"testing_stage\">\n" +
                    "    <engage-addressing-in/>\n" +
                    "    <send/>\n " +
                    "</stage>\n" +
                    "</synapse>";

    public void setUp() throws Exception {
        msgCtx = Axis2EvnSetup.axis2Deployment("target/synapse-repository");
        msgCtx.setSoapAction(null);
        config = Axis2EvnSetup.getSynapseConfigElement(synapsexml);
        env = new Axis2SynapseEnvironment(config,
                Thread.currentThread().getContextClassLoader());
        targetServer = new SimpleHTTPServer(
                "target/synapse-repository-sendonAxis2", 5043);
        targetServer.start();
    }

    public void testFaultScenario() {
        try {
            env.injectMessage(new Axis2SynapseMessage(msgCtx));
            fail("Native End Point Throws an Exception");
        } catch (Exception e) {
        }

    }

    protected void tearDown() throws Exception {
        targetServer.stop();
    }

}
