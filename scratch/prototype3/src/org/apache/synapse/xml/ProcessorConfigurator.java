package org.apache.synapse.xml;

import javax.xml.namespace.QName;

import org.apache.axis2.om.OMElement;
import org.apache.synapse.Processor;
import org.apache.synapse.SynapseEnvironment;

public interface ProcessorConfigurator {
	public Processor compile(SynapseEnvironment se, OMElement el);
	public QName getTagQName();

}
