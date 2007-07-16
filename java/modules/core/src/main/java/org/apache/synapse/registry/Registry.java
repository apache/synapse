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

package org.apache.synapse.registry;

import org.apache.axiom.om.OMNode;
import org.apache.synapse.config.Entry;

import java.util.Map;

/**
 * This is the interface to a Registry from Synapse.
 */
public interface Registry {

    /**
     * Perform an actual lookup for for an XML resource as an OMNode for the given key
     * @param key the key for the registry lookup
     * @return the XML content from the registry as an OMNode
     */
    public OMNode lookup(String key);

    /**
     * This is the publicly used interface to the registry. It will fetch
     * the content from the registry and cache if required.
     * @see AbstractRegistry
     *
     * @param entry the registry Entry
     * @return the value from the registry or local cache
     */
    public Object getResource(Entry entry);

    /**
     * Get the registry entry for the given key
     * @param key the registry key
     * @return The registry entry for the given key
     */
    public RegistryEntry getRegistryEntry(String key);

    /**
     * Set a configuration property on the registry. Could be used to initialize a registry
     * @param name property name
     * @param value simple String value
     */
    public void addConfigProperty(String name, String value);

    /**
     * Returns the child elements of a given registry entry
     * @param entry - parent registry entry
     * @return Array of child registry entries of the given parent registry entry
     */
    public RegistryEntry[] getChildren(RegistryEntry entry);

    /**
     * Returns all decendant entries of the given registry entry
     * @param entry - parent registry entry
     * @return Array of decendant registry entries of the given registry entry
     */
    public RegistryEntry[] getDescendants(RegistryEntry entry);

    /**
     * Return the name of the implementation class
     * @return name of the registry provider implementation class name
     */
    public String getProviderClass();

    /**
     * Return the list of configuration properties set on this instance
     * @return a Map of configuration properties
     */
    public Map getConfigProperties();
}
