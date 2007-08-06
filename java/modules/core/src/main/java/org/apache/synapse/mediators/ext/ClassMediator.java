/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.mediators.ext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Constants;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;

import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.axiom.om.OMElement;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * The class mediator delegates the mediation to a new instance of a specified
 * class. The specified class must implement the Mediator interface
 * 
 * @see Mediator
 */
public class ClassMediator extends AbstractMediator implements ManagedLifecycle {

	private static final Log log = LogFactory.getLog(ClassMediator.class);

	private static final Log trace = LogFactory.getLog(Constants.TRACE_LOGGER);

	private Mediator mediator = null;

    private List properties = new ArrayList();

    /**
	 * Don't use a new instance... do one instance of the object per instance of
	 * this mediator
	 * 
	 * @param synCtx
	 *            the message context
	 * @return as per standard semantics
	 */
	public boolean mediate(MessageContext synCtx) {

		if (log.isDebugEnabled()) {
			log.debug("Class mediator <" + mediator.getClass()
					+ ">:: mediate()");
		}
		boolean shouldTrace = shouldTrace(synCtx.getTracingState());
		if (shouldTrace) {
			trace.trace("Start : Class mediator");
		}

		try {

			if (mediator == null) {
				if (log.isDebugEnabled()) {
					log.debug("The instance of the specified mediator is null");
				}
				return true;
			}
			if (shouldTrace) {
				trace.trace("Executing an instance of the specified class : "
						+ mediator.getClass());
			}
			return mediator.mediate(synCtx);
		} finally {
			if (shouldTrace) {
				trace.trace("End : Class mediator");
			}
		}
	}

	public void destroy() {
		log.debug("destroy");
		if (mediator instanceof ManagedLifecycle) {
			((ManagedLifecycle) mediator).destroy();
		}
	}

	public void init(SynapseEnvironment se) {
		log.debug("init");
		if (mediator == null) {
			log.debug("init called before mediator set");
			return;
		}

		if (mediator instanceof ManagedLifecycle) {
			((ManagedLifecycle) mediator).init(se);

		}
	}

	public void setMediator(Mediator mediator) {
		this.mediator = mediator;
	}

	public Mediator getMediator() {
		return mediator;
	}

    public void addProperty(OMElement property) {
        properties.add(property);
    }

    public List getProperties() {
        return this.properties;
    }
}
