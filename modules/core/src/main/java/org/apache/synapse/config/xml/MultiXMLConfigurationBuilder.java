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
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Mediator;
import org.apache.synapse.Startup;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.commons.executors.PriorityExecutor;
import org.apache.synapse.config.Entry;
import org.apache.synapse.config.SynapseConfigUtils;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.axis2.ProxyService;
import org.apache.synapse.deployers.SynapseArtifactDeploymentStore;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.eventing.SynapseEventSource;
import org.apache.synapse.mediators.base.SequenceMediator;

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
 *  <li>CONF_HOME/event-sources</li>
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
    public static final String EVENTS_DIR          = "event-sources";
    public static final String EXECUTORS_DIR       = "priorityExecutors";

    public static final String REGISTRY_FILE       = "registry.xml";

    public static final String SEPARATE_REGISTRY_DEFINITION = "__separateRegDef";

    private static Log log = LogFactory.getLog(MultiXMLConfigurationBuilder.class);

    private static FileFilter filter = new FileFilter() {
        public boolean accept(File pathname) {
            return (pathname.isFile() && pathname.getName().endsWith(".xml"));
        }
    };

    public static SynapseConfiguration getConfiguration(String root) throws XMLStreamException {

        log.info("Building synapse configuration from the " +
                "synapse artifact repository at : " + root);

        // First try to load the configuration from synapse.xml
        SynapseConfiguration synapseConfig = createConfigurationFromSynapseXML(root);
        if (synapseConfig == null) {
            synapseConfig = SynapseConfigUtils.newConfiguration();
            synapseConfig.setDefaultQName(XMLConfigConstants.DEFINITIONS_ELT);
        } else if (log.isDebugEnabled()) {
            log.debug("Found a synapse configuration in the " + SynapseConstants.SYNAPSE_XML
                    + " file at the artifact repository root, which gets the presedence "
                    + "over other definitions");
        }

        if (synapseConfig.getRegistry() == null) {
            // If the synapse.xml does not define a registry look for a registry.xml
            createRegistry(synapseConfig, root);
        } else if (log.isDebugEnabled()) {
            log.debug("Using the registry defined in the " + SynapseConstants.SYNAPSE_XML
                    + " as the registry, any definitions in the "
                    + REGISTRY_FILE + " will be neglected");
        }

        createLocalEntries(synapseConfig, root);
        createEndpoints(synapseConfig, root);
        createSequences(synapseConfig, root);
        createProxyServices(synapseConfig, root);
        createTasks(synapseConfig, root);
        createEventSources(synapseConfig, root);
        createExecutors(synapseConfig, root);

        return synapseConfig;
    }

    private static SynapseConfiguration createConfigurationFromSynapseXML(String rootDirPath)
            throws XMLStreamException {

        File synapseXML = new File(rootDirPath, SynapseConstants.SYNAPSE_XML);
        if (synapseXML.exists() && synapseXML.isFile()) {
            try {
                return XMLConfigurationBuilder.getConfiguration(new FileInputStream(synapseXML));
            } catch (FileNotFoundException ignored) {}
        }
        return null;
    }

    private static void createRegistry(SynapseConfiguration synapseConfig, String rootDirPath)
            throws XMLStreamException {

        File registryDef = new File(rootDirPath, REGISTRY_FILE);
        if (registryDef.exists() && registryDef.isFile()) {
            if (log.isDebugEnabled()) {
                log.debug("Initializing Synapse registry from the configuration at : " +
                        registryDef.getPath());
            }
            try {
                OMElement document = parseFile(registryDef);
                SynapseXMLConfigurationFactory.defineRegistry(synapseConfig, document);
                synapseConfig.setProperty(SEPARATE_REGISTRY_DEFINITION,
                        String.valueOf(Boolean.TRUE));
            } catch (FileNotFoundException ignored) {}
        }
    }

    private static void createLocalEntries(SynapseConfiguration synapseConfig, String rootDirPath) 
            throws XMLStreamException {

        File localEntriesDir = new File(rootDirPath, LOCAL_ENTRY_DIR);
        if (localEntriesDir.exists()) {
            if (log.isDebugEnabled()) {
                log.debug("Loading local entry definitions from : " + localEntriesDir.getPath());
            }
            File[] entryDefinitions = localEntriesDir.listFiles(filter);
            for (File file : entryDefinitions) {
                try {
                    OMElement document = parseFile(file);
                    Entry entry = SynapseXMLConfigurationFactory.defineEntry(
                            synapseConfig, document);
                    entry.setFileName(file.getName());
                    SynapseArtifactDeploymentStore.getInstance().addArtifact(
                            file.getAbsolutePath(), entry.getKey());
                } catch (FileNotFoundException ignored) {}
            }
        }
    }

    private static void createProxyServices(SynapseConfiguration synapseConfig, String rootDirPath)
            throws XMLStreamException {

        File proxyServicesDir = new File(rootDirPath, PROXY_SERVICES_DIR);
        if (proxyServicesDir.exists()) {
            if (log.isDebugEnabled()) {
                log.debug("Loading proxy services from : " + proxyServicesDir.getPath());
            }
            File[] proxyDefinitions = proxyServicesDir.listFiles(filter);
            for (File file : proxyDefinitions) {
                try {
                    OMElement document = parseFile(file);
                    ProxyService proxy = SynapseXMLConfigurationFactory.defineProxy(
                            synapseConfig, document);
                    proxy.setFileName(file.getName());
                    SynapseArtifactDeploymentStore.getInstance().addArtifact(
                            file.getAbsolutePath(), proxy.getName());
                } catch (FileNotFoundException ignored) {}
            }
        }
    }

    private static void createTasks(SynapseConfiguration synapseConfig, String rootDirPath)
            throws XMLStreamException {

        File tasksDir = new File(rootDirPath, TASKS_DIR);
        if (tasksDir.exists()) {
            if (log.isDebugEnabled()) {
                log.debug("Loading tasks from : " + tasksDir.getPath());
            }
            File[] taskDefinitions = tasksDir.listFiles(filter);
            for (File file : taskDefinitions) {
                try {
                    OMElement document = parseFile(file);
                    Startup startup = SynapseXMLConfigurationFactory.defineStartup(
                            synapseConfig, document);
                    startup.setFileName(file.getName());
                    SynapseArtifactDeploymentStore.getInstance().addArtifact(
                            file.getAbsolutePath(), startup.getName());
                } catch (FileNotFoundException ignored) {}
            }
        }
    }

    private static void createSequences(SynapseConfiguration synapseConfig, String rootDirPath)
            throws XMLStreamException {

        File sequencesDir = new File(rootDirPath, SEQUENCES_DIR);
        if (sequencesDir.exists()) {
            if (log.isDebugEnabled()) {
                log.debug("Loading sequences from : " + sequencesDir.getPath());
            }
            File[] sequences = sequencesDir.listFiles(filter);
            for (File file : sequences) {
                try {
                    OMElement document = parseFile(file);
                    Mediator seq = SynapseXMLConfigurationFactory.defineSequence(
                            synapseConfig, document);
                    if (seq instanceof SequenceMediator) {
                        SequenceMediator sequence = (SequenceMediator) seq;
                        sequence.setFileName(file.getName());
                        SynapseArtifactDeploymentStore.getInstance().addArtifact(
                                file.getAbsolutePath(), sequence.getName());
                    }
                } catch (FileNotFoundException ignored) {}
            }
        }
    }

    private static void createEndpoints(SynapseConfiguration synapseConfig, String rootDirPath)
            throws XMLStreamException {

        File endpointsDir = new File(rootDirPath, ENDPOINTS_DIR);
        if (endpointsDir.exists()) {
            if (log.isDebugEnabled()) {
                log.debug("Loading endpoints from : " + endpointsDir.getPath());
            }
            File[] endpoints = endpointsDir.listFiles(filter);
            for (File file : endpoints) {
                try {
                    OMElement document = parseFile(file);
                    Endpoint endpoint = SynapseXMLConfigurationFactory.defineEndpoint(
                            synapseConfig, document);
                    endpoint.setFileName(file.getName());
                    SynapseArtifactDeploymentStore.getInstance().addArtifact(
                            file.getAbsolutePath(), endpoint.getName());
                } catch (FileNotFoundException ignored) {}
            }
        }
    }

    private static void createEventSources(SynapseConfiguration synapseConfig, String rootDirPath)
            throws XMLStreamException {

        File eventsDir = new File(rootDirPath, EVENTS_DIR);
        if (eventsDir.exists()) {
            if (log.isDebugEnabled()) {
                log.debug("Loading event sources from : " + eventsDir.getPath());
            }
            File[] events = eventsDir.listFiles(filter);
            for (File file : events) {
                try {
                    OMElement document = parseFile(file);
                    SynapseEventSource eventSource = SynapseXMLConfigurationFactory.
                            defineEventSource(synapseConfig, document);
                    eventSource.setFileName(file.getName());
                    SynapseArtifactDeploymentStore.getInstance().addArtifact(
                            file.getAbsolutePath(), eventSource.getName());
                } catch (FileNotFoundException ignored) {}
           }
        }
    }

    private static void createExecutors(SynapseConfiguration synapseConfig, String rootDirPath)
            throws XMLStreamException {

        File eventsDir = new File(rootDirPath, EXECUTORS_DIR);
        if (eventsDir.exists()) {
            if (log.isDebugEnabled()) {
                log.debug("Loading event sources from : " + eventsDir.getPath());
            }
            File[] events = eventsDir.listFiles(filter);
            for (File file : events) {
                try {
                    OMElement document = parseFile(file);
                    PriorityExecutor executor = SynapseXMLConfigurationFactory.
                            defineExecutor(synapseConfig, document);
                    executor.setFileName(file.getName());
                    SynapseArtifactDeploymentStore.getInstance().addArtifact(
                            file.getAbsolutePath(), executor.getName());
                } catch (FileNotFoundException ignored) {}
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
}