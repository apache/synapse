package org.apache.synapse.xml;

import javax.xml.namespace.QName;

import org.apache.axis2.om.OMAttribute;
import org.apache.axis2.om.OMElement;
import org.apache.synapse.Constants;
import org.apache.synapse.Processor;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseException;
import org.apache.synapse.processors.mediatortypes.ClassMediatorProcessor;

public class ClassMediatorProcessorConfigurator extends AbstractProcessorConfigurator {
	private static final QName CLM_Q = new QName(Constants.SYNAPSE_NAMESPACE,
			"classmediator");
	public Processor compile(SynapseEnvironment se, OMElement el) {
		ClassMediatorProcessor cmp = new ClassMediatorProcessor();
		super.compile(se, el, cmp);

		OMAttribute clsName = el.getAttribute(new QName("class"));
		if (clsName == null)
			throw new SynapseException("missing class attribute on element"
					+ el.toString());
		try {
			Class clazz = se.getClassLoader().loadClass(clsName.getAttributeValue());
			cmp.setClazz(clazz);
		} catch (ClassNotFoundException e) {
			throw new SynapseException("class loading error", e);
		}
		return cmp;

	}

	
	public QName getTagQName() {
		return CLM_Q;
	}

}
