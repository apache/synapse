package org.apache.synapse;


import org.apache.axis2.om.OMElement;

public interface Mediator {
	public boolean mediate(OMElement message);
	
}
