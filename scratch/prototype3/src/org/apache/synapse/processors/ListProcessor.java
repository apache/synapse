package org.apache.synapse.processors;

import java.util.Iterator;

import java.util.List;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Processor;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseMessage;


/**
 * @author Paul Fremantle This class simply runs the message through all rules,
 *         stages, mediations that it has as subelements It is a way of grouping
 *         stuff.
 * 
 */
public abstract class ListProcessor extends AbstractProcessor {
	
	private Log log = LogFactory.getLog(getClass());

	List processors = null;
	
	public boolean process(SynapseEnvironment se, SynapseMessage smc) {
		log.debug("process");
		if (processors == null) {
			log.info("process called on empty processor list");
			return true;
		}
		Iterator it = processors.iterator();
		while (it.hasNext()) {
			Processor p = (Processor) it.next();
			log.debug(p.getName() + " = "+ p.getClass());
			if (!p.process(se,smc))
				return false;
		}
		return true;
	}

	public void setList(List p) {
		log.debug("setting list");
		Iterator it = p.iterator();
		while (it.hasNext()) {
			Processor x = (Processor)it.next();
			log.debug(x.getName() +" = "+ x.getClass());
		}
		processors = p;
	}
	public List getList() {
		return processors;
	}

}
