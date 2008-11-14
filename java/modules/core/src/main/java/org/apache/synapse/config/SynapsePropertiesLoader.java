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
package org.apache.synapse.config;

import org.apache.synapse.SynapseConstants;
import org.apache.synapse.util.MiscellaneousUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Properties;

/**
 * Provides a Factory method load synapse properties.
 * Cache the properties to make sure properties loading only is occurred  onetime
 */
public class SynapsePropertiesLoader {

    private static Log log = LogFactory.getLog(SynapsePropertiesLoader.class);

    private SynapsePropertiesLoader() {
    }

    private static Properties cacheProperties;

    /**
     * Loads the properties
     * This happen only cached properties are null.
     *
     * @return Synapse Properties
     */
    public static Properties loadSynapseProperties() {

        if (cacheProperties == null) {

            if (log.isDebugEnabled()) {
                log.debug("Loading synapse properties from a property file");
            }

            cacheProperties = MiscellaneousUtil.loadProperties(
                    SynapseConstants.SYNAPSE_PROPERTIES);

        } else {

            if (log.isDebugEnabled()) {
                log.debug("Retrieving synapse properties from the cache");
            }
        }

        // Original properties needed to be preserved
        Properties tempProperties = new Properties();
        tempProperties.putAll(cacheProperties);
        return tempProperties;
    }

    /**
     * Reloading properties from file
     *
     * @return Reloaded properties
     */
    public static Properties reloadSynapseProperties() {
        if (log.isDebugEnabled()) {
            log.debug("Reloading synapse properties");
        }
        cacheProperties = null;
        return loadSynapseProperties();
    }
}
