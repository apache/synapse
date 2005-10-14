package org.apache.synapse.engine;

import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.clientapi.MessageSender;
import org.apache.axis2.soap.SOAPEnvelope;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.util.UUIDGenerator;
import org.apache.axis2.addressing.MessageInformationHeaders;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.addressing.miheaders.RelatesTo;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.rules.SynapseRuleEngine;
import org.jaxen.JaxenException;

/**
 * Created by IntelliJ IDEA.
 * User: saminda
 * Date: Oct 12, 2005
 * Time: 12:31:35 PM
 * To change this template use File | Settings | File Templates.
 */
public class SynapseEngine {

    private AxisEngine axisEngine;

    private SynapseRuleEngine ruleEngine;

    private int synapseState = 0;

    public SynapseEngine() {
        ruleEngine = new SynapseRuleEngine();
    }

    public AxisEngine getAxisEngine() {
        return axisEngine;
    }

    public void setAxisEngine(AxisEngine axisEngine) {
        this.axisEngine = axisEngine;
    }

    public void excecuite(MessageContext messageContext) throws AxisFault {

        messageContext.setProperty(
                SynapseConstants.SynapseRuleEngine.SYNAPSE_RECEIVER,
                ruleEngine.getOperationName());
        ruleEngine.ruleConfiguration(messageContext);


        try {
            ruleEngine.validateXpath(messageContext);
            messageContext.setProperty(
                    SynapseConstants.SynapseRuleEngine.SYNAPSE_RULE_ENGINE,
                    ruleEngine);
        } catch (JaxenException e) {
            throw new AxisFault(e);
        }

        messageContext
                .setProperty(SynapseConstants.SYNAPSE_STATE,
                        synapseStateValidation(messageContext));


        if (axisEngine != null) {
            if (messageContext.getEnvelope().getBody().hasFault()) {
                axisEngine.receiveFault(messageContext);
            } else {
                axisEngine.receive(messageContext);
            }
            /**
             * Now the looping happens for new Rules
             *
             */

            moreRules(messageContext);
            /**
             * If no rules available then time the message to reach its orgianl
             * destiantion, so at this point Synapse will be the client
             *
             * engine.send(MessageContext) will be called at the end.
             *
             */
            synapseAsClient(messageContext);



        }


    }

    private void moreRules(MessageContext returnMsgCtx)
            throws AxisFault {

        Boolean returnValue = (Boolean) returnMsgCtx
                .getProperty(SynapseConstants.MEDEATOT_STATE);
        if (!returnValue.booleanValue()) {
            return;
        } else {
            /**
             * states related to Synapse
             */
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
            newContext.setProperty(
                    SynapseConstants.SynapseRuleEngine.SYNAPSE_RECEIVER,
                    returnMsgCtx.getProperty(
                            SynapseConstants.SynapseRuleEngine.SYNAPSE_RECEIVER));
            newContext.setProperty(
                    SynapseConstants.SynapseRuleEngine.GENERAT_RULE_ARRAY_LIST,
                    returnMsgCtx.getProperty(
                            SynapseConstants.SynapseRuleEngine.GENERAT_RULE_ARRAY_LIST));
            newContext.setServerSide(true);
            newContext.setEnvelope(returnMsgCtx.getEnvelope());
            newContext.setServiceContextID(returnMsgCtx.getServiceContextID());
            axisEngine.receive(newContext);
            moreRules(newContext);
            return;
        }
    }

    public Integer synapseStateValidation(MessageContext messageContext) {
        Integer state = (Integer) messageContext
                .getProperty(SynapseConstants.SYNAPSE_STATE);

        if (state == null) {
            synapseState = 1;
            return new Integer(synapseState);
        } else {
            return state;
        }
    }

    private void synapseAsClient(MessageContext messageContext)
            throws AxisFault {
        SOAPEnvelope env = messageContext.getEnvelope();
        //just for the sake of clarity nothing else
        //need to do more code once the architecture is finalize
        EndpointReference epr = new EndpointReference(
                "http://localhost:8080/axis2/services/MyService");

        MessageSender msgSender = new MessageSender();
        msgSender.setTo(epr);
        msgSender.setSenderTransport(Constants.TRANSPORT_HTTP);

        //need more modification here..
        //need to do more modification once the architecure is finalize..

        msgSender.send("ping", env.getBody().getFirstElement());

    }

}
