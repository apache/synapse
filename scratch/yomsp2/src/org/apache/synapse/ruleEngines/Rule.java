package org.apache.synapse.ruleEngines;

import java.util.Iterator;

import org.apache.axis2.om.OMAttribute;
import org.apache.axis2.om.OMElement;
import org.apache.synapse.Constants;
import org.apache.synapse.MediatorConfiguration;
import org.apache.synapse.MediatorConfigurator;
import org.apache.synapse.MediatorTypes;
import org.apache.synapse.SynapseException;

public class Rule {
	private RuleCondition rc = null;
	private MediatorConfiguration mc = null;
	private int mediatorType = 0; 
	
	public void init(OMElement ruleContainer) {
		Iterator it = ruleContainer.getChildrenWithName(Constants.MEDIATOR_Q);
		if (it.hasNext()) {
			OMElement med = (OMElement)it.next(); // only use first mediator
			OMAttribute type = med.getAttribute(Constants.TYPE_ATT_Q);
			if (type==null) throw new SynapseException("no type declaration on "+med.toString());
			MediatorConfigurator config = MediatorTypes.getMediatorConfigurator(type.getAttributeValue());
			mc = config.parse(med);
			mediatorType = MediatorTypes.getType(type.getAttributeValue());
		}
		// deal with QoS apply elements here TODO
	}
	
	//public QoS getQoS();
	
	public MediatorConfiguration getMediatorConfiguration() {
		return mc;
	}
	public int getMediatorType() { 
		return mediatorType;
	}
	
	
	public RuleCondition getRuleCondition() {
		return rc;
	}
	public void setRuleCondition(RuleCondition rc) {
		this.rc = rc;
	}
	
	

}
