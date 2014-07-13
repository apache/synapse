package org.apache.synapse.handler;

import org.apache.axis2.handlers.AbstractHandler;
import org.apache.axis2.engine.Handler;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.AxisFault;
import org.apache.synapse.rules.SynapseRuleConfiguration;
import org.apache.synapse.SynapseConstants;
import org.jaxen.JaxenException;

/**
 * Created by IntelliJ IDEA.
 * User: saminda
 * Date: Oct 18, 2005
 * Time: 10:10:26 AM
 * To change this template use File | Settings | File Templates.
 */
public class SynapseInHandler extends AbstractHandler implements Handler {


    private SynapseRuleConfiguration ruleConfiguration;

    private int synapseState = 0;

    public SynapseInHandler() {
        ruleConfiguration = new SynapseRuleConfiguration();
    }

    public void invoke(MessageContext messageContext) throws AxisFault {


        Boolean loopBoolean = (Boolean) messageContext
                .getProperty(SynapseConstants.MEDEATOT_STATE);
        if (loopBoolean == null) {
            messageContext.setProperty(
                    SynapseConstants.SynapseRuleEngine.SYNAPSE_RECEIVER,
                    ruleConfiguration.getOperationName());
            ruleConfiguration.ruleConfiguration(messageContext);


            try {
                ruleConfiguration.validateXpath(messageContext);
                messageContext.setProperty(
                        SynapseConstants.SynapseRuleEngine.SYNAPSE_RULE_ENGINE,
                        ruleConfiguration);
            } catch (JaxenException e) {
                throw new AxisFault(e);
            }

            messageContext
                    .setProperty(SynapseConstants.SYNAPSE_STATE,
                            synapseStateValidation(messageContext));
        }

    }


    public Integer synapseStateValidation(MessageContext messageContext) {
        Integer state = (Integer) messageContext
                .getProperty(SynapseConstants.SYNAPSE_STATE);

        if (state == null) {
            synapseState = 1;
            return new Integer(synapseState);
        } else {
            return state;
        }
    }
}
