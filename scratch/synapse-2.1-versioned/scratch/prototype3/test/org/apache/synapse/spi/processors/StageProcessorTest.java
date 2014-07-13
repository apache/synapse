package org.apache.synapse.spi.processors;

import junit.framework.TestCase;
import org.apache.synapse.processors.StageProcessor;
import org.apache.synapse.processors.rules.RegexProcessor;
import org.apache.synapse.processors.rules.XPathProcessor;
import org.apache.synapse.SynapseMessage;
import org.apache.synapse.util.Axis2EvnSetup;
import org.apache.synapse.axis2.Axis2SynapseMessage;

import java.util.List;
import java.util.LinkedList;
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

public class StageProcessorTest extends TestCase {
    public void testStageProcessor() throws Exception {
        SynapseMessage sm = new Axis2SynapseMessage(
                Axis2EvnSetup.axis2Deployment("target/synapse-repository"));
        StageProcessor pro = new StageProcessor();
        boolean result = pro.process(null, sm);
        assertTrue(result);

        List list = new LinkedList();
        list.add(new RegexProcessor());
        list.add(new XPathProcessor());
        pro.setList(list);

        boolean ret = pro.process(null,sm);

        assertTrue(ret);

    }
}
