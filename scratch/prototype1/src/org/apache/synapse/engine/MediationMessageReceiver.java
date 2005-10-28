package org.apache.synapse.engine;

import org.apache.axis2.receivers.AbstractInMessageReceiver;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.AxisFault;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.mediator.Mediator;

public class MediationMessageReceiver extends AbstractInMessageReceiver {

    public void invokeBusinessLogic(MessageContext messageContext)
            throws AxisFault {
        boolean result = true;
        Mediator[] mediators = (Mediator[]) messageContext
                .getProperty(SynapseConstants.MEDIATORS);
        for (int i = 0; i < mediators.length; i++) {
            Mediator mediator = mediators[i];
            result = mediator.mediate(messageContext);
            if (!result) {
                messageContext.setProperty(SynapseConstants.MEDIATION_RESULT,
                        Boolean.FALSE);
                return;
            }
        }
        messageContext
                .setProperty(SynapseConstants.MEDIATION_RESULT, Boolean.TRUE);
    }
}
