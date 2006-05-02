package org.apache.synapse.api;

import org.apache.synapse.SynapseMessage;

/**
 * The filter mediator is a list mediator, which executes the given list of mediators
 * if the specified condition is satisfied
 */
public interface FilterMediator extends ListMediator {
    public boolean test(SynapseMessage sm);
}
