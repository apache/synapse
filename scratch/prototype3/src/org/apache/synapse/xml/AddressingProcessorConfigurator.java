package org.apache.synapse.xml;

import javax.xml.namespace.QName;


import org.apache.axis2.om.OMElement;
import org.apache.synapse.Constants;
import org.apache.synapse.Processor;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.processors.builtin.axis2.AddressingProcessor;

/**
 * @author Paul Fremantle
 *         <p>
 *         This class turns on the addressing module and then calls an empty
 *         service There's probably a better way but this should work!
 * 
 */
public class AddressingProcessorConfigurator extends AbstractProcessorConfigurator {
	private static final QName ADD_Q = new QName(Constants.SYNAPSE_NAMESPACE,
			"addressing");


	public QName getTagQName() {

		return ADD_Q;
	}


	public Processor compile(SynapseEnvironment se, OMElement el) {
		AddressingProcessor ap = new AddressingProcessor();
		super.compile(se,el,ap);
		return ap;
	}

}
