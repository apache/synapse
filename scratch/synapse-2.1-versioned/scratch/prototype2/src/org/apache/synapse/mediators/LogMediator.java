package org.apache.synapse.mediators;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.api.Mediator;
import org.apache.synapse.api.SOAPMessageContext;


public class LogMediator implements Mediator {

	Log log = LogFactory.getLog(getClass());
	public boolean mediate(SOAPMessageContext smc) {
		log.info(smc.getTo());
		log.info(smc.getFrom());
		log.info(smc.getReplyTo());
		log.info(smc.getMessageID());
		log.info(smc.getEnvelope());
		return true;
	}

}
