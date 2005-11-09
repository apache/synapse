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


public class SynapseEngine {
	private List rulesets = new LinkedList();

	private RuleEngine[] inphase = null, outphase = null;

	private Map ruleSetNames = new HashMap();

	boolean inoutseparate = false;

	public SynapseEngine(OMElement om) {
		parse(om);
	}

	public void parse(OMElement om) {
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

			if (node.getType() != OMNode.ELEMENT_NODE) {
				node = node.getNextOMSibling();
				continue;
			} // loop thru Elements only

			OMElement el = (OMElement) node;

			if (el.getNamespace().getName().toLowerCase().equals(
					Constants.SYNAPSE_NAMESPACE)) {
				node = node.getNextOMSibling();
				continue;
			} // ignore non-synapse elements

			if (el.getLocalName().toLowerCase().equals(Constants.RULESET)) {
				OMAttribute attr = el
						.getAttribute(Constants.TYPE_ATT_Q);
				String type = attr.getAttributeValue();
				RuleEngine re = RuleEngineTypes.getRuleEngine(type);
				if (re != null) {
					re.init(el);
				}
				rulesets.add(re);
				OMAttribute name = el
						.getAttribute(Constants.RULESET_NAME_ATT_Q);
				if (name != null) {
					ruleSetNames.put(name.getAttributeValue(), re);
				}
			} else if (el.getLocalName().toLowerCase()
					.equals(Constants.INPHASE)) {
				OMAttribute attr = el
						.getAttribute(Constants.PHASE_ORDER_ATTR_Q);
				if (attr != null) {
					inPhaseNames = attr.getAttributeValue();
				}
			} else if (el.getLocalName().equals(Constants.OUTPHASE)) {
				OMAttribute attr = el
						.getAttribute(Constants.PHASE_ORDER_ATTR_Q);
				if (attr != null) {
					outPhaseNames = attr.getAttributeValue();
				}

			}
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
		
	}	
	

	public void processMessage(SOAPMessageContext smc) {
		SynapseContext sc = getSynapseContext();
		if (inoutseparate && smc.isResponse()) {
				for (int i=0; i<outphase.length;i++) {
					boolean ret = outphase[i].process(sc, smc);
					if (ret) break;
				}
			
		}else {
			for (int i=0; i<outphase.length;i++) {
				boolean ret = outphase[i].process(sc, smc);
				if (ret) break;
			}
		}
	}
	
	
	private SynapseContext getSynapseContext() {
		
		return null;
	}

	private String[] parsePhaseOrder(String order) {
		return order.trim().toLowerCase().split("[ \t\n\f\r]+");
	}
}
