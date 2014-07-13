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

import java.util.Map;
import java.util.HashMap;

public class SimpleInMemoryRegistry extends AbstractRegistry {    

    private Map<String,InMemoryRegistryEntry> registry = new HashMap<String,InMemoryRegistryEntry>();
    private long cacheDuration;
    private int hitCount;

    public SimpleInMemoryRegistry(Map<String,OMNode> data, long cacheDuration) {
        this.cacheDuration = cacheDuration;
        this.hitCount = 0;

        for (String key : data.keySet()) {
            InMemoryRegistryEntry entry = new InMemoryRegistryEntry(key);
            entry.setValue(data.get(key));
            entry.setCachableDuration(cacheDuration);
            registry.put(key, entry);
        }
    }

    public OMNode lookup(String key) {
        hitCount++;
        InMemoryRegistryEntry entry = registry.get(key);
        if (entry != null) {
            return (OMNode) entry.getValue();
        }
        return null;
    }

    public RegistryEntry getRegistryEntry(String key) {
        return registry.get(key);
    }

    public RegistryEntry[] getChildren(RegistryEntry entry) {
        return null;
    }

    public RegistryEntry[] getDescendants(RegistryEntry entry) {
        return null;
    }

    public void delete(String key) {
        registry.remove(key);
    }

    public void newResource(String key, boolean isDirectory) {
        registry.put(key, new InMemoryRegistryEntry(key));
    }

    public void updateResource(String key, Object value) {
        InMemoryRegistryEntry entry = registry.get(key);
        if (entry != null) {
            entry.setValue(value);
        }
    }

    public void updateRegistryEntry(RegistryEntry entry) {

    }

    public int getHitCount() {
        return hitCount;
    }
}
