package org.apache.synapse.axis2;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.receivers.AbstractMessageReceiver;
import org.apache.synapse.Constants;
import org.apache.synapse.SynapseMessage;
import org.apache.synapse.api.Mediator;

public class ServiceMediatorMessageReceiver extends AbstractMessageReceiver {
    public void receive(MessageContext messageContext) throws AxisFault {
        Object obj = makeNewServiceObject(messageContext);

        Mediator mediator = (Mediator) obj;
/*
        if (EnvironmentAware.class.isAssignableFrom(mediator.getClass())) {
            SynapseContext se = (SynapseContext) messageContext
                    .getProperty(Constants.MEDIATOR_SYNAPSE_CTX_PROPERTY);
            ((EnvironmentAware) mediator).setSynapseContext(se);
            ((EnvironmentAware) mediator).setClassLoader(
                    messageContext.getAxisService().getClassLoader());
        }
        
*/
        SynapseMessage smc = new Axis2SynapseMessage(messageContext, Axis2SynapseContextFinder.getSynapseContext(messageContext));
        boolean returnValue = mediator.mediate(smc);
        messageContext.setProperty(Constants.MEDIATOR_RESPONSE_PROPERTY,
            new Boolean(returnValue));
    }
}
