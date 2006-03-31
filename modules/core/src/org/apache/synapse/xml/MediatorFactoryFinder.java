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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseException;
import org.apache.synapse.mediators.builtin.xslt.XSLTMediatorFactory;
import org.apache.synapse.mediators.conditions.DefaultMediatorFactory;
import org.apache.synapse.mediators.conditions.ExactlyOneMediatorFactory;
import org.apache.synapse.resources.xml.PropertyMediatorFactory;
import org.apache.synapse.resources.xml.ResourceMediatorFactory;
import org.apache.synapse.api.Mediator;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNamespace;

import sun.misc.Service;

/**
 *
 * 
 * This class is based on J2SE Service Provider model
 * http://java.sun.com/j2se/1.3/docs/guide/jar/jar.html#Service%20Provider
 */

public class MediatorFactoryFinder {

	private static Map lookup = null;

	private static Log log = LogFactory
			.getLog(MediatorFactoryFinder.class);

	private static Class[] mediatorFactories = {
			SynapseMediatorFactory.class,
			StageMediatorFactory.class, RegexMediatorFactory.class,
			XPathMediatorFactory.class,
			HeaderMediatorFactory.class,
			ClassMediatorFactory.class,
			ServiceMediatorFactory.class,
			LogMediatorFactory.class, SendMediatorFactory.class,
			FaultMediatorFactory.class,
			AddressingInMediatorFactory.class,
			AddressingOutMediatorFactory.class,
			InMediatorFactory.class, OutMediatorFactory.class,
			NeverMediatorFactory.class, RefMediatorFactory.class,
            XSLTMediatorFactory.class,DefineMediatorFactory.class,
            SendNowMediatorFactory.class,SendMediatorFactory.class,
            DropMediatorFactory.class,
            RefDefineMediatorFactory.class, ExactlyOneMediatorFactory.class,
            DefaultMediatorFactory.class,
            PropertyMediatorFactory.class, ResourceMediatorFactory.class,
            };

	private static void initialise() {

		if (lookup != null)
			return;
		lookup = new HashMap();

		for (int i = 0; i < mediatorFactories.length; i++) {
			Class c = mediatorFactories[i];
			try {
				lookup.put(((MediatorFactory) c.newInstance())
						.getTagQName(), c);
			} catch (Exception e) {
				throw new SynapseException("problem instantiating "+c.getName(), e);
			}
		}
		log.debug("registering extensions");
		log.debug(System.getProperty("java.class.path"));
		// now try additional processors
		Iterator it = Service.providers(MediatorFactory.class);
		while (it.hasNext()) {
			MediatorFactory mf = (MediatorFactory) it.next();
			QName tag = mf.getTagQName();
			lookup.put(tag, mf.getClass());
			log.debug("added MediatorFactory " + mf.getClass() + " to handle " + tag);
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
	public static Mediator getMediator(SynapseEnvironment synapseEnv, OMElement element) {
		OMNamespace n = element.getNamespace();
		
		Class cls = find(new QName(n.getName(), element
				.getLocalName()));
		try {
			MediatorFactory mf = (MediatorFactory) cls.newInstance();
			Mediator m = mf.createMediator(synapseEnv, element);
			return m;
		} catch (InstantiationException e) {
			throw new SynapseException(e);
		} catch (IllegalAccessException e) {
			throw new SynapseException(e);
		}
	}
}
