package org.apache.synapse.mediators;

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.AxisFault;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.rules.SynapseRuleEngine;
import org.apache.synapse.rules.RuleBean;

import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: saminda
 * Date: Oct 12, 2005
 * Time: 11:40:48 AM
 * To change this template use File | Settings | File Templates.
 */
public class SimpleDummyMediator implements Mediator {

    public boolean mediate(MessageContext messageContext) throws AxisFault {

        System.out.println(
                "you are in the dummy mediator..logging facility ... now\n");
        Boolean retbool = (Boolean) messageContext
                .getProperty(SynapseConstants.VALUE_FALSE);
        if (retbool != null) {
            return false;
        } else {
            return true;
        }
    }
}
