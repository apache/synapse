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
        ArrayList cumulativeRuleList = (ArrayList) messageContext.getProperty(
                SynapseConstants.SynapseRuleEngine.CUMULATIVE_RUEL_ARRAY_LIST);

        Integer state = (Integer) messageContext
                .getProperty(SynapseConstants.SYNAPSE_STATE);


        if (cumulativeRuleList != null) {
            if (cumulativeRuleList.size() > 0) {
                if (state.intValue() <= cumulativeRuleList.size()) {
                    bean = (SynapaseRuleBean) cumulativeRuleList
                            .get(state.intValue() - 1);
                    serviceName = bean.getMediator();
                }
            }
        }

        /**
         * No rule Just send the damn mesage to it's destination.
         */
        return serviceName;
    }
}
