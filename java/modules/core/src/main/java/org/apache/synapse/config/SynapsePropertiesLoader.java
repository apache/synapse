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

import java.util.Properties;

/**
 * Provides a Factory method load synapse properties.
 * Cache the properties to make sure properties loading only is occurred  onetime
 */
public class SynapsePropertiesLoader {

    private SynapsePropertiesLoader() {
    }

    private static Properties properties;

    /**
     * Loads the properties
     * This happen only cached properties are null.
     *
     * @return Synapse Properties
     */
    public static Properties loadSynapseProperties() {
        if (properties == null) {
            properties = MiscellaneousUtil.loadProperties(
                    SynapseConstants.SYNAPSE_PROPERTIES);
        }
        return properties;
    }

}
