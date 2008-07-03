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
package org.apache.synapse.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;

import java.util.Properties;
import java.io.InputStream;
import java.io.File;
import java.io.IOException;

/**
 *
 */
public class MiscellaneousUtil {
    private static Log log = LogFactory.getLog(MiscellaneousUtil.class);

    private MiscellaneousUtil() {
    }

    /**
     * Helper method to get the value of the property from a given property bag
     *
     * @param dsProperties The property collection
     * @param name         The name of the property
     * @param def          The default value for the property
     * @return The value of the property if it is found , otherwise , default value
     */
    public static String getProperty(Properties dsProperties, String name, String def) {

        String result = dsProperties.getProperty(name);
        if ((result == null || result.length() == 0 || "".equals(result)) && def != null) {
            if (log.isDebugEnabled()) {
                log.debug("The name with ' " + name + " ' cannot be found. " +
                        "Using default value " + def);
            }
            result = def;
        }
        if (result != null) {
            return result.trim();
        } else {
            return def;
        }
    }

    /**
     * Loads the properties from a given property file path
     *
     * @param filePath Path of the property file
     * @return Properties loaded from given file
     */
    public static Properties loadProperties(String filePath) {

        Properties properties = new Properties();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        if (log.isDebugEnabled()) {
            log.debug("Loading a file ' " + filePath + " ' from classpath");
        }

        InputStream in = cl.getResourceAsStream(filePath);
        if (in == null) {
            if (log.isDebugEnabled()) {
                log.debug("Unable to load file  ' " + filePath + " '");
            }

            filePath = SynapseConstants.CONF_DIRECTORY +
                    File.separatorChar + filePath;
            if (log.isDebugEnabled()) {
                log.debug("Loading a file ' " + filePath + " ' from classpath");
            }

            in = cl.getResourceAsStream(filePath);
            if (in == null) {
                if (log.isDebugEnabled()) {
                    log.debug("Unable to load file  ' " + filePath + " '");
                }
            }
        }
        if (in != null) {
            try {
                properties.load(in);
            } catch (IOException e) {
                String msg = "Error loading properties from a file at :" + filePath;
                log.error(msg, e);
                throw new SynapseException(msg, e);
            }
        }
        return properties;
    }

}
