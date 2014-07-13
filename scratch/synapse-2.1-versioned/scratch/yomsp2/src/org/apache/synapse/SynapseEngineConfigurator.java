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

package org.apache.synapse;


import java.util.HashMap;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.axis2.om.OMAttribute;
import org.apache.axis2.om.OMElement;

import org.apache.axis2.om.OMNode;
import org.apache.synapse.ruleEngines.RuleEngineTypes;


public class SynapseEngineConfigurator {

	
	public static void parse(SynapseEngine se, OMElement om) {
		List rulesets = new LinkedList();

		RuleEngine[] inphase = null, outphase = null;

		Map ruleSetNames = new HashMap();

		boolean inoutseparate = false;
		String inPhaseNames = null, outPhaseNames = null;
		if (!om.getLocalName().equals(Constants.SYNAPSE)
				|| !om.getNamespace().getName().equals(
						Constants.SYNAPSE_NAMESPACE)) {
			throw new SynapseException("OMElement is not of namespace "
					+ Constants.SYNAPSE_NAMESPACE + "or not localname "
					+ Constants.SYNAPSE);
		}

		OMNode node = om.getFirstOMChild();
		while (node != null) {
			System.out.println(node.toString());
			if (node.getType() != OMNode.ELEMENT_NODE) {
				node = node.getNextOMSibling();
				continue;
			} // loop thru Elements only

			OMElement el = (OMElement) node;

			if (!el.getNamespace().getName().toLowerCase().equals(
					Constants.SYNAPSE_NAMESPACE)) {
				node = node.getNextOMSibling();
				continue;
			} // ignore non-synapse elements

			if (el.getLocalName().toLowerCase().equals(Constants.STAGE)) {
				
				OMAttribute attr = el
						.getAttribute(Constants.RULE_TYPE_ATT_Q);
				if (attr==null) throw new SynapseException("no "+Constants.RULE_TYPE_ATT_Q+" attribute on element "+el.toString());
				String type = attr.getAttributeValue();
				RuleEngine re = RuleEngineTypes.getRuleEngine(type);
				if (re != null) {
					re.init(el, se.getSynapseEnvironment().getClassLoader());
				}
				rulesets.add(re);
				OMAttribute name = el
						.getAttribute(Constants.STAGE_NAME_ATT_Q);
				if (name != null) {
					ruleSetNames.put(name.getAttributeValue(), re);
				}
			} else if (el.getLocalName().toLowerCase()
					.equals(Constants.IN)) {
				OMAttribute attr = el
						.getAttribute(Constants.IN_ORDER_ATTR_Q);
				if (attr != null) {
					inPhaseNames = attr.getAttributeValue();
				}
			} else if (el.getLocalName().equals(Constants.OUT)) {
				OMAttribute attr = el
						.getAttribute(Constants.OUT_ORDER_ATTR_Q);
				if (attr != null) {
					outPhaseNames = attr.getAttributeValue();
				}

			}
			node = node.getNextOMSibling();
		}
		if (inPhaseNames != null || outPhaseNames != null) {
			inoutseparate = true;
			String inOrder[] = parsePhaseOrder(inPhaseNames);
			inphase = new RuleEngine[inOrder.length];
			for (int i = 0; i < inOrder.length; i++) {
				RuleEngine r = (RuleEngine) ruleSetNames.get(inOrder[i]);
				if (r == null) {
					throw new SynapseException(
							"missing ruleset specified in inPhase order: "
									+ inOrder[i]);
				} else
					inphase[i] = r;
			}
			String outOrder[] = parsePhaseOrder(outPhaseNames);
			outphase = new RuleEngine[outOrder.length];
			for (int i = 0; i < outOrder.length; i++) {
				RuleEngine r = (RuleEngine) ruleSetNames.get(outOrder[i]);
				if (r == null) {
					throw new SynapseException(
							"missing ruleset specified in outPhase order: "
									+ outOrder[i]);
				} else
					outphase[i] = r;
			}
		} else {
			inphase = new RuleEngine[rulesets.size()];
			for (int i=0;i<inphase.length;i++) {
				inphase[i] = (RuleEngine)rulesets.get(i);
			}
			
			
		}
		se.setInoutseparate(inoutseparate);
		se.setInphase(inphase);
		se.setOutphase(outphase);
		System.out.println("configured");
		
	}	

	private static String[] parsePhaseOrder(String order) {
		return order.trim().toLowerCase().split("[ \t\n\f\r]+");
	}
}
