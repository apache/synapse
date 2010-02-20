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
import org.apache.synapse.config.Entry;
import org.apache.synapse.config.xml.eventing.EventSourceSerializer;
import org.apache.synapse.config.xml.endpoints.EndpointSerializer;
import org.apache.synapse.deployers.SynapseArtifactDeploymentStore;
import org.apache.synapse.registry.Registry;
import org.apache.synapse.core.axis2.ProxyService;
import org.apache.synapse.eventing.SynapseEventSource;
import org.apache.synapse.Startup;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.commons.executors.PriorityExecutor;
import org.apache.synapse.commons.executors.config.PriorityExecutorSerializer;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.endpoints.AbstractEndpoint;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.startup.AbstractStartup;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.axis2.util.XMLPrettyPrinter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.GregorianCalendar;
import java.util.Collection;

/**
 * Serializes the Synapse configuration to a specified directory
 */
@SuppressWarnings({"UnusedDeclaration"})
public class MultiXMLConfigurationSerializer {

    /** The directory to where the configuration should be serialized */
    private File rootDirectory;
    /** The backup directory to be created when the target directory already exists */
    private File backupDirectory;

    private SynapseArtifactDeploymentStore deploymentStore;

    private static Log log = LogFactory.getLog(MultiXMLConfigurationSerializer.class);

    public MultiXMLConfigurationSerializer(String directoryPath) {
        rootDirectory = new File(directoryPath);
        deploymentStore = SynapseArtifactDeploymentStore.getInstance();
    }

    public void serialize(SynapseConfiguration synapseConfig) {
        if (log.isDebugEnabled()) {
            log.debug("Starting to serialize the Synapse configuration to the directory : " +
                    rootDirectory);
        }
        
        OMFactory fac = OMAbstractFactory.getOMFactory();
        OMNamespace synNS = fac.createOMNamespace(XMLConfigConstants.SYNAPSE_NAMESPACE, "syn");
        OMElement definitions = fac.createOMElement("definitions", synNS);

        try {
            // TO start with clean up the existing configuration files
            cleanUpDirectory();
            createDirectoryStructure();

            // Serialize various elements in the SynapseConfiguration
            if (synapseConfig.getRegistry() != null) {
                serializeSynapseRegistry(synapseConfig.getRegistry(), synapseConfig, definitions);
            }

            serializeProxyServices(synapseConfig.getProxyServices(), definitions);
            serializeEventSources(synapseConfig.getEventSources(), definitions);
            serializeTasks(synapseConfig.getStartups(), definitions);
            serializeLocalRegistryValues(synapseConfig.getLocalRegistry().values(), definitions);
            serializeExecutors(synapseConfig.getPriorityExecutors().values(), definitions);

            // Now serialize the content to synapse.xml
            serializeSynapseXML(definitions);

            log.info("Done serializing the Synapse configuration to : " + rootDirectory.getPath());

            // If a backup was created, clean it up
            if (backupDirectory != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Cleaning up the backup files at : " + backupDirectory.getPath());
                }
                deleteDirectory(backupDirectory);
                backupDirectory = null;
            }

        } catch (Exception e) {
            log.error("Error occured while serializing the Synapse configuration.", e);
            // Attempt to perform a restore using the backups available
            restoreBackup();
        }
    }

    public void createDirectoryStructure() throws Exception {

        File proxyDir = new File(rootDirectory, MultiXMLConfigurationBuilder.PROXY_SERVICES_DIR);
        if (!proxyDir.exists() && !proxyDir.mkdirs()) {
            throw new Exception("Error while creating the directory for proxy services : " +
                    proxyDir.getAbsolutePath());
        }

        File eventsDir = new File(rootDirectory, MultiXMLConfigurationBuilder.EVENTS_DIR);
        if (!eventsDir.exists() && !eventsDir.mkdirs()) {
            throw new Exception("Error while creating the directory for events : " +
                    eventsDir.getAbsolutePath());
        }

        File entriesDir = new File(rootDirectory, MultiXMLConfigurationBuilder.LOCAL_ENTRY_DIR);
        if (!entriesDir.exists() && !entriesDir.mkdirs()) {
            throw new Exception("Error while creating the local entries directory : " +
                    entriesDir.getAbsolutePath());
        }

        File eprDir = new File(rootDirectory, MultiXMLConfigurationBuilder.ENDPOINTS_DIR);
        if (!eprDir.exists() && !eprDir.mkdirs()) {
            throw new Exception("Error while creating the directory for endpoints : " +
                    eprDir.getAbsolutePath());
        }

        File seqDir = new File(rootDirectory, MultiXMLConfigurationBuilder.SEQUENCES_DIR);
        if (!seqDir.exists() && !seqDir.mkdirs()) {
            throw new Exception("Error while creating the directory for sequences : " +
                    seqDir.getAbsolutePath());
        }

        File tasksDir = new File(rootDirectory, MultiXMLConfigurationBuilder.TASKS_DIR);
        if (!tasksDir.exists() && !tasksDir.mkdirs()) {
            throw new Exception("Error while creating the directory for tasks : " +
                    tasksDir.getAbsolutePath());
        }

        File executorDir = new File(rootDirectory, MultiXMLConfigurationBuilder.EXECUTORS_DIR);
        if (!executorDir.exists() && !executorDir.mkdirs()) {
            throw new Exception("Error while creating the directory for tasks : " +
                    executorDir.getAbsolutePath());
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
            if (task instanceof AbstractStartup &&
                    ((AbstractStartup) task).getFileName() == null) {
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

        serializeSynapseXML(definitions);
    }

    private void serializeSynapseXML(OMElement definitions) throws Exception {
        File synapseXML = new File(rootDirectory, SynapseConstants.SYNAPSE_XML);
        if (!rootDirectory.exists() && !rootDirectory.mkdirs()) {
            throw new Exception("Error while creating the root configuration directory " +
                    "at: " + rootDirectory.getAbsolutePath());                
        }

        writeToFile(definitions, synapseXML);
    }

    public OMElement serializeSynapseRegistry(Registry registry, SynapseConfiguration synapseConfig,
                                         OMElement parent) throws Exception {
        OMElement registryElem = RegistrySerializer.serializeRegistry(null, registry);
        if (!Boolean.valueOf(synapseConfig.getProperty(
                MultiXMLConfigurationBuilder.SEPARATE_REGISTRY_DEFINITION)) && parent != null) {
            parent.addChild(registryElem);
            return registryElem;
        }

        File registryConf = new File(rootDirectory, MultiXMLConfigurationBuilder.REGISTRY_FILE);
        if (log.isDebugEnabled()) {
            log.debug("Serializing Synapse registry definition to : " + registryConf.getPath());
        }

        if (registryConf.createNewFile()) {
            OutputStream out = new FileOutputStream(registryConf);
            XMLPrettyPrinter.prettify(registryElem, out);
            out.flush();
        } else {
            throw new Exception("Error while creating the registry configuration file at : " +
                    registryConf.getAbsolutePath());
        }

        return registryElem;
    }

    public OMElement serializeProxy(ProxyService service, OMElement parent) throws Exception {

        File proxyDir = new File(rootDirectory, MultiXMLConfigurationBuilder.PROXY_SERVICES_DIR);
        if (!proxyDir.exists() && !proxyDir.mkdirs()) {
            throw new Exception("Error while creating the directory for proxy services : " +
                    proxyDir.getAbsolutePath());
        }

        OMElement proxyElem = ProxyServiceSerializer.serializeProxy(null, service);

        String fileName = service.getFileName();
        if (fileName != null) {
            handleDeployment(proxyDir.getAbsolutePath()
                    + File.separator + fileName, service.getName());
            File proxyFile = new File(proxyDir, fileName);
            writeToFile(proxyElem, proxyFile);
        } else if (parent != null) {
            parent.addChild(proxyElem);
        }

        return proxyElem;
    }

    public OMElement serializeEventSource(SynapseEventSource source, OMElement parent) throws Exception {
        File eventsDir = new File(rootDirectory, MultiXMLConfigurationBuilder.EVENTS_DIR);
        if (!eventsDir.exists() && !eventsDir.mkdirs()) {
            throw new Exception("Error while creating the directory for events : " +
                    eventsDir.getAbsolutePath());
        }

        OMElement eventSrcElem = EventSourceSerializer.serializeEventSource(null, source);

        String fileName = source.getFileName();
        if (fileName != null) {
            handleDeployment(eventsDir.getAbsolutePath()
                    + File.separator + fileName, source.getName());
            File eventSrcFile = new File(eventsDir, source.getFileName());
            writeToFile(eventSrcElem, eventSrcFile);
        } else if (parent != null) {
            parent.addChild(eventSrcElem);
        }

        return eventSrcElem;
    }

    public OMElement serializeTask(Startup task, OMElement parent) throws Exception {

        File tasksDir = new File(rootDirectory, MultiXMLConfigurationBuilder.TASKS_DIR);
        if (!tasksDir.exists() && !tasksDir.mkdirs()) {
            throw new Exception("Error while creating the directory for tasks : " +
                    tasksDir.getAbsolutePath());
        }

        OMElement taskElem = StartupFinder.getInstance().serializeStartup(null, task);

        if (task instanceof AbstractStartup && ((AbstractStartup) task).getFileName() != null) {
            String fileName = ((AbstractStartup) task).getFileName();
            handleDeployment(tasksDir.getAbsolutePath()
                    + File.separator +  fileName, task.getName());
            File taskFile = new File(tasksDir, fileName);
            writeToFile(taskElem, taskFile);
        } else if (parent != null) {
            parent.addChild(taskElem);
        }

        return taskElem;
    }

    public OMElement serializeSequence(SequenceMediator seq, OMElement parent) throws Exception {

        File seqDir = new File(rootDirectory, MultiXMLConfigurationBuilder.SEQUENCES_DIR);
        if (!seqDir.exists() && !seqDir.mkdirs()) {
            throw new Exception("Error while creating the directory for sequences : " +
                    seqDir.getAbsolutePath());
        }

        OMElement seqElem = MediatorSerializerFinder.getInstance().getSerializer(seq).
                serializeMediator(null, seq);
        String fileName = seq.getFileName();
        if (fileName != null) {
            handleDeployment(seqDir.getAbsolutePath()
                    + File.separator + fileName, seq.getName());
            File seqFile = new File(seqDir, fileName);
            writeToFile(seqElem, seqFile);
        } else if (parent != null) {
            parent.addChild(seqElem);
        }

        return seqElem;
    }

    public OMElement serializeEndpoint(Endpoint epr, OMElement parent) throws Exception {

        File eprDir = new File(rootDirectory, MultiXMLConfigurationBuilder.ENDPOINTS_DIR);
        if (!eprDir.exists() && !eprDir.mkdirs()) {
            throw new Exception("Error while creating the directory for endpoints : " +
                    eprDir.getAbsolutePath());
        }

        OMElement eprElem = EndpointSerializer.getElementFromEndpoint(epr);

        String fileName = epr.getFileName();
        if (fileName != null) {
            handleDeployment(eprDir.getAbsolutePath()
                    + File.separator + fileName, epr.getName());
            File eprFile = new File(eprDir, fileName);
            writeToFile(eprElem, eprFile);
        } else if (parent != null) {
            parent.addChild(eprElem);
        }

        return eprElem;
    }

    public OMElement serializeLocalEntry(Object o, OMElement parent) throws Exception {
        if (o instanceof SequenceMediator) {
            return serializeSequence((SequenceMediator) o, parent);
        } else if (o instanceof Endpoint) {
            return serializeEndpoint((Endpoint) o, parent);
        } else if (o instanceof Entry) {
            Entry entry = (Entry) o;
            if ((SynapseConstants.SERVER_HOST.equals(entry.getKey())
                    || SynapseConstants.SERVER_IP.equals(entry.getKey()))
                    || entry.getType() == Entry.REMOTE_ENTRY) {
                return null;
            }

            File entriesDir = new File(rootDirectory, MultiXMLConfigurationBuilder.
                        LOCAL_ENTRY_DIR);
            OMElement entryElem = EntrySerializer.serializeEntry(entry, null);
            if (!entriesDir.exists() && !entriesDir.mkdirs()) {
                throw new Exception("Error while creating the local entries directory : " +
                        entriesDir.getAbsolutePath());
            }

            String fileName = entry.getFileName();
            if (fileName != null) {
                handleDeployment(entriesDir.getAbsolutePath()
                        + File.separator + fileName, entry.getKey());
                File entryFile  = new File(entriesDir, fileName);
                writeToFile(entryElem, entryFile);
            } else if (parent != null) {
                parent.addChild(entryElem);
            }

            return entryElem;
        }
        return null;
    }

    private void writeToFile(OMElement content, File file) throws Exception {
        OutputStream out = new FileOutputStream(file);
        XMLPrettyPrinter.prettify(content, out);
        out.flush();
        out.close();
    }

    private void cleanUpDirectory()  throws Exception {
        // If the target directory already exists and contains any files simply rename it to
        // create a backup - This method does not delete the target directory
        if (rootDirectory.exists() && rootDirectory.isDirectory() &&
                rootDirectory.listFiles().length > 0) {
            if (log.isDebugEnabled()) {
                log.debug("The directory :" + rootDirectory.getPath() + " already exists. " +
                        "Creating a backup.");
            }

            backupDirectory = new File(rootDirectory.getParentFile(), "__tmp" +
                    new GregorianCalendar().getTimeInMillis());
            if (!rootDirectory.renameTo(backupDirectory)) {
                throw new Exception("Error occured while backing up the existing file structure " +
                        "at : " + rootDirectory.getAbsolutePath());
            }
        }

        // Create a new target directory
        if (!rootDirectory.mkdirs()) {
            throw new Exception("Error while creating the directory at : " +
                    rootDirectory.getAbsolutePath());
        }
    }

    private void restoreBackup() {
        if (backupDirectory != null) {
            if (log.isDebugEnabled()) {
                log.debug("Attempting to restore the directory : " + rootDirectory.getPath() +
                        " using the available backups");
            }

            if (rootDirectory.exists() && rootDirectory.isDirectory()) {
                deleteDirectory(rootDirectory);
            }

            if (backupDirectory.renameTo(rootDirectory)) {
                log.info("Successfully restored the directory at : " + rootDirectory.getPath());
                backupDirectory = null;
            } else {
                log.error("Failed to restore the directory at : " + rootDirectory.getPath() +
                        " from the available backup. You will need to restore the directory " +
                        "manually. A backup is available at : " + backupDirectory.getPath());
            }
        }
    }

    private boolean deleteDirectory(File dir) {
        if (log.isDebugEnabled()) {
            log.debug("Deleting the file or directory : " + dir.getPath());
        }

        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (String child : children) {
                boolean success = deleteDirectory(new File(dir, child));
                if (!success) {
                    return false;
                }
            }
        }

        // The directory is now empty so delete it
        return dir.delete();
    }

    private void serializeProxyServices(Collection<ProxyService> proxyServices, OMElement parent)
            throws Exception {
        for (ProxyService service : proxyServices) {
            serializeProxy(service, parent);
        }
    }

    private void serializeLocalRegistryValues(Collection localValues, OMElement parent)
            throws Exception {
        for (Object o : localValues) {
            serializeLocalEntry(o, parent);
        }
    }

    private void serializeTasks(Collection<Startup> tasks, OMElement parent) throws Exception {
        for (Startup task : tasks) {
            serializeTask(task, parent);
        }
    }

    private void serializeEventSources(Collection<SynapseEventSource> eventSources,
                                       OMElement parent) throws Exception {
        for (SynapseEventSource source : eventSources) {
            serializeEventSource(source, parent);
        }
    }

    private void serializeExecutors(Collection<PriorityExecutor> executors,
                                       OMElement parent) throws Exception {
        for (PriorityExecutor source : executors) {
            serializeExecutor(source, parent);
        }
    }

    private OMElement serializeExecutor(PriorityExecutor source, OMElement parent) throws Exception {
        File executorDir = new File(rootDirectory, MultiXMLConfigurationBuilder.EXECUTORS_DIR);
        if (!executorDir.exists() && !executorDir.mkdirs()) {
            throw new Exception("Error while creating the directory for executors : " +
                    executorDir.getAbsolutePath());
        }

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

    private void handleDeployment(String fileName, String artifactName) {
        if (!deploymentStore.containsFileName(fileName)) {
            deploymentStore.addArtifact(fileName, artifactName);
        }
        deploymentStore.addRestoredArtifact(fileName);
    }
}