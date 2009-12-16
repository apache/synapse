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
import org.apache.synapse.registry.Registry;
import org.apache.synapse.core.axis2.ProxyService;
import org.apache.synapse.eventing.SynapseEventSource;
import org.apache.synapse.Startup;
import org.apache.synapse.SynapseConstants;
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
public class MultiXMLConfigurationSerializer {

    /** The directory to where the configuration should be serialized */
    private File rootDirectory;
    /** The backup directory to be created when the target directory already exists */
    private File backupDirectory;

    private static Log log = LogFactory.getLog(MultiXMLConfigurationSerializer.class);

    public MultiXMLConfigurationSerializer(String directoryPath) {
        rootDirectory = new File(directoryPath);
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

            // Now serialize the content to synapse.xml
            serializeSynapseXML(definitions);

            log.info("Done serializing the Synapse configuration to : " + rootDirectory.getPath());

            // If a backup was created, clean it up
            if (backupDirectory != null) {
                log.info("Cleaning up the backup files at : " + backupDirectory.getPath());
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
        if (!proxyDir.exists() && !proxyDir.mkdir()) {
            throw new Exception("Error while creating the directory for proxy services : " +
                    proxyDir.getAbsolutePath());
        }

        File eventsDir = new File(rootDirectory, MultiXMLConfigurationBuilder.EVENTS_DIR);
        if (!eventsDir.exists() && !eventsDir.mkdir()) {
            throw new Exception("Error while creating the directory for events : " +
                    eventsDir.getAbsolutePath());
        }

        File entriesDir = new File(rootDirectory, MultiXMLConfigurationBuilder.LOCAL_ENTRY_DIR);
        if (!entriesDir.exists() && !entriesDir.mkdir()) {
            throw new Exception("Error while creating the local entries directory : " +
                    entriesDir.getAbsolutePath());
        }

        File eprDir = new File(rootDirectory, MultiXMLConfigurationBuilder.ENDPOINTS_DIR);
        if (!eprDir.exists() && !eprDir.mkdir()) {
            throw new Exception("Error while creating the directory for endpoints : " +
                    eprDir.getAbsolutePath());
        }

        File seqDir = new File(rootDirectory, MultiXMLConfigurationBuilder.SEQUENCES_DIR);
        if (!seqDir.exists() && !seqDir.mkdir()) {
            throw new Exception("Error while creating the directory for sequences : " +
                    seqDir.getAbsolutePath());
        }

        File tasksDir = new File(rootDirectory, MultiXMLConfigurationBuilder.TASKS_DIR);
        if (!tasksDir.exists() && !tasksDir.mkdir()) {
            throw new Exception("Error while creating the directory for tasks : " +
                    tasksDir.getAbsolutePath());
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
                            getSerializer(seq).serializeMediator(null, seq);
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

        serializeSynapseXML(definitions);
    }

    private void serializeSynapseXML(OMElement definitions) throws Exception {
        File synapseXML = new File(rootDirectory, SynapseConstants.SYNAPSE_XML);
        if (!rootDirectory.exists() && !rootDirectory.mkdir()) {
            throw new Exception("Error while creating the root configuration directory " +
                    "at: " + rootDirectory.getAbsolutePath());                
        }

        if (synapseXML.exists() && !synapseXML.delete()) {
            throw new Exception("Error while deleting the existing synapse.xml file");            
        }

        if (synapseXML.createNewFile()) {
            OutputStream out = new FileOutputStream(synapseXML);
            XMLPrettyPrinter.prettify(definitions, out);
            out.close();
        } else {
            throw new Exception("Error while creating the Synapse configuration " +
                    "file at : " + synapseXML.getAbsolutePath());
        }
    }

    public void serializeSynapseRegistry(Registry registry, SynapseConfiguration synapseConfig,
                                         OMElement parent) throws Exception {
        OMElement registryElem = RegistrySerializer.serializeRegistry(null, registry);
        if (!String.valueOf(Boolean.TRUE).equals(
                synapseConfig.getProperty(MultiXMLConfigurationBuilder.SEPARATE_REGISTRY_DEFINITION))) {
            parent.addChild(registryElem);
            return;
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
    }

    public void serializeProxy(ProxyService service, OMElement parent) throws Exception {
        File proxyDir = new File(rootDirectory, MultiXMLConfigurationBuilder.PROXY_SERVICES_DIR);
        if (!proxyDir.exists() && !proxyDir.mkdir()) {
            throw new Exception("Error while creating the directory for proxy services : " +
                    proxyDir.getAbsolutePath());
        }

        OMElement proxyElem = ProxyServiceSerializer.serializeProxy(null, service);

        if (service.getFileName() != null) {
            File proxyFile = new File(proxyDir, service.getFileName());
            if (proxyFile.createNewFile()) {
                OutputStream out = new FileOutputStream(proxyFile);
                XMLPrettyPrinter.prettify(proxyElem, out);
                out.flush();
            } else {
                throw new Exception("Error while creating the file : " +
                        proxyFile.getAbsolutePath());
            }
        } else {
            parent.addChild(proxyElem);
        }
    }

    public void serializeEventSource(SynapseEventSource source, OMElement parent) throws Exception {
        File eventsDir = new File(rootDirectory, MultiXMLConfigurationBuilder.EVENTS_DIR);
        if (!eventsDir.exists() && !eventsDir.mkdir()) {
            throw new Exception("Error while creating the directory for events : " +
                    eventsDir.getAbsolutePath());
        }

        OMElement eventSrcElem = EventSourceSerializer.serializeEventSource(null, source);

        if (source.getFileName() != null) {
            File eventSrcFile = new File(eventsDir, source.getFileName());
            if (eventSrcFile.createNewFile()) {
                OutputStream out = new FileOutputStream(eventSrcFile);
                XMLPrettyPrinter.prettify(eventSrcElem, out);
                out.flush();
            } else {
                throw new Exception("Error while creating the file : " +
                        eventSrcFile.getAbsolutePath());
            }
        } else {
            parent.addChild(eventSrcElem);
        }
    }

    public void serializeTask(Startup task, OMElement parent) throws Exception {
        File tasksDir = new File(rootDirectory, MultiXMLConfigurationBuilder.TASKS_DIR);
        if (!tasksDir.exists() && !tasksDir.mkdir()) {
            throw new Exception("Error while creating the directory for tasks : " +
                    tasksDir.getAbsolutePath());
        }

        OMElement taskElem = StartupFinder.getInstance().serializeStartup(null, task);

        if (task instanceof AbstractStartup && ((AbstractStartup) task).getFileName() != null) {
            File taskFile = new File(tasksDir, ((AbstractStartup) task).getFileName());
            if (taskFile.createNewFile()) {
                OutputStream out = new FileOutputStream(taskFile);
                XMLPrettyPrinter.prettify(taskElem, out);
                out.flush();
            } else {
                throw new Exception("Error while creating the file : " +
                        taskFile.getAbsolutePath());
            }
        } else {
            parent.addChild(taskElem);
        }
    }

    public void serializeSequence(SequenceMediator seq, OMElement parent) throws Exception {

        File seqDir = new File(rootDirectory, MultiXMLConfigurationBuilder.SEQUENCES_DIR);
        if (!seqDir.exists() && !seqDir.mkdir()) {
            throw new Exception("Error while creating the directory for sequences : " +
                    seqDir.getAbsolutePath());
        }

        OMElement seqElem = MediatorSerializerFinder.getInstance().getSerializer(seq).
                serializeMediator(null, seq);
        File seqFile;
        if (seq.getFileName() != null) {
            seqFile = new File(seqDir, seq.getFileName());
            if (seqFile.createNewFile()) {
                OutputStream out = new FileOutputStream(seqFile);
                XMLPrettyPrinter.prettify(seqElem, out);
                out.close();
            } else {
                throw new Exception("Error while creating the file : " + seqFile.getAbsolutePath());
            }
        } else {
            parent.addChild(seqElem);
        }

    }

    public void serializeEndpoint(Endpoint epr, OMElement parent) throws Exception {
        File eprDir = new File(rootDirectory, MultiXMLConfigurationBuilder.ENDPOINTS_DIR);
        if (!eprDir.exists() && !eprDir.mkdir()) {
            throw new Exception("Error while creating the directory for endpoints : " +
                    eprDir.getAbsolutePath());
        }

        OMElement eprElem = EndpointSerializer.getElementFromEndpoint(epr);
        File eprFile;
        if (epr instanceof AbstractEndpoint && ((AbstractEndpoint) epr).getFileName() != null) {
            eprFile = new File(eprDir, ((AbstractEndpoint) epr).getFileName());
            if (eprFile.createNewFile()) {
                OutputStream out = new FileOutputStream(eprFile);
                XMLPrettyPrinter.prettify(eprElem, out);
                out.flush();
            } else {
                throw new Exception("Error while creating the file : " + eprFile.getAbsolutePath());
            }
        } else {
            parent.addChild(eprElem);
        }

    }

    public void serializeLocalEntry(Object o, OMElement parent) throws Exception {
        if (o instanceof SequenceMediator) {
            serializeSequence((SequenceMediator) o, parent);
        } else if (o instanceof Endpoint) {
            serializeEndpoint((Endpoint) o, parent);
        } else if (o instanceof Entry) {
            Entry entry = (Entry) o;
            if ((SynapseConstants.SERVER_HOST.equals(entry.getKey())
                    || SynapseConstants.SERVER_IP.equals(entry.getKey()))
                    || entry.getType() == Entry.REMOTE_ENTRY) {
                return;
            }

            File entriesDir = new File(rootDirectory, MultiXMLConfigurationBuilder.
                        LOCAL_ENTRY_DIR);
            OMElement entryElem = EntrySerializer.serializeEntry(entry, null);
            if (!entriesDir.exists() && !entriesDir.mkdir()) {
                throw new Exception("Error while creating the local entries directory : " +
                        entriesDir.getAbsolutePath());
            }

            File entryFile;
            if (entry.getFileName() != null) {
               entryFile  = new File(entriesDir, entry.getFileName());
                if (entryFile.createNewFile()) {
                    OutputStream out = new FileOutputStream(entryFile);
                    XMLPrettyPrinter.prettify(entryElem, out);
                    out.flush();
                } else {
                    throw new Exception("Error while creating the file : " +
                            entryFile.getAbsolutePath());
                }
            } else {
                parent.addChild(entryElem);
            }
        }
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

}