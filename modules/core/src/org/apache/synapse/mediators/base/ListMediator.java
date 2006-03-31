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

import java.util.Iterator;

import java.util.List;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.api.Mediator;
import org.apache.synapse.SynapseMessage;


/**
 *
 *         stages, mediations that it has as subelements It is a way of grouping
 *         stuff.
 * 
 */
public abstract class ListMediator implements Mediator {
	
	private Log log = LogFactory.getLog(getClass());

	protected List mediators = null;
	
	public boolean mediate(SynapseMessage smc) {
		log.debug("mediate()");
		if (mediators == null) {
			log.info("mediate called on empty mediator list");
			return true;
		}
		Iterator it = mediators.iterator();
		while (it.hasNext()) {
			Mediator m = (Mediator) it.next();
			log.debug(m.getClass());
			if (!m.mediate(smc))
				return false;
		}
		return true;
	}

	public void setList(List m) {
		log.debug("setting list");
		Iterator it = m.iterator();
		while (it.hasNext()) {
			Mediator x = (Mediator)it.next();
			log.debug(x.getClass());
		}
		mediators = m;
	}
	public List getList() {
		return mediators;
	}

}
