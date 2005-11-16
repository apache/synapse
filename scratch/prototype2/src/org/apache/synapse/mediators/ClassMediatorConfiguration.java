package org.apache.synapse.mediators;

import org.apache.axis2.om.OMElement;
import org.apache.synapse.api.MediatorConfiguration;

public class ClassMediatorConfiguration implements MediatorConfiguration {

	private OMElement mediatorElement = null;
	private String name = null;
	
	private Class clazz=null;
	
	public OMElement getMediatorElement() {
		return  mediatorElement;

	}

	public String getMediatorName() {

		return name;
	}

	public int getMediatorType() {
		return MediatorTypes.CLASS;
	}
	public Class getMediatorClass() {
		return clazz;
	}

	public void setMediatorName(String name) {
		this.name=name;
	}
	public void setMediatorClass(Class clazz) {
		this.clazz = clazz;
	}
	public void setMediatorElement(OMElement el) {
		this.mediatorElement = el;
	}
	
	
}
