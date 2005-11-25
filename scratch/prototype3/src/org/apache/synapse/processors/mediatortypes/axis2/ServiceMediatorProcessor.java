package org.apache.synapse.processors.mediatortypes.axis2;



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
import org.apache.synapse.Constants;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseMessage;
import org.apache.synapse.axis2.Axis2SOAPMessageContext;
import org.apache.synapse.processors.AbstractProcessor;

public class ServiceMediatorProcessor extends AbstractProcessor {
	

	private String serviceName = null;

	public boolean process(SynapseEnvironment se, SynapseMessage smc) {
		MessageContext messageContext = null;
		try {
			messageContext = ((Axis2SOAPMessageContext) smc)
					.getMessageContext();
		} catch (ClassCastException cce) {
			throw new SynapseException(
					"A non-Axis2 MC SOAPMessageContext has been passed to the Axis2 MediationExecutor",
					cce);
		}

		

		try {
			ConfigurationContext cc = messageContext.getSystemContext();
			AxisConfiguration ac = cc.getAxisConfiguration();
			AxisEngine ae = new AxisEngine(cc);

			AxisService as = null;
			AxisOperation ao = null;
			
			as = ac.getService(getServiceName());
			if (as == null)
				throw new SynapseException("cannot locate service "
						+ getServiceName());
			
			ao = as.getOperation(Constants.MEDIATE_OPERATION_NAME);
			OperationContext oc = OperationContextFactory
					.createOperationContext(ao.getAxisSpecifMEPConstant(), ao);
			ao.registerOperationContext(messageContext, oc);

			ServiceContext sc = Utils.fillContextInformation(ao, as, cc);
			oc.setParent(sc);

			messageContext.setOperationContext(oc);
			messageContext.setServiceContext(sc);

			messageContext.setAxisOperation(ao);
			messageContext.setAxisService(as);

			ae.receive(messageContext);

		} catch (AxisFault e) {
			throw new SynapseException(e);

		}

		return ((Boolean) messageContext
				.getProperty(Constants.MEDIATOR_RESPONSE_PROPERTY))
				.booleanValue();

	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getServiceName() {
		return serviceName;
	}

}
