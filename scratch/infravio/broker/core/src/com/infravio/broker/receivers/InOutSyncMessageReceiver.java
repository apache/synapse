package com.infravio.broker.receivers;

import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.clientapi.Call;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.ServiceDescription;
import org.apache.axis2.engine.MessageReceiver;
import org.apache.axis2.om.OMElement;
import org.apache.axis2.receivers.AbstractInOutSyncMessageReceiver;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.namespace.QName;
import java.lang.reflect.Method;

/**
 * This is a Simple java Provider.
 */

public class InOutSyncMessageReceiver extends AbstractInOutSyncMessageReceiver
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
    public InOutSyncMessageReceiver() {
        scope = Constants.APPLICATION_SCOPE;
    }

    public void invokeBusinessLogic(MessageContext msgContext,
                                    MessageContext newmsgContext) throws AxisFault {
        // Get the implementation class for Infravio Proxy

        Object obj = getTheImplementationObject(msgContext);

        // For proxy there will be no method to call just make a call to the
        // remote
        // provider service with the soap request and set the response from the
        // remote
        // provider service in the message context

        ServiceDescription service = msgContext.getOperationContext()
                .getServiceContext().getServiceConfig();
        ClassLoader classLoader = service.getClassLoader();
        Parameter implInfoParam = service.getParameter(SERVICE_CLASS);
        invoke(msgContext, newmsgContext, service);

    }

    public void invoke(MessageContext msgContext, MessageContext newmsgContext,
                       ServiceDescription sd) throws AxisFault {

        System.out.println("InOutSyncMessageReceiver Invoked!");
        String ServiceName = sd.getName().getLocalPart();
        String EPR = ServiceName + ".RoutingInHandler.targetEPR";
        String targetURL = (String) msgContext.getProperty(EPR);
        System.out.println(targetURL);
        QName qname = msgContext.getOperationContext().getAxisOperation()
                .getName();
        String targetEPR = targetURL.concat(qname.getLocalPart());
        EndpointReference epr = new EndpointReference(targetEPR);
        msgContext.setTo(epr);
        System.out.println("The EndPointReference hit: " + targetEPR);

        try {
            OMElement payload = msgContext.getEnvelope().getBody()
                    .getFirstElement();
            Call call = new Call();
            call.setTo(epr);
            call.setTransportInfo(Constants.TRANSPORT_HTTP,
                    Constants.TRANSPORT_HTTP, false);

            // Blocking invocation

            OMElement result = (OMElement) call.invokeBlocking(qname
                    .getLocalPart(), payload);
            newmsgContext.setEnvelope(call.getLastResponseMessage()
                    .getEnvelope());
        } catch (AxisFault axisFault) {
            axisFault.printStackTrace();
        }
    }
}