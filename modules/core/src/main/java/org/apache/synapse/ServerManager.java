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

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.AxisModule;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.ListenerManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.format.BinaryBuilder;
import org.apache.synapse.format.PlainTextBuilder;
import org.apache.synapse.transport.nhttp.NhttpConstants;
import org.apache.synapse.util.ClasspathURLStreamHandler;
import org.apache.synapse.util.RMIRegistryController;

import java.io.File;
import java.net.*;
import java.util.Map;

/**
 * This is the core class that starts up a Synapse instance.
 *
 * From the command line scripts synapse.sh and synapse-daemon.sh (though the wrapper.conf)
 * the SynapseServer is invoked which inturn calls on this to start the instance
 *
 * When the WAR deployment is used, the SynapseStartUpServlet servlet calls on this class to
 * initialize Synapse
 */

public class ServerManager {

    private static final Log log = LogFactory.getLog(ServerManager.class);

    /** The singleton server manager instance */
    private static ServerManager instance = new ServerManager();

    /** The Axis2 repository location */
    private String axis2Repolocation;
    /** The path to the axis2.xml file */
    private String axis2Xml;
    /** The synapse home is the home directory of the Synapse installation */
    private String synapseHome;
    /** The path to the synapse.xml file */
    private String synapseXMLPath;
    /** The root directory to resolve paths for registry, default to synapse.home/repository */
    private String resolveRoot;
    /** An optional server name to activate pinned services, tasks etc.. and to differentiate instances on a cluster */
    private String serverName = "localhost";

    /** The Axis2 listener Manager */
    private ListenerManager listenerManager;
    /** The Axis2 configuration context used by Synapse */
    private ConfigurationContext configctx;
    /** Reference to the Synapse configuration */
    private SynapseConfiguration synConfig = null;
    private Map callbackStore = null;

    /**
     * return the singleton server manager
     * @return  ServerManager Instance
     */
    public static ServerManager getInstance() {
        return instance;
    }

    /**
     * starting all the listeners
     */
    public void start() {

        // validate if we can start
        validate();

        // Register custom protocol handler classpath://
		try {
			URL.setURLStreamHandlerFactory(new URLStreamHandlerFactoryImpl());
		} catch (Throwable t) {
			log.debug("Unable to register a URLStreamHandlerFactory - " +
					"Custom URL protocols may not work properly (e.g. classpath://)");
		}

        try {
            configctx = ConfigurationContextFactory.
                    createConfigurationContextFromFileSystem(axis2Repolocation, axis2Xml);
            
            addDefaultBuildersAndFormatters(configctx.getAxisConfiguration());
            
            listenerManager = configctx.getListenerManager();
            if (listenerManager == null) {
                listenerManager = new ListenerManager();
                listenerManager.init(configctx);
            }

            for (Object o : configctx.getAxisConfiguration().getTransportsIn().keySet()) {
                
                String trp = (String) o;
                TransportInDescription trsIn = (TransportInDescription)
                    configctx.getAxisConfiguration().getTransportsIn().get(trp);

                String msg = "Starting transport " + trsIn.getName();
                if (trsIn.getParameter("port") != null) {
                    msg += " on port " + trsIn.getParameter("port").getValue();
                }
                log.info(msg);

                listenerManager.addListener(trsIn, false);
            }

            // now initialize SynapseConfig
            Parameter synEnv
                = configctx.getAxisConfiguration().getParameter(SynapseConstants.SYNAPSE_ENV);
            Parameter synCfg
                = configctx.getAxisConfiguration().getParameter(SynapseConstants.SYNAPSE_CONFIG);

            String message = "Unable to initialize the Synapse Configuration : Cannot find the ";
            if (synCfg == null || synCfg.getValue() == null
                || !(synCfg.getValue() instanceof SynapseConfiguration)) {
                log.fatal(message + "Synapse Configuration");
                throw new SynapseException(message + "Synapse Configuration");
            } else {
                synConfig = (SynapseConfiguration) synCfg.getValue();
            }

            if (synEnv == null || synEnv.getValue() == null
                || !(synEnv.getValue() instanceof SynapseEnvironment)) {
                log.fatal(message + "Synapse Environment");
                throw new SynapseException(message + "Synapse Environment");
            } else {

                ((SynapseEnvironment) synEnv.getValue()).setInitialized(true);

                // initialize the startups
                for (Startup stp : ((SynapseConfiguration) synCfg.getValue()).getStartups()) {
                    if (stp != null) {
                        stp.init((SynapseEnvironment) synEnv.getValue());
                    }
                }
            }

            log.info("Ready for processing");

        } catch (Throwable t) {
            log.fatal("Synaps startup failed...", t);
            throw new SynapseException("Synapse startup failed", t);
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

    /**
     * stop all the listeners
     */
    public void stop() {
        try {
            RMIRegistryController.getInstance().removeLocalRegistry();

            // stop all services
            if (configctx != null && configctx.getAxisConfiguration() != null) {
                Map<String, AxisService> serviceMap = configctx.getAxisConfiguration().getServices();
                for (AxisService svc : serviceMap.values()) {
                    svc.setActive(false);
                }

                // stop all modules
                Map<String, AxisModule> moduleMap = configctx.getAxisConfiguration().getModules();
                for (AxisModule mod : moduleMap.values()) {
                    if (mod.getModule() != null && !"synapse".equals(mod.getName())) {
                        mod.getModule().shutdown(configctx);
                    }
                }
            }

            // stop all transports
            if (listenerManager != null) {
                listenerManager.stop();
                listenerManager.destroy();
            }
            
            // we need to call this method to clean the temp files we created.
            if (configctx != null) {
                configctx.terminate();
            }
        } catch (Exception e) {
            log.error("Error stopping the ServerManager", e);
        }
    }

    /**
     * Expose the number of callbacks in the callback store
     * @return the number of callbacks (messages) waiting for responses
     */
    public int pendingCallbacks() {
        if (callbackStore != null) {
            return callbackStore.size();
        } else {
            return 0;
        }
    }

    private static final class URLStreamHandlerFactoryImpl implements URLStreamHandlerFactory {

        public URLStreamHandler createURLStreamHandler(String protocol) {

            if (protocol == null) {
                throw new IllegalArgumentException("'protocol' cannot be null");
            }
            URLStreamHandler urlSH = null;
            if (protocol.equals("classpath")) {
                urlSH = new ClasspathURLStreamHandler();
            }
            return urlSH;
        }
    }

    /**
     * Validate core settings for startup
     */
    private void validate() {
        if (synapseHome == null || !new File(synapseHome).exists()) {
            handleFatal("Synapse home");
        } else {
            log.info("Using Synapse home as : " + synapseHome);
        }

        if (axis2Repolocation == null || !new File(axis2Repolocation).exists()) {
            handleFatal("Axis2 repository");
        } else {
            log.info("Using the Axis2 Repository : " + new File(axis2Repolocation).getAbsolutePath());
        }

        if (axis2Xml == null || !new File(axis2Xml).exists()) {
            handleFatal("axis2.xml location");
        } else {
            log.info("Using the axis2.xml : " + new File(axis2Xml).getAbsolutePath());
        }

        if (synapseXMLPath == null || !new File(synapseXMLPath).exists()) {
            handleFatal("synapse.xml path");
        }

        if (serverName == null) {
            try {
                serverName = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException ignore) {}
            log.info("The server name was not specified, defaulting to : " + serverName);
        } else {
            log.info("Using server name : " + serverName);
        }

        log.info("The timeout handler will run every : " + (getTimeoutHandlerInterval()/1000) + "s");
    }

    public void handleFatal(String msgPre) {
        String msg = "The " + msgPre + " must be set as a system property or init-parameter";
        log.fatal(msg);
        throw new SynapseException(msg);
    }

    // getters and setters
    public ConfigurationContext getConfigurationContext() {
        return configctx;
    }

    public void setCallbackStore(Map callbackStore) {
        this.callbackStore = callbackStore;
    }

    public void setAxis2Repolocation(String axis2Repolocation) {
        if (!new File(axis2Repolocation).isAbsolute() && synapseHome != null) {
            this.axis2Repolocation = synapseHome + File.separator + axis2Repolocation;
        } else {
            this.axis2Repolocation = axis2Repolocation;
        }
    }

    public void setAxis2Xml(String axis2Xml) {
        if (!new File(axis2Xml).isAbsolute() && synapseHome != null) {
            this.axis2Xml = synapseHome + File.separator + axis2Xml;
        } else {
            this.axis2Xml = axis2Xml;
        }
    }

    public String getSynapseHome() {
        return synapseHome;
    }

    public void setSynapseHome(String synapseHome) {
        this.synapseHome = synapseHome;
    }

    public String getResolveRoot() {
        return resolveRoot;
    }

    public void setResolveRoot(String resolveRoot) {
        if (!new File(resolveRoot).isAbsolute() && synapseHome != null) {
            this.resolveRoot = synapseHome + File.separator + resolveRoot;
        } else {
            this.resolveRoot = resolveRoot;
        }
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getSynapseXMLPath() {
        return synapseXMLPath;
    }

    public void setSynapseXMLPath(String synapseXMLPath) {
        if (!new File(synapseXMLPath).isAbsolute() && synapseHome != null) {
            this.synapseXMLPath = synapseHome + File.separator + synapseXMLPath;
        } else {
            this.synapseXMLPath = synapseXMLPath;
        }
    }

    public int getConnectTimeout() {
        if (synConfig == null) {
            return (int) SynapseConstants.DEFAULT_GLOBAL_TIMEOUT;
        } else {
            return (int) synConfig.getProperty(
                SynapseConstants.CONNECTTIMEOUT, SynapseConstants.DEFAULT_CONNECTTIMEOUT);
        }
    }

    public int getReadTimeout() {
        if (synConfig == null) {
            return SynapseConstants.DEFAULT_READTIMEOUT;
        } else {
            return (int) synConfig.getProperty(
                SynapseConstants.READTIMEOUT, SynapseConstants.DEFAULT_READTIMEOUT);
        }
    }

    public long getTimeoutHandlerInterval() {
        if (synConfig == null) {
            return SynapseConstants.DEFAULT_TIMEOUT_HANDLER_INTERVAL;
        } else {
            return synConfig.getProperty(
                SynapseConstants.TIMEOUT_HANDLER_INTERVAL, SynapseConstants.DEFAULT_TIMEOUT_HANDLER_INTERVAL);
        }
    }

    public long getGlobalTimeoutInterval() {
        if (synConfig == null) {
            return SynapseConstants.DEFAULT_GLOBAL_TIMEOUT;
        } else {
            return synConfig.getProperty(
                SynapseConstants.GLOBAL_TIMEOUT_INTERVAL, SynapseConstants.DEFAULT_GLOBAL_TIMEOUT);
        }
    }
}