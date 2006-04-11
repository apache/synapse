package org.apache.synapse.registry;

import java.util.List;
import java.util.Properties;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.addressing.EndpointReference;
//import org.apache.ws.commons.schema.XmlSchema;
//import org.apache.ws.policy.Policy;
//import org.apache.wsdl.WSDLDescription;

public interface Registry {
	public OMElement getXML(String identifier);
	public String getString(String identifier);
	public String getURI(String identifier); // guaranteed a URI
	public EndpointReference getEPR(String identifier);
	public List getURIList(String identifier); // could be a list of links to other entries
//	public Policy getPolicy(String identifier);
//	public XmlSchema getSchema(String identifier);
//	public WSDLDescription getWSDL(String identifier);
	public Properties getProperties(String identifier); 
}
