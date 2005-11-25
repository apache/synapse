package org.apache.synapse.spi;

import junit.framework.TestCase;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.MessageContext;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseMessage;
import org.apache.synapse.axis2.Axis2SOAPMessageContext;
import org.apache.synapse.axis2.Axis2SynapseEnvironment;
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

public class RegexProcessorTest extends TestCase {

    private String synapsexml =
            "<synapse xmlns=\"http://ws.apache.org/ns/synapse\">\n" +
                    "<stage name=\"regex\">\n" +
                    "    <regex message-address=\"to\" pattern=\"http://xmethods..\\*\"/>\n" +
                    "</stage>\n" +
            "</synapse>";

    public void testRegexProcessor() throws Exception {
        MessageContext mc = Axis2EvnSetup.axis2Deployment("target/synapse-repository");
        mc.setTo(new EndpointReference("http://xmethods.org"));
        SynapseMessage smc = new Axis2SOAPMessageContext(mc);
        SynapseEnvironment env = new Axis2SynapseEnvironment(
                Axis2EvnSetup.getSynapseConfigElement(synapsexml),
                Thread.currentThread().getContextClassLoader());
        env.injectMessage(smc);

    }
}
