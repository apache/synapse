package org.apache.synapse.impl;

import org.apache.synapse.RuleEngine;
import org.apache.synapse.SynapseConstant;
import org.apache.synapse.Mediators;
import org.apache.synapse.RuleObject;
import org.apache.axis2.context.MessageContext;

import java.util.HashMap;
import java.util.Iterator;
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
* @author : Deepal Jayasinghe (deepal@apache.org)
*
*/

public class SynapseDefaultRuleEngine implements RuleEngine {

    private HashMap ruls = new HashMap();

    public void addRules(String ruleId, Mediators rule) {
        ruls.put(ruleId,rule);
    }

    public RuleObject processRule(MessageContext context) {
        RuleObject ruleObj = new RuleObject();
        //todo this has to modified support , to get the Rules for the
        //current msgContxt
        Iterator keys =  ruls.keySet().iterator();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            ruleObj.addRules(key,(Mediators)ruls.get(key));
        }
        return ruleObj;
    }
}
