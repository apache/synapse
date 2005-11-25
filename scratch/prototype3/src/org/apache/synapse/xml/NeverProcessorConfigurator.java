package org.apache.synapse.xml;

import javax.xml.namespace.QName;

import org.apache.axis2.om.OMElement;
import org.apache.synapse.Constants;
import org.apache.synapse.Processor;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.processors.NeverProcessor;


public class NeverProcessorConfigurator extends
		AbstractListProcessorConfigurator {
	private static final QName NEVER_Q = new QName(Constants.SYNAPSE_NAMESPACE,
			"never");

	public QName getTagQName() {
		return NEVER_Q;
	}

	public Processor createProcessor(SynapseEnvironment se, OMElement el) {
		NeverProcessor sp = new NeverProcessor();
		super.addChildrenAndSetName(se, el, sp);
		return sp;
	}

}
