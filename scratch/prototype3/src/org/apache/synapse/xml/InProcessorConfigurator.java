package org.apache.synapse.xml;

import javax.xml.namespace.QName;

import org.apache.axis2.om.OMElement;
import org.apache.synapse.Constants;
import org.apache.synapse.Processor;

import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.processors.InProcessor;



public class InProcessorConfigurator extends
		AbstractListProcessorConfigurator {
	private static final QName IN_Q = new QName(Constants.SYNAPSE_NAMESPACE,
			"in");

	public QName getTagQName() {
		return IN_Q;
	}

	public Processor createProcessor(SynapseEnvironment se, OMElement el) {
		InProcessor sp = new InProcessor();
		super.addChildrenAndSetName(se, el, sp);
		return sp;
	}

}
