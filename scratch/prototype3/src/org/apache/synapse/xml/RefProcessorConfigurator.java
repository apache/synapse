package org.apache.synapse.xml;

import javax.xml.namespace.QName;

import org.apache.axis2.om.OMAttribute;
import org.apache.axis2.om.OMElement;
import org.apache.synapse.Constants;
import org.apache.synapse.Processor;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseException;
import org.apache.synapse.processors.RefProcessor;


public class RefProcessorConfigurator extends
		AbstractProcessorConfigurator {
	private static final QName REF_Q = new QName(Constants.SYNAPSE_NAMESPACE,
			"ref");

	public QName getTagQName() {
		return REF_Q;
	}

	public Processor createProcessor(SynapseEnvironment se, OMElement el) {
		RefProcessor rp = new RefProcessor();
		super.setNameOnProcessor(se, el, rp);
		OMAttribute attr = el.getAttribute(new QName("ref"));
		if (attr==null) throw new SynapseException("<ref> must have attribute ref");
		rp.setRef(attr.getAttributeValue());
		return rp;
	}

}
