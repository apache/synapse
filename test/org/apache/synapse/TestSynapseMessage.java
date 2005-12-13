package org.apache.synapse;

import org.apache.axis2.AxisFault;

import org.apache.axis2.addressing.AddressingConstants.Submission;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.om.OMAbstractFactory;
import org.apache.axis2.om.OMElement;

import org.apache.axis2.soap.SOAPEnvelope;
import org.apache.synapse.axis2.Axis2SynapseMessage;

public class TestSynapseMessage {
//    public static final String URN_SAMPLE_TO_ADDRESS = "urn:sample-toAddress";
    public static final String URN_SAMPLE_TO_ADDRESS = "http://localhost:7777/axis2/services/Myservice";


    public static Axis2SynapseMessage createSampleSOAP11MessageWithoutAddressing(
            String testingRepository) {
        // create a lightweight Axis Config with no addressing to demonstrate
        // "dumb" SOAP
        MessageContext msgCtx;

        try {
            ConfigurationContextFactory conFac =
                    new ConfigurationContextFactory();
            ConfigurationContext configCtx = conFac
                    .buildConfigurationContext(testingRepository);
            msgCtx = new MessageContext(configCtx);
            msgCtx.setServerSide(true);

            SOAPEnvelope env = OMAbstractFactory.getSOAP11Factory()
                    .getDefaultEnvelope();

            OMElement body = OMAbstractFactory.getOMFactory().createOMElement(
                    "test-body", "urn:test", "test");
            OMAbstractFactory.getOMFactory().createText(body,
                    "Do not be alarmed, this is just a test");

            env.getBody().addChild(body);
            msgCtx.setEnvelope(env);
        } catch (AxisFault e) {
            throw new SynapseException(e);
        }

        return new Axis2SynapseMessage(msgCtx);
    }

    public static Axis2SynapseMessage createSampleSOAP11MessageWithAddressing(
            String testingRepository) {
        Axis2SynapseMessage sm =
                createSampleSOAP11MessageWithoutAddressing(testingRepository);

        OMElement addressingTo = OMAbstractFactory.getOMFactory()
                .createOMElement("To", Submission.WSA_NAMESPACE, "wsa");
        OMAbstractFactory.getOMFactory().createText(addressingTo,
                URN_SAMPLE_TO_ADDRESS);
        sm.getEnvelope().getHeader().addChild(addressingTo);
        SOAPEnvelope env = sm.getEnvelope();
        System.out.println(env);
        return sm;

    }
}
