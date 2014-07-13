package org.apache.synapse.ruleEngines;

import java.util.Iterator;

import javax.xml.namespace.QName;


import org.apache.axis2.om.OMAttribute;
import org.apache.axis2.om.OMElement;
import org.apache.axis2.om.OMNamespace;
import org.apache.axis2.om.xpath.AXIOMXPath;

import org.apache.synapse.Constants;

import org.apache.synapse.SynapseException;
import org.apache.synapse.api.SOAPMessageContext;
import org.jaxen.JaxenException;

public class XPathRuleEngine extends OnceRuleEngine {
	private static final String XPATH="xpath";
	private static final QName XPATH_RULE_Q = new QName(
			Constants.SYNAPSE_NAMESPACE, "rule");
	private static final QName XPATH_CONDITION_ATT_Q = new QName(XPATH); 

	public RuleCondition getRuleCondition(OMElement om) {
		OMAttribute xpath=om.getAttribute(XPATH_CONDITION_ATT_Q);
		if (xpath==null) {
			throw new SynapseException("rule must have xpath attribute: "+om.toString());
		}
		RuleCondition rc = null;
		try {
			
			rc = new XPathRuleCondition(om.getAllDeclaredNamespaces(), xpath.getAttributeValue().trim());
		}
		catch (JaxenException e) {
			throw new SynapseException("Problem with xpath expression "+xpath.getAttributeValue(), e);
		}
		return rc;
	}

	public QName getRuleQName() {
		return XPATH_RULE_Q;
	}

	protected class XPathRuleCondition implements RuleCondition {
		private AXIOMXPath xp = null;
		protected XPathRuleCondition(Iterator namespaces, String xpath) throws JaxenException {
			
			this.xp = new AXIOMXPath(xpath);
			while (namespaces!=null && namespaces.hasNext()) {
				OMNamespace n = (OMNamespace)namespaces.next();
				xp.addNamespace(n.getPrefix(),n.getName());
			}
		}
		
		public boolean matches(SOAPMessageContext smc) {
			try {
				return xp.booleanValueOf(smc.getEnvelope());
			} catch (JaxenException e) {
				throw new SynapseException ("Problem trying to evaluate XPATH "+xp.getRootExpr()+" on "+smc.getEnvelope().toString(),e);
				
			}
		}
		public String toString() { return "xpath: "+xp.getRootExpr().getText(); }
	}

	public String getRulesetType() {
		
		return XPATH;
	}
	
}
