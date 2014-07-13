package org.apache.synapse.mediators;

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.AxisFault;

/**
 * Created by IntelliJ IDEA.
 * User: saminda
 * Date: Oct 18, 2005
 * Time: 11:28:57 AM
 * To change this template use File | Settings | File Templates.
 */
public interface Mediator {
    public boolean mediate(MessageContext messageContext) throws AxisFault;
}
