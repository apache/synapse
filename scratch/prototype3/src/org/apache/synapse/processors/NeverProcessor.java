package org.apache.synapse.processors;


import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseMessage;

public class NeverProcessor extends ListProcessor{

	public boolean process(SynapseEnvironment se, SynapseMessage sm) {
		return true;
	}
	

}
