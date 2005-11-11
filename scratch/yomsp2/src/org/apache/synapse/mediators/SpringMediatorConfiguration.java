package org.apache.synapse.mediators;

import org.apache.axis2.om.OMElement;
import org.apache.synapse.MediatorConfiguration;
import org.apache.synapse.MediatorTypes;

import org.springframework.context.support.GenericApplicationContext;


public class SpringMediatorConfiguration implements MediatorConfiguration {

	private OMElement mediatorElement = null;
	private GenericApplicationContext ctx = null;
	private String name = null;
	
	public void setMediatorElement(OMElement me) {
		this.mediatorElement = me;
	}
	
	public OMElement getMediatorElement() {
	
		return mediatorElement;
	}

	public void setApplicationContext(GenericApplicationContext ctx) {
		this.ctx = ctx;
		
	}
	public GenericApplicationContext getApplicationContext() {
		return ctx;
	}

	public String getMediatorName() {
		
		return name;
		
	}
	public void setMediatorName(String name) {
		this.name=name;
	}

	public int getMediatorType() {
		
		return MediatorTypes.SPRING;
	}
	


}
