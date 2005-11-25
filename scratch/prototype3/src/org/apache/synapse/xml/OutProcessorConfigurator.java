package org.apache.synapse.xml;

import javax.xml.namespace.QName;

import org.apache.axis2.om.OMElement;
import org.apache.synapse.Constants;
import org.apache.synapse.Processor;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.processors.OutProcessor;


public class OutProcessorConfigurator extends
		AbstractListProcessorConfigurator {
	private static final QName OUT_Q = new QName(Constants.SYNAPSE_NAMESPACE,
			"out");

	public QName getTagQName() {
		return OUT_Q;
	}

	public Processor createProcessor(SynapseEnvironment se, OMElement el) {
		OutProcessor sp = new OutProcessor();
		super.addChildrenAndSetName(se, el, sp);
		return sp;
	}

}
