package org.apache.synapse;

import org.apache.axis2.AxisFault;

import org.apache.axis2.addressing.AddressingConstants.Submission;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.om.OMAbstractFactory;
import org.apache.axis2.om.OMElement;

import org.apache.axis2.soap.SOAPEnvelope;
import org.apache.synapse.axis2.Axis2SynapseMessage;

public class SynapseMessageTest {
	public static final String URN_SAMPLE_TO_ADDRESS = "urn:sample-toAddress";

	public static void main(String[] args) {
		SynapseMessage sm = createSampleSOAP11MessageWithoutAddressing();
		System.out.println(sm.getEnvelope());
		SynapseMessage sm2 = createSampleSOAP11MessageWithAddressing();
		System.out.println(sm2.getEnvelope());
	}

	public static Axis2SynapseMessage createSampleSOAP11MessageWithoutAddressing() {
		// create a lightweight Axis Config with no addressing to demonstrate
		// "dumb" SOAP
		AxisConfiguration ac = new AxisConfiguration();
		ConfigurationContext cc = new ConfigurationContext(ac);
		MessageContext msgCtx;
		try {
			msgCtx = new MessageContext(cc);

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

	public static Axis2SynapseMessage createSampleSOAP11MessageWithAddressing() {
		Axis2SynapseMessage sm = createSampleSOAP11MessageWithoutAddressing();

		OMElement addressingTo = OMAbstractFactory.getOMFactory()
				.createOMElement("To", Submission.WSA_NAMESPACE, "wsa");
		OMAbstractFactory.getOMFactory().createText(addressingTo,
				URN_SAMPLE_TO_ADDRESS);
		sm.getEnvelope().getHeader().addChild(addressingTo);
		return sm;

	}
}
