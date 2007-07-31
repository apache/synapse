package org.apache.synapse;

import org.apache.synapse.core.SynapseEnvironment;

public interface ManagedLifecycle {
	public void init(SynapseEnvironment se);
	public void destroy();
}
