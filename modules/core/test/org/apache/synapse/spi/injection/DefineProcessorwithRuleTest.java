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
 */
package org.apache.synapse.spi.injection;

import junit.framework.TestCase;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseMessage;
import org.apache.synapse.mediators.base.ListMediator;
import org.apache.synapse.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.axis2.Axis2SynapseMessage;
import org.apache.synapse.util.Axis2EnvSetup;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axiom.om.OMElement;

import java.util.List;


public class DefineProcessorwithRuleTest extends TestCase {
    private SynapseEnvironment env;
    private OMElement config;
    private MessageContext mc;
    private String synapsexml =
            "<synapse xmlns=\"http://ws.apache.org/ns/synapse\">\n" +
                    "<refdefine ref=\"test_define\"/>\n" +
                    "<refdefine ref=\"test_define_addressing\"/>\n" +
                    "<define name=\"test_define\">\n" +
                    "    <log/>" +
                    "    <regex message-address=\"to\" pattern=\"http://xmethods..\\*\"/>\n" +
                    "</define>\n" +

                    "<define name=\"test_define_addressing\">\n" +
                    "    <engage-addressing-in/>" +
                    "</define>\n" +
            "</synapse>";
    public void setUp() throws Exception{
        mc = Axis2EnvSetup.axis2Deployment("target/synapse-repository");
        mc.setTo(new EndpointReference("http://xmethods.org"));
        config = Axis2EnvSetup.getSynapseConfigElement(synapsexml);
        env = new Axis2SynapseEnvironment(config,
                Thread.currentThread().getContextClassLoader());
    }

    public void testRegexProcessor() throws Exception {
        SynapseMessage smc = new Axis2SynapseMessage(mc);
        env.injectMessage(smc);
        //assertEquals("test_define", env.lookupMediator("test_define").getName());
        //assertEquals("test_define_addressing", env.lookupMediator("test_define_addressing").getName());
        List embeddedProcessors = ((ListMediator)env.lookupMediator("test_define")).getList();
        assertEquals(2,embeddedProcessors.size());
        ListMediator masterMediator = (ListMediator)env.getMasterMediator();
        List masterMediatorList = masterMediator.getList();
        assertEquals(4,masterMediatorList.size());
    }

}
