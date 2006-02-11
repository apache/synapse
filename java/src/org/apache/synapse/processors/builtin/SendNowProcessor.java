package org.apache.synapse.processors.builtin;

import org.apache.synapse.processors.AbstractProcessor;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseMessage;
import org.apache.synapse.SynapseException;
import org.apache.synapse.axis2.Axis2SynapseMessage;
import org.apache.synapse.axis2.Axis2FlexibleMEPClient;
import org.apache.synapse.axis2.DynamicAxisOperation;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ServiceGroupContext;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.AxisFault;
import org.apache.axis2.transport.TransportUtils;
import org.apache.axis2.soap.SOAPEnvelope;
import org.apache.axis2.client.OperationClient;
import org.apache.axis2.client.Options;
import org.apache.axis2.util.UUIDGenerator;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.deployment.util.PhasesInfo;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.AxisEngine;
import org.apache.wsdl.WSDLConstants;

import javax.xml.namespace.QName;

/**
 */
public class SendNowProcessor extends AbstractProcessor {
    public boolean process(SynapseEnvironment se, SynapseMessage sm) {
        MessageContext mc = ((Axis2SynapseMessage)sm).getMessageContext();

        try {
            MessageContext response = send(mc);
            if (response.getProperty(MessageContext.TRANSPORT_IN) !=null) {
                SOAPEnvelope resenvelope = TransportUtils.createSOAPMessage(
                response, mc.getEnvelope().getNamespace()
                .getName());

                response.setEnvelope(resenvelope);
                outMessageSerialization(response,mc,sm);
            }


        } catch (AxisFault axisFault) {
            throw new SynapseException(axisFault);
        }
        // this will stop processing message and what User
        return false;
    }

    public MessageContext send(MessageContext smc) throws AxisFault {
        ConfigurationContext cc = smc.getConfigurationContext();
        AxisConfiguration ac = cc.getAxisConfiguration();
        PhasesInfo phasesInfo = ac.getPhasesInfo();

        // setting operation default chains
        if (ac.getService("__ANONYMOUS_SERVICE__") == null) {
            DynamicAxisOperation operation = new DynamicAxisOperation(new QName(
                            "__DYNAMIC_OPERATION__"));
            AxisService axisAnonymousService =
                    new AxisService("__ANONYMOUS_SERVICE__");
            axisAnonymousService.addOperation(operation);
            ac.addService(axisAnonymousService);
            phasesInfo.setOperationPhases(operation);
        }
        ServiceGroupContext sgc = new ServiceGroupContext(cc,
                ac.getService("__ANONYMOUS_SERVICE__").getParent());
        ServiceContext sc =
                sgc.getServiceContext(new AxisService("__ANONYMOUS_SERVICE__"));

        MessageContext mc = new MessageContext();
        mc.setConfigurationContext(sc.getConfigurationContext());
        ///////////////////////////////////////////////////////////////////////
        // filtering properties
        if (smc.getSoapAction() != null)
            mc.setSoapAction(smc.getSoapAction());
        if (smc.getWSAAction() != null)
            mc.setWSAAction(smc.getWSAAction());
        if (smc.getFrom() != null)
            mc.setFrom(smc.getFrom());
        if (smc.getMessageID() != null)
            mc.setMessageID(smc.getMessageID());
        else
            mc.setMessageID(String.valueOf("uuid:"
                    + UUIDGenerator.getUUID()));
        if (smc.getReplyTo() != null)
            mc.setReplyTo(smc.getReplyTo());
        if (smc.getRelatesTo() != null)
            mc.setRelatesTo(smc.getRelatesTo());
        if (smc.getTo() != null) {
            mc.setTo(smc.getTo());
        } else {
            throw new AxisFault(
                    "To canno't be null, if null Synapse can't infer the transport");
        }
        if (smc.isDoingREST()) {
            mc.setDoingREST(true);
        }
        mc.setEnvelope(Axis2FlexibleMEPClient.outEnvelopeConfiguration(smc));

        AxisOperation axisAnonymousOperation =
                ac.getService("__ANONYMOUS_SERVICE__")
                        .getOperation(new QName("__DYNAMIC_OPERATION__"));

        Options options = new Options();
        OperationClient mepClient =
                axisAnonymousOperation.createClient(sc,options);

        mepClient.addMessageContext(mc);
        mepClient.execute(true);
        MessageContext response = mepClient
                .getMessageContext(WSDLConstants.MESSAGE_LABEL_IN_VALUE);


        response.setProperty(MessageContext.TRANSPORT_OUT,
                smc.getProperty(MessageContext.TRANSPORT_OUT));
        response.setProperty(org.apache.axis2.Constants.OUT_TRANSPORT_INFO,
                smc.getProperty(
                        org.apache.axis2.Constants.OUT_TRANSPORT_INFO));

        // If request is REST we assume the response is REST, so set the
        // variable
        response.setDoingREST(smc.isDoingREST());

        return response;
    }

    private void outMessageSerialization(MessageContext mcn,MessageContext mco,SynapseMessage sm) throws AxisFault {

        // copying important configuration stuff
        sm.setResponse(true);
        // as agreed upone
        mcn.setTo(null);
        Object os = mco
                .getProperty(MessageContext.TRANSPORT_OUT);
        mcn.setProperty(MessageContext.TRANSPORT_OUT, os);
        TransportInDescription ti = mco.getTransportIn();
        mcn.setTransportIn(ti);
        mcn.setServerSide(true);

        AxisEngine ae = new AxisEngine(mco.getConfigurationContext());
        ae.send(mcn);


    }

}
