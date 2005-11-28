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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Processor;
import org.apache.synapse.SynapseEnvironment;

/**
 * @author Paul Fremantle
 * 
 * <p> This is the abstract superclass of any tag that isn't a "node". I.e. mediators or builtin
 * mediators like log
 *
 */
public abstract class AbstractProcessorConfigurator implements ProcessorConfigurator {

	
	
	private Log log = LogFactory.getLog(getClass()); 
	public void setNameOnProcessor(SynapseEnvironment se, OMElement el, Processor p) {
		
		OMAttribute nm = el.getAttribute(new QName("name"));
		if (nm != null) {
			String name = nm.getAttributeValue().trim();
			p.setName(name);
			se.addProcessor(p);
		}
		log.debug("compile "+el.getLocalName()+" with name '"+p.getName() +"' on "+p.getClass());
		
	}

	
}
