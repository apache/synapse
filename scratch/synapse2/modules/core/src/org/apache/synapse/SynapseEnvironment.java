/*
 * Copyright 2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.synapse;


import org.apache.synapse.config.SynapseConfiguration;


/**
 * The Synapse Environment is available to mediators through the SynapseMessage. It
 * allows one to call to the underlying SOAP engine (such as Axis2) where required.
 * e.g. send message, get classloader etc. It also holds a reference to the current
 * SynapseConfiguration.
 */
public interface SynapseEnvironment {

    /*
    * This method injects a new message into the Synapse engine
    * It is used in a couple of ways. Firstly, this is how, for example,
    * Axis2 kicks messages into Synapse to start with.
    * <p>
    * Also mediators can use this to send messages that they want to be mediated by Synapse
    * <p>For example if you want to send a copy of a message somewhere, you can clone it and then
    * injectMessage()
    */
    public void injectMessage(SynapseMessage smc);

    /*
     * Mediators that wish to load classes should use the ClassLoader given here
     */
    public ClassLoader getClassLoader();

    /**
     * This method allows you send messages on. As opposed to injectMessage send message does not
     * process these through Synapse.
     * <p/>
     * This will send request messages on, and send response messages back to the client
     */
    public void send(SynapseMessage smc);

    /**
     * Get a reference to the current SynapseConfiguration
     *
     * @return the current synapse configuration
     */
    public SynapseConfiguration getConfiguration();

    /**
     * Set or replace the Synapse Configuration instance to be used
     *
     * @param cfg The new synapse configuration instance
     */
    public void setConfiguration(SynapseConfiguration cfg);

}
