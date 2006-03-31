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

package org.apache.synapse.mediators;


import org.apache.axis2.context.MessageContext;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseMessage;
import org.apache.synapse.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.axis2.Axis2SynapseMessage;
import org.apache.synapse.util.Axis2EnvSetup;
import org.apache.axiom.om.OMElement;
import junit.framework.TestCase;


public class MediatorFalseReturnTest extends TestCase {
    private MessageContext msgCtx;
    private SynapseEnvironment env;
    private OMElement config;
    private String synapsexml =
            "<synapse xmlns=\"http://ws.apache.org/ns/synapse\">\n" +
                    "<stage name=\"mediator_false\">\n" +
                    "    <classmediator name=\"mediator_false\" class=\"org.apache.synapse.mediators.SampleFalseReturnMediator1\"/>\n" +
                    "    <log/>" +
                    "</stage>\n" +
                    "<stage name=\"do_loggin\">\n" +
                    "    <classmediator name=\"mediator_false\" class=\"org.apache.synapse.mediators.SampleMediator2\"/>\n" +
                    "</stage>\n" +
                    "</synapse>";

    public void setUp() throws Exception {
        msgCtx = Axis2EnvSetup.axis2Deployment("target/synapse-repository");
        config = Axis2EnvSetup.getSynapseConfigElement(synapsexml);
        env = new Axis2SynapseEnvironment(config,
                Thread.currentThread().getContextClassLoader());
    }

    public void testFalseReturnFromMediator() throws Exception {

        SynapseMessage smc = new Axis2SynapseMessage(msgCtx,env);
        env.injectMessage(smc);
        assertNotNull(env.lookupMediator("mediator_false"));
        assertNull(smc.getProperty("test_string"));
    }

}
