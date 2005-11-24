package org.apache.synapse.processors.mediators;

import javax.xml.namespace.QName;

import org.apache.axis2.om.OMAttribute;
import org.apache.axis2.om.OMElement;
import org.apache.synapse.Constants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.api.Mediator;
import org.apache.synapse.api.SOAPMessageContext;
import org.apache.synapse.api.SynapseEnvironment;
import org.apache.synapse.processors.AbstractProcessor;

public class ClassMediatorProcessor extends AbstractProcessor {
	private static final QName CLM_Q = new QName(Constants.SYNAPSE_NAMESPACE,
			"classmediator");

	private Class clazz = null;

	public void compile(SynapseEnvironment se, OMElement el) {
		super.compile(se, el);

		OMAttribute clsName = el.getAttribute(new QName("class"));
		if (clsName == null)
			throw new SynapseException("missing class attribute on element"
					+ el.toString());
		try {
			clazz = se.getClassLoader().loadClass(clsName.getAttributeValue());
		} catch (ClassNotFoundException e) {
			throw new SynapseException("class loading error", e);
		}

	}

	public boolean process(SynapseEnvironment se, SOAPMessageContext smc) {
		Mediator m = null;

		try {
			m = (Mediator) clazz.newInstance();
		} catch (Exception e) {
			throw new SynapseException(e);
		}
		return m.mediate(smc);

	}

	public QName getTagQName() {
		return CLM_Q;
	}

}
