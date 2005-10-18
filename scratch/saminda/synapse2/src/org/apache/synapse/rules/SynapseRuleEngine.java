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
    public static  String findService(MessageContext messageContext) {

        SynapaseRuleBean bean = null;
        String serviceName = null;

        boolean generalListBoolean = true;
        boolean xpathListBoolean = true;

        ArrayList generalList = (ArrayList) messageContext.getProperty(
                SynapseConstants.SynapseRuleEngine.GENERAT_RULE_ARRAY_LIST);

        ArrayList xpathList = (ArrayList) messageContext.getProperty(
                SynapseConstants.SynapseRuleEngine.XPATH_RULE_ARRAY_LIST);

        Integer state = (Integer) messageContext
                .getProperty(SynapseConstants.SYNAPSE_STATE);

        if (generalList.size() > 0) {
            if ((state.intValue() <= generalList.size()) &&
                    generalListBoolean) {
                if (state.intValue() == generalList.size()) {
                    generalListBoolean = false;
                    messageContext.setProperty(SynapseConstants.SYNAPSE_STATE,
                            new Integer(1));
                }
                bean = (SynapaseRuleBean) generalList.get(state.intValue() - 1);
                serviceName = bean.getMediator();
            }
        }

        if (xpathList.size() > 0) {
            if (!generalListBoolean) {
                if ((state.intValue() <= generalList.size()) &&
                        xpathListBoolean) {
                    if (state.intValue() == generalList.size()) {
                        xpathListBoolean = false;
                        messageContext.setProperty(
                                SynapseConstants.SYNAPSE_STATE, new Integer(1));
                    }
                    bean = (SynapaseRuleBean) generalList
                            .get(state.intValue() - 1);
                    serviceName = bean.getMediator();
                }

            }

        }
        return serviceName;
    }
}
