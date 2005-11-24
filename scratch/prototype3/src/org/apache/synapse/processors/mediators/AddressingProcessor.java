package org.apache.synapse.processors.mediators;

import javax.xml.namespace.QName;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.context.OperationContextFactory;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.AxisEngine;

import org.apache.axis2.util.Utils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Constants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.api.SOAPMessageContext;
import org.apache.synapse.api.SynapseEnvironment;

import org.apache.synapse.axis2.Axis2SOAPMessageContext;

import org.apache.synapse.processors.AbstractProcessor;

/**
 * @author Paul Fremantle
 *         <p>
 *         This class turns on the addressing module and then calls an empty
 *         service There's probably a better way but this should work!
 * 
 */
public class AddressingProcessor extends AbstractProcessor {
	private static final QName ADD_Q = new QName(Constants.SYNAPSE_NAMESPACE,
			"addressing");

	private Log log = LogFactory.getLog(getClass());

	public boolean process(SynapseEnvironment se, SOAPMessageContext smc) {
		log.debug("process");
		try {
			MessageContext mc = ((Axis2SOAPMessageContext) smc)
					.getMessageContext();
			ConfigurationContext cc = mc.getSystemContext();
			AxisConfiguration ac = cc.getAxisConfiguration();
			AxisEngine ae = new AxisEngine(cc);
			AxisService as = ac.getService(Constants.EMPTYMEDIATOR);
			if (as == null)
				throw new SynapseException("cannot locate service "
						+ Constants.EMPTYMEDIATOR);
			ac.engageModule(new QName(
					org.apache.axis2.Constants.MODULE_ADDRESSING));
			AxisOperation ao = as
					.getOperation(Constants.MEDIATE_OPERATION_NAME);
			OperationContext oc = OperationContextFactory
					.createOperationContext(ao.getAxisSpecifMEPConstant(), ao);
			ao.registerOperationContext(mc, oc);

			ServiceContext sc = Utils.fillContextInformation(ao, as, cc);
			oc.setParent(sc);

			mc.setOperationContext(oc);
			mc.setServiceContext(sc);

			mc.setAxisOperation(ao);
			mc.setAxisService(as);

			ae.receive(mc);

		} catch (AxisFault e) {
			throw new SynapseException(e);
		}
		return true;
	}

	public QName getTagQName() {

		return ADD_Q;
	}

}
