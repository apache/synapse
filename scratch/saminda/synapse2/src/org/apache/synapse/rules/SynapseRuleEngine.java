package org.apache.synapse.rules;

import org.apache.axis2.context.MessageContext;
import org.apache.synapse.SynapseConstants;

import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: saminda
 * Date: Oct 18, 2005
 * Time: 4:41:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class SynapseRuleEngine {
    /**
     * Synapse Rule Engine, which is able to select between "general" and "xpath"
     * rules and applying it.
     * This code has been written giving support to Rule Extensibility.
     * <p/>
     * 1. First all the general rules will be apply to all the incoming messages.
     * 2. Xpaht Rule apply to relevent messages.
     */
    public static String findService(MessageContext messageContext) {

        SynapaseRuleBean bean = null;
        String serviceName = null;

        ArrayList generalList = (ArrayList) messageContext.getProperty(
                SynapseConstants.SynapseRuleEngine.GENERAT_RULE_ARRAY_LIST);

        ArrayList xpathList = (ArrayList) messageContext.getProperty(
                SynapseConstants.SynapseRuleEngine.XPATH_RULE_ARRAY_LIST);

        Integer state = (Integer) messageContext
                .getProperty(SynapseConstants.SYNAPSE_STATE);

        if (generalList.size() > 0) {
            if ((state.intValue() <= generalList.size()) &&
                    ((Boolean) messageContext.getProperty(
                            SynapseConstants.SynapseRuleEngine.GENERAT_RULE_ARRAY_BOOLEAN))
                            .booleanValue()) {
                if (state.intValue() == generalList.size()) {
                    messageContext.setProperty(
                            SynapseConstants.SynapseRuleEngine.GENERAT_RULE_ARRAY_BOOLEAN,
                            new Boolean(false));
                    messageContext.setProperty(SynapseConstants.SYNAPSE_STATE,
                            new Integer(1));
                }
                bean = (SynapaseRuleBean) generalList.get(state.intValue() - 1);
                serviceName = bean.getMediator();
            }
        }

        if (xpathList.size() > 0) {
            if (!((Boolean) messageContext.getProperty(
                    SynapseConstants.SynapseRuleEngine.GENERAT_RULE_ARRAY_BOOLEAN))
                    .booleanValue()) {
                if ((state.intValue() <= xpathList.size()) &&
                        ((Boolean) messageContext.getProperty(
                                SynapseConstants.SynapseRuleEngine.XPATH_RULE_ARRAY_BOOLEAN))
                                .booleanValue()) {
                    if (state.intValue() == xpathList.size()) {
                        ((Boolean) messageContext.getProperty(
                                SynapseConstants.SynapseRuleEngine.XPATH_RULE_ARRAY_BOOLEAN))
                                .booleanValue();
                        messageContext.setProperty(
                                SynapseConstants.SYNAPSE_STATE, new Integer(1));
                    }
                    bean = (SynapaseRuleBean) xpathList
                            .get(state.intValue() - 1);
                    serviceName = bean.getMediator();
                }

            }

        }
        return serviceName;
    }
}
