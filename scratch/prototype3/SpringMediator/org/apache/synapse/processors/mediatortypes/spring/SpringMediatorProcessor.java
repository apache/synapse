package org.apache.synapse.processors.mediatortypes.spring;



import javax.xml.namespace.QName;

import org.apache.synapse.Constants;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseMessage;

import org.apache.synapse.api.EnvironmentAware;
import org.apache.synapse.api.Mediator;
import org.apache.synapse.processors.AbstractProcessor;
import org.springframework.context.support.GenericApplicationContext;

public class SpringMediatorProcessor extends AbstractProcessor {
	private static final QName tagName = new QName(Constants.SYNAPSE_NAMESPACE
			+ "/spring", "springmediator");

	private GenericApplicationContext ctx = null;

	private String beanName = null;

	public boolean process(SynapseEnvironment se, SynapseMessage smc) {
		Mediator m = (Mediator) getContext().getBean(getBeanName());
		if (EnvironmentAware.class.isAssignableFrom(m.getClass())) {
			((EnvironmentAware) m).setSynapseEnvironment(se);
		}
		return m.mediate(smc);

	}

	public QName getTagQName() {

		return tagName;
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
