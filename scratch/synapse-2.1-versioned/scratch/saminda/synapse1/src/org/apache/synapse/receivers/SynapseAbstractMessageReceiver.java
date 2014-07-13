package org.apache.synapse.receivers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.AxisFault;
import org.apache.axis2.receivers.AbstractMessageReceiver;

/**
 * Created by IntelliJ IDEA.
 * User: saminda
 * Date: Oct 10, 2005
 * Time: 5:54:59 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class SynapseAbstractMessageReceiver extends AbstractMessageReceiver {
    protected Log log = LogFactory.getLog(getClass());

    public abstract void invokeBusinessLogic(MessageContext inMessage) throws AxisFault;

    public final void receive(final MessageContext messgeCtx) throws AxisFault {
        invokeBusinessLogic(messgeCtx);
    }
}
