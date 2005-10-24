package org.apache.synapse;

import org.apache.axis2.context.MessageContext;

import java.util.HashMap;
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


/**
 * To store rules corresponding to the current message
 */
public class RuleObject {

    private HashMap ruls = new HashMap();

    public void addRules(String ruleId, Mediators rule) {
        ruls.put(ruleId,rule);
    }

    public Mediators processRule(MessageContext context) {
        //todo requird QOS has to handle here
        String pid =(String)context.getProperty(SynapseConstant.PRIVOUS_RULE_ID);
        if(pid == null){
            //todo do requird QOS
            Mediators mediators = (Mediators) ruls.get("Rule1");
            context.setProperty(SynapseConstant.CURRENT_RULE_ID,"Rule1");
            return mediators;
        } else if ("Rule1".equals(pid)){
            //todo do requird QOS
            Mediators mediators = (Mediators) ruls.get("Rule2");
            context.setProperty(SynapseConstant.CURRENT_RULE_ID,"Rule2");
            return mediators;
        } else {
           return null;
        }
    }
}
