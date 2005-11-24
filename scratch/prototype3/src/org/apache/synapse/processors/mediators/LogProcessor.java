package org.apache.synapse.processors.mediators;

import javax.xml.namespace.QName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Constants;
import org.apache.synapse.api.SOAPMessageContext;
import org.apache.synapse.api.SynapseEnvironment;

import org.apache.synapse.processors.AbstractProcessor;

public class LogProcessor extends AbstractProcessor {
	private static final QName LOG_Q = new QName(Constants.SYNAPSE_NAMESPACE,
			"log");

	private Log log = LogFactory.getLog(getClass());

	public boolean process(SynapseEnvironment se, SOAPMessageContext smc) {
		if (smc.getTo() != null)
			log.info("To: " + smc.getTo().getAddress());
		if (smc.getFrom() != null)
			log.info("From: " + smc.getFrom().getAddress());
		if (smc.getReplyTo() != null)
			log.info("ReplyTo: " + smc.getReplyTo().getAddress());
		if (smc.getEnvelope() != null)
			log.info("Envelope: " + smc.getEnvelope().toString());
		return true;
	}

	public QName getTagQName() {
		return LOG_Q;
	}

}
