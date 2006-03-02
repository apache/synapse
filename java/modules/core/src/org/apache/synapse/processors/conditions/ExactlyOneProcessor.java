package org.apache.synapse.processors.conditions;

import org.apache.synapse.processors.ListProcessor;
import org.apache.synapse.processors.rules.XPathProcessor;
import org.apache.synapse.processors.rules.RegexProcessor;
import org.apache.synapse.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Iterator;

/**
 * Corresponding to <exactly/>
 * This conatins conditional mediators such as <regex/> and <xpath/>
 * It will look for the first match, if the first match is true then all the other
 * mediators listed thereafter will be ingnored
 */
public class ExactlyOneProcessor extends ListProcessor {

    private Log log = LogFactory.getLog(getClass());

    public boolean process(SynapseEnvironment se, SynapseMessage smc) {
        log.debug("processing in the SwitchProcessor");
        if (processors == null) {
            log.info("process called on empty processor list");
            return true;
        }
        Iterator it = processors.iterator();
        int processorListSize = processors.size();
        int count = 0;
        while (it.hasNext()) {
            Processor p = (Processor) it.next();
            log.debug(p.getName() + " = " + p.getClass());
            ++count;
            if (count <= processorListSize) {
                if (p instanceof XPathProcessor) {
                    XPathProcessor xp = (XPathProcessor) p;
                    if (xp.process(se, smc)) {
                        if (smc.getProperty(
                                SynapseMessageConstants.MATCHED) != null &&
                                ((Boolean) smc.getProperty(
                                        SynapseMessageConstants.MATCHED))
                                        .booleanValue()) {
                            return true;
                        }
                    }
                } else if (p instanceof RegexProcessor) {
                    RegexProcessor rp = (RegexProcessor) p;
                    if (rp.process(se, smc)) {
                        if (smc.getProperty(
                                SynapseMessageConstants.MATCHED) != null &&
                                ((Boolean) smc.getProperty(
                                        SynapseMessageConstants.MATCHED))
                                        .booleanValue()) {
                            return true;
                        }
                    }
                } else if (count == processorListSize &&
                        (p instanceof DefaultProcessor)) {
                    DefaultProcessor defaultProcessor =
                            (DefaultProcessor) p;
                    if (!defaultProcessor.process(se, smc)) {
                        return false;
                    }
                } else {
                    throw new SynapseException("Mismatch with the Type ");
                }
            }
        }
        return true;
    }

}
