package org.apache.synapse.xml;

import javax.xml.namespace.QName;

import org.apache.axis2.om.OMElement;
import org.apache.synapse.Constants;
import org.apache.synapse.Processor;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.processors.SynapseProcessor;

public class SynapseProcessorConfigurator extends
		AbstractListProcessorConfigurator {

	private final static QName tagname = new QName(Constants.SYNAPSE_NAMESPACE,
			"synapse");

	public QName getTagQName() {
		return tagname;
	}

	public Processor compile(SynapseEnvironment se, OMElement el) {
		SynapseProcessor sp = new SynapseProcessor();
		super.compile(se, el, sp);
		return sp;
	}

}
