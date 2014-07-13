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
package org.apache.synapse.spi.injection;

import junit.framework.TestCase;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.util.Axis2EnvSetup;
import org.apache.synapse.axis2.Axis2SynapseEnvironment;


public class BuiltInProcessorWithRuleTest extends TestCase {

    private String synapsexml =
            "<synapse xmlns=\"http://ws.apache.org/ns/synapse\">\n" +
                    "<stage name=\"logall\">\n" +
                    "    <log/>\n" +
                    "</stage>\n"+
             "</synapse>";
    private SynapseEnvironment env;

    public void testSynapseEnvironment() throws Exception {
        env = new Axis2SynapseEnvironment(
                Axis2EnvSetup.getSynapseConfigElement(synapsexml),
                Thread.currentThread().getContextClassLoader());
        assertNotNull(env.getMasterProcessor());
     }

}
