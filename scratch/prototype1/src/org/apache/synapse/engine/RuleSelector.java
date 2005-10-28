package org.apache.synapse.engine;

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.om.OMElement;

import java.util.Iterator;


public interface RuleSelector {

    /**
     * This will give a chance for the rules processor to compile or do whatever
     * stuff he needs to optimize rules processing.
     * Normally the user who inits the RuleSelector should pass the rules relevant to
     * a particular stage for a given direction
     * In other words, RuleSelector should be created per stage per direction
     */
    public void init(OMElement ruleSet);
    Rule [] getRules();

    /**
     * This will contain a list of Rule objects
     *
     * @param messageContext
     */
    public Iterator match(MessageContext messageContext);

    public Rule getBestMatch(MessageContext messageContext);
}
