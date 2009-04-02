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
package org.apache.synapse;

import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.description.*;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.ListenerManager;
import org.apache.axis2.format.BinaryBuilder;
import org.apache.axis2.format.PlainTextBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.commons.util.datasource.DataSourceInformationRepositoryHelper;
import org.apache.synapse.config.Entry;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.config.SynapseConfigurationBuilder;
import org.apache.synapse.config.SynapsePropertiesLoader;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.core.axis2.MessageContextCreatorForAxis2;
import org.apache.synapse.core.axis2.ProxyService;
import org.apache.synapse.core.axis2.SynapseMessageReceiver;
import org.apache.synapse.eventing.SynapseEventSource;
import org.apache.synapse.task.*;

import java.util.*;

/**
 * Axis2 Based Synapse Controller
 *
 * @see org.apache.synapse.SynapseController
 */
public class Axis2SynapseController implements SynapseController {

    private static final Log log = LogFactory.getLog(Axis2SynapseController.class);

    /*The Axis2 listener Manager */
    private ListenerManager listenerManager;
    /*The Axis2 configuration context used by Synapse */
    private ConfigurationContext configurationContext;
    /*Reference to the Synapse configuration */
    private SynapseConfiguration synapseConfiguration;
    /*Reference to the Synapse configuration */
    private SynapseEnvironment synapseEnvironment;
    /*Indicate initialization state */
    private boolean initialized;
    /* ServerConfiguration Information */
    private ServerConfigurationInformation information;

    /**
     * Initiates the  Axis2 Based Server Environment
     *
     * @param configurationInformation ServerConfigurationInformation Instance
     * @param contextInformation       Server Context if the Axis2 Based Server
     *                                 Environment has been already set up.
     */
    public void init(ServerConfigurationInformation configurationInformation,
                     ServerContextInformation contextInformation) {

        log.info("Initializing Synapse at : " + new Date());

        this.information = configurationInformation;

        if (contextInformation == null || contextInformation.getServerContext() == null ||
                configurationInformation.isCreateNewInstance()) {

            if (log.isDebugEnabled()) {
                log.debug("Initializing Synapse in a new axis2 server environment instance ");
            }
            createNewInstance(configurationInformation);
        } else {
            Object context = contextInformation.getServerContext();
            if (context instanceof ConfigurationContext) {
                if (log.isDebugEnabled()) {
                    log.debug("Initializing Synapse in an already existing " +
                            "axis2 server environment instance");
                }
                configurationContext = (ConfigurationContext) context;
                configurationContext.setProperty(
                        AddressingConstants.ADDR_VALIDATE_ACTION, Boolean.FALSE);
            } else {
                handleFatal("Synapse startup initialization failed : Provided server context is" +
                        " invalid, expected an Axis2 ConfigurationContext instance");
            }
        }
        initDefault(contextInformation);
        initialized = true;
    }

    /**
     * Destroy the  Axis2 Based Server Environment
     */
    public void destroy() {

        try {
            cleanupDefault();
            // stop all services
            if (information.isCreateNewInstance()) {  // only if we have created the server

                if (configurationContext != null &&
                        configurationContext.getAxisConfiguration() != null) {

                    Map<String, AxisService> serviceMap =
                            configurationContext.getAxisConfiguration().getServices();
                    for (AxisService svc : serviceMap.values()) {
                        svc.setActive(false);
                    }

                    // stop all modules
                    Map<String, AxisModule> moduleMap =
                            configurationContext.getAxisConfiguration().getModules();
                    for (AxisModule mod : moduleMap.values()) {
                        if (mod.getModule() != null && !"synapse".equals(mod.getName())) {
                            mod.getModule().shutdown(configurationContext);
                        }
                    }
                }

                // stop all transports
                if (listenerManager != null) {
                    listenerManager.stop();
                    listenerManager.destroy();
                }

                // we need to call this method to clean the temp files we created.
                if (configurationContext != null) {
                    configurationContext.terminate();
                }
            }
            initialized = false;
        } catch (Exception e) {
            log.error("Error stopping the Axis2 Based Server Environment", e);
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Setup synapse in axis2 environment and then , creates and returns
     * a SynapseEnvironment instance
     *
     * @return SynapseEnvironment instance
     */
    public SynapseEnvironment createSynapseEnvironment() {

        try {
            setupSynapse();
        } catch (AxisFault axisFault) {
            log.fatal("Synapse startup failed...", axisFault);
            throw new SynapseException("Synapse startup failed", axisFault);
        }

        Parameter synapseEnvironmentParameter = new Parameter(SynapseConstants.SYNAPSE_ENV, null);
        synapseEnvironment = new Axis2SynapseEnvironment(
                configurationContext, synapseConfiguration);
        synapseEnvironmentParameter.setValue(synapseEnvironment);

        MessageContextCreatorForAxis2.setSynEnv(synapseEnvironment);

        try {
            configurationContext.getAxisConfiguration().addParameter(synapseEnvironmentParameter);
        } catch (AxisFault e) {
            handleFatal("Could not set parameter '" + SynapseConstants.SYNAPSE_ENV +
                    "' to the Axis2 configuration : " + e.getMessage(), e);

        }
        synapseEnvironment.setInitialized(true);
        synapseConfiguration.init(synapseEnvironment);
        return synapseEnvironment;
    }

    public void destroySynapseEnvironment() {
        if (synapseEnvironment != null) {
            synapseEnvironment.setInitialized(false);
        }
    }

    public SynapseConfiguration createSynapseConfiguration() {

        String synapseXMLLocation = information.getSynapseXMLLocation();

        if (synapseXMLLocation != null) {
            synapseConfiguration = SynapseConfigurationBuilder.getConfiguration(synapseXMLLocation);
        } else {
            log.warn("System property or init-parameter '" + SynapseConstants.SYNAPSE_XML +
                    "' is not specified. Using default configuration..");
            synapseConfiguration = SynapseConfigurationBuilder.getDefaultConfiguration();
        }

        synapseConfiguration.setProperties(SynapsePropertiesLoader.loadSynapseProperties());

        // Set the Axis2 ConfigurationContext to the SynapseConfiguration
        synapseConfiguration.setAxisConfiguration(configurationContext.getAxisConfiguration());

        // set the Synapse configuration and environment into the Axis2 configuration
        Parameter synapseConfigurationParameter = new Parameter(
                SynapseConstants.SYNAPSE_CONFIG, null);
        synapseConfigurationParameter.setValue(synapseConfiguration);
        MessageContextCreatorForAxis2.setSynConfig(synapseConfiguration);

        try {
            configurationContext.getAxisConfiguration().addParameter(synapseConfigurationParameter);

        } catch (AxisFault e) {
            handleFatal("Could not set parameters '" + SynapseConstants.SYNAPSE_CONFIG +
                    "' to the Axis2 configuration : " + e.getMessage(), e);
        }
        return synapseConfiguration;
    }

    public void destroySynapseConfiguration() {
        if (synapseConfiguration != null) {
            synapseConfiguration.destroy();
        }
    }

    public Object getContext() {
        return configurationContext;
    }

    /**
     * Create a Axis2 Based Server Environment
     *
     * @param information ServerConfigurationInformation instance
     */
    private void createNewInstance(ServerConfigurationInformation information) {

        try {
            configurationContext = ConfigurationContextFactory.
                    createConfigurationContextFromFileSystem(information.getAxis2RepoLocation(),
                            information.getAxis2Xml());

            configurationContext.setProperty(
                    AddressingConstants.ADDR_VALIDATE_ACTION, Boolean.FALSE);

            listenerManager = configurationContext.getListenerManager();
            if (listenerManager == null) {
                listenerManager = new ListenerManager();
                listenerManager.init(configurationContext);
            }

            for (String trp : configurationContext.getAxisConfiguration()
                    .getTransportsIn().keySet()) {

                TransportInDescription trsIn =
                        configurationContext.getAxisConfiguration().getTransportsIn().get(trp);
                log.info("Starting transport " + trsIn.getName());
                listenerManager.addListener(trsIn, false);
            }
            
        } catch (Throwable t) {
            handleFatal("Synapse startup failed...", t);
        }
    }

    /**
     * Setup required setting for enable main message mediation
     *
     * @throws AxisFault For any in setup
     */
    private void setupMessageMediation() throws AxisFault {

        log.info("Deploying the Synapse service...");
        // Dynamically initialize the Synapse Service and deploy it into Axis2
        AxisConfiguration axisCfg = configurationContext.getAxisConfiguration();
        AxisService synapseService = new AxisService(SynapseConstants.SYNAPSE_SERVICE_NAME);
        AxisOperation mediateOperation = new InOutAxisOperation(
                SynapseConstants.SYNAPSE_OPERATION_NAME);
        mediateOperation.setMessageReceiver(new SynapseMessageReceiver());
        synapseService.addOperation(mediateOperation);
        List<String> transports = new ArrayList<String>();
        transports.add(Constants.TRANSPORT_HTTP);
        transports.add(Constants.TRANSPORT_HTTPS);
        // todo: is this correct? this limits the message mediation to http/s
        synapseService.setExposedTransports(transports);
        axisCfg.addService(synapseService);
    }

    /**
     * Setup required setting for enable proxy message mediation
     */
    private void setupProxyServiceMediation() {

        log.info("Deploying Proxy services...");
        String thisServerName = information.getServerName();
        if (thisServerName == null || "".equals(thisServerName)) {
            thisServerName = information.getHostName();
            if (thisServerName == null || "".equals(thisServerName)) {
                thisServerName = "localhost";
            }
        }

        for (ProxyService proxy : synapseConfiguration.getProxyServices()) {

            // start proxy service if either,
            // pinned server name list is empty
            // or pinned server list has this server name
            List pinnedServers = proxy.getPinnedServers();
            if (pinnedServers != null && !pinnedServers.isEmpty()) {
                if (!pinnedServers.contains(thisServerName)) {
                    log.info("Server name not in pinned servers list." +
                            " Not deploying Proxy service : " + proxy.getName());
                    continue;
                }
            }

            proxy.buildAxisService(synapseConfiguration,
                    configurationContext.getAxisConfiguration());
            log.info("Deployed Proxy service : " + proxy.getName());
            if (!proxy.isStartOnLoad()) {
                proxy.stop(synapseConfiguration);
            }
        }
    }

    private void setupSynapse() throws AxisFault {
        addServerIPAndHostEnrties();
        setupMessageMediation();
        setupProxyServiceMediation();
        setupEventSources();
    }

    private void initDefault(ServerContextInformation contextInformation) {
        addDefaultBuildersAndFormatters(configurationContext.getAxisConfiguration());
        loadMediatorExtensions();
        setupDataSources();
        setupTaskHelper(contextInformation);
    }

    private void loadMediatorExtensions() {
        // this will deploy the mediators in the mediator extensions folder
        log.info("Loading mediator extensions...");
        configurationContext.getAxisConfiguration().getConfigurator().loadServices();
    }

    private void setupEventSources() throws AxisFault {
        for (SynapseEventSource eventSource : synapseConfiguration.getEventSources()) {
            eventSource.buildService(configurationContext.getAxisConfiguration());
        }
    }

    private void setupDataSources() {
        Properties synapseProperties = SynapsePropertiesLoader.loadSynapseProperties();
        DataSourceInformationRepositoryHelper.
                initializeDataSourceInformationRepository(
                        configurationContext.getAxisConfiguration(), synapseProperties);
    }

    /**
     *  Intialize TaskHelper - with any existing  TaskDescriptionRepository and TaskScheduler
     *  or without those
     * @param contextInformation  ServerContextInformation instance
     */
    private void setupTaskHelper(ServerContextInformation contextInformation) {

        TaskHelper taskHelper = TaskHelper.getInstance();
        if (taskHelper.isInitialized()) {
            if (log.isDebugEnabled()) {
                log.debug("TaskHelper has been already initialized.");
            }
            return;
        }

        Object repo = contextInformation.getProperty(TaskConstants.TASK_DESCRIPTION_REPOSITORY);
        Object taskScheduler = contextInformation.getProperty(TaskConstants.TASK_SCHEDULER);

        if (repo instanceof TaskDescriptionRepository && taskScheduler instanceof TaskScheduler) {
            taskHelper.init((TaskDescriptionRepository) repo, (TaskScheduler) taskScheduler);
        } else {

            if (repo == null && taskScheduler == null) {
                taskHelper.init(
                        TaskDescriptionRepositoryFactory.getTaskDescriptionRepository(
                                TaskConstants.TASK_DESCRIPTION_REPOSITORY),
                        TaskSchedulerFactory.getTaskScheduler(TaskConstants.TASK_SCHEDULER));
            } else {
                handleFatal("Invalid property values for " +
                        "TaskDescriptionRepository or / and TaskScheduler ");
            }
        }
    }

    private void cleanupDefault() {
        TaskHelper taskHelper = TaskHelper.getInstance();
        if (taskHelper.isInitialized()) {
            taskHelper.cleanup();
        }
    }

    private void addDefaultBuildersAndFormatters(AxisConfiguration axisConf) {
        if (axisConf.getMessageBuilder("text/plain") == null) {
            axisConf.addMessageBuilder("text/plain", new PlainTextBuilder());
        }
        if (axisConf.getMessageBuilder("application/octet-stream") == null) {
            axisConf.addMessageBuilder("application/octet-stream", new BinaryBuilder());
        }
    }

    private void addServerIPAndHostEnrties() {
        String hostName = information.getHostName();
        String ipAddress = information.getIpAddress();
        if (hostName != null && !"".equals(hostName)) {
            Entry entry = new Entry(SynapseConstants.SERVER_HOST);
            entry.setValue(hostName);
            synapseConfiguration.addEntry(SynapseConstants.SERVER_HOST, entry);
        }

        if (ipAddress != null && !"".equals(ipAddress)) {
            Entry entry = new Entry(SynapseConstants.SERVER_IP);
            entry.setValue(ipAddress);
            synapseConfiguration.addEntry(SynapseConstants.SERVER_IP, entry);
        }
    }

    private void handleFatal(String msg, Throwable e) {
        log.fatal(msg, e);
        throw new SynapseException(msg, e);
    }

    private void handleFatal(String msg) {
        log.fatal(msg);
        throw new SynapseException(msg);
    }
}
