package org.apache.synapse.processors;

import java.util.List;

import javax.xml.namespace.QName;

import org.apache.axis2.om.OMAttribute;
import org.apache.axis2.om.OMElement;
import org.apache.synapse.Processor;
import org.apache.synapse.SynapseEnvironment;


public abstract class AbstractProcessor implements Processor {
	private String name = null;

	List processors = null;

	public void compile(SynapseEnvironment se, OMElement el) {
		OMAttribute nm = el.getAttribute(new QName("name"));
		if (nm != null)
			name = nm.getAttributeValue();
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
}
