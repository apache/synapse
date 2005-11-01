package org.apache.synapse.rule;

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.AxisFault;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.util.UUIDGenerator;
import org.apache.axis2.addressing.MessageInformationHeaders;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.addressing.miheaders.RelatesTo;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.rule.Rule;

public class RuleExecutor {
    // this should get a mediator and should invoke it with an AxisEngine
    public static boolean execute(Rule rule, MessageContext messageContext,ConfigurationContext configurationContext) throws SynapseException {
        try {
            AxisEngine engine = new AxisEngine(configurationContext);
            MessageContext newMessageContext = copyMessageContext(messageContext,
                    configurationContext);
            newMessageContext.setProperty(SynapseConstants.MEDIATORS, rule.getMediators());
            engine.receive(newMessageContext);


            return (Boolean.TRUE == (Boolean) newMessageContext
                    .getProperty(SynapseConstants.MEDIATION_RESULT));

        } catch (AxisFault axisFault) {
            throw new SynapseException(axisFault);
        }
    }

    /**
     * Note : This will not copy whole thing from the original msgCtx to the new msgCtx
     * @param configurationContext
     */
    private static MessageContext copyMessageContext(MessageContext inMessageContext,
                                                     ConfigurationContext configurationContext) throws SynapseException {
        MessageContext newmsgCtx =
                null;
        try {
            newmsgCtx = new MessageContext(configurationContext);
            MessageInformationHeaders oldMessageInfoHeaders =
                    inMessageContext.getMessageInformationHeaders();
            MessageInformationHeaders messageInformationHeaders =
                    new MessageInformationHeaders();
            messageInformationHeaders.setMessageId(UUIDGenerator.getUUID());
            messageInformationHeaders.setTo(oldMessageInfoHeaders.getReplyTo());
            messageInformationHeaders.setFaultTo(
                    oldMessageInfoHeaders.getFaultTo());
            messageInformationHeaders.setFrom(oldMessageInfoHeaders.getTo());
            messageInformationHeaders.setRelatesTo(
                    new RelatesTo(oldMessageInfoHeaders.getMessageId(),
                            AddressingConstants.Submission.WSA_RELATES_TO_RELATIONSHIP_TYPE_DEFAULT_VALUE));
            messageInformationHeaders.setAction(oldMessageInfoHeaders.getAction());
            newmsgCtx.setMessageInformationHeaders(messageInformationHeaders);
            newmsgCtx.setOperationContext(inMessageContext.getOperationContext());
            newmsgCtx.setServiceContext(inMessageContext.getServiceContext());
            newmsgCtx.setProperty(MessageContext.TRANSPORT_OUT,
                    inMessageContext.getProperty(MessageContext.TRANSPORT_OUT));
            newmsgCtx.setProperty(HTTPConstants.HTTPOutTransportInfo,
                    inMessageContext.getProperty(HTTPConstants.HTTPOutTransportInfo));

            //Setting the charater set encoding
            newmsgCtx.setProperty(MessageContext.CHARACTER_SET_ENCODING, inMessageContext
                    .getProperty(MessageContext.CHARACTER_SET_ENCODING));

            newmsgCtx.setDoingREST(inMessageContext.isDoingREST());
            newmsgCtx.setDoingMTOM(inMessageContext.isDoingMTOM());
            newmsgCtx.setServerSide(inMessageContext.isServerSide());
            newmsgCtx.setServiceGroupContextId(inMessageContext.getServiceGroupContextId());
        } catch (AxisFault axisFault) {
            throw new SynapseException(axisFault);
        }

        return newmsgCtx;
    }
}
