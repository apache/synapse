package org.apache.synapse.axis2;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.SOAPMessageContext;
import org.apache.axis2.engine.MessageReceiver;
import org.apache.synapse.ConfigurationAware;
import org.apache.synapse.Constants;
import org.apache.synapse.EnvironmentAware;
import org.apache.synapse.Mediator;
import org.apache.synapse.SynapseException;
import org.apache.synapse.mediators.ClassMediatorConfiguration;

public class ClassMessageReceiver implements MessageReceiver {

	  public void receive(MessageContext mc) throws AxisFault {
	        Mediator mediator = (Mediator) makeNewServiceObject(mc);
	        boolean resp = mediator.mediate((SOAPMessageContext)mc);
	        mc.setProperty(Constants.RESPONSE_PROPERTY, Boolean.valueOf(resp));
	    }

	    
	    protected Object makeNewServiceObject(MessageContext msgContext) {
	        
	        ClassMediatorConfiguration medConfig = (ClassMediatorConfiguration)msgContext.getProperty(Constants.MEDIATOR_CONFIGURATION);
	        Class clazz = medConfig.getMediatorClass();
	        
	        Object o;
			try {
				o = clazz.newInstance();
			} catch (Exception e) {
				throw new SynapseException(e);
			}
	        if (EnvironmentAware.class.isAssignableFrom(o.getClass())) {
	        	((EnvironmentAware)o).setSynapseEnvironment(Axis2SynapseEnvironmentFinder.getSynapseEnvironment(msgContext));
	        }
	        if (ConfigurationAware.class.isAssignableFrom(o.getClass())) {
	        	((ConfigurationAware)o).setMediationConfiguration(medConfig);
	        }
	        return o;
	    }

}
