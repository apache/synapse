package org.apache.synapse.processors.builtin;

import org.apache.synapse.processors.AbstractProcessor;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseMessage;

/**
 */
public class DropProcessor extends AbstractProcessor {
    
    public boolean process(SynapseEnvironment se, SynapseMessage sm) {
        if (sm.getTo() == null) {
            return false;
        }else{
            sm.setTo(null);
            return false;
        }
    }
}
