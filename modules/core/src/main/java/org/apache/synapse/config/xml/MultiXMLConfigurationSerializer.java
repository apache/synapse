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

import org.apache.synapse.deployers.SynapseArtifactDeploymentStore;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.config.Entry;
import org.apache.synapse.config.xml.eventing.EventSourceSerializer;
import org.apache.synapse.config.xml.endpoints.EndpointSerializer;
import org.apache.synapse.registry.Registry;
import org.apache.synapse.core.axis2.ProxyService;
import org.apache.synapse.eventing.SynapseEventSource;
import org.apache.synapse.Startup;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.message.store.MessageStore;
import org.apache.synapse.startup.AbstractStartup;
import org.apache.synapse.commons.executors.PriorityExecutor;
import org.apache.synapse.commons.executors.config.PriorityExecutorSerializer;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.endpoints.AbstractEndpoint;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.axis2.util.XMLPrettyPrinter;

import java.io.File;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;

public class MultiXMLConfigurationSerializer {

    private static final Log log = LogFactory.getLog(MultiXMLConfigurationSerializer.class);

    private File rootDirectory;
    private File currentDirectory;

    //private SynapseArtifactDeploymentStore deploymentStore;

    public MultiXMLConfigurationSerializer(String directoryPath) {
        rootDirectory = new File(directoryPath);
        currentDirectory = rootDirectory;
        //deploymentStore = SynapseArtifactDeploymentStore.getInstance();
    }

    /**
     * Serializes the given SynapseConfiguration to the file system. This method is NOT
     * thread safe and hence it must not be called by multiple concurrent threads. This method
     * will first serialize the configuration to a temporary directory at the same level as the
     * rootDirectory and then rename/move it as the new rootDirectory. If an error occurs
     * while saving the configuration, the temporaty files will be removed and the old
     * rootDirectory will be left intact. 
     *
     * @param synapseConfig configuration to be serialized
     */
    public void serialize(SynapseConfiguration synapseConfig) {
        if (log.isDebugEnabled()) {
            log.debug("Serializing Synapse configuration to the file system");
        }

        OMFactory fac = OMAbstractFactory.getOMFactory();
        OMNamespace synNS = fac.createOMNamespace(XMLConfigConstants.SYNAPSE_NAMESPACE, "syn");
        OMElement definitions = fac.createOMElement("definitions", synNS);

        try {
            currentDirectory = createTempDirectoryStructure();

            if (synapseConfig.getRegistry() != null) {
                serializeSynapseRegistry(synapseConfig.getRegistry(), synapseConfig, definitions);
            }

            serializeProxyServices(synapseConfig.getProxyServices(), synapseConfig, definitions);
            serializeEventSources(synapseConfig.getEventSources(), synapseConfig, definitions);
            serializeTasks(synapseConfig.getStartups(), synapseConfig, definitions);
            serializeLocalRegistryValues(synapseConfig.getLocalRegistry().values(),
                    synapseConfig, definitions);
            serializeExecutors(synapseConfig.getPriorityExecutors().values(), definitions);
            serializeMessageStores(synapseConfig.getMessageStores().values(), definitions);
            serializeSynapseXML(definitions);

            markConfigurationForSerialization(synapseConfig);
            if (rootDirectory.exists()) {
                if (log.isDebugEnabled()) {
                    log.debug("Deleting existing files at : " + rootDirectory.getAbsolutePath());
                }
                FileUtils.deleteDirectory(rootDirectory);
            }

            if (log.isDebugEnabled()) {
                log.debug("Finished serializing the configuration to : " +
                        currentDirectory.getAbsolutePath() + " - Moving the directory to : " +
                        rootDirectory.getAbsolutePath());
            }
            FileUtils.moveDirectory(currentDirectory, rootDirectory);

        } catch (Exception e) {
            log.error("Error occured while serializing the Synapse configuration.", e);

        } finally {
            deleteTempDirectory();
            currentDirectory = rootDirectory;
        }
    }

    /**
     * Serialize only the elements defined in the top level synapse.xml file back to the
     * synapse.xml file. This method ignores the elements defined in files other than the
     * synapse.xml. Can be used in situations where only the synapse.xml file should be
     * updated at runtime.
     *
     * @param synapseConfig Current Synapse configuration
     * @throws Exception on file I/O error
     */
    public void serializeSynapseXML(SynapseConfiguration synapseConfig) throws Exception {
        OMFactory fac = OMAbstractFactory.getOMFactory();
        OMNamespace synNS = fac.createOMNamespace(XMLConfigConstants.SYNAPSE_NAMESPACE, "syn");
        OMElement definitions = fac.createOMElement("definitions", synNS);

        if (synapseConfig.getRegistry() != null && !Boolean.valueOf(synapseConfig.getProperty(
                MultiXMLConfigurationBuilder.SEPARATE_REGISTRY_DEFINITION))) {
            RegistrySerializer.serializeRegistry(definitions, synapseConfig.getRegistry());
        }

        Collection<ProxyService> proxyServices = synapseConfig.getProxyServices();
        Collection<SynapseEventSource> eventSources = synapseConfig.getEventSources();
        Collection<Startup> tasks = synapseConfig.getStartups();
        Collection localEntries = synapseConfig.getLocalRegistry().values();
        Collection<PriorityExecutor> executors = synapseConfig.getPriorityExecutors().values();
        Collection<MessageStore> messageStores = synapseConfig.getMessageStores().values();

        for (ProxyService service : proxyServices) {
            if (service.getFileName() == null) {
                ProxyServiceSerializer.serializeProxy(definitions, service);
            }
        }

        for (SynapseEventSource source : eventSources) {
            if (source.getFileName() == null) {
                EventSourceSerializer.serializeEventSource(definitions, source);
            }
        }

        for (Startup task : tasks) {
            if (task instanceof AbstractStartup && task.getFileName() == null) {
                StartupFinder.getInstance().serializeStartup(definitions, task);
            }
        }

        for (Object o : localEntries) {
            if (o instanceof SequenceMediator) {
                SequenceMediator seq = (SequenceMediator) o;
                if (seq.getFileName() == null) {
                    MediatorSerializerFinder.getInstance().
                            getSerializer(seq).serializeMediator(definitions, seq);
                }
            } else if (o instanceof AbstractEndpoint) {
                AbstractEndpoint endpoint = (AbstractEndpoint) o;
                if (endpoint.getFileName() == null) {
                    OMElement endpointElem = EndpointSerializer.getElementFromEndpoint(endpoint);
                    definitions.addChild(endpointElem);
                }
            } else if (o instanceof Entry) {
                Entry entry = (Entry) o;
                if (entry.getFileName() == null) {
                    if ((SynapseConstants.SERVER_HOST.equals(entry.getKey())
                            || SynapseConstants.SERVER_IP.equals(entry.getKey()))
                            || entry.getType() == Entry.REMOTE_ENTRY) {
                        continue;
                    }

                    EntrySerializer.serializeEntry(entry, definitions);
                }
            }
        }

        for (PriorityExecutor executor : executors) {
            PriorityExecutorSerializer.serialize(definitions, executor,
                    SynapseConstants.SYNAPSE_NAMESPACE);
        }

        for (MessageStore messageStore : messageStores) {
            if(messageStore.getFileName() == null) {
                MessageStoreSerializer.serializeMessageStore(definitions,messageStore);
            }
        }

        serializeSynapseXML(definitions);
    }

    public OMElement serializeSynapseRegistry(Registry registry, SynapseConfiguration synapseConfig,
                                         OMElement parent) throws Exception {
        OMElement registryElem = RegistrySerializer.serializeRegistry(null, registry);
        if (!Boolean.valueOf(synapseConfig.getProperty(
                MultiXMLConfigurationBuilder.SEPARATE_REGISTRY_DEFINITION)) && parent != null) {
            parent.addChild(registryElem);
            return registryElem;
        }

        File registryConf = new File(currentDirectory, MultiXMLConfigurationBuilder.REGISTRY_FILE);
        if (log.isDebugEnabled()) {
            log.debug("Serializing Synapse registry definition to : " + registryConf.getPath());
        }

        writeToFile(registryElem, registryConf);
        return registryElem;
    }

    public OMElement serializeProxy(ProxyService service, SynapseConfiguration synapseConfig,
                                    OMElement parent) throws Exception {

        File proxyDir = createDirectory(currentDirectory,
                MultiXMLConfigurationBuilder.PROXY_SERVICES_DIR);
        OMElement proxyElem = ProxyServiceSerializer.serializeProxy(null, service);

        String fileName = service.getFileName();
        if (fileName != null) {
            handleDeployment(proxyDir, fileName, service.getName(),
                    synapseConfig.getArtifactDeploymentStore());
            File proxyFile = new File(proxyDir, fileName);
            writeToFile(proxyElem, proxyFile);
        } else if (parent != null) {
            parent.addChild(proxyElem);
        }

        return proxyElem;
    }

    public OMElement serializeEventSource(SynapseEventSource source,
                                          SynapseConfiguration synapseConfig,
                                          OMElement parent) throws Exception {

        File eventsDir = createDirectory(currentDirectory, MultiXMLConfigurationBuilder.EVENTS_DIR);
        OMElement eventSrcElem = EventSourceSerializer.serializeEventSource(null, source);

        String fileName = source.getFileName();
        if (fileName != null) {
            handleDeployment(eventsDir, fileName, source.getName(),
                    synapseConfig.getArtifactDeploymentStore());
            File eventSrcFile = new File(eventsDir, source.getFileName());
            writeToFile(eventSrcElem, eventSrcFile);
        } else if (parent != null) {
            parent.addChild(eventSrcElem);
        }

        return eventSrcElem;
    }

    public OMElement serializeTask(Startup task, SynapseConfiguration synapseConfig,
                                   OMElement parent) throws Exception {

        File tasksDir = createDirectory(currentDirectory, MultiXMLConfigurationBuilder.TASKS_DIR);
        OMElement taskElem = StartupFinder.getInstance().serializeStartup(null, task);

        if (task.getFileName() != null) {
            String fileName = task.getFileName();
            handleDeployment(tasksDir, fileName, task.getName(),
                    synapseConfig.getArtifactDeploymentStore());
            File taskFile = new File(tasksDir, fileName);
            writeToFile(taskElem, taskFile);
        } else if (parent != null) {
            parent.addChild(taskElem);
        }

        return taskElem;
    }

    public OMElement serializeSequence(SequenceMediator seq, SynapseConfiguration synapseConfig,
                                       OMElement parent) throws Exception {

        File seqDir = createDirectory(currentDirectory, MultiXMLConfigurationBuilder.SEQUENCES_DIR);

        OMElement seqElem = MediatorSerializerFinder.getInstance().getSerializer(seq).
                serializeMediator(null, seq);
        String fileName = seq.getFileName();
        if (fileName != null) {
            handleDeployment(seqDir, fileName, seq.getName(),
                    synapseConfig.getArtifactDeploymentStore());
            File seqFile = new File(seqDir, fileName);
            writeToFile(seqElem, seqFile);
        } else if (parent != null) {
            parent.addChild(seqElem);
        }

        return seqElem;
    }

    public OMElement serializeEndpoint(Endpoint epr, SynapseConfiguration synapseConfig,
                                       OMElement parent) throws Exception {

        File eprDir = createDirectory(currentDirectory, MultiXMLConfigurationBuilder.ENDPOINTS_DIR);
        OMElement eprElem = EndpointSerializer.getElementFromEndpoint(epr);

        String fileName = epr.getFileName();
        if (fileName != null) {
            handleDeployment(eprDir, fileName, epr.getName(),
                    synapseConfig.getArtifactDeploymentStore());
            File eprFile = new File(eprDir, fileName);
            writeToFile(eprElem, eprFile);
        } else if (parent != null) {
            parent.addChild(eprElem);
        }

        return eprElem;
    }

    public OMElement serializeLocalEntry(Object o, SynapseConfiguration synapseConfig,
                                         OMElement parent) throws Exception {
        if (o instanceof SequenceMediator) {
            return serializeSequence((SequenceMediator) o, synapseConfig, parent);
        } else if (o instanceof Endpoint) {
            return serializeEndpoint((Endpoint) o, synapseConfig, parent);
        } else if (o instanceof Entry) {
            Entry entry = (Entry) o;
            if ((SynapseConstants.SERVER_HOST.equals(entry.getKey())
                    || SynapseConstants.SERVER_IP.equals(entry.getKey()))
                    || entry.getType() == Entry.REMOTE_ENTRY) {
                return null;
            }

            File entriesDir = createDirectory(currentDirectory,
                    MultiXMLConfigurationBuilder.LOCAL_ENTRY_DIR);
            OMElement entryElem = EntrySerializer.serializeEntry(entry, null);

            String fileName = entry.getFileName();
            if (fileName != null) {
                handleDeployment(entriesDir, fileName, entry.getKey(),
                        synapseConfig.getArtifactDeploymentStore());
                File entryFile  = new File(entriesDir, fileName);
                writeToFile(entryElem, entryFile);
            } else if (parent != null) {
                parent.addChild(entryElem);
            }

            return entryElem;
        }
        return null;
    }

    public OMElement serializeExecutor(PriorityExecutor source, OMElement parent) throws Exception {
        File executorDir = createDirectory(currentDirectory,
                MultiXMLConfigurationBuilder.EXECUTORS_DIR);

        OMElement eventDirElem = PriorityExecutorSerializer.serialize(null, source,
                SynapseConstants.SYNAPSE_NAMESPACE);

        if (source.getFileName() != null) {
            File eventSrcFile = new File(executorDir, source.getFileName());
            writeToFile(eventDirElem, eventSrcFile);
        } else if (parent != null) {
            parent.addChild(eventDirElem);
        }

        return eventDirElem;
    }

    public OMElement serializeMessageStore(MessageStore messagestore,
                                           OMElement parent) throws Exception {

        File messageStoreDir = createDirectory(currentDirectory,
                MultiXMLConfigurationBuilder.MESSAGE_STORE_DIR);
        OMElement messageStoreElem = MessageStoreSerializer.serializeMessageStore(null,
                messagestore);

        String fileName = messagestore.getFileName();
        if (fileName != null) {
            File messageStoreFile = new File(messageStoreDir , fileName);
            writeToFile(messageStoreElem , messageStoreFile);

        } else if (parent != null) {
            parent.addChild(messageStoreElem);
        }

        return messageStoreElem;
    }

    private void writeToFile(OMElement content, File file) throws Exception {
        File tempFile = File.createTempFile("syn_mx_", ".xml");
        OutputStream out = new FileOutputStream(tempFile);
        XMLPrettyPrinter.prettify(content, out);
        out.flush();
        out.close();

        FileUtils.copyFile(tempFile, file);
        FileUtils.deleteQuietly(tempFile);
    }

    private void handleDeployment(File parent, String child, String artifactName,
                                  SynapseArtifactDeploymentStore deploymentStore) {
        String fileName = parent.getAbsolutePath() + File.separator + child;
        if (!deploymentStore.containsFileName(fileName)) {
            deploymentStore.addArtifact(fileName, artifactName);
        }
        deploymentStore.addRestoredArtifact(fileName);
    }

    private void serializeProxyServices(Collection<ProxyService> proxyServices,
                                        SynapseConfiguration synapseConfig, OMElement parent)
            throws Exception {
        for (ProxyService service : proxyServices) {
            serializeProxy(service, synapseConfig, parent);
        }
    }

    private void serializeLocalRegistryValues(Collection localValues,
                                              SynapseConfiguration synapseConfig,
                                              OMElement parent)
            throws Exception {
        for (Object o : localValues) {
            serializeLocalEntry(o, synapseConfig, parent);
        }
    }

    private void serializeTasks(Collection<Startup> tasks,
                                SynapseConfiguration synapseConfig,
                                OMElement parent) throws Exception {
        for (Startup task : tasks) {
            serializeTask(task, synapseConfig, parent);
        }
    }

    private void serializeEventSources(Collection<SynapseEventSource> eventSources,
                                       SynapseConfiguration synapseConfig,
                                       OMElement parent) throws Exception {
        for (SynapseEventSource source : eventSources) {
            serializeEventSource(source, synapseConfig, parent);
        }
    }

    private void serializeExecutors(Collection<PriorityExecutor> executors,
                                       OMElement parent) throws Exception {
        for (PriorityExecutor source : executors) {
            serializeExecutor(source, parent);
        }
    }

    private void serializeMessageStores(Collection<MessageStore> messaegeStores,
                                         OMElement parent) throws Exception{
        for (MessageStore messageStore : messaegeStores) {
            serializeMessageStore(messageStore, parent);
        }
    }

    private void serializeSynapseXML(OMElement definitions) throws Exception {
        File synapseXML = new File(currentDirectory, SynapseConstants.SYNAPSE_XML);
        if (!currentDirectory.exists()) {
            FileUtils.forceMkdir(currentDirectory);
        }

        writeToFile(definitions, synapseXML);
    }

    private File createTempDirectoryStructure() throws IOException {
        String tempDirName = "__tmp" + new Date().getTime();
        File tempDirectory = new File(rootDirectory.getParentFile(), tempDirName);

        if (log.isDebugEnabled()) {
            log.debug("Creating temporary files at : " + tempDirectory.getAbsolutePath());
        }

        FileUtils.forceMkdir(tempDirectory);
        createDirectory(tempDirectory, MultiXMLConfigurationBuilder.PROXY_SERVICES_DIR);
        createDirectory(tempDirectory, MultiXMLConfigurationBuilder.EVENTS_DIR);
        createDirectory(tempDirectory, MultiXMLConfigurationBuilder.LOCAL_ENTRY_DIR);
        createDirectory(tempDirectory, MultiXMLConfigurationBuilder.ENDPOINTS_DIR);
        createDirectory(tempDirectory, MultiXMLConfigurationBuilder.SEQUENCES_DIR);
        createDirectory(tempDirectory, MultiXMLConfigurationBuilder.TASKS_DIR);
        createDirectory(tempDirectory, MultiXMLConfigurationBuilder.EXECUTORS_DIR);

        return tempDirectory;
    }

    private void deleteTempDirectory() {
        try {
            if (currentDirectory != rootDirectory && currentDirectory.exists()) {
                FileUtils.deleteDirectory(currentDirectory);
            }
        } catch (IOException e) {
            log.warn("Error while deleting the temporary files at : " +
                    currentDirectory.getAbsolutePath() + " - You may delete them manually.", e);
        }
    }

    private File createDirectory(File parent, String name) throws IOException {
        File dir = new File(parent, name);
        if (!dir.exists()) {
            FileUtils.forceMkdir(dir);
        }
        return dir;
    }

    /**
     * Get the existing configuration and mark those files not effect on deployers for
     * deletion
     * @param synapseConfig synapse configuration
     */
    private void markConfigurationForSerialization(SynapseConfiguration synapseConfig) {                
        SynapseArtifactDeploymentStore deploymentStore = synapseConfig.getArtifactDeploymentStore();

        for (SequenceMediator seq : synapseConfig.getDefinedSequences().values()) {
            if (seq.getFileName() != null) {
                handleDeployment(new File(rootDirectory, MultiXMLConfigurationBuilder.
                        SEQUENCES_DIR), seq.getFileName(), seq.getName(), deploymentStore);
            }
        }

        for (Endpoint ep : synapseConfig.getDefinedEndpoints().values()) {
            if (ep.getFileName() != null) {
                handleDeployment(new File(rootDirectory, MultiXMLConfigurationBuilder.
                        ENDPOINTS_DIR), ep.getFileName(), ep.getName(), deploymentStore);
            }
        }

        for (ProxyService proxy : synapseConfig.getProxyServices()) {
            if (proxy.getFileName() != null) {
                handleDeployment(new File(rootDirectory, MultiXMLConfigurationBuilder.
                        PROXY_SERVICES_DIR), proxy.getFileName(), proxy.getName(), deploymentStore);
            }
        }

        for (Entry e : synapseConfig.getDefinedEntries().values()) {
            if (e.getFileName() != null) {
                handleDeployment(new File(rootDirectory, MultiXMLConfigurationBuilder.
                        LOCAL_ENTRY_DIR), File.separator +e.getFileName(), e.getKey(),
                        deploymentStore);
            }
        }

        for (SynapseEventSource es : synapseConfig.getEventSources()) {
            if (es.getFileName() != null) {
                handleDeployment(new File(rootDirectory, MultiXMLConfigurationBuilder.
                        EVENTS_DIR), es.getFileName(), es.getName(), deploymentStore);
            }
        }

        for (Startup s : synapseConfig.getStartups()) {
            if (s.getFileName() != null) {
                handleDeployment(new File(rootDirectory, MultiXMLConfigurationBuilder.
                        TASKS_DIR), s.getFileName(), s.getName(), deploymentStore);
            }
        }

        for (PriorityExecutor exec : synapseConfig.getPriorityExecutors().values()) {
            if (exec.getFileName() != null) {
                handleDeployment(new File(rootDirectory, MultiXMLConfigurationBuilder.
                        EXECUTORS_DIR), exec.getFileName(), exec.getName(), deploymentStore);
            }
        }
    }

}