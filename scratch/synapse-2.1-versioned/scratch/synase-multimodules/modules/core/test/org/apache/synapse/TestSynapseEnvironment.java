package org.apache.synapse;

import org.apache.synapse.axis2.Axis2SynapseEnvironment;

public class TestSynapseEnvironment {

	public static SynapseEnvironment createAxis2SynapseEnvironment() {
		Axis2SynapseEnvironment se = new Axis2SynapseEnvironment(null, null);
		return se;
	}

}
