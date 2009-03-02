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

import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;

/**
 * The controller for synapse
 * Create and Destroy synapse artifacts in a particular environment
 */
public interface SynapseController {

    /**
     * Initialization of the synapse controller
     *
     * @param configurationInformation server information instance Information about the server
     * @param contextInformation    if there is a context already has been built.
     */
    public void init(ServerConfigurationInformation configurationInformation,
                     ServerContextInformation contextInformation);

    /**
     * Destroy event for destroy synapse
     */
    public void destroy();

    /**
     * Explicit check for initialization
     *
     * @return true if the initialization has been success.
     */
    public boolean isInitialized();

    /**
     * Create the SynapseEnvironment instance
     *
     * @return SynapseEnvironment instance if success
     */
    public SynapseEnvironment createSynapseEnvironment();

    /**
     * Destroy the SynapseEnvironment instance
     */
    public void destroySynapseEnvironment();

    /**
     * Create the synapse configuration instance
     *
     * @return SynapseConfiguration instance if success
     */
    public SynapseConfiguration createSynapseConfiguration();

    /**
     * Destroy the SynapseConfiguration instance
     */
    public void destroySynapseConfiguration();

    /**
     * Returns underlying environment context
     *
     * @return Underlying environment context
     */
    public Object getContext();

}
