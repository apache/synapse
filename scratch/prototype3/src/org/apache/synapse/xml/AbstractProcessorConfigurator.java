package org.apache.synapse.xml;

import javax.xml.namespace.QName;

import org.apache.axis2.om.OMAttribute;
import org.apache.axis2.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Processor;
import org.apache.synapse.SynapseEnvironment;

public abstract class AbstractProcessorConfigurator implements ProcessorConfigurator {

	
	
	private Log log = LogFactory.getLog(getClass()); 
	public void setNameOnProcessor(SynapseEnvironment se, OMElement el, Processor p) {
		
		OMAttribute nm = el.getAttribute(new QName("name"));
		if (nm != null) {
			String name = nm.getAttributeValue().trim();
			p.setName(name);
			se.addProcessor(p);
		}
		log.debug("compile "+el.getLocalName()+" with name '"+p.getName() +"' on "+p.getClass());
		
	}

	
}
