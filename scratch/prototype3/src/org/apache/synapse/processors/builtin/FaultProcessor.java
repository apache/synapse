package org.apache.synapse.processors.builtin;

import javax.xml.namespace.QName;

import org.apache.axis2.AxisFault;

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.om.OMAbstractFactory;

import org.apache.axis2.om.OMElement;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.util.Utils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Constants;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseMessage;
import org.apache.synapse.axis2.Axis2SOAPMessageContext;

import org.apache.synapse.processors.AbstractProcessor;

/**
 * @author Paul Fremantle
 *         <p>
 *         <xmp><synapse:fault/> </xmp>
 * 
 * 
 */
public class FaultProcessor extends AbstractProcessor {
	private static final QName HEADER_Q = new QName(
			Constants.SYNAPSE_NAMESPACE, "fault");

	private Log log = LogFactory.getLog(getClass());

	
	public void compile(SynapseEnvironment se, OMElement el) {
		super.compile(se, el);
	}

	public boolean process(SynapseEnvironment se, SynapseMessage smc) {
		log.debug("process");
		try {

			MessageContext messageContext = ((Axis2SOAPMessageContext) smc)
					.getMessageContext();
			MessageContext outMC = Utils
					.createOutMessageContext(messageContext);
			outMC.setConfigurationContext(messageContext.getSystemContext());
			outMC.setServerSide(true);

			outMC.setEnvelope(OMAbstractFactory.getSOAP11Factory()
					.getDefaultFaultEnvelope());

			AxisEngine ae = new AxisEngine(messageContext.getSystemContext());
			Object os = messageContext
					.getProperty(MessageContext.TRANSPORT_OUT);
			outMC.setProperty(MessageContext.TRANSPORT_OUT, os);
			Object ti = messageContext
					.getProperty(HTTPConstants.HTTPOutTransportInfo);
			outMC.setProperty(HTTPConstants.HTTPOutTransportInfo, ti);

			ae.send(outMC);
		} catch (AxisFault e) {
			throw new SynapseException(e);
		}
		return false;
	}

	public QName getTagQName() {
		return HEADER_Q;
	}

}
