package org.apache.synapse.config.xml;

import java.io.OutputStream;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.apache.synapse.config.SynapseConfiguration;

public interface ConfigurationSerializer {

	void serializeConfiguration(SynapseConfiguration synCfg, OutputStream outputStream) throws XMLStreamException;

	QName getTagQName();

}
