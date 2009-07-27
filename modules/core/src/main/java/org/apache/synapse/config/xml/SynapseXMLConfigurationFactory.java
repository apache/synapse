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
import org.apache.synapse.registry.Registry;
import org.apache.synapse.aspects.AspectConfiguration;
import org.apache.synapse.eventing.SynapseEventSource;
import org.apache.synapse.config.Entry;
import org.apache.synapse.config.SynapseConfigUtils;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.config.xml.endpoints.EndpointFactory;
import org.apache.synapse.config.xml.eventing.EventSourceFactory;
import org.apache.synapse.core.axis2.ProxyService;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.mediators.MediatorProperty;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.mediators.builtin.DropMediator;
import org.apache.synapse.mediators.builtin.LogMediator;
import org.apache.synapse.util.xpath.SynapseXPath;
import org.jaxen.JaxenException;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.util.Iterator;

public class SynapseXMLConfigurationFactory implements ConfigurationFactory {

    private static Log log = LogFactory.getLog(SynapseXMLConfigurationFactory.class);

    public SynapseConfiguration getConfiguration(OMElement definitions) {

        if (!definitions.getQName().equals(XMLConfigConstants.DEFINITIONS_ELT)) {
            throw new SynapseException(
                    "Wrong QName for this configuration factory " + definitions.getQName());
        }
        SynapseConfiguration config = SynapseConfigUtils.newConfiguration();               
        config.setDefaultQName(definitions.getQName());

        SequenceMediator rootSequence = new SequenceMediator();
        rootSequence.setName(org.apache.synapse.SynapseConstants.MAIN_SEQUENCE_KEY);

        // aspect configuration
        AspectConfiguration configuration = new AspectConfiguration(rootSequence.getName());
        rootSequence.configure(configuration);
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
                } else if (XMLConfigConstants.EVENT_SOURCE_ELT.equals(elt.getQName())) {
                    defineEventSource(config, elt);
                } else if (StartupFinder.getInstance().isStartup(elt.getQName())) {
                    defineStartup(config, elt);
                } else {
                    Mediator m = MediatorFactoryFinder.getInstance().getMediator(elt);
                    rootSequence.addChild(m);
                }
            }
        }

        Registry localConfigReg = config.getRegistry();
        if (config.getLocalRegistry().isEmpty() && config.getProxyServices().isEmpty() &&
                rootSequence.getList().isEmpty() && localConfigReg != null) {
            OMNode remoteConfigNode = localConfigReg.lookup("synapse.xml");
            try {
                config = XMLConfigurationBuilder.getConfiguration(SynapseConfigUtils
                        .getStreamSource(remoteConfigNode).getInputStream());
                if (config.getRegistry() == null) {
                    config.setRegistry(localConfigReg);
                }
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

    public static Registry defineRegistry(SynapseConfiguration config, OMElement elem) {
        if (config.getRegistry() != null) {
            handleException("Only one remote registry can be defined within a configuration");
        }
        Registry registry = RegistryFactory.createRegistry(elem);
        config.setRegistry(registry);
        return registry;
    }

    public static Startup defineStartup(SynapseConfiguration config, OMElement elem) {
        Startup startup = StartupFinder.getInstance().getStartup(elem);
        config.addStartup(startup);
        return startup;
    }

    public static ProxyService defineProxy(SynapseConfiguration config, OMElement elem) {
        ProxyService proxy = ProxyServiceFactory.createProxy(elem);
        config.addProxyService(proxy.getName(), proxy);
        return proxy;
    }

   public static Entry defineEntry(SynapseConfiguration config, OMElement elem) {
        Entry entry = EntryFactory.createEntry(elem);
        config.addEntry(entry.getKey(), entry);
        return entry;
    }

    public static Mediator defineSequence(SynapseConfiguration config, OMElement ele) {

        String name = ele.getAttributeValue(new QName(XMLConfigConstants.NULL_NAMESPACE, "name"));
        if (name != null) {
            Mediator mediator = MediatorFactoryFinder.getInstance().getMediator(ele);
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

    public static Endpoint defineEndpoint(SynapseConfiguration config, OMElement ele) {

        String name = ele.getAttributeValue(new QName(XMLConfigConstants.NULL_NAMESPACE, "name"));
        if (name != null) {
            Endpoint endpoint = EndpointFactory.getEndpointFromElement(ele, false);
            config.addEndpoint(name.trim(), endpoint);
            return endpoint;
        } else {
            handleException("Invalid endpoint definition without a name");
        }
        return null;
    }

    public static SynapseEventSource defineEventSource(SynapseConfiguration config, OMElement elem) {
        SynapseEventSource eventSource = EventSourceFactory.createEventSource(elem);
        config.addEventSource(eventSource.getName(), eventSource);
        return eventSource;
    }

    /**
     * Return the main sequence if one is not defined. This implementation defaults to
     * a simple sequence with a <send/>
     *
     * @param config the configuration to be updated
     */
    public static void setDefaultMainSequence(SynapseConfiguration config) {
        SequenceMediator main = new SequenceMediator();
        main.setName(SynapseConstants.MAIN_SEQUENCE_KEY);
        main.addChild(new LogMediator());
        main.addChild(new DropMediator());
        config.addSequence(SynapseConstants.MAIN_SEQUENCE_KEY, main);
        // set the aspect configuration
        AspectConfiguration configuration = new AspectConfiguration(main.getName());
        main.configure(configuration);
    }

    /**
     * Return the fault sequence if one is not defined. This implementation defaults to
     * a simple sequence :
     * <log level="full">
     *   <property name="MESSAGE" value="Executing default "fault" sequence"/>
     *   <property name="ERROR_CODE" expression="get-property('ERROR_CODE')"/>
     *   <property name="ERROR_MESSAGE" expression="get-property('ERROR_MESSAGE')"/>
     * </log>
     * <drop/>
     *
     * @param config the configuration to be updated
     */
    public static void setDefaultFaultSequence(SynapseConfiguration config) {
        SequenceMediator fault = new SequenceMediator();
        fault.setName(org.apache.synapse.SynapseConstants.FAULT_SEQUENCE_KEY);
        LogMediator log = new LogMediator();
        log.setLogLevel(LogMediator.FULL);

        MediatorProperty mp = new MediatorProperty();
        mp.setName("MESSAGE");
        mp.setValue("Executing default \"fault\" sequence");
        log.addProperty(mp);

        mp = new MediatorProperty();
        mp.setName("ERROR_CODE");
        try {
            mp.setExpression(new SynapseXPath("get-property('ERROR_CODE')"));
        } catch (JaxenException ignore) {}
        log.addProperty(mp);

        mp = new MediatorProperty();
        mp.setName("ERROR_MESSAGE");
        try {
            mp.setExpression(new SynapseXPath("get-property('ERROR_MESSAGE')"));
        } catch (JaxenException ignore) {}
        log.addProperty(mp);

        fault.addChild(log);
        fault.addChild(new DropMediator());

        // set aspect configuration
        AspectConfiguration configuration = new AspectConfiguration(fault.getName());
        fault.configure(configuration);

        config.addSequence(org.apache.synapse.SynapseConstants.FAULT_SEQUENCE_KEY, fault);
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
