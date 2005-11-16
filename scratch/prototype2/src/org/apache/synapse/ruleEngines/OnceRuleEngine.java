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


import org.apache.axis2.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;



import org.apache.synapse.SynapseException;
import org.apache.synapse.api.MediatorConfiguration;
import org.apache.synapse.api.SOAPMessageContext;
import org.apache.synapse.api.SynapseEnvironment;
import org.apache.synapse.spi.RuleEngine;

// This implements a class of rule engine. These rule engines have a specific
// behaviour which is to
// 
public abstract class OnceRuleEngine implements RuleEngine {

	Log log = LogFactory.getLog(getClass());
	private List rules = new LinkedList();
	

	public abstract RuleCondition getRuleCondition(OMElement om);

	public abstract QName getRuleQName();

	public void init(OMElement om, ClassLoader cl) {
		log.debug("initialising rule engine"+om.toString());
		Iterator it = om.getChildrenWithName(getRuleQName());
		if (!it.hasNext()) { throw new SynapseException("no rules in stage"+om.toString()); }
		while (it.hasNext()) {
			OMElement rule = (OMElement) it.next();
			RuleCondition rc = getRuleCondition(rule);
			Rule ra = new Rule();
			ra.init(rule, cl);
			ra.setRuleCondition(rc);
			rules.add(ra);
		}

	}

	public boolean process(SynapseEnvironment se, SOAPMessageContext smc) {
		log.debug("processing message "+smc.getEnvelope());
		
		Iterator it = rules.iterator();
		while (it.hasNext()) {

			Rule ra = (Rule) it.next();
			RuleCondition rc = ra.getRuleCondition();
			if (rc.matches(smc)) {
				log.info("matched: "+ra.getRuleCondition().toString());
				List medConfigs = ra.getMediatorConfigurations();
				if (medConfigs==null) return true;
				Iterator mcs = medConfigs.iterator();
				while (mcs.hasNext()) {
					MediatorConfiguration mc = (MediatorConfiguration)mcs.next();
					boolean ret = se.executeMediator(mc, smc);
					if (!ret) return false;
				}
				
			}
			else {
				log.info("did NOT match"+ra.getRuleCondition().toString());
			}
		}
		return true;
	}

}
