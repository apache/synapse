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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.commons.util.MBeanRegistrar;

/**
 * This is the core class that starts up a Synapse instance.
 * <p/>
 * From the command line scripts synapse.sh and synapse-daemon.sh (though the wrapper.conf)
 * the SynapseServer is invoked which inturn calls on this to start the instance
 * <p/>
 * When the WAR deployment is used, the SynapseStartUpServlet servlet calls on this class to
 * initialize Synapse
 */

public class ServerManager {

    private static final Log log = LogFactory.getLog(ServerManager.class);

    private final static ServerManager instance = new ServerManager();

    /* The controller for synapse create and Destroy synapse artifacts in a particular environment
       Only for internal usage - DON"T PUT GETTER ,SETTER */
    private SynapseController synapseController;
    /* Server Configuration  */
    private ServerConfigurationInformation configurationInformation;
    /* Server context */
    private ServerContextInformation contextInformation;
    /*Only represents whether server manager has been initialized by given required
     configuration information - not server state or internal usage - DON"T PUT SETTER */
    private boolean initialized = false;

    public static ServerManager getInstance() {
        return instance;
    }

    /**
     * Initializes the server
     *
     * @param configurationInformation ServerConfigurationInformation instance
     * @param contextInformation       ServerContextInformation instance
     * @return ServerState - The state of the server after call this operation
     */
    public ServerState init(ServerConfigurationInformation configurationInformation,
                            ServerContextInformation contextInformation) {

        this.configurationInformation = configurationInformation;
        if (contextInformation == null) {
            this.contextInformation = new ServerContextInformation();
        } else {
            this.contextInformation = contextInformation;
        }
        this.synapseController = SynapseControllerFactory
                .createSynapseController(configurationInformation);

        this.initialized = true;
        registerMBean();
        doInit();
        return this.contextInformation.getServerState();
    }

    /**
     * Starting up the server
     *
     * @return ServerState - The state of the server after call this operation
     */
    public ServerState start() {
        assertInitialized();
        doInit();
        doStart();
        return this.contextInformation.getServerState();
    }

    /**
     * Stopping the server
     *
     * @return ServerState - The state of the server after call this operation
     */
    public ServerState stop() {
        assertInitialized();
        doStop();
        return this.contextInformation.getServerState();
    }

    /**
     * Returns the ServerConfigurationInformation
     *
     * @return ServerConfigurationInformation insatnce
     */
    public ServerConfigurationInformation getInformation() {
        assertInitialized();
        return configurationInformation;
    }

    /**
     * Retunrs the ServerContextInformation
     *
     * @return ServerContextInformation instance
     */
    public ServerContextInformation getContextInformation() {
        assertInitialized();
        return contextInformation;
    }

    /**
     * Helper method that to do init
     */
    private void doInit() {

        ServerState serverState = ServerStateDetectionStrategy.currentState(contextInformation,
                configurationInformation);

        if (serverState == ServerState.INITIALIZABLE) {

            this.synapseController.init(configurationInformation, contextInformation);

            if (this.contextInformation == null) {
                this.contextInformation = new ServerContextInformation(
                        this.synapseController.getContext());
            } else if (this.contextInformation.getServerContext() == null) {
                this.contextInformation.setServerContext(this.synapseController.getContext());
            }
            chanageState(ServerState.INITIALIZED);
        } else {
            chanageState(serverState);
        }
    }

    /**
     * Helper method that to do start
     */
    private void doStart() {

        ServerState serverState = ServerStateDetectionStrategy.currentState(contextInformation,
                configurationInformation);

        if (serverState == ServerState.INITIALIZED) {

            this.synapseController.createSynapseConfiguration();
            this.synapseController.createSynapseEnvironment();
            chanageState(ServerState.STARTED);
            log.info("Ready for processing");
        } else {
            chanageState(serverState);
        }
    }

    /**
     * Helper method that to do stop
     */
    private void doStop() {

        ServerState serverState = ServerStateDetectionStrategy.currentState(contextInformation,
                configurationInformation);

        switch (serverState) {
            case INITIALIZED: {
                this.synapseController.destroy();
                break;
            }
            case STARTED: {
                this.synapseController.destroySynapseConfiguration();
                this.synapseController.destroySynapseEnvironment();
                this.synapseController.destroy();
                break;
            }
        }
        chanageState(ServerState.STOPPED);
        this.initialized = false;
    }

    private void chanageState(ServerState serverState) {
        this.contextInformation.setServerState(serverState);
    }

    private void assertInitialized() {
        if (!initialized) {
            String msg = "Server manager has not been initialized by giving " +
                    "required configurations information." +
                    "It is needed to initiate by giving required configurations information ," +
                    " before access any operations";
            log.error(msg);
            throw new SynapseException(msg);
        }
    }

    private void registerMBean() {
        MBeanRegistrar.getInstance().registerMBean(new ServerManagerView(),
                "ServerManager", "ServerManager");
    }

    /**
     * Has server manager  been initialized ?
     *
     * @return true if the server manager has been initialized by given required
     *         configuration information
     */
    public boolean isInitialized() {
        return initialized;
    }
}

