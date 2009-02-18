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
package org.apache.synapse.commons.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.util.Properties;

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
     * @param properties   The property collection
     * @param name         The name of the property
     * @param defaultValue The default value for the property
     * @return The value of the property if it is found , otherwise , default value
     */
    public static String getProperty(Properties properties, String name, String defaultValue) {

        String result = properties.getProperty(name);
        if ((result == null || result.length() == 0) && defaultValue != null) {
            if (log.isDebugEnabled()) {
                log.debug("The name with ' " + name + " ' cannot be found. " +
                        "Using default value " + defaultValue);
            }
            result = defaultValue;
        }
        if (result != null) {
            return result.trim();
        } else {
            return defaultValue;
        }
    }

    /**
     * Helper method to get the value of the property from a given property bag
     * This method will return a value with the type equal to the type
     * given by the Class type parameter. Therefore, The user of this method
     * can ensure that  he is get what he request
     *
     * @param properties   Properties bag
     * @param name         Name of the property
     * @param defaultValue Default value
     * @param type         Expected Type using Class
     * @return Value corresponding to the given property name
     */
    public static Object getProperty(Properties properties, String name, Object defaultValue, Class type) {

        Object result = properties.getProperty(name);
        if (result == null && defaultValue != null) {
            if (log.isDebugEnabled()) {
                log.debug("The name with ' " + name + " ' cannot be found. " +
                        "Using default value " + defaultValue);
            }
            result = defaultValue;
        }

        if (result == null || type == null) {
            return result;
        }

        if (String.class.equals(type)) {

            if (result instanceof String) {
                return result;
            } else {
                handleException("Invalid type , expected String");
            }

        } else if (Boolean.class.equals(type)) {
            if (result instanceof String) {
                return Boolean.parseBoolean((String) result);
            } else if (result instanceof Boolean) {
                return result;
            } else {
                handleException("Invalid type , expected Boolean");
            }

        } else if (Integer.class.equals(type)) {
            if (result instanceof String) {
                return Integer.parseInt((String) result);
            } else if (result instanceof Integer) {
                return result;
            } else {
                handleException("Invalid type , expected Integer");
            }
        } else if (Long.class.equals(type)) {
            if (result instanceof String) {
                return Long.parseLong((String) result);
            } else if (result instanceof Long) {
                return result;
            } else {
                handleException("Invalid type , expected Long");
            }
        } else {
            return result;
        }
        return null;
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

            filePath = "conf" +
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
                throw new SynapseUtilException(msg, e);
            }
        }
        return properties;
    }

    /**
     * Helper method to serialize object into a byte array
     *
     * @param data The object to be serialized
     * @return The byte array representation of the provided object
     */
    public static byte[] serialize(Object data) {

        ObjectOutputStream outputStream = null;
        ByteArrayOutputStream binOut = null;
        byte[] result = null;
        try {
            binOut = new ByteArrayOutputStream();
            outputStream = new ObjectOutputStream(binOut);
            outputStream.writeObject(data);
            result = binOut.toByteArray();
        } catch (IOException e) {
            handleException("Error serializing object :" + data);
        } finally {
            if (binOut != null) {
                try {
                    binOut.close();
                } catch (IOException ignored) {}
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException ignored) {}
            }
        }
        return result;
    }


    /**
     * Helper methods for handle errors.
     *
     * @param msg The error message
     */
    private static void handleException(String msg) {
        log.error(msg);
        throw new SynapseUtilException(msg);
    }
}
