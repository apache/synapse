package org.apache.synapse.axis2;


import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;

import org.apache.axis2.engine.MessageReceiver;
import org.apache.synapse.Constants;
import org.apache.synapse.api.ConfigurationAware;
import org.apache.synapse.api.EnvironmentAware;
import org.apache.synapse.api.Mediator;
import org.apache.synapse.api.SOAPMessageContext;
import org.apache.synapse.mediators.SpringMediatorConfiguration;

import org.springframework.context.support.GenericApplicationContext;

public class SpringMessageReceiver implements MessageReceiver {

   
	
    public void receive(MessageContext mc) throws AxisFault {
        Mediator mediator = (Mediator) makeNewServiceObject(mc);
        SOAPMessageContext smc = new Axis2SOAPMessageContext(mc);
        boolean resp = mediator.mediate(smc);
        
        mc.setProperty(Constants.MEDIATOR_RESPONSE_PROPERTY, Boolean.valueOf(resp));
    }

    
    protected Object makeNewServiceObject(MessageContext msgContext) {
        
        SpringMediatorConfiguration medConfig = (SpringMediatorConfiguration)msgContext.getProperty(Constants.MEDIATOR_CONFIGURATION);
        GenericApplicationContext ctx = medConfig.getApplicationContext();
        
        Object o = ctx.getBean(medConfig.getBeanName());
        if (EnvironmentAware.class.isAssignableFrom(o.getClass())) {
        	((EnvironmentAware)o).setSynapseEnvironment(Axis2SynapseEnvironmentFinder.getSynapseEnvironment(msgContext));
        }
        if (ConfigurationAware.class.isAssignableFrom(o.getClass())) {
        	((ConfigurationAware)o).setMediationConfiguration(medConfig);
        }
        return o;
    }
	

}
