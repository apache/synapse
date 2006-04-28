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

package org.apache.synapse.mediators.base;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseMessage;
import org.apache.synapse.api.Mediator;

/**
 *
 * 
 * Calls another processor which is referred to by the Ref property.
 *
 */
public class RefMediator implements Mediator {
	private Log log = LogFactory.getLog(getClass());
	private String ref = null;
	
	public boolean mediate(SynapseMessage sm) {
		log.debug("mediate");
		Mediator m = sm.getSynapseEnvironment().lookupMediator(getRef());
		if (m==null) log.debug("mediator with name "+this.getRef()+" not found");
		else return m.mediate(sm);
		return true;
	}

	public void setRef(String ref) {
		this.ref = ref;
	}

	public String getRef() {
		return ref;
	}

		

}
