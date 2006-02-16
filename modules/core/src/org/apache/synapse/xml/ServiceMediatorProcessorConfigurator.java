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
