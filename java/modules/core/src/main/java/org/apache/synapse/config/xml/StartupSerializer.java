package org.apache.synapse.config.xml;



import org.apache.axiom.om.OMElement;
import org.apache.synapse.Startup;

public interface StartupSerializer {

	public void serializeStartup(OMElement parent, Startup startup);
}