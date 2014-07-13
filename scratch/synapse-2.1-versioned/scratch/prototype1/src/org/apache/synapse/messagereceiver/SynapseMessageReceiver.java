package org.apache.synapse.messagereceiver;

import org.apache.axis2.receivers.AbstractInMessageReceiver;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.AxisFault;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.engine.SynapseEngine;


public class SynapseMessageReceiver extends AbstractInMessageReceiver {
    public void invokeBusinessLogic(MessageContext messageContext)
            throws AxisFault {
       SynapseEngine synapseEngine = (SynapseEngine) messageContext
                .getParameter(SynapseConstants.SYNAPSE_ENGINE).getValue();
        synapseEngine.processIncoming(messageContext);
    }
}
