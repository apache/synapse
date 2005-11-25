package org.apache.synapse.processors.mediatortypes;


import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseMessage;
import org.apache.synapse.api.Mediator;
import org.apache.synapse.processors.AbstractProcessor;

public class ClassMediatorProcessor extends AbstractProcessor {

	private Class clazz = null;

	

	public boolean process(SynapseEnvironment se, SynapseMessage smc) {
		Mediator m = null;

		try {
			m = (Mediator) getClazz().newInstance();
		} catch (Exception e) {
			throw new SynapseException(e);
		}
		return m.mediate(smc);

	}

	
	public void setClazz(Class clazz) {
		this.clazz = clazz;
	}

	public Class getClazz() {
		return clazz;
	}

}
