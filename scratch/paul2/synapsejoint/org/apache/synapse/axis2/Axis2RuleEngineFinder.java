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

package org.apache.synapse.axis2;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.ParameterImpl;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.synapse.RuleEngine;

import org.apache.synapse.SynapseException;

public class Axis2RuleEngineFinder {
	private static final String RULE_LIST_XMLFILE = "RuleListXMLFile";

	public static final String RULE_ENGINE = "org.apache.synapse.RuleEngine";
	
	
	public static synchronized RuleEngine getRuleEngine(MessageContext mc) {
		
		AxisConfiguration ac = mc.getSystemContext().getAxisConfiguration();
		Parameter ruleEngineParam = ac.getParameter(RULE_ENGINE);
		if (ruleEngineParam == null) {
			System.out.println("setting rule engine on"+ac.hashCode());
			Parameter param = ac.getParameter(RULE_LIST_XMLFILE);
			if (param == null) {
				throw new SynapseException("no parameter '" + RULE_LIST_XMLFILE
						+ "' in axis2.xml");
			}
			String ruleFile = (String) param.getValue();
			RuleEngine re = new RuleEngine(ruleFile);
			ruleEngineParam = new ParameterImpl(RULE_ENGINE, null);
			ruleEngineParam.setValue(re);
			try {
				ac.addParameter(ruleEngineParam);
			} catch (AxisFault e) {
				throw new SynapseException(e);
			}
		}
		RuleEngine ruleEngine = (RuleEngine) ruleEngineParam.getValue();
		return ruleEngine;
	}


}
