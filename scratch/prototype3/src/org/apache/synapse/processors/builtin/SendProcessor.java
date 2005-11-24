package org.apache.synapse.processors.builtin;

import javax.xml.namespace.QName;

import org.apache.axis2.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.processors.AbstractProcessor;
import org.apache.synapse.Constants;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseMessage;

public class SendProcessor extends AbstractProcessor {
	

	private static final QName SEND_Q = new QName(Constants.SYNAPSE_NAMESPACE,
			"send");

	private Log log = LogFactory.getLog(getClass());

	public void compile(SynapseEnvironment se, OMElement el) {
		super.compile(se, el);
		
	}

	public boolean process(SynapseEnvironment se, SynapseMessage smc) {
		log.debug("process");
		if (smc.isResponse()) {
			log.debug("sendback");
			se.sendBack(smc);
		} else {
			log.debug("sendon");
			se.sendOn(smc);
		}
		return false;

	}

	public QName getTagQName() {

		return SEND_Q;
	}

}
