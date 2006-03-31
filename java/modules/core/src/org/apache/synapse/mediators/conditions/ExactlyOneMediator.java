package org.apache.synapse.mediators.conditions;

import org.apache.synapse.SynapseMessage;
import org.apache.synapse.SynapseMessageConstants;
import org.apache.synapse.api.Mediator;
import org.apache.synapse.mediators.base.ListMediator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Iterator;

/**
 * Corresponding to <exactly/> This conatins conditional mediators such as
 * <regex/> and <xpath/> It will look for the first match, if the first match is
 * true then all the other mediators listed thereafter will be ingnored
 */
public class ExactlyOneMediator extends ListMediator {

	private Log log = LogFactory.getLog(getClass());

public boolean mediate( SynapseMessage smc) {
        log.debug("ExactlyOneMediator");
        if (mediators == null || mediators.size()==0) {
            log.info("no mediators called -  empty list");
            return true;
        }
        Iterator it = mediators.iterator();
                
        while (it.hasNext()) {
            Mediator m = (Mediator) it.next();
            log.debug(m.getClass());
            
            boolean cont = m.mediate(smc);
            if (!cont) return false; // if any mediator says finish then
										// finish
            
            if (smc.getProperty(SynapseMessageConstants.MATCHED) == null) {
            	// should never get here --- famous last words.
            	log.error("Condition mediator "+m.getClass()+" did not properly signal matching");
            	return true;  
            }
            	
            boolean matched = ((Boolean) smc.getProperty(
                         SynapseMessageConstants.MATCHED))
                         .booleanValue();
            if (matched) return true;      
        }
        return true;
    }
}
