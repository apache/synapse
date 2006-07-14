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
package org.apache.synapse.config;

/**
 * A DynamicProperty is a Synapse (global) property (defined within the 'definitions'
 * section) of a SynapseConfiguration. However, these properties are implicitly
 * tied to a Registry, and will be automatically refreshed after a cacheable lease
 * expires for such a property. The cachable duration for each property is given
 * by the registry when the RegistryEntry for a property is requested. This duration
 * can change while the property is being used, and will be refreshed at each
 * cache expiry. However, the resource will be loaded, iff the RegistryEntry returned
 * after a cache expiry as a version later than the version cached. Else, the
 * existing cache of the resource will be renewed for a further period, as stated by
 * the new RegistryEntry lookup.
 */
public class DynamicProperty {
    /** The registry key */
    private String key;
    /** The cached object */
    private Object cache = null;
    /** An XML to Object mapper - if one is available */
    private XMLToObjectMapper mapper = null;
    /** The version of the cached resource */
    private long version;
    /** The local expiry time for the cached resource */
    private long expiryTime;

    public DynamicProperty(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Object getCache() {
        return cache;
    }

    public void setCache(Object cache) {
        this.cache = cache;
    }

    public XMLToObjectMapper getMapper() {
        return mapper;
    }

    public void setMapper(XMLToObjectMapper mapper) {
        this.mapper = mapper;
    }

    public long getExpiryTime() {
        return expiryTime;
    }

    public void setExpiryTime(long expiryTime) {
        this.expiryTime = expiryTime;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiryTime;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }
}
