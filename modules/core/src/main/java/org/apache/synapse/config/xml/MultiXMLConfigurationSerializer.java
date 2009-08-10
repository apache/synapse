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

            // Serialize various elements in the SynapseConfiguration
            if (synapseConfig.getRegistry() != null) {
                serializeSynapseRegistry(synapseConfig.getRegistry());
            }

            Collection<ProxyService> proxyServices = synapseConfig.getProxyServices();
            if (!proxyServices.isEmpty()) {
                serializeProxyServices(proxyServices, definitions);
            }

            Collection<SynapseEventSource> eventSources = synapseConfig.getEventSources();
            if (!eventSources.isEmpty()) {
                serializeEventSources(eventSources, definitions);
            }

            Collection<Startup> tasks = synapseConfig.getStartups();
            if (!tasks.isEmpty()) {
                serializeTasks(tasks, definitions);
            }

            Collection localRegistryValues = synapseConfig.getLocalRegistry().values();
            if (!localRegistryValues.isEmpty()) {
                serializeLocalRegistryValues(localRegistryValues, definitions);
            }

            // Now serialize the content to synapse.xml
            File synapseXML = new File(rootDirectory, SynapseConstants.SYNAPSE_XML);
            if (synapseXML.createNewFile()) {
                OutputStream out = new FileOutputStream(synapseXML);
                definitions.serializeAndConsume(out);
                out.flush();
            } else {
                throw new Exception("Error while creating the Synapse configuration file at : " +
                synapseXML.getAbsolutePath());
            }

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

    private void serializeSynapseRegistry(Registry registry) throws Exception {
        OMElement registryElem = RegistrySerializer.serializeRegistry(null, registry);
        File registryConf = new File(rootDirectory, MultiXMLConfigurationBuilder.REGISTRY_FILE);
        if (log.isDebugEnabled()) {
            log.debug("Serializing Synapse registry definition to : " + registryConf.getPath());
        }

        if (registryConf.createNewFile()) {
            OutputStream out = new FileOutputStream(registryConf);
            registryElem.serializeAndConsume(out);
            out.flush();
        } else {
            throw new Exception("Error while creating the registry configuration file at : " +
                    registryConf.getAbsolutePath());
        }
    }

    private void serializeProxyServices(Collection<ProxyService> proxyServices, OMElement parent)
            throws Exception {

        File proxyDir = new File(rootDirectory, MultiXMLConfigurationBuilder.PROXY_SERVICES_DIR);
        if (log.isDebugEnabled()) {
            log.debug("Serializing Synapse proxy services to : " + proxyDir.getPath());
        }

        if (!proxyDir.mkdir()) {
            throw new Exception("Error while creating the directory for proxy services : " +
                    proxyDir.getAbsolutePath());
        }

        for (ProxyService service : proxyServices) {
            OMElement proxyElem = ProxyServiceSerializer.serializeProxy(null, service);

            if (service.getFileName() != null) {
                File proxyFile = new File(proxyDir, service.getFileName());
                if (proxyFile.createNewFile()) {
                    OutputStream out = new FileOutputStream(proxyFile);
                    proxyElem.serializeAndConsume(out);
                    out.flush();
                } else {
                    throw new Exception("Error while creating the file : " +
                            proxyFile.getAbsolutePath());
                }
            } else {
                parent.addChild(proxyElem);
            }
        }
    }

    private void serializeEventSources(Collection<SynapseEventSource> eventSources,
                                       OMElement parent) throws Exception {

        File eventsDir = new File(rootDirectory, MultiXMLConfigurationBuilder.EVENTS_DIR);
        if (log.isDebugEnabled()) {
            log.debug("Serializing Synapse event sources to : " + eventsDir.getPath());
        }

        if (!eventsDir.mkdir()) {
            throw new Exception("Error while creating the directory for events : " +
                    eventsDir.getAbsolutePath());
        }

        for (SynapseEventSource source : eventSources) {
            OMElement eventSrcElem = EventSourceSerializer.serializeEventSource(null, source);

            if (source.getFileName() != null) {
                File eventSrcFile = new File(eventsDir, source.getFileName());
                if (eventSrcFile.createNewFile()) {
                    OutputStream out = new FileOutputStream(eventSrcFile);
                    eventSrcElem.serializeAndConsume(out);
                    out.flush();
                } else {
                    throw new Exception("Error while creating the file : " +
                            eventSrcFile.getAbsolutePath());
                }
            } else {
                parent.addChild(eventSrcElem);
            }
        }
    }

    private void serializeTasks(Collection<Startup> tasks, OMElement parent) throws Exception {

        File tasksDir = new File(rootDirectory, MultiXMLConfigurationBuilder.TASKS_DIR);
        if (log.isDebugEnabled()) {
            log.debug("Serializing Synapse startup tasks to : " + tasksDir.getPath());
        }

        if (!tasksDir.mkdir()) {
            throw new Exception("Error while creating the directory for tasks : " +
                    tasksDir.getAbsolutePath());
        }

        for (Startup task : tasks) {
            File taskFile;
            OMElement taskElem = StartupFinder.getInstance().serializeStartup(null, task);

            if (task instanceof AbstractStartup && ((AbstractStartup) task).getFileName() != null) {
                taskFile = new File(tasksDir, ((AbstractStartup) task).getFileName());
                if (taskFile.createNewFile()) {
                    OutputStream out = new FileOutputStream(taskFile);
                    taskElem.serializeAndConsume(out);
                    out.flush();
                } else {
                    throw new Exception("Error while creating the file : " +
                            taskFile.getAbsolutePath());
                }
            } else {
                parent.addChild(taskElem);
            }
        }
    }

    private void serializeSequence(SequenceMediator seq, OMElement parent) throws Exception {

        File seqDir = new File(rootDirectory, MultiXMLConfigurationBuilder.SEQUENCES_DIR);
        if (!seqDir.exists()) {
            if (!seqDir.mkdir()) {
                throw new Exception("Error while creating the directory for sequences : " +
                        seqDir.getAbsolutePath());
            }
        }

        OMElement seqElem = MediatorSerializerFinder.getInstance().getSerializer(seq).
                serializeMediator(null, seq);
        File seqFile;
        if (seq.getFileName() != null) {
            seqFile = new File(seqDir, seq.getFileName());
            if (seqFile.createNewFile()) {
                OutputStream out = new FileOutputStream(seqFile);
                seqElem.serializeAndConsume(out);
                out.flush();
            } else {
                throw new Exception("Error while creating the file : " + seqFile.getAbsolutePath());
            }
        } else {
            parent.addChild(seqElem);
        }

    }

    private void serializeEndpoint(Endpoint epr, OMElement parent) throws Exception {
        File eprDir = new File(rootDirectory, MultiXMLConfigurationBuilder.ENDPOINTS_DIR);
        if (!eprDir.exists()) {
            if (!eprDir.mkdir()) {
                throw new Exception("Error while creating the directory for endpoints : " +
                        eprDir.getAbsolutePath());
            }
        }

        OMElement eprElem = EndpointSerializer.getElementFromEndpoint(epr);
        File eprFile;
        if (epr instanceof AbstractEndpoint && ((AbstractEndpoint) epr).getFileName() != null) {
            eprFile = new File(eprDir, ((AbstractEndpoint) epr).getFileName());
            if (eprFile.createNewFile()) {
                OutputStream out = new FileOutputStream(eprFile);
                eprElem.serializeAndConsume(out);
                out.flush();
            } else {
                throw new Exception("Error while creating the file : " + eprFile.getAbsolutePath());
            }
        } else {
            parent.addChild(eprElem);
        }

    }

    private void serializeLocalRegistryValues(Collection localValues, OMElement parent)
            throws Exception {

        if (log.isDebugEnabled()) {
            log.debug("Serializing the local registry values (sequences/endpoints/local entries)");
        }

        boolean entriesDirCreated = false;

        for (Object o : localValues) {
            if (o instanceof SequenceMediator) {
                serializeSequence((SequenceMediator) o, parent);
            } else if (o instanceof Endpoint) {
                serializeEndpoint((Endpoint) o, parent);
            } else if (o instanceof Entry) {
                Entry entry = (Entry) o;
                if ((SynapseConstants.SERVER_HOST.equals(entry.getKey())
                        || SynapseConstants.SERVER_IP.equals(entry.getKey()))
                        || entry.getType() == Entry.REMOTE_ENTRY) {
                    continue;
                }

                File entriesDir = null;
                OMElement entryElem = EntrySerializer.serializeEntry(entry, null);
                if (!entriesDirCreated) {
                    entriesDir = new File(rootDirectory, MultiXMLConfigurationBuilder.
                            LOCAL_ENTRY_DIR);
                    if (!entriesDir.mkdir()) {
                        throw new Exception("Error while creating the local entries directory : " +
                                entriesDir.getAbsolutePath());
                    }
                    entriesDirCreated = true;
                }

                File entryFile;
                if (entry.getFileName() != null) {
                   entryFile  = new File(entriesDir, entry.getFileName());
                    if (entryFile.createNewFile()) {
                        OutputStream out = new FileOutputStream(entryFile);
                        entryElem.serializeAndConsume(out);
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

}