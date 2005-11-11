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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.axis2.context.SOAPMessageContext;
import org.apache.axis2.om.OMElement;
import org.apache.synapse.MediatorConfiguration;

import org.apache.synapse.RuleEngine;


import org.apache.synapse.SynapseEnvironment;

// This implements a class of rule engine. These rule engines have a specific
// behaviour which is to
// 
public abstract class OnceRuleEngine implements RuleEngine {

	private List rules = new LinkedList();

	public abstract RuleCondition getRuleCondition(OMElement om);

	public abstract QName getRuleQName();

	public void init(OMElement om) {
		Iterator it = om.getChildrenWithName(getRuleQName());
		while (it.hasNext()) {
			OMElement rule = (OMElement) it.next();
			Rule ra = new Rule();
			ra.init(rule);
			ra.setRuleCondition(getRuleCondition(om));
			rules.add(ra);

		}

	}

	public boolean process(SynapseEnvironment se, SOAPMessageContext smc) {
		Iterator it = rules.iterator();
		while (it.hasNext()) {

			Rule ra = (Rule) it.next();
			RuleCondition rc = ra.getRuleCondition();
			if (rc.matches(smc)) {
				MediatorConfiguration mc = ra.getMediatorConfiguration();
				boolean ret = se.executeMediator(mc, smc);
				if (!ret)
					return false;
			}
		}
		return true;
	}

}
