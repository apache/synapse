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

package org.apache.synapse.processors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Processor;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseMessage;

/**
 * @author Paul Fremantle
 * 
 * Calls another processor which is referred to by the Ref property.
 *
 */
public class RefProcessor extends AbstractProcessor {
	private Log log = LogFactory.getLog(getClass());
	private String ref = null;
	
	public boolean process(SynapseEnvironment se, SynapseMessage sm) {
		log.debug("process");
		Processor p = se.lookupProcessor(getRef());
		if (p==null) log.debug("processor with name "+this.getRef()+" not found");
		else return p.process(se, sm);
		return true;
	}

	public void setRef(String ref) {
		this.ref = ref;
	}

	public String getRef() {
		return ref;
	}

		

}
