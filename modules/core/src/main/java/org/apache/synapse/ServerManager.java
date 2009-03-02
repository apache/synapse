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

    /* The controller for synapse create and Destroy synapse artifacts in a particular environment*/
    private SynapseController synapseController;
    /* Server Configuration  */
    private ServerConfigurationInformation configurationInformation;
    /* Server context */
    private ServerContextInformation contextInformation;
    /* The state of the server - the state that marked at last operation on server */
    private ServerState serverState = ServerState.UNDETERMINED;

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
        this.contextInformation = contextInformation;
        this.synapseController = SynapseControllerFactory
                .createSynapseController(configurationInformation);
        doInit();
        return this.serverState;
    }

    /**
     * Starting up the server
     *
     * @return ServerState - The state of the server after call this operation
     */
    public ServerState start() {
        doInit();
        doStart();
        return this.serverState;
    }

    /**
     * Stopping the server
     *
     * @return ServerState - The state of the server after call this operation
     */
    public ServerState stop() {
        doStop();
        return this.serverState;
    }

    /**
     * Returns the ServerConfigurationInformation
     *
     * @return ServerConfigurationInformation insatnce
     */
    public ServerConfigurationInformation getInformation() {
        return configurationInformation;
    }

    /**
     * Retunrs the ServerContextInformation
     *
     * @return ServerContextInformation instance
     */
    public ServerContextInformation getContextInformation() {
        return contextInformation;
    }

    /**
     * Helper method that to do init
     */
    private void doInit() {

        this.serverState = ServerStateDetectionStrategy.currentState(serverState,
                configurationInformation);

        if (this.serverState == ServerState.INITIALIZABLE) {
            
            this.synapseController.init(configurationInformation, contextInformation);

            if (this.contextInformation == null) {
                this.contextInformation = new ServerContextInformation(
                        this.synapseController.getContext());
            } else if (this.contextInformation.getServerContext() == null) {
                this.contextInformation.setServerContext(this.synapseController.getContext());
            }

            this.serverState = ServerState.INITIALIZED;
        }
    }

    /**
     * Helper method that to do start
     */
    private void doStart() {

        this.serverState = ServerStateDetectionStrategy.currentState(serverState,
                configurationInformation);

        if (this.serverState == ServerState.INITIALIZED) {

            this.synapseController.createSynapseConfiguration();
            this.synapseController.createSynapseEnvironment();
            this.serverState = ServerState.STARTED;
            log.info("Ready for processing");
        }
    }

    /**
     * Helper method that to do stop
     */
    private void doStop() {

        this.serverState = ServerStateDetectionStrategy.currentState(serverState,
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
        this.serverState = ServerState.STOPPED;
    }
}

