package org.apache.synapse.handlers;

import org.apache.axis2.modules.Module;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.AxisFault;

/**
 * Created by IntelliJ IDEA.
 * User: saminda
 * Date: Oct 10, 2005
 * Time: 6:19:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class LoggingModule implements Module {
    public void init(AxisConfiguration axisConfiguration) throws AxisFault {
        System.out.println("I'm in the Logging Module");
    }

    public void shutdown(AxisConfiguration axisConfiguration) throws AxisFault {
        
    }
}
