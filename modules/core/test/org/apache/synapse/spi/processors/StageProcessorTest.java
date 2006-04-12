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
package org.apache.synapse.spi.processors;

import junit.framework.TestCase;

import org.apache.synapse.mediators.base.StageMediator;
import org.apache.synapse.mediators.filters.RegexMediator;
import org.apache.synapse.mediators.filters.XPathMediator;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseMessage;
import org.apache.synapse.util.Axis2EnvSetup;
import org.apache.synapse.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.axis2.Axis2SynapseMessage;

import java.util.List;
import java.util.LinkedList;

public class StageProcessorTest extends TestCase {
    public void testStageProcessor() throws Exception {
    	SynapseEnvironment env = new Axis2SynapseEnvironment(null,null);
        SynapseMessage sm = new Axis2SynapseMessage(
                Axis2EnvSetup.axis2Deployment("target/synapse-repository"),env);
        StageMediator med = new StageMediator();
        boolean result = med.mediate(sm);
        assertTrue(result);

        List list = new LinkedList();
        list.add(new RegexMediator());
        list.add(new XPathMediator());
        med.setList(list);

        boolean ret = med.mediate(sm);

        assertTrue(ret);

    }
}
