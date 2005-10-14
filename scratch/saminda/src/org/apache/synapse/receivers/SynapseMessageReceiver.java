package org.apache.synapse.receivers;

import org.apache.axis2.engine.*;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.i18n.Messages;
import org.apache.axis2.soap.SOAPEnvelope;
import org.apache.axis2.om.OMElement;
import org.apache.axis2.description.OperationDescription;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.wsdl.WSDLService;
import org.apache.synapse.SynapseConstants;

import java.lang.reflect.Method;

/**
 * Created by IntelliJ IDEA.
 * User: saminda
 * Date: Oct 10, 2005
 * Time: 5:54:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class SynapseMessageReceiver extends SynapseAbstractMessageReceiver implements MessageReceiver {

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
     * Constructor RawXMLProvider
     */
    public SynapseMessageReceiver() {
        scope = Constants.APPLICATION_SCOPE;
    }

    public void invokeBusinessLogic(MessageContext msgContext)
            throws AxisFault {
        try {

            // get the implementation class for the Web Service
            Object obj = getTheImplementationObject(msgContext);

            
            /**
             * Injecting messageContext for medaite method
             * Will not be on use in the M1
             */
            Boolean mediatorState = DependencyManager.mediatorBusinessLogicProvider(obj, msgContext);
            msgContext.setProperty(SynapseConstants.MEDEATOT_STATE,mediatorState);

        } catch (Exception e) {
            throw AxisFault.makeFault(e);
        }

    }
}
