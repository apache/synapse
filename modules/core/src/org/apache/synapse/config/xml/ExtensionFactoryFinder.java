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
import org.apache.synapse.config.Extension;
import org.apache.synapse.SynapseException;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMAttribute;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import sun.misc.Service;

import javax.xml.namespace.QName;

/**
 * This class loads available ConfigurationFactory implementations from the
 * classpath and makes them available to the Synapse configuration builder.
 *
 * This class is based on J2SE Service Provider model
 * http://java.sun.com/j2se/1.3/docs/guide/jar/jar.html#Service%20Provider
 */

public class ExtensionFactoryFinder {

    private static Map factoryMap = new HashMap();
    private static final Log log = LogFactory.getLog(ExtensionFactoryFinder.class);
    private static ExtensionFactoryFinder instance = null;

    public static synchronized ExtensionFactoryFinder getInstance() {
        if (instance == null) {
            instance = new ExtensionFactoryFinder();
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

    private ExtensionFactoryFinder() {
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

        log.debug("Registering extensions found in the classpath : " +
            System.getProperty("java.class.path"));

        // register ExtensionFactory extensions
        Iterator it = Service.providers(ExtensionFactory.class);
        while (it.hasNext()) {
            ExtensionFactory ef = (ExtensionFactory) it.next();
            factoryMap.put(ef.getTagQName(), ef.getClass());
            log.debug("Added extension factory " + ef.getClass() +
                " to handle '" + ef.getTagQName() + "' extension elements");
        }
    }

    /**
     * This method returns an Extension instance of the correct type given an OMElement.
     *
     * @param elem an OMElement defining extension
     * @return a correct Extension instance
     */
    public Extension getExtension(OMElement elem) {

        QName qName = new QName(elem.getNamespace().getName(), elem.getLocalName());
        log.debug("getConfiguration(" + qName + ")");

        Class cls = (Class) factoryMap.get(qName);

        if (cls == null) {
            String msg = "Unknown extension factory referenced by element : " + qName;
            log.error(msg);
            throw new SynapseException(msg);
        }

        try {
            ExtensionFactory ef = (ExtensionFactory) cls.newInstance();
            return ef.createExtension(elem);

        } catch (InstantiationException e) {
            String msg = "Error initializing extension factory : " + cls;
            log.error(msg);
            throw new SynapseException(msg, e);

        } catch (IllegalAccessException e) {
            String msg = "Error initializing extension factory : " + cls;
            log.error(msg);
            throw new SynapseException(msg, e);
        }
    }
}
