package org.apache.synapse.ruleEngines;

import java.util.List;
import java.util.Iterator;
import java.util.LinkedList;

import org.apache.axis2.om.OMAttribute;
import org.apache.axis2.om.OMElement;
import org.apache.synapse.Constants;
import org.apache.synapse.MediatorConfiguration;
import org.apache.synapse.MediatorConfigurator;
import org.apache.synapse.MediatorTypes;
import org.apache.synapse.SynapseException;

public class Rule {
	private RuleCondition rc = null;
	private List mcs = new LinkedList();
	
	
	public void init(OMElement ruleContainer, ClassLoader cl) {
		Iterator it = ruleContainer.getChildrenWithName(Constants.MEDIATOR_Q);
		
		
		while (it.hasNext()) {
			OMElement med = (OMElement)it.next(); 
			OMAttribute type = med.getAttribute(Constants.TYPE_ATT_Q);
			if (type==null) throw new SynapseException("no type declaration on "+med.toString());
			MediatorConfigurator config = MediatorTypes.getMediatorConfigurator(type.getAttributeValue());
			if (config!=null) {
				MediatorConfiguration mc = config.parse(med, cl);
				if (mc!=null) {
					mcs.add(mc);
				}
				else throw new SynapseException("failed to parse mediator component"+med.toString());
			}
			else throw new SynapseException("could not find mediator configurator for type "+type.getAttributeValue());
			
			
		}
		
		
		// deal with QoS apply elements here TODO
	}
	
	//public QoS getQoS();
	
	public List getMediatorConfigurations() {
		return mcs;
	}
	
	public RuleCondition getRuleCondition() {
		return rc;
	}
	public void setRuleCondition(RuleCondition rc) {
		this.rc = rc;
	}

}
