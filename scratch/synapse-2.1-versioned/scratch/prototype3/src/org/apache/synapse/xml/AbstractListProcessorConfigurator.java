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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;



import org.apache.axis2.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Processor;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.processors.ListProcessor;

/**
 *
 * 
 * <p> This is the abstract parent of any tag which is a "Node" - so &ltstage>, &ltin>&ltout> and &ltnever> all fit this model
 * <p>It recursively creates a list of processors from the children. 
 *
 */
public abstract class AbstractListProcessorConfigurator extends AbstractProcessorConfigurator {

	Log log = LogFactory.getLog(getClass());
	
	public void addChildrenAndSetName(SynapseEnvironment se, OMElement el, ListProcessor p)
	{
		super.setNameOnProcessor(se, el, p);

		Iterator it = el.getChildElements();
		List processors = new LinkedList();
		while (it.hasNext()) {
			OMElement child = (OMElement) it.next();
			Processor proc = ProcessorConfiguratorFinder.getProcessor(se, child);
			if (proc != null)
				processors.add(proc);
			else
				log.info("Unknown child of all" + child.getLocalName());
		}
		p.setList(processors);
		
	}

		

}
