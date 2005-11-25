package org.apache.synapse.processors.builtin;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.processors.AbstractProcessor;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseMessage;

public class SendProcessor extends AbstractProcessor {
	

	private Log log = LogFactory.getLog(getClass());

	
	public boolean process(SynapseEnvironment se, SynapseMessage smc) {
		log.debug("process");
		se.send(smc);
		return false;

	}

	

}
