package org.apache.synapse.processors;


import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseMessage;

public class InProcessor extends ListProcessor{

	public boolean process(SynapseEnvironment se, SynapseMessage sm) {
		if (!sm.isResponse()) return super.process(se,sm);
		return true;
		
	}
	

}
