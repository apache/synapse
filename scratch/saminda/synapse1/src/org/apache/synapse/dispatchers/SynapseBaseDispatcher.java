package org.apache.synapse.dispatchers;

import org.apache.axis2.engine.AbstractDispatcher;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.description.ServiceDescription;
import org.apache.axis2.description.OperationDescription;
import org.apache.axis2.description.HandlerDescription;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.AxisFault;
import org.apache.synapse.rules.SynapseRuleEngine;
import org.apache.synapse.rules.RuleBean;
import org.apache.synapse.SynapseConstants;

import javax.xml.namespace.QName;
import java.util.ArrayList;

/**
 * This is place where all the dispatching will take place, all the ruling will be done
 */
public class SynapseBaseDispatcher extends AbstractDispatcher {

    public static final QName NAME = new QName("http://synapse.ws.apache.org",
            "SynapseBaseDispatcher");

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

        ArrayList generalList = (ArrayList) messageContext.getProperty(
                SynapseConstants.SynapseRuleEngine.GENERAT_RULE_ARRAY_LIST);
        Integer state = (Integer) messageContext
                .getProperty(SynapseConstants.SYNAPSE_STATE);
        RuleBean bean = null;
        if (state.intValue() <= generalList.size()) {
            bean = (RuleBean) generalList.get(state.intValue()-1);
        }
        serviceName = bean.getMediator();

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
