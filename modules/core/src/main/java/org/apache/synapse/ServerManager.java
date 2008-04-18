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
import org.apache.axis2.engine.ListenerManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.transport.nhttp.NhttpConstants;
import org.apache.synapse.util.ClasspathURLStreamHandler;
import org.apache.synapse.util.RMIRegistryController;

import java.io.File;
import java.net.*;

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

    private static ServerManager instance;
    private static final Log log = LogFactory.getLog(ServerManager.class);
    private String axis2Repolocation;
    private ListenerManager listenerManager;
    private ConfigurationContext configctx;
    private static final int DEFAULT_HTTP_PORT = 8080;
    private static final int ALTERNATIVE_HTTP_PORT = 8008;

    /**
     * To ensure that there is a only one Manager
     * @return  Server Manager Instance
     */
    public static ServerManager getInstance() {
        if (instance == null) {
            instance = new ServerManager();
        }
        return instance;
    }

    public void setAxis2Repolocation(String axis2Repolocation) {
        this.axis2Repolocation = axis2Repolocation;
    }

    /**
     * starting all the listeners
     */
    public void start() {

        // Register custom protocol handler classpath://
		try {
			URL.setURLStreamHandlerFactory(new URLStreamHandlerFactoryImpl());
		} catch (Throwable t) {
			log.warn("Unable to register a URLStreamHandlerFactory - " +
					"Custom URL protocols may not work properly (e.g. classpath://)");
		}

        if (axis2Repolocation == null) {
            log.fatal("The Axis2 Repository must be provided");
            return;
        }
        log.info("Using the Axis2 Repository "
                           + new File(axis2Repolocation).getAbsolutePath());
        try {
            configctx = ConfigurationContextFactory.
                    createConfigurationContextFromFileSystem(axis2Repolocation, null);
            
            listenerManager = configctx.getListenerManager();
            if (listenerManager == null) {
                listenerManager = new ListenerManager();
                listenerManager.init(configctx);
            }
            // decide on HTTP port to execute
            selectPort(configctx);

            for (Object o : configctx.getAxisConfiguration().getTransportsIn().keySet()) {
                
                String trp = (String) o;
                TransportInDescription trsIn = (TransportInDescription)
                    configctx.getAxisConfiguration().getTransportsIn().get(trp);
                listenerManager.addListener(trsIn, false);
                
                String msg = "Starting transport " + trsIn.getName();
                if (trsIn.getParameter("port") != null) {
                    msg += " on port " + trsIn.getParameter("port").getValue();
                }
                
                log.info(msg);
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
            log.fatal("Startup failed...", t);
        }
    }

    /**
     * stop all the listeners
     */
    public void stop() {
        try {
            RMIRegistryController.getInstance().removeLocalRegistry();
            
            if (listenerManager != null) {
                listenerManager.stop();
                listenerManager.destroy();
            }
            //we need to call this method to clean the temp files we created.
            if (configctx != null) {
                configctx.terminate();
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Helper method to select a alternate port if the currently selected port is in use
     * @param configCtx configuration context 
     */
    private static void selectPort(ConfigurationContext configCtx) {
        // check if configured port is available
        TransportInDescription trsIn = (TransportInDescription)
                configCtx.getAxisConfiguration().getTransportsIn().get("http");

        if (trsIn != null) {

            int port = DEFAULT_HTTP_PORT;
            String bindAddress = null;

            String strPort = System.getProperty("port");
            if (strPort != null) {
                // port is specified as a VM parameter
                try {
                    port = Integer.parseInt(strPort);
                } catch (NumberFormatException e) {
                    // user supplied parameter is not a valid integer. so use the port in configuration.
                    log.error("System property 'port' does not provide a valid integer");
                }
            }

            Parameter param = trsIn.getParameter("port");
            if (param != null && param.getValue() != null) {
                port = Integer.parseInt(param.getValue().toString());
            }

            param = trsIn.getParameter(NhttpConstants.BIND_ADDRESS);
            if (param != null && param.getValue() != null) {
                bindAddress = ((String) param.getValue()).trim();
            }

            while (true) {
                ServerSocket sock = null;
                try {
                    if (bindAddress == null) {
                        sock = new ServerSocket(port);
                    } else {
                        sock = new ServerSocket(port, 50, InetAddress.getByName(bindAddress));
                    }
                    trsIn.getParameter("port").setValue(Integer.toString(port));
                    break;
                } catch (Exception e) {
                    log.warn("Port " + port + " already in use. Trying alternate");
                    if (port == DEFAULT_HTTP_PORT) {
                        port = ALTERNATIVE_HTTP_PORT;
                    } else {
                        port++;
                    }
                } finally {
                    if (sock != null) {
                        try {
                            sock.close();
                        } catch (Exception e) {
                            // do nothing
                        }
                    }
                }
            }
        }
    }

    public ConfigurationContext getConfigurationContext() {
        return configctx;
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
}