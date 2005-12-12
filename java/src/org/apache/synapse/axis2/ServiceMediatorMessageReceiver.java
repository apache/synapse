package org.apache.synapse.axis2;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.receivers.AbstractMessageReceiver;
import org.apache.synapse.Constants;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseMessage;
import org.apache.synapse.api.EnvironmentAware;
import org.apache.synapse.api.Mediator;

public class ServiceMediatorMessageReceiver extends AbstractMessageReceiver {
    public void receive(MessageContext messageContext) throws AxisFault {
        Object obj = makeNewServiceObject(messageContext);
        
        Mediator mediator = (Mediator)obj;
        
        if (EnvironmentAware.class.isAssignableFrom(mediator.getClass())) {
        	SynapseEnvironment se = (SynapseEnvironment)messageContext.getProperty(Constants.MEDIATOR_SYNAPSE_ENV_PROPERTY);
			((EnvironmentAware) mediator).setSynapseEnvironment(se);
			((EnvironmentAware) mediator).setClassLoader(messageContext.getAxisService().getClassLoader());
		}
        SynapseMessage smc = new Axis2SynapseMessage(messageContext);
        boolean returnValue = mediator.mediate(smc);
        messageContext.setProperty(Constants.MEDIATOR_STATUS, new Boolean(returnValue));
    }
}
