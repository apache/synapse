package org.apache.synapse.spi.injection;

import junit.framework.TestCase;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.synapse.util.Axis2EvnSetup;
import org.apache.synapse.SynapseMessage;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.axis2.Axis2SynapseMessage;
import org.apache.synapse.axis2.Axis2SynapseEnvironment;
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

public class XpathProcessorWithRuleTest extends TestCase {
     private String synapsexml =
            "<synapse xmlns=\"http://ws.apache.org/ns/synapse\">\n" +
                    "<stage name=\"xpath\">\n" +
                    "    <xpath expr=\"//ns:text\" xmlns:ns=\"urn:text-body\"/>\n" +
                    "</stage>\n" +
            "</synapse>";
    public void testXpathProcessor() throws Exception {
        MessageContext mc = Axis2EvnSetup.axis2Deployment("target/synapse-repository");
        SynapseMessage smc = new Axis2SynapseMessage(mc);
        SynapseEnvironment env = new Axis2SynapseEnvironment(
                Axis2EvnSetup.getSynapseConfigElement(synapsexml),
                Thread.currentThread().getContextClassLoader());
        env.injectMessage(smc);
        assertEquals("xpath",env.lookupProcessor("xpath").getName());
    }
}


