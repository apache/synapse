package com.infravio.broker.receivers;

import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.clientapi.MessageSender;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.ServiceDescription;
import org.apache.axis2.engine.MessageReceiver;
import org.apache.axis2.om.OMElement;
import org.apache.axis2.receivers.AbstractInMessageReceiver;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.namespace.QName;
import java.lang.reflect.Method;

/**
 * This is a Simple java Provider.
 */

public class InOnlySyncMessageReceiver extends AbstractInMessageReceiver
        implements MessageReceiver {

    /**
     * Field log
     */
    protected Log log = LogFactory.getLog(getClass());

    /**
     * Field scope
     */
    private String scope;

    /**
     * Field method
     */
    private Method method;

    /**
     * Field classLoader
     */
    private ClassLoader classLoader;

    /**
     * Constructor InOutSyncMessageReceiver
     */
    public InOnlySyncMessageReceiver() {
        scope = Constants.APPLICATION_SCOPE;
    }

    public void invokeBusinessLogic(MessageContext msgContext) throws AxisFault {
        // Get the implementation class for Infravio Proxy

        Object obj = getTheImplementationObject(msgContext);

        // For proxy there will be no method to call just make a call to the
        // remote
        // provider service with the soap request
        ServiceDescription service = msgContext.getOperationContext()
                .getServiceContext().getServiceConfig();
        ClassLoader classLoader = service.getClassLoader();
        Parameter implInfoParam = service.getParameter(SERVICE_CLASS);
        invoke(msgContext, service);

    }

    public void invoke(MessageContext msgContext, ServiceDescription sd)
            throws AxisFault {

        String ServiceName = sd.getName().getLocalPart();
        String EPR = ServiceName + ".RoutingInHandler.targetEPR";
        System.out.println("InOnlySyncMessageReceiver Invoked!");
        String targetURL = (String) msgContext.getProperty(EPR);
        System.out.println(targetURL);
        QName qname = msgContext.getOperationContext().getAxisOperation()
                .getName();
        String targetEPR = targetURL.concat(qname.getLocalPart());
        EndpointReference epr = new EndpointReference(targetEPR);
        msgContext.setTo(epr);
        System.out.println("The EndPointReference Hit : " + targetEPR);

        try {
            OMElement payload = msgContext.getEnvelope().getBody()
                    .getFirstElement();
            MessageSender msgSender = new MessageSender();
            msgSender.setTo(epr);
            msgSender.setSenderTransport(Constants.TRANSPORT_HTTP);
            msgSender.send(qname.getLocalPart(), payload);
        } catch (AxisFault axisFault) {
            axisFault.printStackTrace();
        }
    }
}