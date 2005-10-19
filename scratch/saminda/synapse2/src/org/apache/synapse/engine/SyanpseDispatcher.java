package org.apache.synapse.engine;

import org.apache.axis2.engine.AbstractDispatcher;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.description.HandlerDescription;
import org.apache.axis2.description.ServiceDescription;
import org.apache.axis2.description.OperationDescription;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.AxisFault;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.rules.SynapseRuleEngine;

import javax.xml.namespace.QName;

/**
 * Created by IntelliJ IDEA.
 * User: saminda
 * Date: Oct 18, 2005
 * Time: 10:10:16 AM
 * To change this template use File | Settings | File Templates.
 */
public class SyanpseDispatcher extends AbstractDispatcher {
    public static final QName NAME = new QName("http://synapse.ws.apache.org",
            "SyanpseDispatcher");

    protected String serviceName;
    protected QName operationName;


    public void initDispatcher() {
        init(new HandlerDescription(NAME));
    }

    public ServiceDescription findService(MessageContext messageContext)
            throws AxisFault {

        String opName = (String) messageContext.getProperty(
                SynapseConstants.SynapseRuleEngine.SYNAPSE_RECEIVER);

        operationName = new QName(opName);
        /**
         *  Synapse Rule Engine, which determine which rules to apply,
         *  general of xpath {Scope of M1}
         */
        serviceName = SynapseRuleEngine.findService(messageContext);

            AxisConfiguration registry =
                    messageContext.getSystemContext()
                            .getAxisConfiguration();
        return registry.getService(serviceName);

    }

    public OperationDescription findOperation(
            ServiceDescription serviceDescription,
            MessageContext messageContext) throws AxisFault {
        OperationDescription operationDis;
        if (operationName != null) {
            operationDis = serviceDescription.getOperation(operationName);
            return operationDis;
        }
        return null;
    }

}
