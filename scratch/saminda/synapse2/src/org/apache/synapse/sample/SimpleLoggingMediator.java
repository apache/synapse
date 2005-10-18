package org.apache.synapse.sample;

import org.apache.synapse.mediators.Mediator;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.AxisFault;

/**
 * Created by IntelliJ IDEA.
 * User: saminda
 * Date: Oct 18, 2005
 * Time: 11:30:59 AM
 * To change this template use File | Settings | File Templates.
 */
public class SimpleLoggingMediator implements Mediator {
    public boolean mediate(MessageContext messageContext) throws AxisFault {
        System.out.println("i'm at the sample logging mediator");
        return true;
    }
}
