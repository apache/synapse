package org.apache.synapse.spi.injection;

import junit.framework.TestCase;
import org.apache.axis2.context.MessageContext;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseMessage;
import org.apache.synapse.mediators.types.axis2.ServiceMediator;
import org.apache.synapse.xml.ServiceMediatorFactory;
import org.apache.synapse.api.Mediator;
import org.apache.synapse.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.axis2.Axis2SynapseMessage;
import org.apache.synapse.util.Axis2EnvSetup;
import org.apache.axiom.om.OMElement;
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

public class ServiceMediatorProcessorWithRuleTest extends TestCase {
    private MessageContext msgCtx;
    private SynapseEnvironment env;
    private OMElement config;
    private String synapsexml =
            "<synapse xmlns=\"http://ws.apache.org/ns/synapse\">\n" +
                    "<stage name=\"test-service-mediator\">\n" +
                    "    <servicemediator name=\"service-mediator\" service=\"test-mediator\"/>\n" +
                    "</stage>\n" +
                    "</synapse>";

    public void setUp() throws Exception {
        msgCtx = Axis2EnvSetup.axis2Deployment("target/synapse-repository-sendonAxis2");
        config =Axis2EnvSetup.getSynapseConfigElement(synapsexml);
        env = new Axis2SynapseEnvironment(config,
                Thread.currentThread().getContextClassLoader());
    }

    public void testAxis2MediatorProcessor() throws Exception {

        SynapseMessage smc = new Axis2SynapseMessage(msgCtx);
        env.injectMessage(smc);
        assertNotNull(env.lookupMediator("service-mediator"));
    }

    public void testAxis2MediatorConfigurator() throws Exception {
        ServiceMediatorFactory fac = new ServiceMediatorFactory();

        Mediator med = fac.createMediator(env,config.getFirstElement().getFirstElement());
        assertTrue(med instanceof ServiceMediator);
        //assertNotNull(pro.getName());
    }
}
