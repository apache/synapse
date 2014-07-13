package org.apache.synapse.handlers;

import org.apache.axis2.handlers.AbstractHandler;
import org.apache.axis2.engine.Handler;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.AxisFault;

/**
 * Created by IntelliJ IDEA.
 * User: saminda
 * Date: Oct 10, 2005
 * Time: 6:21:19 PM
 * To change this template use File | Settings | File Templates.
 */
public class LoggingHandler extends AbstractHandler implements Handler {
    public void invoke(MessageContext messageContext) throws AxisFault {
        //todo dosmoething..
        System.out.println("I'm in the logging handler....\n");
    }
}
