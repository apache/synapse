package org.apache.synapse;

import java.util.Iterator;

import org.apache.axis2.om.OMElement;
import org.apache.axis2.om.xpath.AXIOMXPath;
import org.jaxen.JaxenException;

public class SimpleDispatcher {


	private RuleList rl = null;

	private MediatorFinder finder = null;
	
	
	public void setMediatorFinder(MediatorFinder mf) {
		finder = mf;
	}

	public SimpleDispatcher(RuleList rl, MediatorFinder mf) {
		this.setRuleList(rl);
		this.setMediatorFinder(mf);
	}



	public void execute(OMElement message) throws JaxenException{
		Iterator iterator = rl.iterator();
		while (iterator.hasNext()) {
				Rule r = (Rule) iterator.next();
				AXIOMXPath xp = new AXIOMXPath(r.getXpath());
				if (xp.booleanValueOf(message)) {
					Mediator m = finder.getMediator(r.getMediatorName());
					boolean cont = m.mediate(message);
					if (!cont) return;
				}
		}
		// send now
		System.out.println("sending");
	}

	private void setRuleList(RuleList rl) {
		this.rl = rl;
	}

}
