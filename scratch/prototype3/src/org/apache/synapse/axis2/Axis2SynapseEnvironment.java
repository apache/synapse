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

package org.apache.synapse.axis2;

import java.util.HashMap;
import java.util.Map;

import org.apache.axis2.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.synapse.Processor;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseMessage;

import org.apache.synapse.xml.ProcessorConfiguratorFinder;

/**
 *
 *
 * 
 * <p> This is the Axis2 implementation of the SynapseEnvironment 
 *
 */
public class Axis2SynapseEnvironment implements SynapseEnvironment {
	private Processor mainprocessor = null;

	private ClassLoader cl = null;

	private Map processors = new HashMap();

	private Log log = LogFactory.getLog(getClass());

	public Axis2SynapseEnvironment(OMElement synapseConfiguration,
			ClassLoader cl) {
		super();
		this.cl = cl;
		mainprocessor = ProcessorConfiguratorFinder.getProcessor(this, synapseConfiguration);
	}

	public void injectMessage(SynapseMessage smc) {
		mainprocessor.process(this, smc);
	}

	public ClassLoader getClassLoader() {
		return cl;
	}

	public void setClassLoader(ClassLoader cl) {
		this.cl = cl;
	}

	public void send(SynapseMessage sm) {
		if (sm.isResponse()) 
			Axis2Sender.sendBack(sm);
		else 
			Axis2Sender.sendOn(sm);
	}

	
	public Processor lookupProcessor(String name) {
		return (Processor) processors.get(name);
	}

	public void addProcessor(Processor p) {
		log.debug("adding processor with name " + p.getName());
		if (processors.containsKey(p.getName()))
			log.warn("name " + p.getName() + "already present");
		processors.put(p.getName(), p);
	}

	public Processor getMasterProcessor() {
		return mainprocessor;
	}
	
	public void setMasterProcessor(Processor p) {
		mainprocessor = p;
	}
}
