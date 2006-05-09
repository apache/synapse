package org.apache.synapse.api;

import org.apache.synapse.SynapseMessageContext;

/**
 * The filter mediator is a list mediator, which executes the given (sub) list of mediators
 * if the specified condition is satisfied
 *
 * @see FilterMediator#test(org.apache.synapse.SynapseMessageContext)
 */
public interface FilterMediator extends ListMediator {

    /**
     * Should return true if the sub/child mediators should execute. i.e. if the filter
     * condition is satisfied
     * @param synCtx
     * @return true if the configured filter condition evaluates to true
     */
    public boolean test(SynapseMessageContext synCtx);
}
