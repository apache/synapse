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
import org.apache.synapse.core.SynapseEnvironment;


/**
 * The Synapse Context is available to mediators through the SynapseMessage. It
 * allows one to call to the underlying SynapseEnvironment (i.e. the SOAP engine
 * - such as Axis2 - where required. It also allows one to access the current
 * SynapseConfiguration. Additionally it holds per message properties (i.e. local
 * properties valid for the lifetime of the message)
 */
public interface SynapseMessageContext {

    /**
     * Get a reference to the current SynapseConfiguration
     *
     * @return the current synapse configuration
     */
    public SynapseConfiguration getConfiguration();

    /**
     * Set or replace the Synapse Configuration instance to be used. May be used to
     * programatically change the configuration at runtime etc.
     *
     * @param cfg The new synapse configuration instance
     */
    public void setConfiguration(SynapseConfiguration cfg);

    /**
     * Returns a reference to the host Synapse Environment
     * @return the Synapse Environment
     */
    public SynapseEnvironment getSynapseEnvironment();

    /**
     * Sets the SynapseEnvironment reference to this context
     * @param se the reference to the Synapse Environment
     */
    public void setSynapseEnvironment(SynapseEnvironment se);

    /**
     * Sets the associated Synapse message
     * @param sm the synapse message associated with this context
     */
    public void setSynapseMessage(SynapseMessage sm);

    /**
     * Return the associated SynapseMessage
     * @return the associated Synapse message
     */
    public SynapseMessage getSynapseMessage();

    /**
     * Get the value of a custom (local) property set on the message instance
     * @param key key to look up property
     * @return value for the given key
     */
    public Object getProperty(String key);

    /**
     * Set a custom (local) property with the given name on the message instance
     * @param key key to be used
     * @param value value to be saved
     */
    public void setProperty(String key, Object value);

}
