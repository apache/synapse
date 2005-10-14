package org.apache.synapse.dispatchers;

import org.apache.axis2.engine.AbstractDispatcher;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.description.ServiceDescription;
import org.apache.axis2.description.OperationDescription;
import org.apache.axis2.description.HandlerDescription;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
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


    /**
     * Rules which is going to be populated via services.xml
     */
    protected SynapseRuleEngine ruleEngine;

    /**
     * to keep the state
     */
    private int synapseState = 0;

    public SynapseBaseDispatcher() {
        ruleEngine = new SynapseRuleEngine();
    }

    public void initDispatcher() {
        init(new HandlerDescription(NAME));
    }

    public ServiceDescription findService(MessageContext messageContext)
            throws AxisFault {

        /**
         * testing the rule searching.
         *
         */
        Object ruleObj = messageContext
                .getProperty(SynapseConstants.RULE_STATE);
        if (ruleObj == null) {
            ruleEngine.ruleConfiguration(messageContext);
            messageContext.setProperty(SynapseConstants.RULE_STATE, ruleEngine);
        }

        /**
         * here "*" all will be handled.
         */
        operationName = new QName(this.ruleEngine.getOperationName());

        /**
         * mediator selection
         */

        Integer state = (Integer) messageContext
                .getProperty(SynapseConstants.SYNAPSE_STATE);
        if (state != null) {
            synapseState = state.intValue();
        } else {
            synapseState = 1;
        }

        String key = null;
        if (synapseState == 1) {
            key = mediatorType(messageContext, ruleEngine, synapseState);
        } else {
            key = mediatorType(messageContext, ruleEngine, synapseState);
        }
        serviceName = key;

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

    private String mediatorType(MessageContext messageContext,
                                SynapseRuleEngine ruleEngine,
                                int synapseState) {
        ArrayList ruleArrayList = ruleEngine.getArrayList();
        String key = null;
        Integer state = null;
        if (synapseState <= ruleArrayList.size()) {
            String rule = (String) ruleArrayList.get(synapseState - 1);
            if (synapseState == ruleArrayList.size()) {
                messageContext.setProperty(SynapseConstants.VALUE_FALSE,
                        new Boolean(false));
            } else {
                state = new Integer(++synapseState);
                messageContext
                        .setProperty(SynapseConstants.SYNAPSE_STATE, state);
            }

            key = rule;
        }
        return key;
    }
}
