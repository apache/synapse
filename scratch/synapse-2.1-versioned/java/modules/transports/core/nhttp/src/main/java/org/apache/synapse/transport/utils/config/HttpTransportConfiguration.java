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

package org.apache.synapse.transport.utils.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.synapse.commons.util.MiscellaneousUtil;

import java.nio.charset.CodingErrorAction;
import java.util.Properties;

/**
 * A base class for parsing transport configuration files and initializing basic
 * HTTP Core configuration objects.
 */
public abstract class HttpTransportConfiguration {

    protected Log log = LogFactory.getLog(this.getClass());

    private Properties props;

    /**
     * Create a new HttpTransportConfiguration instance.
     *
     * @param fileName Name of the file (without extensions) from where the transport
     *                 configuration should be loaded.
     */
    public HttpTransportConfiguration(String fileName) {
        try {
            props = MiscellaneousUtil.loadProperties(fileName + ".properties");
        } catch (Exception ignored) {}
    }

    /**
     * Get the number of I/O dispatcher threads that should be used in each IOReactor.
     *
     * @return A positive integer
     */
    abstract protected int getThreadsPerReactor();

    /**
     * Get the listening I/O reactor configuration
     *
     * @return A fully initialized IOReactorConfig instance
     */
    public IOReactorConfig getListeningReactorConfig() {
        IOReactorConfig.Builder builder = IOReactorConfig.custom()
                .setIoThreadCount(getThreadsPerReactor())
                .setSoTimeout(getIntProperty(HttpConfigConstants.LISTENER_SO_TIMEOUT,
                        getIntProperty(HttpConfigConstants.SO_TIMEOUT, 60000)))
                .setConnectTimeout(getIntProperty(HttpConfigConstants.CONNECTION_TIMEOUT, 0))
                .setInterestOpQueued(getBooleanProperty(HttpConfigConstants.INTEREST_OPS_QUEUEING, false))
                .setTcpNoDelay(getBooleanProperty(HttpConfigConstants.TCP_NODELAY, true));

        if (getIntProperty(HttpConfigConstants.SOCKET_RCV_BUFFER_SIZE) != null) {
            builder.setRcvBufSize(getIntProperty(HttpConfigConstants.SOCKET_RCV_BUFFER_SIZE));
        }

        if (getIntProperty(HttpConfigConstants.SOCKET_SND_BUFFER_SIZE) != null) {
            builder.setSndBufSize(getIntProperty(HttpConfigConstants.SOCKET_SND_BUFFER_SIZE));
        }

        if (getIntProperty(HttpConfigConstants.SO_LINGER) != null) {
            builder.setSoLinger(getIntProperty(HttpConfigConstants.SO_LINGER));
        }

        if (getBooleanProperty(HttpConfigConstants.SO_REUSEADDR) != null) {
            builder.setSoReuseAddress(getBooleanProperty(HttpConfigConstants.SO_REUSEADDR));
        }

        if (getIntProperty(HttpConfigConstants.SELECT_INTERVAL) != null) {
            builder.setSelectInterval(getIntProperty(HttpConfigConstants.SELECT_INTERVAL));
        }
        return builder.build();
    }

    /**
     * Get the connecting I/O reactor configuration
     *
     * @return A fully initialized IOReactorConfig instance
     */
    public IOReactorConfig getConnectingReactorConfig() {
        IOReactorConfig.Builder builder = IOReactorConfig.custom()
                .setIoThreadCount(getThreadsPerReactor())
                .setSoTimeout(getIntProperty(HttpConfigConstants.SENDER_SO_TIMEOUT,
                        getIntProperty(HttpConfigConstants.SO_TIMEOUT, 60000)))
                .setConnectTimeout(getIntProperty(HttpConfigConstants.CONNECTION_TIMEOUT, 0))
                .setInterestOpQueued(getBooleanProperty(HttpConfigConstants.INTEREST_OPS_QUEUEING, false))
                .setTcpNoDelay(getBooleanProperty(HttpConfigConstants.TCP_NODELAY, true));

        if (getIntProperty(HttpConfigConstants.SOCKET_RCV_BUFFER_SIZE) != null) {
            builder.setRcvBufSize(getIntProperty(HttpConfigConstants.SOCKET_RCV_BUFFER_SIZE));
        }

        if (getIntProperty(HttpConfigConstants.SOCKET_SND_BUFFER_SIZE) != null) {
            builder.setSndBufSize(getIntProperty(HttpConfigConstants.SOCKET_SND_BUFFER_SIZE));
        }

        if (getIntProperty(HttpConfigConstants.SO_LINGER) != null) {
            builder.setSoLinger(getIntProperty(HttpConfigConstants.SO_LINGER));
        }

        if (getBooleanProperty(HttpConfigConstants.SO_REUSEADDR) != null) {
            builder.setSoReuseAddress(getBooleanProperty(HttpConfigConstants.SO_REUSEADDR));
        }

        if (getIntProperty(HttpConfigConstants.SELECT_INTERVAL) != null) {
            builder.setSelectInterval(getIntProperty(HttpConfigConstants.SELECT_INTERVAL));
        }
        return builder.build();
    }

    /**
     * Get the connection configuration
     *
     * @return A fully initialized ConnectionConfig instance
     */
    public ConnectionConfig getConnectionConfig() {
        return ConnectionConfig.custom()
                .setBufferSize(getIntProperty(HttpConfigConstants.SOCKET_BUFFER_SIZE, 8 * 1024))
                .setMalformedInputAction(getMalformedInputActionValue())
                .setUnmappableInputAction(getUnMappableInputActionValue())
                .build();
    }

    /**
     * Get an int property that tunes the http transport. Prefer system properties
     *
     * @param name name of the system/config property
     * @param def  default value to return if the property is not set
     * @return the value of the property to be used
     */
    public Integer getIntProperty(String name, Integer def) {
        String val = System.getProperty(name);
        if (val == null) {
            val = props.getProperty(name);
        }

        if (val != null) {
            int intVal;
            try {
                intVal = Integer.valueOf(val);
            } catch (NumberFormatException e) {
                log.warn("Invalid http tuning property value. " + name + " must be an integer");
                return def;
            }
            if (log.isDebugEnabled()) {
                log.debug("Using http tuning parameter : " + name + " = " + val);
            }
            return intVal;
        }

        return def;
    }

    /**
     * Get an int property that tunes the http transport. Prefer system properties
     *
     * @param name name of the system/config property
     * @return the value of the property, null if the property is not found
     */
    public Integer getIntProperty(String name) {
        return getIntProperty(name, null);
    }

    /**
     * Get a boolean property that tunes the http transport. Prefer system properties
     *
     * @param name name of the system/config property
     * @param def  default value to return if the property is not set
     * @return the value of the property to be used
     */
    public Boolean getBooleanProperty(String name, Boolean def) {
        String val = System.getProperty(name);
        if (val == null) {
            val = props.getProperty(name);
        }

        if (val != null) {
            if (log.isDebugEnabled()) {
                log.debug("Using http tuning parameter : " + name + " = " + val);
            }
            return Boolean.valueOf(val);
        }

        return def;
    }

    /**
     * Get a Boolean property that tunes the http transport. Prefer system properties
     *
     * @param name name of the system/config property
     * @return the value of the property, null if the property is not found
     */
    public Boolean getBooleanProperty(String name) {
        return getBooleanProperty(name, null);
    }

    /**
     * Get a String property that tunes the http transport. Prefer system properties
     *
     * @param name name of the system/config property
     * @param def  default value to return if the property is not set
     * @return the value of the property to be used
     */
    public String getStringProperty(String name, String def) {
        String val = System.getProperty(name);
        if (val == null) {
            val = props.getProperty(name);
        }

        return val == null ? def : val;
    }

    private CodingErrorAction getMalformedInputActionValue() {
        String val = getStringProperty(HttpConfigConstants.HTTP_MALFORMED_INPUT_ACTION, "report");
        return getCodingErrorAction(val);
    }

    private CodingErrorAction getUnMappableInputActionValue() {
        String val = getStringProperty(HttpConfigConstants.HTTP_UNMAPPABLE_INPUT_ACTION, "report");
        return getCodingErrorAction(val);
    }

    private CodingErrorAction getCodingErrorAction(String action) {
        if ("report".equals(action)) {
            return CodingErrorAction.REPORT;
        } else if ("ignore".equals(action)) {
            return CodingErrorAction.IGNORE;
        } else if ("replace".equals(action)) {
            return CodingErrorAction.REPLACE;
        } else {
            return CodingErrorAction.REPORT;
        }
    }
}
