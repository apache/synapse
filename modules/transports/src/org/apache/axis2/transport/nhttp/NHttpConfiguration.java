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

package org.apache.axis2.transport.nhttp;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.apache.http.params.HttpConnectionParams;

import java.util.Properties;
import java.io.IOException;
import java.net.URL;

/**
 * Store and manage properties that tune the nhttp transport
 */
public class NHttpConfiguration {

    // defaults
    private static final int WORKERS_CORE_THREADS  = 20;
    private static final int WORKERS_MAX_THREADS   = 100;
    private static final int WORKER_KEEP_ALIVE     = 5;
    private static final int BLOCKING_QUEUE_LENGTH = -1;
    private static final int IO_WORKER_COUNT = 2;
    private static final int BUFFER_SIZE           = 2048;

    // server listener
    private static final String S_T_CORE     = "snd_t_core";
    private static final String S_T_MAX      = "snd_t_max";
    private static final String S_T_ALIVE    = "snd_alive_sec";
    private static final String S_T_QLEN     = "snd_qlen";
    private static final String S_IO_WORKERS = "snd_io_threads";

    // client sender
    private static final String C_T_CORE     = "lst_t_core";
    private static final String C_T_MAX      = "lst_t_max";
    private static final String C_T_ALIVE    = "lst_alive_sec";
    private static final String C_T_QLEN     = "lst_qlen";
    private static final String C_IO_WORKERS = "lst_io_threads";

    // general
    private static final String G_BUFFER_SIZE  = "nhttp_buffer_size";

    private static final Log log = LogFactory.getLog(NHttpConfiguration.class);
    private static NHttpConfiguration _instance = new NHttpConfiguration();
    private Properties props = new Properties();

    private NHttpConfiguration() {
        try {
            props.load(getClass().getClassLoader().getResourceAsStream("nhttp.properties"));
        } catch (Exception ignore) {}
    }

    public static NHttpConfiguration getInstance() {
        return _instance;
    }

    public int getServerCoreThreads() {
        return getProperty(S_T_CORE, WORKERS_CORE_THREADS);
    }

    public int getServerMaxThreads() {
        return getProperty(S_T_MAX, WORKERS_MAX_THREADS);
    }

    public int getServerKeepalive() {
        return getProperty(S_T_ALIVE, WORKER_KEEP_ALIVE);
    }

    public int getServerQueueLen() {
        return getProperty(S_T_QLEN, BLOCKING_QUEUE_LENGTH);
    }

    public int getServerIOWorkers() {
        return getProperty(S_IO_WORKERS, IO_WORKER_COUNT);
    }


    public int getClientCoreThreads() {
        return getProperty(C_T_CORE, WORKERS_CORE_THREADS);
    }

    public int getClientMaxThreads() {
        return getProperty(C_T_MAX, WORKERS_MAX_THREADS);
    }

    public int getClientKeepalive() {
        return getProperty(C_T_ALIVE, WORKER_KEEP_ALIVE);
    }

    public int getClientQueueLen() {
        return getProperty(C_T_QLEN, BLOCKING_QUEUE_LENGTH);
    }

    public int getClientIOWorkers() {
        return getProperty(C_IO_WORKERS, IO_WORKER_COUNT);
    }

    public int getBufferZise() {
        return getProperty(G_BUFFER_SIZE, BUFFER_SIZE);
    }

    /**
     * Get properties that tune nhttp transport. Preference to system properties
     * @param name name of the system/config property
     * @param def default value to return if the property is not set
     * @return the value of the property to be used
     */
    public int getProperty(String name, int def) {
        String val = System.getProperty(name);
        if (val == null) {
            val = props.getProperty(name);
        }

        if (val != null && Integer.valueOf(val).intValue() > 0) {
            log.debug("Using nhttp tuning parameter : " + name + " = " + val);
            return Integer.valueOf(val).intValue();
        }        
        return def;
    }

}
