package org.apache.synapse.processors.mediatortypes.spring;

import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseMessage;

import org.apache.synapse.api.EnvironmentAware;
import org.apache.synapse.api.Mediator;
import org.apache.synapse.processors.AbstractProcessor;
import org.springframework.context.support.GenericApplicationContext;

/**
 *
 * @see org.apache.synapse.processors.builtin.xslt.XSLTProcessorConfigurator
 * <p> This class is the class that "plugs" Spring-based mediators into Synapse. 
 * <p> A spring based mediator is any object that implements mediator and can be instantiated by
 * Spring (see www.springframework.org). The mediator definition is set up using the 
 *  SpringMediatorProcessorConfigurator class.
 *  
 * This class simply has a Context property which is set with a Spring GenericApplicationContext and 
 * a BeanName property, which is set with the name of the bean  
 *
 */
public class SpringMediatorProcessor extends AbstractProcessor {
	
	private GenericApplicationContext ctx = null;

	private String beanName = null;

	public boolean process(SynapseEnvironment se, SynapseMessage smc) {
		Mediator m = (Mediator) getContext().getBean(getBeanName());
		if (EnvironmentAware.class.isAssignableFrom(m.getClass())) {
			((EnvironmentAware) m).setSynapseEnvironment(se);
		}
		return m.mediate(smc);

	}

	

	public void setContext(GenericApplicationContext ctx) {
		this.ctx = ctx;
	}

	public GenericApplicationContext getContext() {
		return ctx;
	}

	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	public String getBeanName() {
		return beanName;
	}

}
