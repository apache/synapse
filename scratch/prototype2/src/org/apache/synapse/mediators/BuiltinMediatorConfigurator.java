package org.apache.synapse.mediators;

import javax.xml.namespace.QName;

import org.apache.axis2.om.OMAttribute;
import org.apache.axis2.om.OMElement;
import org.apache.synapse.SynapseException;
import org.apache.synapse.api.MediatorConfiguration;
import org.apache.synapse.spi.MediatorConfigurator;

public class BuiltinMediatorConfigurator implements MediatorConfigurator {

	public MediatorConfiguration parse(OMElement el, ClassLoader cl) {
		BuiltinMediatorConfiguration bmc = new BuiltinMediatorConfiguration();
		bmc.setMediatorElement(el);
		OMAttribute attr = el.getAttribute(new QName("name"));
		if (attr==null) throw new SynapseException("name attribute missing on element "+el.toString());
		
		String med = attr.getAttributeValue().trim().toLowerCase();
		if (med.equals("log")) bmc.setMediatorClass(LogMediator.class);
		else if (med.equals("sender")) bmc.setMediatorClass(SendMediator.class);
		
		return bmc;
		
	}

}
