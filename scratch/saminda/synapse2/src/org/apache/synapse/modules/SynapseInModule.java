package org.apache.synapse.modules;

import org.apache.axis2.modules.Module;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.AxisFault;

/**
 * Created by IntelliJ IDEA.
 * User: saminda
 * Date: Oct 18, 2005
 * Time: 10:44:59 AM
 * To change this template use File | Settings | File Templates.
 */
public class SynapseInModule implements Module {
    public void init(AxisConfiguration axisConfiguration) throws AxisFault {
        System.out.println("Synapse Module engaged in PreDispatch Phase");
    }

    public void shutdown(AxisConfiguration axisConfiguration) throws AxisFault {

    }
}
