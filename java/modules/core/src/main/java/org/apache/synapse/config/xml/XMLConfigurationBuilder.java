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

import org.apache.axiom.om.*;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.Mediator;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.config.Entry;
import org.apache.synapse.config.Util;
import org.apache.synapse.config.xml.endpoints.EndpointAbstractFactory;
import org.apache.synapse.core.axis2.ProxyService;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.mediators.builtin.SendMediator;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.mediators.builtin.LogMediator;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;


/**
 * Builds a Synapse Configuration from an XML input stream
 */
public class XMLConfigurationBuilder {

    private static Log log = LogFactory.getLog(XMLConfigurationBuilder.class);

    public static SynapseConfiguration getConfiguration(InputStream is) {

        log.info("Generating the Synapse configuration model by parsing the XML configuration");
        SynapseConfiguration config = new SynapseConfiguration();

        SequenceMediator rootSequence = new SequenceMediator();
        rootSequence.setName(org.apache.synapse.Constants.MAIN_SEQUENCE_KEY);

        OMElement definitions = null;
        try {
            definitions = new StAXOMBuilder(is).getDocumentElement();
            definitions.build();

            OMNamespace namespace = definitions.getNamespace();
            if(namespace == null){
                handleException("No namespace is defined for definitions element");
            }

            if (Constants.SYNAPSE_NAMESPACE.equals(namespace.getNamespaceURI())
                && Constants.DEFINITIONS_ELT.getLocalPart()
                    .equals(definitions.getQName().getLocalPart())) {

                Iterator iter = definitions.getChildren();

                while (iter.hasNext()) {
                    Object o = iter.next();
                    if (o instanceof OMElement) {
                        OMElement elt = (OMElement) o;
                        if (Constants.SEQUENCE_ELT.equals(elt.getQName())) {
                            String key = elt.getAttributeValue(
                                new QName(Constants.NULL_NAMESPACE, "key"));
                            // this could be a sequence def or a mediator of the main sequence
                            if (key != null) {
                                Mediator m = MediatorFactoryFinder.getInstance().getMediator(elt);
                                rootSequence.addChild(m);
                            } else {
                                defineSequence(config, elt);
                            }
                        } else if (Constants.ENDPOINT_ELT.equals(elt.getQName())) {
                            defineEndpoint(config, elt);
                        } else if (Constants.ENTRY_ELT.equals(elt.getQName())) {
                            defineEntry(config, elt);
                        } else if (Constants.PROXY_ELT.equals(elt.getQName())) {
                            defineProxy(config, elt);
                        } else if (Constants.REGISTRY_ELT.equals(elt.getQName())) {
                            defineRegistry(config, elt);
                        } else {
                            Mediator m = MediatorFactoryFinder.getInstance().getMediator(elt);
                            rootSequence.addChild(m);
                        }
                    }
                }
            } else {
                handleException("Invalid Synapse Configuration : No definition element found");
            }

        } catch (XMLStreamException e) {
            handleException("Error parsing Synapse configuration : " + e.getMessage(), e);
        }

        if (is != null) {
            try {
                is.close();
            } catch (IOException ignore) {}
        }

        if (config.getLocalRegistry().isEmpty() && config.getProxyServices().isEmpty() &&
                rootSequence.getList().isEmpty() && config.getRegistry() != null) {
            OMNode remoteConfigNode = config.getRegistry().lookup("synapse.xml");
            config = getConfiguration(Util.getStreamSource(remoteConfigNode).getInputStream());
        }

        if (config.getMainSequence() == null) {
            if (rootSequence.getList().isEmpty()) {
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

        String name = ele.getAttributeValue(new QName(Constants.NULL_NAMESPACE, "name"));
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

        String name = ele.getAttributeValue(new QName(Constants.NULL_NAMESPACE, "name"));
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
        main.setName(org.apache.synapse.Constants.MAIN_SEQUENCE_KEY);
        main.addChild(new SendMediator());
        config.addSequence(org.apache.synapse.Constants.MAIN_SEQUENCE_KEY, main);
    }

    /**
     * Return the fault sequence if one is not defined. This implementation defaults to
     * a simple sequence with a <log level="full"/>
     *
     * @param config the configuration to be updated
     */
    private static void setDefaultFaultSequence(SynapseConfiguration config) {
        SequenceMediator fault = new SequenceMediator();
        fault.setName(org.apache.synapse.Constants.FAULT_SEQUENCE_KEY);
        LogMediator log = new LogMediator();
        log.setLogLevel(LogMediator.FULL);
        fault.addChild(log);
        config.addSequence(org.apache.synapse.Constants.FAULT_SEQUENCE_KEY, fault);
    }

    private static void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    private static void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }
}
