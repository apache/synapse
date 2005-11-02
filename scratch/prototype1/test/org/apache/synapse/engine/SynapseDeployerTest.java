package org.apache.synapse.engine;

import junit.framework.TestCase;

import java.io.File;

import org.apache.synapse.rule.RuleSelectorImpl;
import org.apache.synapse.rule.Rule;

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

public class SynapseDeployerTest extends TestCase {

    public void testSynapseDeployer() throws Exception {
        String file = "/home/saminda/myprojects/scratch-synapse/scratch/prototype1/src/org/apache/synapse/engine";
        File in = new File (file,"synapse.xml");
        SynapseDeployer deployer = new SynapseDeployer(in);

        SynapseConfiguration config = deployer.populteConfig();
        System.out.println(config.getIncomingPreStageRuleSet());
        System.out.println(config.getIncomingProcessingStageRuleSet().toString());
        System.out.println(config.getIncomingPostStageRuleSet().toString());

        System.out.println(config.getOutgoingPreStageRuleSet().toString());
        System.out.println(config.getOutgoingProcessingStageRuleSet());
        System.out.println(config.getOutgoingPostStageRuleSet());

        RuleSelectorImpl selectorImpl = new RuleSelectorImpl();
        selectorImpl.init(config.getIncomingPreStageRuleSet());

        Rule[] rules = selectorImpl.getRules();
        for (int i = 0 ; i < rules.length ; i++) {
            Rule pertRule = rules[i];
            System.out.println(pertRule.getName());
            System.out.println(pertRule.getMediators());

        }



    }

}
