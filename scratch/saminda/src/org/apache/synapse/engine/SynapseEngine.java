package org.apache.synapse.engine;

import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.AxisFault;
import org.apache.synapse.SynapseConstants;

import java.util.LinkedList;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: saminda
 * Date: Oct 12, 2005
 * Time: 12:31:35 PM
 * To change this template use File | Settings | File Templates.
 */
public class SynapseEngine {
    private AxisEngine axisEngine;

    public AxisEngine getAxisEngine() {
        return axisEngine;
    }

    public void setAxisEngine(AxisEngine axisEngine) {
        this.axisEngine = axisEngine;
    }

    public void excecuite(MessageContext messageContext) throws AxisFault {

        if (axisEngine != null) {
            if (messageContext.getEnvelope().getBody().hasFault()) {
                axisEngine.receiveFault(messageContext);
            } else {
                axisEngine.receive(messageContext);
            }
            /**
             * Once the control return we would decide how to do the rule maching next
             */

            /**
             * recursivly check for more rules.
             */

            moreRules(messageContext);

        }


    }

    private void moreRules(MessageContext returnMsgCtx)
            throws AxisFault {

        Boolean returnValue = (Boolean) returnMsgCtx
                .getProperty(SynapseConstants.MEDEATOT_STATE);
        if (!returnValue.booleanValue()) {
            return ;
        } else {
            MessageContext newContext = new MessageContext(
                    returnMsgCtx.getSystemContext());
            newContext.setProperty(SynapseConstants.MEDEATOT_STATE,
                    returnMsgCtx.getProperty(SynapseConstants.MEDEATOT_STATE));

            newContext.setProperty(SynapseConstants.SYNAPSE_STATE,
                    returnMsgCtx.getProperty(SynapseConstants.SYNAPSE_STATE));
            newContext.setProperty(SynapseConstants.VALUE_FALSE,
                    returnMsgCtx.getProperty(SynapseConstants.VALUE_FALSE));
            newContext.setProperty(SynapseConstants.RULE_STATE,
                    returnMsgCtx.getProperty(SynapseConstants.RULE_STATE));
            newContext.setServerSide(true);
            newContext.setEnvelope(returnMsgCtx.getEnvelope());
            newContext.setServiceContextID(returnMsgCtx.getServiceContextID());
            newContext.setWSAAction(returnMsgCtx.getWSAAction());
            newContext.setSoapAction(returnMsgCtx.getSoapAction());
            axisEngine.receive(newContext);
            moreRules(newContext);
            return ;
        }
    }

}
