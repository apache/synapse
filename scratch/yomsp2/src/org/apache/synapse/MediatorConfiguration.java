package org.apache.synapse;

import org.apache.axis2.om.OMElement;

public interface MediatorConfiguration {
	OMElement getMediatorElement();
	String getMediatorName();
	int getMediatorType();
}
