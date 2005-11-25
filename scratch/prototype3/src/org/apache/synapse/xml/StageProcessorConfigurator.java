package org.apache.synapse.xml;

import javax.xml.namespace.QName;

import org.apache.axis2.om.OMElement;
import org.apache.synapse.Constants;
import org.apache.synapse.Processor;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.processors.StageProcessor;

public class StageProcessorConfigurator extends
		AbstractListProcessorConfigurator {
	private static final QName STAGE_Q = new QName(Constants.SYNAPSE_NAMESPACE,
			"stage");

	public QName getTagQName() {
		return STAGE_Q;
	}

	public Processor compile(SynapseEnvironment se, OMElement el) {
		StageProcessor sp = new StageProcessor();
		super.compile(se, el, sp);
		return sp;
	}

}
