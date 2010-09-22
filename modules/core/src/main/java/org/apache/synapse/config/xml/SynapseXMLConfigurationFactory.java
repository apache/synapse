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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Mediator;
import org.apache.synapse.Startup;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.message.store.MessageStore;
import org.apache.synapse.commons.executors.PriorityExecutor;
import org.apache.synapse.commons.executors.config.PriorityExecutorFactory;
import org.apache.synapse.config.Entry;
import org.apache.synapse.config.SynapseConfigUtils;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.config.xml.endpoints.EndpointFactory;
import org.apache.synapse.config.xml.eventing.EventSourceFactory;
import org.apache.synapse.core.axis2.ProxyService;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.eventing.SynapseEventSource;
import org.apache.synapse.registry.Registry;
import org.apache.axis2.AxisFault;

import javax.xml.namespace.QName;
import java.util.Iterator;
import java.util.Properties;

public class SynapseXMLConfigurationFactory implements ConfigurationFactory {

    private static Log log = LogFactory.getLog(SynapseXMLConfigurationFactory.class);

    public SynapseConfiguration getConfiguration(OMElement definitions, Properties properties) {

        if (!definitions.getQName().equals(XMLConfigConstants.DEFINITIONS_ELT)) {
            throw new SynapseException(
                    "Wrong QName for this configuration factory " + definitions.getQName());
        }
        SynapseConfiguration config = SynapseConfigUtils.newConfiguration();               
        config.setDefaultQName(definitions.getQName());

        Iterator itr = definitions.getChildren();
        while (itr.hasNext()) {
            Object o = itr.next();
            if (o instanceof OMElement) {
                OMElement elt = (OMElement) o;
                if (XMLConfigConstants.SEQUENCE_ELT.equals(elt.getQName())) {
                    String key = elt.getAttributeValue(
                            new QName(XMLConfigConstants.NULL_NAMESPACE, "key"));
                    // this could be a sequence def or a referred sequence
                    if (key != null) {
                        handleException("Referred sequences are not allowed at the top level");
                    } else {
                        defineSequence(config, elt, properties);
                    }
                } else if (XMLConfigConstants.ENDPOINT_ELT.equals(elt.getQName())) {
                    defineEndpoint(config, elt, properties);
                } else if (XMLConfigConstants.ENTRY_ELT.equals(elt.getQName())) {
                    defineEntry(config, elt, properties);
                } else if (XMLConfigConstants.PROXY_ELT.equals(elt.getQName())) {
                    defineProxy(config, elt, properties);
                } else if (XMLConfigConstants.REGISTRY_ELT.equals(elt.getQName())) {
                    defineRegistry(config, elt, properties);
                } else if (XMLConfigConstants.EVENT_SOURCE_ELT.equals(elt.getQName())) {
                    defineEventSource(config, elt, properties);
                } else if (XMLConfigConstants.EXECUTOR_ELT.equals(elt.getQName())) {
                    defineExecutor(config, elt, properties);
                } else if(XMLConfigConstants.MESSAGE_STORE_ELT.equals(elt.getQName())) {
                    defineMessageStore(config, elt, properties);
                } else if (StartupFinder.getInstance().isStartup(elt.getQName())) {
                    defineStartup(config, elt, properties);
                } else if (XMLConfigConstants.DESCRIPTION_ELT.equals(elt.getQName())) {
                    config.setDescription(elt.getText());
                } else {
                    handleException("Invalid configuration element at the top level, one of \'sequence\', " +
                            "\'endpoint\', \'proxy\', \'eventSource\', \'localEntry\', \'priorityExecutor\' " +
                            "or \'registry\' is expected");
                }
            }
        }

        return config;
    }

    public static Registry defineRegistry(SynapseConfiguration config, OMElement elem,
                                          Properties properties) {
        if (config.getRegistry() != null) {
            handleException("Only one remote registry can be defined within a configuration");
        }
        Registry registry = RegistryFactory.createRegistry(elem, properties);
        config.setRegistry(registry);
        return registry;
    }

    public static Startup defineStartup(SynapseConfiguration config, OMElement elem,
                                        Properties properties) {
        Startup startup = StartupFinder.getInstance().getStartup(elem, properties);
        config.addStartup(startup);
        return startup;
    }

    public static ProxyService defineProxy(SynapseConfiguration config, OMElement elem,
                                           Properties properties) {
        ProxyService proxy = ProxyServiceFactory.createProxy(elem, properties);
        config.addProxyService(proxy.getName(), proxy);
        return proxy;
    }

   public static Entry defineEntry(SynapseConfiguration config, OMElement elem,
                                   Properties properties) {
        Entry entry = EntryFactory.createEntry(elem, properties);
        config.addEntry(entry.getKey(), entry);
        return entry;
    }

    public static Mediator defineSequence(SynapseConfiguration config, OMElement ele,
                                          Properties properties) {

        String name = ele.getAttributeValue(new QName(XMLConfigConstants.NULL_NAMESPACE, "name"));
        if (name != null) {
            Mediator mediator = MediatorFactoryFinder.getInstance().getMediator(ele, properties);
            config.addSequence(name, mediator);
            // mandatory sequence is treated as a speciall sequence because it will be fetched for
            // each and every message and keeps a direct reference to that from the configuration
            // this also limits the ability of the mandatory sequence to be dynamic
            if (SynapseConstants.MANDATORY_SEQUENCE_KEY.equals(name)) {
                config.setMandatorySequence(mediator);
            }
            return mediator;
        } else {
            handleException("Invalid sequence definition without a name");
        }
        return null;
    }

    public static Endpoint defineEndpoint(SynapseConfiguration config, OMElement ele,
                                          Properties properties) {

        String name = ele.getAttributeValue(new QName(XMLConfigConstants.NULL_NAMESPACE, "name"));
        if (name != null) {
            Endpoint endpoint = EndpointFactory.getEndpointFromElement(ele, false, properties);
            config.addEndpoint(name.trim(), endpoint);
            return endpoint;
        } else {
            handleException("Invalid endpoint definition without a name");
        }
        return null;
    }

    public static SynapseEventSource defineEventSource(SynapseConfiguration config,
                                                       OMElement elem, Properties properties) {
        SynapseEventSource eventSource = EventSourceFactory.createEventSource(elem, properties);
        config.addEventSource(eventSource.getName(), eventSource);
        return eventSource;
    }

    public static PriorityExecutor defineExecutor(SynapseConfiguration config,
                                                       OMElement elem, Properties properties) {
        PriorityExecutor executor = null;
        try {
            executor = PriorityExecutorFactory.createExecutor(
                XMLConfigConstants.SYNAPSE_NAMESPACE, elem, true, properties);
        } catch (AxisFault axisFault) {
            handleException("Failed to create the priorityExecutor configuration");
        }
        assert executor != null;
        config.addPriorityExecutor(executor.getName(), executor);
        return executor;
    }

    public static MessageStore defineMessageStore(SynapseConfiguration config ,
                                                  OMElement elem, Properties properties) {
        MessageStore messageStore = MessageStoreFactory.createMessageStore(elem, properties);
        config.addMessageStore(messageStore.getName(), messageStore);
        return messageStore;
    }

    private static void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    public QName getTagQName() {

        return XMLConfigConstants.DEFINITIONS_ELT;
    }

    public Class getSerializerClass() {
        return SynapseXMLConfigurationSerializer.class;
    }

}
