package org.apache.synapse.api;

import org.apache.synapse.SynapseEnvironment;

public interface EnvironmentAware {
	public void setSynapseEnvironment(SynapseEnvironment se);

}
