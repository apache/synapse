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
import org.apache.synapse.commons.util.jmx.MBeanRegistrar;

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

    /* Keeps the ServerManager instance */
    private final static ServerManager instance = new ServerManager();

    /**
     * The controller for synapse create and Destroy synapse artifacts in a particular environment
     * Only for internal usage - DON"T PUT GETTER ,SETTER
     */
    private SynapseController synapseController;

    /* Server Configuration  */
    private ServerConfigurationInformation configurationInformation;

    /* Server context */
    private ServerContextInformation contextInformation;

    /**
     * Only represents whether server manager has been initialized by given required
     * configuration information - not server state or internal usage - DON"T PUT SETTER
     */
    private boolean initialized = false;

    /**
     * Gives the access to the singleton instance of the ServerManager
     *
     * @return the ServerManager instance
     */
    public static ServerManager getInstance() {
        return instance;
    }

    /**
     * Initializes the server, if we need to create a new axis2 instance, calling this will create
     * the new axis2 environment, but this won't start the transport lsiteners
     *
     * @param configurationInformation ServerConfigurationInformation instance
     * @param contextInformation       ServerContextInformation instance
     * @return ServerState - State of the server which is
     *          {@link org.apache.synapse.ServerState#INITIALIZED}, if successful
     */
    public synchronized ServerState init(ServerConfigurationInformation configurationInformation,
                            ServerContextInformation contextInformation) {

        // sets the initializations parameters
        this.configurationInformation = configurationInformation;
        if (contextInformation == null) {
            this.contextInformation = new ServerContextInformation();
        } else {
            this.contextInformation = contextInformation;
        }
        this.synapseController = SynapseControllerFactory
                .createSynapseController(configurationInformation);

        // does the initialization of the controller
        doInit();
        this.initialized = true;
        return this.contextInformation.getServerState();
    }

    /**
     * Destroyes the Server instance. If the Server is stopped this will destroy the
     * ServerManager, and if it is running (i.e. in the STARTED state) this will first stop the
     * ServerManager and destroy it in turn
     * 
     * @return the state after the destruction, {@link org.apache.synapse.ServerState#UNDETERMINED}
     */
    public synchronized ServerState destroy() {

        ServerState serverState = ServerStateDetectionStrategy.currentState(contextInformation,
                configurationInformation);

        switch (serverState) {
            // if the current state is INITIALIZED, then just destroy
            case INITIALIZED: {
                doDestroy();
                break;
            }
            // if the current state is STOPPED, then again just destroy
            case STOPPED: {
                doDestroy();
                break;
            }
            // if the current state is STARTED, then stop and destroy
            case STARTED: {
                stop();
                doDestroy();
                break;
            }
        }

        // clear the instance parameters
        this.synapseController = null;
        this.contextInformation = null;
        this.configurationInformation = null;
        
        this.initialized = false;
        return ServerState.UNDETERMINED;
    }

    /**
     * Starts the system, if the system is initialized, and if not a Runtime exception of type
     * {@link org.apache.synapse.SynapseException} will be thrown
     *
     * @return the state of the server after starting, for a successful start
     *          {@link org.apache.synapse.ServerState#STARTED}
     */
    public synchronized ServerState start() {

        // if the system is not initialized we are not happy
        assertInitialized();
        
        // starts the system
        doStart();
        return this.contextInformation.getServerState();
    }

    /**
     * Stops the system, if it is started and if not a Runtime exception of type
     * {@link org.apache.synapse.SynapseException} will be thrown
     *
     * @return the state of the system after stopping, which is
     *          {@link org.apache.synapse.ServerState#STOPPED} for a successful stopping
     */
    public synchronized ServerState stop() {

        ServerState serverState = ServerStateDetectionStrategy.currentState(contextInformation,
                configurationInformation);

        // if the system is started then stop if not we are not happy
        if (serverState == ServerState.STARTED) {
            doStop();
        } else {
            String message = "Couldn't stop the ServerManager, it has not been started yet";
            handleException(message);
        }
        
        return this.contextInformation.getServerState();
    }

    /**
     * Returns the ServerConfigurationInformation, if the system is initialized and if not a
     * Runtime exception of type {@link org.apache.synapse.SynapseException} will be thrown
     *
     * @return the configuration information of the initialized system
     */
    public ServerConfigurationInformation getConfigurationInformation() {
        assertInitialized();
        return configurationInformation;
    }

    /**
     * Retunrs the ServerContextInformation, if the system is initialized and if not a Runtime
     * Exception of type {@link org.apache.synapse.SynapseException} will be thrown
     *
     * @return the context information of the initialized system
     */
    public ServerContextInformation getContextInformation() {
        assertInitialized();
        return contextInformation;
    }

    /**
     * Has server manager been initialized ?
     *
     * @return true if the server manager has been initialized by given required
     *         configuration information
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Helper method for initializing the ServerManager
     */
    private void doInit() {

        ServerState serverState = ServerStateDetectionStrategy.currentState(contextInformation,
                configurationInformation);

        // if the server is ready for the initialization, this will make sure that we are not
        // calling the initialization on an already initialized/started system
        if (serverState == ServerState.INITIALIZABLE) {

            // register the ServerManager MBean
            registerMBean();

            // initializes the SynapseController
            this.synapseController.init(configurationInformation, contextInformation);

            // sets the server context and the controller context
            if (this.contextInformation == null) {
                this.contextInformation = new ServerContextInformation(
                        this.synapseController.getContext());
            } else if (this.contextInformation.getServerContext() == null) {
                this.contextInformation.setServerContext(this.synapseController.getContext());
            }

            // mark as initialized
            chanageState(ServerState.INITIALIZED);
        } else {
            // if the server cannot be initialized just set the current state as the server state
            chanageState(serverState);
        }
    }

    /**
     * Helper method for destroying the ServerManager
     */
    private void doDestroy() {
        ServerState serverState = ServerStateDetectionStrategy.currentState(contextInformation,
                configurationInformation);

        if (serverState == ServerState.INITIALIZED || serverState == ServerState.STOPPED) {

            // unregister the ServerManager MBean
            unRegisterMBean();

            // destroy the SynapseController
            synapseController.destroy();

            // mark as destroyed
            chanageState(ServerState.UNDETERMINED);
        } else {
            // if the server cannot be destroyed just set the current state as the server state
            chanageState(serverState);
        }
    }

    /**
     * Helper method to start the ServerManager
     */
    private void doStart() {

        ServerState serverState = ServerStateDetectionStrategy.currentState(contextInformation,
                configurationInformation);

        if (serverState == ServerState.INITIALIZED || serverState == ServerState.STOPPED) {

            // creates the Synapse Configuration using the SynapseController
            contextInformation.setSynapseConfiguration(
                    synapseController.createSynapseConfiguration());
            // creates the Synapse Environment using the SynapseController
            contextInformation.setSynapseEnvironment(
                    synapseController.createSynapseEnvironment());
            // starts the SynapseController
            synapseController.start();

            chanageState(ServerState.STARTED);
            log.info("Server ready for processing...");
        } else {
            // if the server cannot be started just set the current state as the server state
            chanageState(serverState);
        }
    }

    /**
     * Helper method that to do stop
     */
    private void doStop() {

        ServerState serverState = ServerStateDetectionStrategy.currentState(contextInformation,
                configurationInformation);

        if (serverState == ServerState.STARTED) {

            // stop the SynapseController
            synapseController.stop();
            // destroy the created Synapse Configuration
            synapseController.destroySynapseConfiguration();
            contextInformation.setSynapseConfiguration(null);
            // destroy the created Synapse Environment
            synapseController.destroySynapseEnvironment();
            contextInformation.setSynapseEnvironment(null);

            chanageState(ServerState.STOPPED);
        } else {
            // if the server cannot be stopped just set the current state as the server state
            chanageState(serverState);
        }
    }

    private void chanageState(ServerState serverState) {
        this.contextInformation.setServerState(serverState);
    }

    private void assertInitialized() {
        if (!initialized) {
            String msg = "Server manager has not been initialized, it requires to be " +
                    "initialized, with the required configurations before starting";
            handleException(msg);
        }
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    private void registerMBean() {
        MBeanRegistrar.getInstance().registerMBean(new ServerManagerView(),
                SynapseConstants.SERVER_MANAGER_MBEAN, SynapseConstants.SERVER_MANAGER_MBEAN);
    }

    private void unRegisterMBean() {
        MBeanRegistrar.getInstance().unRegisterMBean(
                SynapseConstants.SERVER_MANAGER_MBEAN, SynapseConstants.SERVER_MANAGER_MBEAN);
    }
}
