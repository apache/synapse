package org.apache.synapse.xml;

import javax.xml.namespace.QName;


import org.apache.axis2.om.OMAttribute;
import org.apache.axis2.om.OMElement;

import org.apache.synapse.Constants;
import org.apache.synapse.Processor;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseException;
import org.apache.synapse.processors.mediatortypes.axis2.ServiceMediatorProcessor;

public class ServiceMediatorProcessorConfigurator extends AbstractProcessorConfigurator {
	private static final QName tagName = new QName(Constants.SYNAPSE_NAMESPACE,
			"servicemediator");
	public Processor createProcessor(SynapseEnvironment se, OMElement el) {
		ServiceMediatorProcessor smp = new ServiceMediatorProcessor();
		super.setNameOnProcessor(se,el,smp);
		
		OMAttribute attr = el.getAttribute(new QName("service"));
		if (attr == null)
			throw new SynapseException(
					"<servicemediator> must have <service> attribute");
		smp.setServiceName(attr.getAttributeValue());
		return smp;
	}

	public QName getTagQName() {

		return tagName;
	}

}
