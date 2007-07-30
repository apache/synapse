package org.apache.synapse.config.xml;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.synapse.config.SynapseConfiguration;

public interface ConfigurationFactory {

	QName getTagQName();
	SynapseConfiguration getConfiguration(OMElement element);
	Class getSerializerClass();

}
