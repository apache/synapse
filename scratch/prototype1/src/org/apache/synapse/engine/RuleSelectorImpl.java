package org.apache.synapse.engine;

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.om.OMElement;

import java.util.Iterator;

public class RuleSelectorImpl implements RuleSelector {
    /**
     * This will give a chance for the rules processor to compile or do whatever
     * stuff he needs to optimize rules processing.
     * Normally the user who inits the RuleSelector should pass the rules relevant to
     * a particular stage for a given direction
     * In other words, RuleSelector should be created per stage per direction
     */
    public void init(OMElement ruleSet) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * This will give a chance for the rules processor to compile or do whatever
     * stuff he needs to optimize rules processing.
     * Normally the user who passes the rules should pass the rules relevant to
     * a particular stage for a given direction.
     */


    public Iterator match(MessageContext messageContext) {
        return null;
    }

    public Rule getBestMatch(MessageContext messageContext) {
        return null;
    }
}
