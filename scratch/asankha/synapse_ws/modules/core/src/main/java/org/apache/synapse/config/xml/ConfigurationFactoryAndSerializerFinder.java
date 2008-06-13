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

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.config.XMLToObjectMapper;
import sun.misc.Service;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This class is based on J2SE Service Provider model
 * http://java.sun.com/j2se/1.3/docs/guide/jar/jar.html#Service%20Provider
 * <p/>
 * It deals with both the problem of turning an XML into a Synapse config and vice-versa
 */
public class ConfigurationFactoryAndSerializerFinder implements XMLToObjectMapper {

    private static final Log log = LogFactory
            .getLog(ConfigurationFactoryAndSerializerFinder.class);

    private static final Class[] configurationFactories = {
            SynapseXMLConfigurationFactory.class,
    };


    private static ConfigurationFactoryAndSerializerFinder instance = null;

    /**
     * A map of mediator QNames to implementation class
     */
    private static Map factoryMap = new HashMap();

    private static Map serializerMap = new HashMap();

    public static synchronized ConfigurationFactoryAndSerializerFinder getInstance() {
        if (instance == null) {
            instance = new ConfigurationFactoryAndSerializerFinder();
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

    private ConfigurationFactoryAndSerializerFinder() {

        factoryMap = new HashMap();

        for (int i = 0; i < configurationFactories.length; i++) {
            Class c = configurationFactories[i];
            try {
                ConfigurationFactory fac = (ConfigurationFactory) c.newInstance();
                factoryMap.put(fac.getTagQName(), c);
                serializerMap.put(fac.getTagQName(), fac.getSerializerClass());
            } catch (Exception e) {
                throw new SynapseException("Error instantiating " + c.getName(), e);
            }
        }
        // now iterate through the available pluggable mediator factories
        registerExtensions();
    }

    private void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    /**
     * Register pluggable mediator factories from the classpath
     * <p/>
     * This looks for JAR files containing a META-INF/services that adheres to the following
     * http://java.sun.com/j2se/1.3/docs/guide/jar/jar.html#Service%20Provider
     */
    private void registerExtensions() {

        // register MediatorFactory extensions
        Iterator it = Service.providers(ConfigurationFactory.class);
        while (it.hasNext()) {
            ConfigurationFactory cf = (ConfigurationFactory) it.next();
            QName tag = cf.getTagQName();
            factoryMap.put(tag, cf.getClass());
            serializerMap.put(tag, cf.getSerializerClass());
            if (log.isDebugEnabled()) {
                log.debug("Added MediatorFactory " + cf.getClass() + " to handle " + tag);
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
    public SynapseConfiguration getConfiguration(OMElement element) {

        String localName = element.getLocalName();
        QName qName = null;
        if (element.getNamespace() != null) {
            qName = new QName(element.getNamespace().getNamespaceURI(), localName);
        } else {
            qName = new QName(localName);
        }
        if (log.isDebugEnabled()) {
            log.debug("getConfiguration(" + qName + ")");
        }
        Class cls = (Class) factoryMap.get(qName);


        if (cls == null) {
            String msg = "Unknown Configuration type " +
                    "referenced by configuration element : " + qName;
            log.error(msg);
            throw new SynapseException(msg);
        }

        try {
            ConfigurationFactory cf = (ConfigurationFactory) cls.newInstance();
            return cf.getConfiguration(element);

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
     * @param synCfg
     * @return
     */
    public static OMElement serializeConfiguration(SynapseConfiguration synCfg) {
        if (synCfg.getDefaultQName() == null) {
            return serializeConfiguration(synCfg, XMLConfigConstants.DEFINITIONS_ELT);
        } else {
            return serializeConfiguration(synCfg, synCfg.getDefaultQName());
        }
    }

    /**
     * This method will serialize the config using the supplied QName
     * (looking up the right class to do it)
     *
     * @param synCfg
     * @param qName
     * @throws XMLStreamException
     */
    public static OMElement serializeConfiguration(SynapseConfiguration synCfg, QName qName) {

        Class cls = (Class) serializerMap.get(qName);
        if (cls == null) {
            String msg = "Unknown Configuration type " +
                    "referenced by configuration element : " + qName;
            log.error(msg);
            throw new SynapseException(msg);
        }

        try {
            ConfigurationSerializer cs = (ConfigurationSerializer) cls.newInstance();
            return cs.serializeConfiguration(synCfg);

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
     * This method exposes all the ConfigurationFactories and its Extensions
     *
     * @return Map of factories
     */
    public Map getFactoryMap() {
        return factoryMap;
    }

    /**
     * This method exposes all the ConfigurationSerializer and its Extensions
     *
     * @return Map of serializers
     */
    public static Map getSerializerMap() {
        return serializerMap;
    }

    /**
     * Allow the mediator factory finder to act as an XMLToObjectMapper for Mediators
     * (i.e. Sequence Mediator) loaded dynamically from a Registry
     *
     * @param om
     * @return
     */
    public Object getObjectFromOMNode(OMNode om) {
        if (om instanceof OMElement) {
            return getConfiguration((OMElement) om);
        } else {
            handleException("Invalid configuration XML : " + om);
        }
        return null;
    }
}
