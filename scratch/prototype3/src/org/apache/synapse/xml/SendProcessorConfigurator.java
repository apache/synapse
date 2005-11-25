package org.apache.synapse.xml;

import javax.xml.namespace.QName;

import org.apache.axis2.om.OMElement;

import org.apache.synapse.processors.builtin.SendProcessor;
import org.apache.synapse.Constants;
import org.apache.synapse.Processor;
import org.apache.synapse.SynapseEnvironment;

public class SendProcessorConfigurator extends AbstractProcessorConfigurator {
	

	private static final QName SEND_Q = new QName(Constants.SYNAPSE_NAMESPACE,
			"send");

	

	public Processor createProcessor(SynapseEnvironment se, OMElement el) {
		SendProcessor sp =  new SendProcessor();
		super.setNameOnProcessor(se, el,sp);
		return sp;
		
	}

	public QName getTagQName() {

		return SEND_Q;
	}

}
