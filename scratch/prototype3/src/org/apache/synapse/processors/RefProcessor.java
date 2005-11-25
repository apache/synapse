package org.apache.synapse.processors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Processor;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseMessage;

public class RefProcessor extends AbstractProcessor {
	private Log log = LogFactory.getLog(getClass());
	private String ref = null;
	
	public boolean process(SynapseEnvironment se, SynapseMessage sm) {
		log.debug("process");
		Processor p = se.lookupProcessor(getRef());
		if (p==null) log.debug("processor with name "+this.getRef()+" not found");
		else return p.process(se, sm);
		return true;
	}

	public void setRef(String ref) {
		this.ref = ref;
	}

	public String getRef() {
		return ref;
	}

		

}
