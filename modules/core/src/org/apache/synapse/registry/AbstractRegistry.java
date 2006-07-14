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
package org.apache.synapse.registry;

import org.apache.axiom.om.OMNode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.config.XMLToObjectMapper;
import org.apache.synapse.config.DynamicProperty;

import java.util.HashMap;
import java.util.Map;
import java.net.URI;

/**
 * Implements the core Registry lookup algorithm
 */
public abstract class AbstractRegistry implements Registry {

    private static final Log log = LogFactory.getLog(AbstractRegistry.class);

    /** A local cache of Objects */
    protected Map localCache = new HashMap();

    /** The name of the registry */
    protected String name = null;

    /**
     * Get the object for the given key from this registry
     * @param key the key for the registry lookup
     * @return the matching object
     */
    public Object getProperty(String key) {

        DynamicProperty dp = null;

        // check local cache for the given key
        Object obj = localCache.get(key);

        if (obj != null) {
            // if the local cache contains an object which is not a DynamicProperty, return it
            if (!(obj instanceof DynamicProperty)) {
                log.debug("Returning non dynamic object from cache for key : " + key);
                return obj;

            } else {
                log.debug("Cache contains DynamicProperty for key : " + key);
                dp = (DynamicProperty) obj;
            }

        } else {
            log.debug("Object not available in local registry cache for key : " + key);
        }

        OMNode omNode = null;
        RegistryEntry re = null;

        // we are dealing with a DynamicProperty. Have we seen this before and processed
        // it at least once and have it in the localCache?
        if (dp != null) {
            // if we have an unexpired cached copy, return the cached object
            if (!dp.isExpired() && dp.getCache() != null) {
                return dp.getCache();

            // if we have not cached the referenced object, fetch it and its RegistryEntry
            } else if (dp.getCache() == null) {
                omNode = lookup(key);
                re = getRegistryEntry(key);

            // if we have cached it before, and not the cache has expired
            // get its *new* registry entry and compare versions and pick new cache duration
            } else if (dp.isExpired()) {

                log.debug("Cached object has expired for key : " + key);
                re = getRegistryEntry(key);

                if (re.getVersion() != Long.MIN_VALUE &&
                    re.getVersion() == dp.getVersion()) {
                    log.debug("Expired version number is same as current version in registry");

                    // renew cache lease for another cachable duration (as returned by the
                    // last getRegistryEntry() call
                    dp.setExpiryTime(
                        System.currentTimeMillis() + re.getCachableDuration());
                    log.debug("Renew cache lease for another " + re.getCachableDuration() / 1000 + "s");

                    // return cached object
                    return dp.getCache();

                } else {
                    omNode = lookup(key);
                }
            }

        } else {
            log.debug("Processing DynamicProperty for the first time. key : " + key);
            dp = new DynamicProperty(key);
            omNode = lookup(key);
            re = getRegistryEntry(key);
        }

        // if we get here, we have received the raw omNode from the
        // registry and our previous copy (if we had one) has expired or is not valid

        // if the type of the object is known to have a mapper, create the
        // resultant Object using the known mapper, and cache this Object
        // else cache the raw OMNode
        if (re != null && re.getType() != null) {

            XMLToObjectMapper mapper = getMapper(re.getType());
            if (mapper != null) {
                dp.setMapper(mapper);
                dp.setCache(mapper.getObjectFromOMNode(omNode));

            } else {
                dp.setCache(omNode);
            }
        }

        // increment cache expiry time as specified by the last getRegistryEntry() call
        dp.setExpiryTime(
            System.currentTimeMillis() + re.getCachableDuration());
        dp.setVersion(re.getVersion());

        // place DynamicProperty in local cache
        localCache.put(key, dp);

        return dp.getCache();
    }

    private XMLToObjectMapper getMapper(URI type) {
        return null;
    }

    public void setRegistryName(String name) {
        this.name = name;
    }

    public String getRegistryName() {
        return name;
    }
}
