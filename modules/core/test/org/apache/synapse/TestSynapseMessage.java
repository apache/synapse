package org.apache.synapse;

import org.apache.axis2.AxisFault;

import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.synapse.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.axis2.Axis2SynapseMessage;
import org.apache.synapse.util.Axis2EnvSetup;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPHeaderBlock;

public class TestSynapseMessage {
    public static final String URN_SAMPLE_TO_ADDRESS = "urn:sample-toAddress";


    public static Axis2SynapseMessage createSampleSOAP11MessageWithoutAddressing(
            String testingRepository) {
        // create a lightweight Axis Config with no addressing to demonstrate
        // "dumb" SOAP
        MessageContext msgCtx;

        try {
            ConfigurationContext configCtx = ConfigurationContextFactory
                    .createConfigurationContextFromFileSystem(testingRepository,null);
            msgCtx = new MessageContext();
            msgCtx.setConfigurationContext(configCtx);
            msgCtx.setServerSide(true);

            SOAPEnvelope env = OMAbstractFactory.getSOAP11Factory()
                    .getDefaultEnvelope();

            OMElement body = OMAbstractFactory.getOMFactory().createOMElement(
                    "test-body", "urn:test", "test");
            OMAbstractFactory.getOMFactory().createOMText(body,
                    "Do not be alarmed, this is just a test");

            env.getBody().addChild(body);
            msgCtx.setEnvelope(env);
        } catch (AxisFault e) {
            throw new SynapseException(e);
        }
        
        return new Axis2SynapseMessage(msgCtx,null);
    }

    public static Axis2SynapseMessage createSampleSOAP11MessageWithAddressing(
            String testingRepository) {
        Axis2SynapseMessage sm =
                createSampleSOAP11MessageWithoutAddressing(testingRepository);
        SOAPFactory fac = OMAbstractFactory.getSOAP11Factory();
        OMNamespace wsaNS =
                fac.createOMNamespace(AddressingConstants.Final.WSA_NAMESPACE, AddressingConstants.WSA_DEFAULT_PREFIX);
        SOAPHeaderBlock addressingToHeaderBlock =
                fac.createSOAPHeaderBlock(AddressingConstants.WSA_TO, wsaNS);
        SOAPHeaderBlock addressingActionHeaderBlock =
                fac.createSOAPHeaderBlock(AddressingConstants.WSA_ACTION, wsaNS);
        addressingToHeaderBlock.setText(URN_SAMPLE_TO_ADDRESS);
        sm.getEnvelope().getHeader().addChild(addressingToHeaderBlock);
        sm.getEnvelope().getHeader().addChild(addressingActionHeaderBlock);

        return sm;

    }
}
