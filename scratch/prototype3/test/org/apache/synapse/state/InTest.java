package org.apache.synapse.state;

import junit.framework.TestCase;
import org.apache.axis2.context.MessageContext;
import org.apache.synapse.util.Axis2EvnSetup;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseMessage;
import org.apache.synapse.Constants;
import org.apache.synapse.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.axis2.Axis2SOAPMessageContext;
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

public class InTest extends TestCase {
    private MessageContext msgCtx;
    private String synapsexml =
            "<synapse xmlns=\"http://ws.apache.org/ns/synapse\">\n" +
                    "<in>" +
                    "    <ref ref=\"add\"/>\n" +
                    "</in>\n" +
                    "<never>\n"+
                        "<stage name=\"add\">\n"+
                            "<addressing/>\n" +
                        "</stage>\n"+
                    "</never>\n" +
            "</synapse>\n";

    public void setUp() throws Exception {
        msgCtx = Axis2EvnSetup.axis2Deployment("target/synapse-repository");
    }

    public void testAddressingProcessor() throws Exception {
        SynapseEnvironment env = new Axis2SynapseEnvironment(
                Axis2EvnSetup.getSynapseConfigElement(synapsexml),
                Thread.currentThread().getContextClassLoader());
        SynapseMessage smc = new Axis2SOAPMessageContext(msgCtx);
        env.injectMessage(smc);
        assertTrue(((Boolean) smc.getProperty(
                Constants.MEDIATOR_RESPONSE_PROPERTY)).booleanValue());
    }

}
