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

import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.config.SynapseConfigUtils;
import org.apache.synapse.config.Entry;
import org.apache.synapse.SynapseException;
import org.apache.synapse.Startup;
import org.apache.synapse.Mediator;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.eventing.SynapseEventSource;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.endpoints.AbstractEndpoint;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.startup.AbstractStartup;
import org.apache.synapse.core.axis2.ProxyService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;

import javax.xml.stream.XMLStreamException;
import java.io.*;

/**
 * <p>
 * This optional configuration builder creates the Synapse configuration by processing
 * a specified file hierarchy. If the root of the specified file hierarchy is CONF_HOME,
 * then the following directories are expected to be in CONF_HOME.
 * <ul>
 *  <li>CONF_HOME/proxy-services</li>
 *  <li>CONF_HOME/sequences</li>
 *  <li>CONF_HOME/endpoints</li>
 *  <li>CONF_HOME/local-entries</li>
 *  <li>CONF_HOME/tasks</li>
 *  <li>CONF_HOME/events</li>
 * </ul>
 *
 * Each of these directories will house a set of XML files. Each file will define exactly
 * one configuration item (eg: a proxy service, an endpoint, a sequence).
 * </p>
 * <p>
 * In addition to the directories mentioned above one can have the following file in
 * CONF_HOME
 * <ul>
 *  <li>CONF_HOME/registry.xml</li>
 * </ul>
 * </p>
 *
 */
public class MultiXMLConfigurationBuilder {

    public static final String PROXY_SERVICES_DIR  = "proxy-services";
    public static final String SEQUENCES_DIR       = "sequences";
    public static final String ENDPOINTS_DIR       = "endpoints";
    public static final String LOCAL_ENTRY_DIR     = "local-entries";
    public static final String TASKS_DIR           = "tasks";
    public static final String EVENTS_DIR          = "events";

    public static final String REGISTRY_FILE       = "registry.xml";

    private static Log log = LogFactory.getLog(MultiXMLConfigurationBuilder.class);

    private static FileFilter filter = new FileFilter() {
        public boolean accept(File pathname) {
            return (pathname.isFile() && pathname.getName().endsWith(".xml"));
        }
    };

    public static SynapseConfiguration getConfiguration(String root) throws XMLStreamException {

        if (log.isDebugEnabled()) {
            log.debug("Building Synapse configuration from the directory heirarchy at : " + root);
        }

        SynapseConfiguration synapseConfig = null;
        try {
            // First try to load the configuration from synapse.xml
            synapseConfig = createConfigurationFromSynapseXML(root);
            if (synapseConfig == null) {
                synapseConfig = SynapseConfigUtils.newConfiguration();
                synapseConfig.setDefaultQName(XMLConfigConstants.DEFINITIONS_ELT);
            }

            if (synapseConfig.getRegistry() == null) {
                // If the synapse.xml does not define a registry look for a registry.xml
                createRegistry(synapseConfig, root);
            }

            createLocalEntries(synapseConfig, root);
            createEndpoints(synapseConfig, root);
            createSequences(synapseConfig, root);
            createProxyServices(synapseConfig, root);
            createTasks(synapseConfig, root);
            createEventSources(synapseConfig, root);

        } catch (FileNotFoundException e) {
            handleException("Error while reading from configuration file", e);
        }

        if (synapseConfig.getLocalRegistry().isEmpty() &&
                synapseConfig.getProxyServices().isEmpty() && synapseConfig.getRegistry() != null) {

            log.info("No proxy service definitions or local entry definitions were " +
                    "found at : " + root + ". Attempting to load the configuration from " +
                    "the Synapse registry.");

            OMNode remoteConfigNode = synapseConfig.getRegistry().lookup("synapse.xml");
            if (remoteConfigNode != null) {
                synapseConfig = XMLConfigurationBuilder.getConfiguration(
                        SynapseConfigUtils.getStreamSource(remoteConfigNode).getInputStream());
            } else {
                log.warn("The resource synapse.xml is not available in the Synapse registry.");
            }
        }

        return synapseConfig;
    }

    private static SynapseConfiguration createConfigurationFromSynapseXML(
            String rootDirPath) throws FileNotFoundException, XMLStreamException {

        File synapseXML = new File(rootDirPath, SynapseConstants.SYNAPSE_XML);
        if (synapseXML.exists()) {
            return XMLConfigurationBuilder.getConfiguration(new FileInputStream(synapseXML));
        }
        return null;
    }

    private static void createRegistry(SynapseConfiguration synapseConfig, String rootDirPath)
            throws FileNotFoundException, XMLStreamException {

        File registryDef = new File(rootDirPath, REGISTRY_FILE);
        if (registryDef.exists()) {
            if (log.isDebugEnabled()) {
                log.debug("Initializing Synapse registry from the configuration at : " +
                        registryDef.getPath());
            }
            OMElement document = parseFile(registryDef);
            SynapseXMLConfigurationFactory.defineRegistry(synapseConfig, document);
        }
    }

    private static void createLocalEntries(SynapseConfiguration synapseConfig, String rootDirPath) 
            throws FileNotFoundException, XMLStreamException {

        File localEntriesDir = new File(rootDirPath, LOCAL_ENTRY_DIR);
        if (localEntriesDir.exists()) {
            if (log.isDebugEnabled()) {
                log.debug("Loading local entry definitions from : " + localEntriesDir.getPath());
            }
            File[] entryDefinitions = localEntriesDir.listFiles(filter);
            for (File file : entryDefinitions) {
                OMElement document = parseFile(file);
                Entry entry = SynapseXMLConfigurationFactory.defineEntry(synapseConfig, document);
                entry.setFileName(file.getName());
            }
        }
    }

    private static void createProxyServices(SynapseConfiguration synapseConfig, String rootDirPath)
            throws FileNotFoundException, XMLStreamException {

        File proxyServicesDir = new File(rootDirPath, PROXY_SERVICES_DIR);
        if (proxyServicesDir.exists()) {
            if (log.isDebugEnabled()) {
                log.debug("Loading proxy services from : " + proxyServicesDir.getPath());
            }
            File[] proxyDefinitions = proxyServicesDir.listFiles(filter);
            for (File file : proxyDefinitions) {
                OMElement document = parseFile(file);
                ProxyService proxy = SynapseXMLConfigurationFactory.defineProxy(synapseConfig,
                        document);
                proxy.setFileName(file.getName());
            }
        }
    }

    private static void createTasks(SynapseConfiguration synapseConfig, String rootDirPath)
            throws FileNotFoundException, XMLStreamException {

        File tasksDir = new File(rootDirPath, TASKS_DIR);
        if (tasksDir.exists()) {
            if (log.isDebugEnabled()) {
                log.debug("Loading tasks from : " + tasksDir.getPath());
            }
            File[] taskDefinitions = tasksDir.listFiles(filter);
            for (File file : taskDefinitions) {
                OMElement document = parseFile(file);
                Startup startup = SynapseXMLConfigurationFactory.defineStartup(synapseConfig,
                        document);
                if (startup instanceof AbstractStartup) {
                    ((AbstractStartup) startup).setFileName(file.getName());
                }
            }
        }
    }

    private static void createSequences(SynapseConfiguration synapseConfig, String rootDirPath)
            throws FileNotFoundException, XMLStreamException {

        File sequencesDir = new File(rootDirPath, SEQUENCES_DIR);
        if (sequencesDir.exists()) {
            if (log.isDebugEnabled()) {
                log.debug("Loading sequences from : " + sequencesDir.getPath());
            }
            File[] sequences = sequencesDir.listFiles(filter);
            for (File file : sequences) {
                OMElement document = parseFile(file);
                Mediator seq = SynapseXMLConfigurationFactory.defineSequence(synapseConfig,
                        document);
                if (seq instanceof SequenceMediator) {
                    ((SequenceMediator) seq).setFileName(file.getName());
                }
            }
        }
    }

    private static void createEndpoints(SynapseConfiguration synapseConfig, String rootDirPath)
            throws FileNotFoundException, XMLStreamException {

        File endpointsDir = new File(rootDirPath, ENDPOINTS_DIR);
        if (endpointsDir.exists()) {
            if (log.isDebugEnabled()) {
                log.debug("Loading endpoints from : " + endpointsDir.getPath());
            }
            File[] endpoints = endpointsDir.listFiles(filter);
            for (File file : endpoints) {
                OMElement document = parseFile(file);
                Endpoint endpoint = SynapseXMLConfigurationFactory.defineEndpoint(synapseConfig,
                        document);
                if (endpoint instanceof AbstractEndpoint) {
                    ((AbstractEndpoint) endpoint).setFileName(file.getName());
                }
            }
        }
    }

    private static void createEventSources(SynapseConfiguration synapseConfig, String rootDirPath)
            throws FileNotFoundException, XMLStreamException {

        File eventsDir = new File(rootDirPath, EVENTS_DIR);
        if (eventsDir.exists()) {
            if (log.isDebugEnabled()) {
                log.debug("Loading event sources from : " + eventsDir.getPath());
            }
            File[] events = eventsDir.listFiles(filter);
            for (File file : events) {
                OMElement document = parseFile(file);
                SynapseEventSource eventSource = SynapseXMLConfigurationFactory.defineEventSource(
                        synapseConfig, document);
                eventSource.setFileName(file.getName());
           }
        }
    }

    private static OMElement parseFile(File file)
            throws FileNotFoundException, XMLStreamException {
        InputStream is = new FileInputStream(file);
        OMElement document = new StAXOMBuilder(is).getDocumentElement();
        document.build();
        return document;
    }

    private static void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }
}