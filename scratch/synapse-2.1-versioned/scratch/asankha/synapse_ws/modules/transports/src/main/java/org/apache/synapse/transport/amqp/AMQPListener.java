package org.apache.synapse.transport.amqp;

import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.description.AxisService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.transport.base.AbstractTransportListener;

public class AMQPListener extends AbstractTransportListener
{
    public static final String TRANSPORT_NAME = "jms";
    private static final Log log = LogFactory.getLog(AMQPListener.class);

    @Override
    protected void startListeningForService(AxisService service)
    {
        // TODO Auto-generated method stub

    }

    @Override
    protected void stopListeningForService(AxisService service)
    {
        // TODO Auto-generated method stub

    }

    public EndpointReference[] getEPRsForService(String arg0, String arg1) throws AxisFault
    {
        // TODO Auto-generated method stub
        return null;
    }

}
