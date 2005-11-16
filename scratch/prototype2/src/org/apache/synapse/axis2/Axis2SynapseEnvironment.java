package org.apache.synapse.axis2;

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
import org.apache.axis2.om.OMElement;
import org.apache.axis2.util.Utils;
import org.apache.synapse.Constants;

import org.apache.synapse.SynapseEngine;
import org.apache.synapse.SynapseEngineConfigurator;
import org.apache.synapse.SynapseException;
import org.apache.synapse.api.MediatorConfiguration;
import org.apache.synapse.api.SOAPMessageContext;
import org.apache.synapse.api.SynapseEnvironment;
import org.apache.synapse.mediators.MediatorTypes;



public class Axis2SynapseEnvironment implements SynapseEnvironment {
	private SynapseEngine se = new SynapseEngine();
	private ClassLoader cl = null;
	
	public Axis2SynapseEnvironment(OMElement synapseConfiguration, ClassLoader cl) {
		this.cl = cl;
		se.setSynapseEnvironment(this);
		SynapseEngineConfigurator.parse(se, synapseConfiguration);
	}
	
	public void injectMessage(SOAPMessageContext smc) {
		se.process(smc);
	}

	public ClassLoader getClassLoader() {
		return cl;
	}
	
	public void setClassLoader(ClassLoader cl) {
		this.cl = cl;
	}

	public boolean executeMediator(MediatorConfiguration mc,
			SOAPMessageContext smc) {
		MessageContext messageContext = null;
		try {
			messageContext= ((Axis2SOAPMessageContext)smc).getMessageContext();
		}
		catch (ClassCastException cce) {
			throw new SynapseException("A non-Axis2 MC SOAPMessageContext has been passed to the Axis2 MediationExecutor",cce);
		}
		
		ConfigurationContext cc = messageContext.getSystemContext();
		AxisConfiguration ac = cc.getAxisConfiguration();
		AxisEngine ae = new AxisEngine(cc);

		AxisService as = null;
		AxisOperation ao = null;
		messageContext.setProperty(
				Constants.MEDIATOR_CONFIGURATION, mc);
		
		try {
			switch (mc.getMediatorType()) { 
				case  MediatorTypes.SPRING: {

					as = ac.getService(Constants.SPRINGMEDIATOR);
					if (as == null) throw new SynapseException("cannot locate service " +Constants.SPRINGMEDIATOR);
					//((SpringMediatorConfiguration)mc).getApplicationContext().setClassLoader(as.getClassLoader());
					//((SpringMediatorConfiguration)mc).getApplicationContext().refresh();
					
					break;
					
				}
				case MediatorTypes.SERVICE: {
					as = ac.getService(mc.getMediatorName());
					if (as == null) throw new SynapseException("cannot locate service " +mc.getMediatorName());
					break;
				}
				case MediatorTypes.CLASS: {
					as = ac.getService(Constants.CLASSMEDIATOR);
					if (as==null) throw new SynapseException("cannot locate service "+Constants.CLASSMEDIATOR);
					break;
					
				}
				case MediatorTypes.BUILTIN: {
					as = ac.getService(Constants.CLASSMEDIATOR);
					if (as==null) throw new SynapseException("cannot locate service "+Constants.CLASSMEDIATOR);
					break;
				}
					
			}
			
			

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

	public void sendOn(SOAPMessageContext smc) {
		Axis2Sender.sendOn(smc);
		
	}

	public void sendBack(SOAPMessageContext smc) {
		Axis2Sender.sendBack(smc);
		
	}

	
}
