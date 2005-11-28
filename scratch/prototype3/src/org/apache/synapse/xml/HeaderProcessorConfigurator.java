/*
 * Copyright 2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.synapse.xml;

import javax.xml.namespace.QName;


import org.apache.axis2.om.OMAttribute;
import org.apache.axis2.om.OMElement;
import org.apache.synapse.xml.Constants;
import org.apache.synapse.Processor;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseException;
import org.apache.synapse.processors.builtin.HeaderProcessor;

/**
 * @author Paul Fremantle
 *         <p>
 *         <xmp><synapse:header name="optional" type="to|from|faultto|replyto|action"
 *         value="newvalue"/> </xmp>
 * 
 * 
 */
public class HeaderProcessorConfigurator extends AbstractProcessorConfigurator{
	private static final QName HEADER_Q = new QName(
			Constants.SYNAPSE_NAMESPACE, "header");

	

		private static final QName TYPE_ATT_Q = new QName("type"),
			VALUE_ATT_Q = new QName("value");

		public Processor createProcessor(SynapseEnvironment se, OMElement el) {
			HeaderProcessor hp = new HeaderProcessor();
			super.setNameOnProcessor(se, el, hp);
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
