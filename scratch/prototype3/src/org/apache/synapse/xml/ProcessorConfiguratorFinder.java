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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.axis2.om.OMElement;
import org.apache.axis2.om.OMNamespace;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.synapse.Processor;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseException;

import sun.misc.Service;

/**
 *
 * 
 * This class is based on J2SE Service Provider model
 * http://java.sun.com/j2se/1.3/docs/guide/jar/jar.html#Service%20Provider
 */

public class ProcessorConfiguratorFinder {

	private static Map lookup = null;

	private static Log log = LogFactory
			.getLog(ProcessorConfiguratorFinder.class);

	private static Class[] processorConfigurators = {
			SynapseProcessorConfigurator.class,
			StageProcessorConfigurator.class, RegexProcessorConfigurator.class,
			XPathProcessorConfigurator.class,
			HeaderProcessorConfigurator.class,
			ClassMediatorProcessorConfigurator.class,
			ServiceMediatorProcessorConfigurator.class,
			LogProcessorConfigurator.class, SendProcessorConfigurator.class,
			FaultProcessorConfigurator.class,
			AddressingProcessorConfigurator.class,
			InProcessorConfigurator.class, OutProcessorConfigurator.class,
			NeverProcessorConfigurator.class, RefProcessorConfigurator.class };

	private static void initialise() {

		if (lookup != null)
			return;
		lookup = new HashMap();

		for (int i = 0; i < processorConfigurators.length; i++) {
			Class c = processorConfigurators[i];
			try {
				lookup.put(((ProcessorConfigurator) c.newInstance())
						.getTagQName(), c);
			} catch (Exception e) {
				throw new SynapseException(e);
			}
		}
		// now try additional processors
		Iterator it = Service.providers(ProcessorConfigurator.class);
		while (it.hasNext()) {
			ProcessorConfigurator p = (ProcessorConfigurator) it.next();
			QName tag = p.getTagQName();
			lookup.put(tag, p.getClass());
			log.debug("added Processor " + p.getClass() + " to handle " + tag);
		}
	}

                            	/**
	 * @param qn
	 * @return the class which implements the Processor for the given QName
	 */
	public static Class find(QName qn) {
		initialise();
		return (Class) lookup.get(qn);
	}
	
	/**
	 * This method returns a Processor given an OMElement. This will be used
	 * recursively by the elements which contain processor elements themselves
	 * (e.g. rules)
	 * 
	 * @param synapseEnv
	 * @param element
	 * @return Processor
	 */
	public static Processor getProcessor(SynapseEnvironment synapseEnv, OMElement element) {
		OMNamespace n = element.getNamespace();
		System.out.println(element.getLocalName());
		Class cls = find(new QName(n.getName(), element
				.getLocalName()));
		try {
			ProcessorConfigurator pc = (ProcessorConfigurator) cls.newInstance();
			Processor p = pc.createProcessor(synapseEnv, element);
			return p;
		} catch (InstantiationException e) {
			throw new SynapseException(e);
		} catch (IllegalAccessException e) {
			throw new SynapseException(e);
		}
	}
}
