package org.apache.synapse.axis2;

import java.io.InputStream;


import javax.xml.stream.XMLStreamException;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.ParameterImpl;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.om.OMElement;
import org.apache.axis2.om.impl.llom.builder.StAXOMBuilder;

import org.apache.synapse.SynapseException;
import org.apache.synapse.api.SynapseEnvironment;

public class Axis2SynapseEnvironmentFinder {

	private static final String SYNAPSECONFIGURATION = "SynapseConfiguration";

	public static final String SYNAPSE_ENVIRONMENT = "synapse.environment";

	public static synchronized SynapseEnvironment getSynapseEnvironment(MessageContext mc) {
		AxisConfiguration ac = mc.getSystemContext().getAxisConfiguration();
		Parameter synapseEnvParam = ac.getParameter(SYNAPSE_ENVIRONMENT);
		if (synapseEnvParam == null) {

			Parameter param = ac.getParameter(SYNAPSECONFIGURATION);
			if (param == null) {
				throw new SynapseException("no parameter '" + SYNAPSECONFIGURATION
						+ "' in axis2.xml");
			}
			String synapseConfig = (String) param.getValue();
			InputStream is = mc.getAxisService().getClassLoader().getResourceAsStream(synapseConfig);
			
			StAXOMBuilder builder;
			try {
				builder =  new StAXOMBuilder(is);

			} catch (XMLStreamException e1) {
				throw new SynapseException("Trouble parsing Synapse Configuration ",e1);
				
			}		
			OMElement config = builder.getDocumentElement();
			Axis2SynapseEnvironment se = new Axis2SynapseEnvironment(config, mc.getAxisService().getClassLoader());
			
		
			synapseEnvParam = new ParameterImpl(SYNAPSE_ENVIRONMENT, null);
			synapseEnvParam.setValue(se);
			try {
				ac.addParameter(synapseEnvParam);
			} catch (AxisFault e) {
				throw new SynapseException(e);
			}
		}
		return (SynapseEnvironment) synapseEnvParam.getValue();
		
	}

}
