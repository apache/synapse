package org.apache.synapse.config.xml;



import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.synapse.Startup;

public interface StartupFactory {
	public Startup createStartup(OMElement elem);
	public QName getTagQName();
	public Class getSerializerClass();
}
