package org.apache.synapse.xml;

import javax.xml.namespace.QName;


import org.apache.axis2.om.OMAttribute;
import org.apache.axis2.om.OMElement;
import org.apache.synapse.Constants;
import org.apache.synapse.Processor;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseException;
import org.apache.synapse.processors.builtin.HeaderProcessor;

/**
 * @author Paul Fremantle
 *         <p>
 *         <xmp><synapse:header type="to|from|faultto|replyto|action"
 *         value="newvalue"/> </xmp>
 * 
 * 
 */
public class HeaderProcessorConfigurator extends AbstractProcessorConfigurator{
	private static final QName HEADER_Q = new QName(
			Constants.SYNAPSE_NAMESPACE, "header");

	

		private static final QName TYPE_ATT_Q = new QName("type"),
			VALUE_ATT_Q = new QName("value");

		public Processor compile(SynapseEnvironment se, OMElement el) {
			HeaderProcessor hp = new HeaderProcessor();
			super.compile(se, el, hp);
			OMAttribute val = el.getAttribute(VALUE_ATT_Q);
			OMAttribute type = el.getAttribute(TYPE_ATT_Q);
			if (val == null || type == null) {
				throw new SynapseException("<header> must have both " + VALUE_ATT_Q
					+ " and " + TYPE_ATT_Q + " attributes: " + el.toString());
			}
			hp.setHeaderType(type.getAttributeValue());
			hp.setValue( val.getAttributeValue());
			return hp;
	}

	public QName getTagQName() {
		return HEADER_Q;
	}

}
