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

package org.apache.synapse.config;

import org.apache.axis2.AxisFault;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.*;
import org.apache.synapse.eventing.SynapseEventSource;
import org.apache.synapse.commons.datasource.DataSourceHelper;
import org.apache.synapse.task.TaskDescriptionRepository;
import org.apache.synapse.task.TaskDescriptionRepositoryFactory;
import org.apache.synapse.task.TaskScheduler;
import org.apache.synapse.task.TaskSchedulerFactory;
import org.apache.synapse.config.xml.MediatorFactoryFinder;
import org.apache.synapse.config.xml.endpoints.XMLToEndpointMapper;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.ProxyService;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.endpoints.dispatch.SALSessions;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.registry.Registry;
import org.apache.axiom.om.OMNode;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.util.*;

/**
 * The SynapseConfiguration holds the global configuration for a Synapse
 * instance.
 */
public class SynapseConfiguration implements ManagedLifecycle {

    private static final Log log = LogFactory.getLog(SynapseConfiguration.class);

    private static final String ENTRY = "entry";
    private static final String ENDPOINT = "endpoint";
    private static final String SEQUENCE = "sequence"; 

    /**
     * The remote registry made available to the Synapse configuration. Only one
     * is supported
     */
    private Registry registry = null;

    /**
     * This holds the default QName of the configuration.
     */
    private QName defaultQName = null;

    /**
     * Mandatory sequence is treated specially since it is required for each and every message.
     * Keeps the reference to the mandatory sequence while it is available as a sequence in the
     * localRegistry map
     */
    private Mediator mandatorySequence = null;

    /**
     * Holds Proxy services defined through Synapse
     */
    private final Map<String, ProxyService> proxyServices = new HashMap<String, ProxyService>();

    /**
     * This holds a Map of ManagedLifecycle objects
     */
    private final Map<String, Startup> startups = new HashMap<String, Startup>();

    /**
     * The local registry is a simple HashMap and provides the ability to
     * override definitions of a remote registry for entries defined locally
     * with the same key
     */
    private final Map<String, Object> localRegistry = new HashMap<String, Object>();

    /** Holds the synapse properties */
    private Properties properties = new Properties();

    /**
     * This will provide the timer daemon object for the scheduled tasks.
     */
    private Timer synapseTimer = new Timer(true);

    /** Hold reference to the Axis2 ConfigurationContext */
    private AxisConfiguration axisConfiguration = null;
    
    private final TaskDescriptionRepository taskDescriptionRepository = 
            TaskDescriptionRepositoryFactory.getTaskDescriptionRepository(
                    SynapseConstants.SYNAPSE_STARTUP_TASK_DESCRIPTIONS_REPOSITORY);        
    
    /**
     * Save the path to the configuration file loaded, to save it later if
     * required
     */
    private String pathToConfigFile = null;


    /**
     * Holds Event Sources defined through Synapse
     */
    private Map<String, SynapseEventSource> eventSources = new HashMap<String, SynapseEventSource>();


    /**
     * Add a named sequence into the local registry. If a sequence already exists by the specified
     * key a runtime exception is thrown.
     *
     * @param key
     *            the name for the sequence
     * @param mediator
     *            a Sequence mediator
     */
    public void addSequence(String key, Mediator mediator) {
        assertAlreadyExists(key,SEQUENCE);
        localRegistry.put(key, mediator);
    }

    /**
     * Allow a dynamic sequence to be cached and made available through the
     * local registry. If a sequence already exists by the specified
     * key a runtime exception is thrown.
     *
     * @param key
     *            the key to lookup the sequence from the remote registry
     * @param entry
     *            the Entry object which holds meta information and the cached
     *            resource
     */
    public void addSequence(String key, Entry entry) {
        assertAlreadyExists(key,ENTRY);
        localRegistry.put(key, entry);
    }

    /**
     * Returns the map of defined sequences in the configuration excluding the
     * fetched sequences from remote registry.
     *
     * @return Map of SequenceMediators defined in the local configuration
     */
    public Map<String, SequenceMediator> getDefinedSequences() {

        Map<String, SequenceMediator> definedSequences = new HashMap<String, SequenceMediator>();

        for (Object o : localRegistry.values()) {

            if (o instanceof SequenceMediator) {
                SequenceMediator seq = (SequenceMediator) o;
                definedSequences.put(seq.getName(), seq);
            }
        }
        return definedSequences;
    }

    /**
     * Gets the mandatory sequence, from the direct reference. This is also available in the
     * {@link SynapseConfiguration#getSequence} but this method improves the performance hence this
     * will be required for all messages
     *
     * @return mandatory sequence direct reference in the local configuration
     */
    public Mediator getMandatorySequence() {
        return mandatorySequence;
    }

    /**
     * Sets the mandatory sequence direct reference
     * 
     * @param mandatorySequence to be set as the direct reference
     */
    public void setMandatorySequence(Mediator mandatorySequence) {
        this.mandatorySequence = mandatorySequence;
    }

    /**
     * Return the sequence specified with the given key
     *
     * @param key
     *            the key being referenced
     * @return the sequence referenced by the key
     */
    public Mediator getSequence(String key) {

        Object o = getEntry(key);
        if (o instanceof Mediator) {
            return (Mediator) o;
        }

        Entry entry = null;
        if (o == null) {
            entry = new Entry(key);
            entry.setType(Entry.REMOTE_ENTRY);
        } else {
            Object object = localRegistry.get(key);
            if (object instanceof Entry) {
                entry = (Entry) object;
            }
        }

        assertEnrtyNull(entry, key);

        if (entry.getMapper() == null) {
            entry.setMapper(MediatorFactoryFinder.getInstance());
        }

        if (entry.getType() == Entry.REMOTE_ENTRY) {
            if (registry != null) {
                o = registry.getResource(entry);
                if (o != null && o instanceof Mediator) {
                    localRegistry.put(key, entry);
                    return (Mediator) o;
                }
            }
        } else {
            Object value = entry.getValue();
            if (value instanceof OMNode) {
                Object object = entry.getMapper().getObjectFromOMNode((OMNode) value);
                if (object instanceof Mediator) {
                    entry.setValue(object);
                    return (Mediator) object;
                }
            }
        }

        return null;
    }

    /**
     * Removes a sequence from the local registry
     *
     * @param key
     *            of the sequence to be removed
     */
    public void removeSequence(String key) {
        Object sequence = localRegistry.get(key);
        if (sequence instanceof Mediator) {
            localRegistry.remove(key);
        } else {
            handleException("No sequence exists by the key/name : " + key);
        }
    }

    /**
     * Return the main/default sequence to be executed. This is the sequence
     * which will execute for all messages when message mediation takes place
     *
     * @return the main mediator sequence
     */
    public Mediator getMainSequence() {
        return getSequence(SynapseConstants.MAIN_SEQUENCE_KEY);
    }

    /**
     * Return the fault sequence to be executed when Synapse encounters a fault
     * scenario during processing
     *
     * @return the fault sequence
     */
    public Mediator getFaultSequence() {
        return getSequence(SynapseConstants.FAULT_SEQUENCE_KEY);
    }

    /**
     * Define a resource to the local registry. All static resources (e.g. URL
     * source) are loaded during this definition phase, and the inability to
     * load such a resource will not allow the definition of the resource to the
     * local registry. If an entry already exists by the specified key a runtime
     * exception is thrown.
     *
     * @param key
     *            the key associated with the resource
     * @param entry
     *            the Entry that holds meta information about the resource and
     *            its contents (or cached contents if the Entry refers to a
     *            dynamic resource off a remote registry)
     */
    public void addEntry(String key, Entry entry) {

        assertAlreadyExists(key, ENTRY);
        
        if (entry.getType() == Entry.URL_SRC && entry.getValue() == null) {
            try {
                entry.setValue(SynapseConfigUtils.getOMElementFromURL(entry.getSrc()
                        .toString()));
                localRegistry.put(key, entry);
            } catch (IOException e) {
                handleException("Can not read from source URL : "
                        + entry.getSrc());
            }
        } else {
            localRegistry.put(key, entry);
        }
    }

    /**
     * Gives the set of remote entries that are cached in localRegistry as mapping of entry key
     * to the Entry definition
     * 
     * @return Map of locally cached entries
     */
    public Map<String, Entry> getCachedEntries() {

        Map<String, Entry> cachedEntries = new HashMap<String, Entry>();
        for (Object o : localRegistry.values()) {

            if (o != null && o instanceof Entry) {
                Entry entry = (Entry) o;
                if (entry.isDynamic() && entry.isCached()) {
                    cachedEntries.put(entry.getKey(), entry);
                }
            }
        }

        return cachedEntries;
    }

    /**
     * Returns the map of defined entries in the configuration excluding the
     * fetched entries from remote registry.
     *
     * @return Map of Entries defined in the local configuraion
     */
    public Map<String, Entry> getDefinedEntries() {

        Map<String, Entry> definedEntries = new HashMap<String, Entry>();
        for (Object o : localRegistry.values()) {

            if (o instanceof Entry
                && ((Entry) o).getType() != Entry.REMOTE_ENTRY) {

                Entry entry = (Entry) o;
                definedEntries.put(entry.getKey(), entry);
            }
        }
        return definedEntries;
    }

    /**
     * Get the resource with the given key
     *
     * @param key
     *            the key of the resource required
     * @return its value
     */
    public Object getEntry(String key) {
        Object o = localRegistry.get(key);
        if (o != null && o instanceof Entry) {
            Entry entry = (Entry) o;
            if (entry.isDynamic()) {
                if (entry.isCached() && !entry.isExpired()) {
                    return entry.getValue();
                } else if (registry != null) {
                    o = registry.getResource(entry);
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Will not  evaluate the value of the remote entry with a key "
                            + key + ",  because the registry is not available");
                    }
                    return null; // otherwise will return an entry with a value null
                    // (method expects return  a value not an entry )
                }
            } else {
                return entry.getValue();
            }
        }
        return o;
    }

    /**
     * Get the Entry object mapped to the given key
     *
     * @param key
     *            the key for which the Entry is required
     * @return its value
     */
    public Entry getEntryDefinition(String key) {
        Object o = localRegistry.get(key);
        if (o == null || o instanceof Entry) {
            if (o == null) {
                // this is not a local definition
                Entry entry = new Entry(key);
                entry.setType(Entry.REMOTE_ENTRY);
                addEntry(key, entry);
                return entry;
            }
            return (Entry) o;
        } else {
            if (log.isDebugEnabled()) {
                log.debug("There is no local registry entry for key : " + key);
            }
            return null;
        }
    }

    /**
     * Deletes any reference mapped to the given key from the local registry
     *
     * @param key
     *            the key of the reference to be removed
     */
    public void removeEntry(String key) {
        Object entry = localRegistry.get(key);
        if (entry instanceof Entry) {
            localRegistry.remove(key);
        } else {
            handleException("No entry exists by the key : " + key);
        }
    }

    /**
     * Clears the cache of the remote entry with the key specified
     * 
     * @param key - String key of the entry
     */
    public void clearCachedEntry(String key) {
        Entry entry = getEntryDefinition(key);
        if (entry != null && entry.isDynamic() && entry.isCached()) {
            entry.clearCache();
        }
    }

    /**
     * Clears the cache of all the remote entries which has been
     * cached in the configuration
     */
    public void clearCache() {

        for (Object o : localRegistry.values()) {
            
            if (o != null && o instanceof Entry) {
                Entry entry = (Entry) o;
                if (entry.isDynamic() && entry.isCached()) {
                    entry.clearCache();
                }
            }
        }
    }

    /**
     * Define a named endpoint with the given key. If an endpoint already exists by the specified
     * name a runtime exception is thrown.
     *
     * @param key
     *            the key for the endpoint
     * @param endpoint
     *            the endpoint definition
     */
    public void addEndpoint(String key, Endpoint endpoint) {
        assertAlreadyExists(key, ENDPOINT);
        localRegistry.put(key, endpoint);
    }

    /**
     * Add a dynamic endpoint definition to the local registry. If an endpoint already exists by
     * the specified name a runtime exception is thrown.
     *
     * @param key
     *            the key for the endpoint definition
     * @param entry
     *            the actual endpoint definition to be added
     */
    public void addEndpoint(String key, Entry entry) {
        assertAlreadyExists(key, ENTRY);
        localRegistry.put(key, entry);
    }

    /**
     * Returns the map of defined endpoints in the configuration excluding the
     * fetched endpoints from remote registry
     *
     * @return Map of Endpoints defined in the local configuration
     */
    public Map<String, Endpoint> getDefinedEndpoints() {

        Map<String, Endpoint> definedEndpoints = new HashMap<String, Endpoint>();
        for (Object o : localRegistry.values()) {

            if (o instanceof Endpoint) {
                Endpoint ep = (Endpoint) o;
                definedEndpoints.put(ep.getName(), ep);
            }
        }

        return definedEndpoints;
    }

    /**
     * Get the definition of the endpoint with the given key
     *
     * @param key
     *            the key of the endpoint
     * @return the endpoint definition
     */
    public Endpoint getEndpoint(String key) {

        Object o = getEntry(key);
        if (o != null && o instanceof Endpoint) {
            return (Endpoint) o;
        }

        Entry entry = null;
        if (o == null) {
            entry = new Entry(key);
            entry.setType(Entry.REMOTE_ENTRY);
        } else {
            Object object = localRegistry.get(key);
            if (object instanceof Entry) {
                entry = (Entry) object;
            }
        }

        assertEnrtyNull(entry, key);

        if (entry.getMapper() == null) {
            entry.setMapper(XMLToEndpointMapper.getInstance());
        }

        if (entry.getType() == Entry.REMOTE_ENTRY) {
            if (registry != null) {
                o = registry.getResource(entry);
                if (o != null && o instanceof Endpoint) {
                    localRegistry.put(key, entry);
                    return (Endpoint) o;
                }
            }
        } else {
            Object value = entry.getValue();
            if (value instanceof OMNode) {
                Object object = entry.getMapper().getObjectFromOMNode((OMNode) value);
                if (object instanceof Endpoint) {
                    entry.setValue(object);
                    return (Endpoint) object;
                }
            }
        }
        return null;
    }

    /**
     * Deletes the endpoint with the given key. If an endpoint does not exist by the specified
     * key a runtime exception is thrown.
     *
     * @param key
     *            of the endpoint to be deleted
     */
    public void removeEndpoint(String key) {
        Object endpoint = localRegistry.get(key);
        if (endpoint instanceof Endpoint) {
            localRegistry.remove(key);
        } else {
            handleException("No endpoint exists by the key/name : " + key);
        }
    }

    /**
     * Add a Proxy service to the configuration. If a proxy service already exists by the
     * specified name a runtime exception is thrown.
     *
     * @param name
     *            the name of the Proxy service
     * @param proxy
     *            the Proxy service instance
     */
    public void addProxyService(String name, ProxyService proxy) {
        if (!proxyServices.containsKey(name)) {
            proxyServices.put(name, proxy);
        } else {
            handleException("Duplicate proxy service by the name : " + name);
        }
    }

    /**
     * Get the Proxy service with the given name
     *
     * @param name
     *            the name being looked up
     * @return the Proxy service
     */
    public ProxyService getProxyService(String name) {
        return proxyServices.get(name);
    }

    /**
     * Deletes the Proxy Service named with the given name. If a proxy service does not exist by
     * the specified name a runtime exception is thrown.
     *
     * @param name
     *            of the Proxy Service to be deleted
     */
    public void removeProxyService(String name) {
        Object o = proxyServices.get(name);
        if (o == null) {
            handleException("Unknown proxy service for name : " + name);
        } else {
            try {
                if (getAxisConfiguration().getServiceForActivation(name) != null) {
                    if (getAxisConfiguration().getServiceForActivation(name)
                            .isActive()) {
                        getAxisConfiguration().getService(name)
                                .setActive(false);
                    }
                    getAxisConfiguration().removeService(name);
                }
                proxyServices.remove(name);
            } catch (AxisFault axisFault) {
                handleException(axisFault.getMessage());
            }
        }
    }

    /**
     * Return the list of defined proxy services
     *
     * @return the proxy services defined
     */
    public Collection<ProxyService> getProxyServices() {
        return proxyServices.values();
    }

    /**
     * Return an unmodifiable copy of the local registry
     *
     * @return an unmodifiable copy of the local registry
     */
    public Map getLocalRegistry() {
        return Collections.unmodifiableMap(localRegistry);
    }

    /**
     * Get the remote registry defined (if any)
     *
     * @return the currently defined remote registry
     */
    public Registry getRegistry() {
        return registry;
    }

    /**
     * Set the remote registry for the configuration
     *
     * @param registry
     *            the remote registry for the configuration
     */
    public void setRegistry(Registry registry) {
        this.registry = registry;
    }

    /**
     * Set the Axis2 AxisConfiguration to the SynapseConfiguration
     *
     * @param axisConfig AxisConfiguration to be set
     */
    public void setAxisConfiguration(AxisConfiguration axisConfig) {
        this.axisConfiguration = axisConfig;
    }

    /**
     * Get the Axis2 AxisConfiguration for the SynapseConfiguration
     *
     * @return AxisConfiguration of the Axis2
     */
    public AxisConfiguration getAxisConfiguration() {
        return axisConfiguration;
    }

    /**
     * The path to the currently loaded configuration file
     *
     * @return file path to synapse.xml
     */
    public String getPathToConfigFile() {
        return pathToConfigFile;
    }

    /**
     * Set the path to the loaded synapse.xml
     *
     * @param pathToConfigFile
     *            path to the synapse.xml loaded
     */
    public void setPathToConfigFile(String pathToConfigFile) {
        this.pathToConfigFile = pathToConfigFile;
    }

    /**
     * Set the default QName of the Synapse Configuration
     * 
     * @param defaultQName
     *          QName specifying the default QName of the configuration
     */
    public void setDefaultQName(QName defaultQName) {
        this.defaultQName = defaultQName;
    }

    /**
     * Get the default QName of the configuration.
     * 
     * @return default QName of the configuration
     */
    public QName getDefaultQName() {
        return defaultQName;
    }

    /**
     * Get the timer object for the Synapse Configuration
     *
     * @return synapseTimer timer object of the configuration
     */
    public Timer getSynapseTimer() {
        return synapseTimer;
    }

    /**
     * Get the startup collection in the configuration
     *
     * @return collection of startup objects registered
     */
    public Collection<Startup> getStartups() {
        return startups.values();
    }

    /**
     * Get the Startup with the specified name
     * 
     * @param id - String name of the startup to be retrieved
     * @return Startup object with the specified name or null
     */
    public Startup getStartup(String id) {
        return startups.get(id);
    }

    /**
     * Add a startup to the startups map in the configuration. If a startup already exists by the
     * specified name a runtime exception is thrown.
     *
     * @param startup - Startup object to be added 
     */
    public void addStartup(Startup startup) {
        if (!startups.containsKey(startup.getName())) {
            startups.put(startup.getName(), startup);
        } else {
            handleException("Duplicate startup by the name : " + startup.getName());
        }
    }

    /**
     * Removes the startup specified by the name. If no startup exists by the specified name a
     * runtime exception is thrown.
     * 
     * @param name - name of the startup that needs to be removed
     */
    public void removeStartup(String name) {
        if (startups.containsKey(name)) {
            startups.remove(name);
        } else {
            handleException("No startup exists by the name : " + name);
        }
    }

    /**
     * Gets the properties to configure the Synapse environment.
     * 
     * @return set of properties as Properties
     */
    public Properties getProperties() {
        return properties;
    }

    /**
     * Sets the properties to configure the Synapse enviornment.
     *
     * @param properties - Properties which needs to be set
     */
    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    /**
     * Gets the String representation of the property value if there is a property for the
     * given propKey or returns the default value passed
     * 
     * @param propKey - key for the property lookup
     * @param def     - default value
     * @return String representation of the property value with the given key or the def value
     */
    public String getProperty(String propKey, String def) {
        String val = System.getProperty(propKey);
        if (val == null) {
            val = properties.getProperty(propKey);
        }

        if (val != null) {
            if (log.isDebugEnabled()) {
                log.debug("Using synapse tuning parameter : " + propKey + " = " + val);
            }
            return val;
        }
        return def;
    }

    /**
     * Gets the String representation of the property value if there is a property for the
     * given propKey or returns the default value passed
     *
     * @param propKey - key for the property lookup
     * @param def     - default value
     * @return String representation of the property value with the given key or the def value
     */
    public long getProperty(String propKey, long def) {
        String val = System.getProperty(propKey);
        if (val == null) {
            val = properties.getProperty(propKey);
        }

        if (val != null) {
            if (log.isDebugEnabled()) {
                log.debug("Using synapse tuning parameter : " + propKey + " = " + val);
            }
            return Long.valueOf(val);
        }
        return def;
    }

    /**
     * Gets the propety value if the property specified by the propKey is there or null else
     *
     * @param propKey - key for the property lookup
     * @return String representation of the property value if found or null else
     */
    public String getProperty(String propKey) {
        String val = System.getProperty(propKey);
        if (val == null) {
            val = properties.getProperty(propKey);
        }

        if (val != null) {
            if (log.isDebugEnabled()) {
                log.debug("Using synapse tuning parameter : " + propKey + " = " + val);
            }
            return val;
        }
        return null;
    }

    /**
     * This method will be called on the soft shutdown or destroying the configuration
     * and will destroy all the stateful managed parts of the configuration.
     */
    public void destroy() {
        
        if (log.isDebugEnabled()) {
            log.debug("Destroying the Synapse Configuration");
        }

        // clear the timer tasks of Synapse
        synapseTimer.cancel();
        synapseTimer = null;

        // stop and shutdown all the proxy services
        for (ProxyService p : getProxyServices()) {

            if (p.getTargetInLineInSequence() != null) {
                p.getTargetInLineInSequence().destroy();
            }

            if (p.getTargetInLineOutSequence() != null) {
                p.getTargetInLineOutSequence().destroy();
            }
        }

        // destroy the managed mediators
        for (ManagedLifecycle seq : getDefinedSequences().values()) {
            seq.destroy();
        }

        // destroy the managed endpoints
        for (Endpoint endpoint : getDefinedEndpoints().values()) {
            endpoint.destroy();
        }

        // destroy the startups
        for (ManagedLifecycle stp : startups.values()) {
            stp.destroy();
        }
        
        TaskScheduler taskScheduler = TaskSchedulerFactory.getTaskScheduler(
                SynapseConstants.SYNAPSE_STARTUP_TASK_SCHEDULER);
        if (taskScheduler != null && taskScheduler.isInitialized()) {
            taskScheduler.shutDown();
        }

        if (taskDescriptionRepository != null) {
            taskDescriptionRepository.clear();
        }
        
        // clear session information used for SA load balancing
        try {
            SALSessions.getInstance().reset();
            DataSourceHelper.getInstance().getDataSourceRepositoryManager().clear();
        } catch (Throwable ignored) {}
    }

    /**
     * This method will be called in the startup of Synapse or in an initiation
     * and will initialize all the managed parts of the Synapse Configuration
     *
     * @param se
     *          SynapseEnvironment specifying the env to be initialized
     */
    public void init(SynapseEnvironment se) {
        
        if (log.isDebugEnabled()) {
            log.debug("Initializing the Synapse Configuration using the SynapseEnvironment");
        }

        // initialize registry
        if (registry != null && registry instanceof ManagedLifecycle) {
            ((ManagedLifecycle) registry).init(se);
        }

        //initialize endpoints
        for (Endpoint endpoint : getDefinedEndpoints().values()) {
            endpoint.init(se);
        }

         // initialize managed mediators
        for (ManagedLifecycle seq : getDefinedSequences().values()) {
            if (seq != null) {
                seq.init(se);
            }
        }

        // initialize all the proxy services
        for (ProxyService proxy : getProxyServices()) {

            if (proxy.getTargetInLineEndpoint() !=null){
                proxy.getTargetInLineEndpoint().init(se);
            }

            if (proxy.getTargetInLineInSequence() != null) {
                proxy.getTargetInLineInSequence().init(se);
            }

            if (proxy.getTargetInLineOutSequence() != null) {
                proxy.getTargetInLineOutSequence().init(se);
            }

            if (proxy.getTargetInLineFaultSequence() != null) {
                proxy.getTargetInLineFaultSequence().init(se);
            }
        }

        // initialize the startups
        for (ManagedLifecycle stp : getStartups()) {
            if (stp != null) {
                stp.init(se);
            }
        }
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    public TaskDescriptionRepository getTaskDescriptionRepository() {
        return taskDescriptionRepository;
    }

    /**
     * Add an event source to the configuration. If an event source already exists by the
     * specified name a runtime exception is thrown.
     *
     * @param name
     *              name of the event source
     * @param eventSource
     *              the event source to be added
     */
    public void addEventSource(String name, SynapseEventSource eventSource) {
        if (!eventSources.containsKey(name)) {
            eventSources.put(name, eventSource);
        } else {
            handleException("Duplicate event source by the name : " + name);
        }
    }

    public SynapseEventSource getEventSource(String name) {
        return eventSources.get(name);
    }

    /**
     * Remove an event source from the configuration. If the specified event source does not
     * exist a runtime exception is thrown.
     *
     * @param name name of the event source to be removed
     */
    public void removeEventSource(String name) {
        if (eventSources.containsKey(name)) {
            eventSources.remove(name);
        } else {
            handleException("No event source exists by the name : " + name);
        }
    }

    public Collection<SynapseEventSource> getEventSources() {
        return eventSources.values();
    }

    public void setEventSources(Map<String, SynapseEventSource> eventSources) {
        this.eventSources = eventSources;
    }

    private void assertAlreadyExists(String key, String type) {

        if (key == null || "".equals(key)) {
            handleException("Given entry key is empty or null.");
        }

        if (localRegistry.containsKey(key.trim())) {
            handleException("Duplicate " + type + " definition for key : " + key);
        }
    }

    private void assertEnrtyNull(Entry entry, String key) {
        if (entry == null) {
            handleException("Cannot locate an either local or remote enrty for key : " + key);
        }
    }
}
