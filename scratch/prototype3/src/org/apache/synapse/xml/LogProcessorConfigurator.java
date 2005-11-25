package org.apache.synapse.xml;

import javax.xml.namespace.QName;

import org.apache.axis2.om.OMElement;

import org.apache.synapse.Constants;
import org.apache.synapse.Processor;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.processors.builtin.LogProcessor;

public class LogProcessorConfigurator extends AbstractProcessorConfigurator {
	private static final QName LOG_Q = new QName(Constants.SYNAPSE_NAMESPACE,
			"log");


	public QName getTagQName() {
		return LOG_Q;
	}


	public Processor createProcessor(SynapseEnvironment se, OMElement el) {
		LogProcessor lp = new LogProcessor();
		super.setNameOnProcessor(se,el,lp);
		return lp;
	}

}
