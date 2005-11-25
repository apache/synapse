package org.apache.synapse.xml;

import javax.xml.namespace.QName;


import org.apache.axis2.om.OMElement;
import org.apache.synapse.Constants;
import org.apache.synapse.Processor;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.processors.builtin.axis2.FaultProcessor;

/**
 * @author Paul Fremantle
 *         <p>
 *         <xmp><synapse:fault/> </xmp>
 * 
 * 
 */
public class FaultProcessorConfigurator extends AbstractProcessorConfigurator {
	private static final QName HEADER_Q = new QName(
			Constants.SYNAPSE_NAMESPACE, "fault");

	

	
	public Processor createProcessor(SynapseEnvironment se, OMElement el) {
		FaultProcessor fp = new FaultProcessor();
		super.setNameOnProcessor(se, el, fp);
		return fp;
	}

		public QName getTagQName() {
		return HEADER_Q;
	}

}
