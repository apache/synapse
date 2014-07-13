package org.apache.synapse.mediators;

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.AxisFault;
import org.apache.synapse.SynapseConstants;

/**
 * Created by IntelliJ IDEA.
 * User: saminda
 * Date: Oct 10, 2005
 * Time: 5:34:01 PM
 * To change this template use File | Settings | File Templates.
 */
public class SimpleLoggingMediator implements Mediator {
    /**
     * Do Some mediation and return true/false
     * false means, mediator consumes the message
     * ture means, medator return for that message, to deal with another Rule
     *
     * @param msgContext
     * @return Boolean
     */
    public boolean mediate(MessageContext msgContext) throws AxisFault {
        System.out.println("i'm doing the logging mediation...now");

        return false;

    }

}
