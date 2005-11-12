package org.apache.synapse.ruleEngines;

import javax.xml.namespace.QName;

import org.apache.axis2.context.SOAPMessageContext;
import org.apache.axis2.om.OMElement;


public class AllRuleEngine extends OnceRuleEngine {
	private static final QName ALL_RULE_Q = new QName(
			"http://ws.apache.org/ns/synapse/allrule", "rule");

	public static final RuleCondition always = new RuleCondition() {
		public boolean matches(SOAPMessageContext smc) {
			return true;
		}

	};

	public RuleCondition getRuleCondition(OMElement om) {
		return always;
	}

	public QName getRuleQName() {
		return ALL_RULE_Q;
	}

	public String getRulesetType() {
		return "all";
		
	}

}
