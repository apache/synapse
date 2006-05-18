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
package org.apache.synapse.config.xml;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.config.Configuration;
import org.apache.synapse.SynapseException;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMAttribute;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import sun.misc.Service;

import javax.xml.namespace.QName;

/**
 *
 *
 * This class is based on J2SE Service Provider model
 * http://java.sun.com/j2se/1.3/docs/guide/jar/jar.html#Service%20Provider
 */

public class ConfigurationFactoryFinder {

    private static Map factoryMap = new HashMap();
    private static final Log log = LogFactory.getLog(ConfigurationFactoryFinder.class);
    private static ConfigurationFactoryFinder instance = null;

    public static synchronized ConfigurationFactoryFinder getInstance() {
        if (instance == null) {
            instance = new ConfigurationFactoryFinder();
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

    private ConfigurationFactoryFinder() {
        factoryMap = new HashMap();
        // now iterate through the available pluggable mediator factories
        registerExtensions();
    }

    /**
     * Register pluggable Configuration factories from the classpath
     *
     * This looks for JAR files containing a META-INF/services that adheres to the following
     * http://java.sun.com/j2se/1.3/docs/guide/jar/jar.html#Service%20Provider
     */
    private void registerExtensions() {

        log.debug("registering extensions found in the classpath : " + System.getProperty("java.class.path"));

        // register ConfigurationFactory extensions
        Iterator it = Service.providers(ConfigurationFactory.class);
        while (it.hasNext()) {
            ConfigurationFactory cf = (ConfigurationFactory) it.next();
            String type = cf.getType();
            factoryMap.put(type, cf.getClass());
            log.debug("Added ConfigurationFactory " + cf.getClass() +
                " to handle '" + type + "' configuration definitions");
        }
    }

    /**
     * This method returns a Configuration of the correct type given an OMElement.
     *
     * @param elem an OMElement defining a named Configuration definition
     * @return a correct Configuration object
     */
    public Configuration getConfiguration(OMElement elem) {

        OMAttribute type = elem.getAttribute(new QName(Constants.NULL_NAMESPACE, "type"));
        log.debug("getConfiguration( type = " + type.getAttributeValue() + " )");

        Class cls = (Class) factoryMap.get(type.getAttributeValue());

        if (cls == null) {
            String msg = "Unknown Configuration factory referenced by type : " + type.getAttributeValue();
            log.error(msg);
            throw new SynapseException(msg);
        }

        try {
            ConfigurationFactory cf = (ConfigurationFactory) cls.newInstance();
            return cf.createConfiguration(elem);

        } catch (InstantiationException e) {
            String msg = "Error initializing Configuration factory : " + cls;
            log.error(msg);
            throw new SynapseException(msg, e);

        } catch (IllegalAccessException e) {
            String msg = "Error initializing Configuration factory : " + cls;
            log.error(msg);
            throw new SynapseException(msg, e);
        }
    }
}
