package org.apache.synapse.api;

import org.apache.synapse.SynapseMessage;

public interface FilterMediator extends ListMediator {
	public boolean test(SynapseMessage sm);
}
