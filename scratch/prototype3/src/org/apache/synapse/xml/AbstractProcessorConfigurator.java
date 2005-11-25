package org.apache.synapse.xml;

import javax.xml.namespace.QName;

import org.apache.axis2.om.OMAttribute;
import org.apache.axis2.om.OMElement;
import org.apache.synapse.Processor;
import org.apache.synapse.SynapseEnvironment;

public abstract class AbstractProcessorConfigurator implements ProcessorConfigurator {

	
	public abstract Processor compile(SynapseEnvironment se, OMElement el);
	
	public void compile(SynapseEnvironment se, OMElement el, Processor p) {
		OMAttribute nm = el.getAttribute(new QName("name"));
		if (nm != null)
			p.setName(nm.getAttributeValue().trim());
		
	}

	public abstract QName getTagQName();

}
