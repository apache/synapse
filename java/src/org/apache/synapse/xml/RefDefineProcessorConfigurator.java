package org.apache.synapse.xml;

import org.apache.synapse.Processor;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseException;
import org.apache.synapse.processors.RefDefineProcessor;
import org.apache.axis2.om.OMElement;
import org.apache.axis2.om.OMAttribute;

import javax.xml.namespace.QName;

/*
 */
public class RefDefineProcessorConfigurator extends AbstractProcessorConfigurator{
    private static final QName REF_DEFINE_Q = new QName(Constants.SYNAPSE_NAMESPACE,
			"refdefine");

    public Processor createProcessor(SynapseEnvironment se, OMElement el) {
        RefDefineProcessor rdp = new RefDefineProcessor();
        super.setNameOnProcessor(se, el, rdp);
		OMAttribute attr = el.getAttribute(new QName("ref"));
		if (attr==null) throw new SynapseException("<ref> must have attribute ref");
		rdp.setRefDefine(attr.getAttributeValue());
		return rdp;
    }

    public QName getTagQName() {
        return REF_DEFINE_Q;
    }
}
