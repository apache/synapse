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

package org.apache.synapse.config.xml;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.axiom.om.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.Startup;
import org.apache.synapse.startup.quartz.SimpleQuartzFactory;

import sun.misc.Service;

public class StartupFinder {

    private static final Log log = LogFactory
            .getLog(ConfigurationFactoryAndSerializerFinder.class);

    private static StartupFinder instance = null;

    /**
     * A map of mediator QNames to implementation class
     */
    private static Map factoryMap = new HashMap(),
            serializerMap = new HashMap();

    public static synchronized StartupFinder getInstance() {
        if (instance == null) {
            instance = new StartupFinder();
        }
        return instance;
    }

    /**
     * Force re initialization next time
     */
    public synchronized void reset() {
        factoryMap.clear();
        instance = null;
    }

    private static final Class[] builtins = {SimpleQuartzFactory.class};

    private StartupFinder() {
        // preregister any built in
        for (int i = 0; i < builtins.length; i++) {
            Class b = builtins[i];
            StartupFactory sf;
            try {
                sf = (StartupFactory) b.newInstance();
            } catch (Exception e) {
                throw new SynapseException("cannot instantiate " + b.getName(), e);

            }
            factoryMap.put(sf.getTagQName(), b);
            serializerMap.put(sf.getTagQName(), sf.getSerializerClass());

        }

        registerExtensions();
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    /**
     * Register pluggable mediator factories from the classpath
     * <p/>
     * This looks for JAR files containing a META-INF/services that adheres to
     * the following
     * http://java.sun.com/j2se/1.3/docs/guide/jar/jar.html#Service%20Provider
     */
    private void registerExtensions() {

        // log.debug("Registering mediator extensions found in the classpath : "
        // + System.getResource("java.class.path"));

        // register MediatorFactory extensions
        Iterator it = Service.providers(StartupFactory.class);
        while (it.hasNext()) {
            StartupFactory sf = (StartupFactory) it.next();
            QName tag = sf.getTagQName();
            factoryMap.put(tag, sf.getClass());
            serializerMap.put(tag, sf.getSerializerClass());
            if (log.isDebugEnabled()) {
                log.debug("Added StartupFactory " + sf.getClass()
                        + " to handle " + tag);
            }
        }
    }

    /**
     * This method returns a Processor given an OMElement. This will be used
     * recursively by the elements which contain processor elements themselves
     * (e.g. rules)
     *
     * @param element
     * @return Processor
     */
    public Startup getStartup(OMElement element) {

        QName qName = element.getQName();
        if (log.isDebugEnabled()) {
            log.debug("Creating the Startup for : " + qName);
        }

        Class cls = (Class) factoryMap.get(qName);
        if (cls == null) {
            String msg = "Unknown Startup type referenced by startup element : " + qName;
            log.error(msg);
            throw new SynapseException(msg);
        }

        try {
            StartupFactory sf = (StartupFactory) cls.newInstance();
            return sf.createStartup(element);

        } catch (InstantiationException e) {
            String msg = "Error initializing configuration factory : " + cls;
            log.error(msg);
            throw new SynapseException(msg, e);

        } catch (IllegalAccessException e) {
            String msg = "Error initializing configuration factory : " + cls;
            log.error(msg);
            throw new SynapseException(msg, e);
        }
    }

    /**
     * This method will serialize the config using the supplied QName (looking
     * up the right class to do it)
     *
     * @param parent  -
     *                Parent OMElement to which the created element will be added if
     *                not null
     * @param startup -
     *                Startup to be serialized
     * @return OMElement startup
     */
    public OMElement serializeStartup(OMElement parent, Startup startup) {

        Class cls = (Class) serializerMap.get(startup.getTagQName());
        if (cls == null) {
            String msg = "Unknown startup type referenced by startup element : "
                    + startup.getTagQName();
            log.error(msg);
            throw new SynapseException(msg);
        }

        try {
            StartupSerializer ss = (StartupSerializer) cls.newInstance();
            return ss.serializeStartup(parent, startup);

        } catch (InstantiationException e) {
            String msg = "Error initializing startup serializer: " + cls;
            log.error(msg);
            throw new SynapseException(msg, e);

        } catch (IllegalAccessException e) {
            String msg = "Error initializing startup ser: " + cls;
            log.error(msg);
            throw new SynapseException(msg, e);
        }
    }

    /*
      * This method exposes all the StartupFactories and its Extensions
      */
    public Map getFactoryMap() {
        return factoryMap;
    }

    /*
	 * This method exposes all the StartupSerializers and its Extensions
	 */
    public Map getSerializerMap() {
        return serializerMap;
    }

    /**
     * Allow the startup finder to act as an XMLToObjectMapper for
     * Startup (i.e. Startup) loaded dynamically from a Registry
     *
     * @param om
     * @return
     */
    public Startup getObjectFromOMNode(OMNode om) {
        if (om instanceof OMElement) {
            return getStartup((OMElement) om);
        } else {
			handleException("Invalid configuration XML : " + om);
		}
		return null;
	}

}
