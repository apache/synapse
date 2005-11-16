package org.apache.synapse.mediators;

import javax.xml.namespace.QName;

import org.apache.axis2.om.OMAttribute;
import org.apache.axis2.om.OMElement;
import org.apache.synapse.SynapseException;
import org.apache.synapse.api.MediatorConfiguration;
import org.apache.synapse.spi.MediatorConfigurator;

public class ClassMediatorConfigurator implements MediatorConfigurator {

	public MediatorConfiguration parse(OMElement el, ClassLoader cl) {
		ClassMediatorConfiguration cmc = new ClassMediatorConfiguration();
		
		OMAttribute name = el.getAttribute(new QName("", "name"));
		if (name==null) throw new SynapseException("missing name attribute on element"+el.toString());
		cmc.setMediatorName(name.getAttributeValue());
		
		OMAttribute clsName = el.getAttribute(new QName("", "class"));
		if (clsName==null) throw new SynapseException("missing class attribute on element"+el.toString());
		try {
		
			// This is probably the wrong place to try to load the class because 
			// we don't have access to the correct class loader
			// maybe we need to pass classloaders into medconfigurators???
			cmc.setMediatorClass(cl.loadClass(clsName.getAttributeValue()));
		} catch (ClassNotFoundException e) {
			throw new SynapseException("class loading error", e);
		}
		cmc.setMediatorElement(el);
		return cmc;
	}

}
