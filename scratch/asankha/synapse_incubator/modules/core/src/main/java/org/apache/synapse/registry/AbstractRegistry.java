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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.config.XMLToObjectMapper;
import org.apache.synapse.config.Property;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Implements the core Registry lookup algorithm
 */
public abstract class AbstractRegistry implements Registry {

    private static final Log log = LogFactory.getLog(AbstractRegistry.class);

    /** The name of the registry */
    protected String name = null;

    /** The list of configuration properties */
    protected Map properties = new HashMap();

    /**
     * Get the object for the given key from this registry
     * @param dp the DynamicProperty for the registry lookup
     * @return the matching resultant object
     */
    public Object getProperty(Property dp) {

        OMNode omNode = null;
        RegistryEntry re = null;

        // we are dealing with a DynamicProperty. Have we seen this before and processed
        // it at least once and have it cached?

        // if we have an unexpired cached copy, return the cached object
        if (dp.isCached() && !dp.isExpired()) {
            return dp.getValue();

        // if we have not cached the referenced object, fetch it and its RegistryEntry
        } else if (!dp.isCached()) {
            omNode = lookup(dp.getKey());
            re = getRegistryEntry(dp.getKey());

        // if we have cached it before, and now the cache has expired
        // get its *new* registry entry and compare versions and pick new cache duration
        } else if (dp.isExpired()) {

            log.debug("Cached object has expired for key : " + dp.getKey());
            re = getRegistryEntry(dp.getKey());

            if (re.getVersion() != Long.MIN_VALUE &&
                re.getVersion() == dp.getVersion()) {
                log.debug("Expired version number is same as current version in registry");

                // renew cache lease for another cachable duration (as returned by the
                // new getRegistryEntry() call
                dp.setExpiryTime(
                    System.currentTimeMillis() + re.getCachableDuration());
                log.debug("Renew cache lease for another " + re.getCachableDuration() / 1000 + "s");

                // return cached object
                return dp.getValue();

            } else {
                omNode = lookup(dp.getKey());
            }
        }

        // if we get here, we have received the raw omNode from the
        // registry and our previous copy (if we had one) has expired or is not valid

        if (dp.getMapper() != null) {
            dp.setValue(
                dp.getMapper().getObjectFromOMNode(omNode));
        } else {
            // if the type of the object is known to have a mapper, create the
            // resultant Object using the known mapper, and cache this Object
            // else cache the raw OMNode
            if (re != null && re.getType() != null) {

                XMLToObjectMapper mapper = getMapper(re.getType());
                if (mapper != null) {
                    dp.setMapper(mapper);
                    dp.setValue(mapper.getObjectFromOMNode(omNode));

                } else {
                    dp.setValue(omNode);
                }
            }
        }

        // increment cache expiry time as specified by the last getRegistryEntry() call
        dp.setExpiryTime(
            System.currentTimeMillis() + re.getCachableDuration());
        dp.setVersion(re.getVersion());

        return dp.getValue();
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

    public String getProviderClass() {
        return this.getClass().getName();
    }

    public Map getConfigProperties() {
        return properties;
    }

    public void addConfigProperty(String name, String value) {
        properties.put(name, value);
    }
}
