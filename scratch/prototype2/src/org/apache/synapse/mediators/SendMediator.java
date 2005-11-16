package org.apache.synapse.mediators;

import org.apache.synapse.SynapseException;
import org.apache.synapse.api.EnvironmentAware;
import org.apache.synapse.api.Mediator;
import org.apache.synapse.api.SOAPMessageContext;
import org.apache.synapse.api.SynapseEnvironment;

public class SendMediator implements Mediator, EnvironmentAware {
	private SynapseEnvironment se = null;
	
	public boolean mediate(SOAPMessageContext smc) {
		if (se==null) throw new SynapseException("engine has failed to do its duty... SynapseEnvironment not set");
		if (smc.isResponse()) {
			se.sendBack(smc);
		}
		else
		{
			se.sendOn(smc);
		}
		return false;
	}

	public void setSynapseEnvironment(SynapseEnvironment se) {
		this.se = se;
		
	}

}
