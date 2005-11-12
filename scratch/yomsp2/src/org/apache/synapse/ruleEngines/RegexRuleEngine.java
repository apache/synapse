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
package org.apache.synapse.ruleEngines;

import org.apache.axis2.context.SOAPMessageContext;
import org.apache.axis2.om.OMElement;
import org.apache.synapse.RuleEngine;


import org.apache.synapse.SynapseEnvironment;

public class RegexRuleEngine implements RuleEngine {

	
	// TBD
	public String getRulesetType() {
		return RuleEngineTypes.REGEX;
	}




	public boolean process(SynapseEnvironment sc, SOAPMessageContext smc) {
		// TODO Auto-generated method stub
		return false;
	}

	public void init(OMElement om, ClassLoader cl) {
		// TODO Auto-generated method stub
		
	}

}
