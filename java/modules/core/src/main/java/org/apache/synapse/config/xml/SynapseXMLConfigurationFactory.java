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
import org.apache.synapse.Mediator;
import org.apache.synapse.Startup;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.Entry;
import org.apache.synapse.config.SynapseConfigUtils;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.config.xml.endpoints.EndpointAbstractFactory;
import org.apache.synapse.core.axis2.ProxyService;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.mediators.builtin.DropMediator;
import org.apache.synapse.mediators.builtin.LogMediator;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.util.Iterator;

public class SynapseXMLConfigurationFactory implements ConfigurationFactory {
    
    private static Log log = LogFactory.getLog(SynapseXMLConfigurationFactory.class);

    public SynapseConfiguration getConfiguration(OMElement definitions) {
        
        if (!definitions.getQName().equals(XMLConfigConstants.DEFINITIONS_ELT)) {
            throw new SynapseException(
                    "Wrong QName for this config factory " + definitions.getQName());
        }

        SynapseConfiguration config = new SynapseConfiguration();
        config.setDefaultQName(definitions.getQName());

        SequenceMediator rootSequence = new SequenceMediator();
        rootSequence.setName(org.apache.synapse.SynapseConstants.MAIN_SEQUENCE_KEY);

        Iterator iter = definitions.getChildren();
        
        while (iter.hasNext()) {
            Object o = iter.next();
            if (o instanceof OMElement) {
                OMElement elt = (OMElement) o;
                if (XMLConfigConstants.SEQUENCE_ELT.equals(elt.getQName())) {
                    String key = elt.getAttributeValue(
                            new QName(XMLConfigConstants.NULL_NAMESPACE, "key"));
                    // this could be a sequence def or a mediator of the main sequence
                    if (key != null) {
                        Mediator m = MediatorFactoryFinder.getInstance().getMediator(elt);
                        rootSequence.addChild(m);
                    } else {
                        defineSequence(config, elt);
                    }
                } else if (XMLConfigConstants.ENDPOINT_ELT.equals(elt.getQName())) {
                    defineEndpoint(config, elt);
                } else if (XMLConfigConstants.ENTRY_ELT.equals(elt.getQName())) {
                    defineEntry(config, elt);
                } else if (XMLConfigConstants.PROXY_ELT.equals(elt.getQName())) {
                    defineProxy(config, elt);
                } else if (XMLConfigConstants.REGISTRY_ELT.equals(elt.getQName())) {
                    defineRegistry(config, elt);
                } else if (XMLConfigConstants.TASK_ELT.equals(elt.getQName())) {
                    defineStartup(config, elt);
                } else {
                    Mediator m = MediatorFactoryFinder.getInstance().getMediator(elt);
                    rootSequence.addChild(m);
                }
            }
        }

        if (config.getLocalRegistry().isEmpty() && config.getProxyServices().isEmpty() &&
                rootSequence.getList().isEmpty() && config.getRegistry() != null) {
            OMNode remoteConfigNode = config.getRegistry().lookup("synapse.xml");
            try {
                config = XMLConfigurationBuilder.getConfiguration(SynapseConfigUtils
                    .getStreamSource(remoteConfigNode).getInputStream());
            } catch (XMLStreamException xse) {
                throw new SynapseException("Problem loading remote synapse.xml ", xse);
            }

        }

        // if there is no sequence named main defined locally look for the set of mediators in
        // the root level before trying to look in the registry (hence config.getMainSequence
        // can not be used here)
        if (!config.getLocalRegistry().containsKey(SynapseConstants.MAIN_SEQUENCE_KEY)) {
            // if the root tag does not contain any mediators & registry does not have a
            // entry with key main then use the defualt main sequence
            if (rootSequence.getList().isEmpty() && config.getMainSequence() == null) {
                setDefaultMainSequence(config);
            } else {
                config.addSequence(rootSequence.getName(), rootSequence);
            }
        } else if (!rootSequence.getList().isEmpty()) {
            handleException("Invalid Synapse Configuration : Conflict in resolving the \"main\" " +
                    "mediator\n\tSynapse Configuration cannot have sequence named \"main\" and " +
                    "toplevel mediators simultaniously");
        }

        if (config.getFaultSequence() == null) {
            setDefaultFaultSequence(config);
        }

        return config;
    }

    private static void defineRegistry(SynapseConfiguration config, OMElement elem) {
        if (config.getRegistry() != null) {
            handleException("Only one remote registry can be defined within a configuration");
        }
        config.setRegistry(RegistryFactory.createRegistry(elem));
    }

    private static void defineStartup(SynapseConfiguration config, OMElement elem) {
        Startup startup = StartupFinder.getInstance().getStartup(elem);
        if (config.getStartup(startup.getName()) != null) {
            handleException("Duplicate startup with name : " + startup.getName());
        }
        config.addStartup(startup);
    }

    private static void defineProxy(SynapseConfiguration config, OMElement elem) {
        ProxyService proxy = ProxyServiceFactory.createProxy(elem);
        if (config.getProxyService(proxy.getName()) != null) {
            handleException("Duplicate proxy service with name : " + proxy.getName());
        }
        config.addProxyService(proxy.getName(), proxy);
    }

    private static void defineEntry(SynapseConfiguration config, OMElement elem) {
        Entry entry = EntryFactory.createEntry(elem);
        if (config.getLocalRegistry().get(entry.getKey()) != null) {
            handleException("Duplicate registry entry definition for key : " + entry.getKey());
        }
        config.addEntry(entry.getKey(), entry);
    }

    public static void defineSequence(SynapseConfiguration config, OMElement ele) {

        String name = ele.getAttributeValue(new QName(XMLConfigConstants.NULL_NAMESPACE, "name"));
        if (name != null) {
            if (config.getLocalRegistry().get(name) != null) {
                handleException("Duplicate sequence definition : " + name);
            }
            config.addSequence(name, MediatorFactoryFinder.getInstance().getMediator(ele));
        } else {
            handleException("Invalid sequence definition without a name");
        }
    }

    public static void defineEndpoint(SynapseConfiguration config, OMElement ele) {

        String name = ele.getAttributeValue(new QName(XMLConfigConstants.NULL_NAMESPACE, "name"));
        if (name != null) {
            if (config.getLocalRegistry().get(name.trim()) != null) {
                handleException("Duplicate endpoint definition : " + name);
            }
            Endpoint endpoint =
                    EndpointAbstractFactory.getEndpointFactroy(ele).createEndpoint(ele, false);
            config.addEndpoint(name.trim(), endpoint);
        } else {
            handleException("Invalid endpoint definition without a name");
        }
    }

    /**
     * Return the main sequence if one is not defined. This implementation defaults to
     * a simple sequence with a <send/>
     *
     * @param config the configuration to be updated
     */
    private static void setDefaultMainSequence(SynapseConfiguration config) {
        SequenceMediator main = new SequenceMediator();
        main.setName(SynapseConstants.MAIN_SEQUENCE_KEY);
        main.addChild(new LogMediator());
        main.addChild(new DropMediator());
        config.addSequence(SynapseConstants.MAIN_SEQUENCE_KEY, main);
    }

    /**
     * Return the fault sequence if one is not defined. This implementation defaults to
     * a simple sequence with a <log level="full"/>
     *
     * @param config the configuration to be updated
     */
    private static void setDefaultFaultSequence(SynapseConfiguration config) {
        SequenceMediator fault = new SequenceMediator();
        fault.setName(org.apache.synapse.SynapseConstants.FAULT_SEQUENCE_KEY);
        LogMediator log = new LogMediator();
        log.setLogLevel(LogMediator.FULL);
        fault.addChild(log);
        config.addSequence(org.apache.synapse.SynapseConstants.FAULT_SEQUENCE_KEY, fault);
    }

    private static void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    private static void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }


    public QName getTagQName() {

        return XMLConfigConstants.DEFINITIONS_ELT;
    }

    public Class getSerializerClass() {
        return SynapseXMLConfigurationSerializer.class;
    }

}
