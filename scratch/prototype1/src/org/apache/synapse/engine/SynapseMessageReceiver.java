package org.apache.synapse.engine;

import org.apache.axis2.receivers.AbstractInMessageReceiver;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.AxisFault;
import org.apache.synapse.SynapseConstants;


public class SynapseMessageReceiver extends AbstractInMessageReceiver {
    public void invokeBusinessLogic(MessageContext messageContext)
            throws AxisFault {
        SynapseEngine synapseEngine = (SynapseEngine) messageContext
                .getProperty(SynapseConstants.SYNAPSE_ENGINE);
        synapseEngine.processIncoming(messageContext);
    }
}
