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

package org.apache.synapse.transport.nhttp;

import org.apache.http.protocol.HTTP;
import org.apache.synapse.transport.utils.config.HttpTransportConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Store and manage properties that tune the nhttp transport
 */
public final class NHttpConfiguration extends HttpTransportConfiguration {

    // defaults
    private static final int WORKERS_CORE_THREADS  = 20;
    private static final int WORKERS_MAX_THREADS   = 100;
    private static final int WORKER_KEEP_ALIVE     = 5;
    private static final int BLOCKING_QUEUE_LENGTH = -1;
    private static final int IO_WORKER_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int BUFFER_SIZE           = 8192;

    // server listener
    private static final String S_T_CORE     = "snd_t_core";
    private static final String S_T_MAX      = "snd_t_max";
    private static final String S_T_ALIVE    = "snd_alive_sec";
    private static final String S_T_QLEN     = "snd_qlen";

    // client sender
    private static final String C_T_CORE     = "lst_t_core";
    private static final String C_T_MAX      = "lst_t_max";
    private static final String C_T_ALIVE    = "lst_alive_sec";
    private static final String C_T_QLEN     = "lst_qlen";

    private static final String IO_WORKERS = "io_threads_per_reactor";

    // general
    private static final String G_BUFFER_SIZE  = "nhttp_buffer_size";
    private static final String G_DISABLED_HTTP_METHODS = "nhttp_disabled_methods";

    private static NHttpConfiguration _instance = new NHttpConfiguration();
    private List<String> methods;
    //Preserve HTTP headers
    private List<String> preserveHeaders;
    /** Comma separated list of blocked uris*/
    public static final String BLOCK_SERVICE_LIST = "http.block_service_list";
    /** Default value for BLOCK_SERVICE_LIST*/
    public static final String BLOCK_SERVICE_LIST_DEFAULT = "false";
    
    private NHttpConfiguration() {
        super("nhttp");
        populatePreserveHttpHeaders();
    }

    @Override
    protected int getThreadsPerReactor() {
        return getIntProperty(IO_WORKERS, IO_WORKER_COUNT);
    }

    public static NHttpConfiguration getInstance() {
        return _instance;
    }

    public int getServerCoreThreads() {
        return getIntProperty(S_T_CORE, WORKERS_CORE_THREADS);
    }

    public int getServerMaxThreads() {
        return getIntProperty(S_T_MAX, WORKERS_MAX_THREADS);
    }

    public int getServerKeepalive() {
        return getIntProperty(S_T_ALIVE, WORKER_KEEP_ALIVE);
    }

    public int getServerQueueLen() {
        return getIntProperty(S_T_QLEN, BLOCKING_QUEUE_LENGTH);
    }

    public int getClientCoreThreads() {
        return getIntProperty(C_T_CORE, WORKERS_CORE_THREADS);
    }

    public int getClientMaxThreads() {
        return getIntProperty(C_T_MAX, WORKERS_MAX_THREADS);
    }

    public int getClientKeepalive() {
        return getIntProperty(C_T_ALIVE, WORKER_KEEP_ALIVE);
    }

    public int getClientQueueLen() {
        return getIntProperty(C_T_QLEN, BLOCKING_QUEUE_LENGTH);
    }

    public int getBufferSize() {
        return getIntProperty(G_BUFFER_SIZE, BUFFER_SIZE);
    }

    public boolean isKeepAliveDisabled() {
        return getIntProperty(NhttpConstants.DISABLE_KEEPALIVE, 0) == 1;
    }

    public boolean isCountConnections() {
        return getBooleanProperty(NhttpConstants.COUNT_CONNECTIONS, false);
    }

    public String isServiceListBlocked() {
        return getStringProperty(BLOCK_SERVICE_LIST, BLOCK_SERVICE_LIST_DEFAULT);
    }

    public boolean isHttpMethodDisabled(String method) {
        if (methods == null) {
            methods = new ArrayList<String>();
            String methodsString = getStringProperty(G_DISABLED_HTTP_METHODS, "");
            for (String methodStr : methodsString.split(",")) {
                methods.add(methodStr.trim().toUpperCase());
            }
        }
        return methods.contains(method);
    }

    /**
     * Check preserving status of the http header field
     *
     * @param httpHeader http header name
     * @return return true if preserve else false
     */
    public boolean isPreserveHttpHeader(String httpHeader) {
        if (preserveHeaders == null || preserveHeaders.isEmpty() || httpHeader == null) {
            return false;
        } else {
            return preserveHeaders.contains(httpHeader.toUpperCase());
        }
    }

    private void populatePreserveHttpHeaders() {
        if (preserveHeaders == null) {
            preserveHeaders = new ArrayList<String>();
            String presHeaders = getStringProperty(NhttpConstants.HTTP_HEADERS_PRESERVE, "");

            if (presHeaders != null && !presHeaders.isEmpty()) {
                String[] splitHeaders = presHeaders.toUpperCase().trim().split(",");

                if (splitHeaders != null && splitHeaders.length > 0) {
                    preserveHeaders.addAll(Arrays.asList(splitHeaders));
                }
            }

            if (getBooleanProperty(NhttpConstants.SERVER_HEADER_PRESERVE, true)
                && !preserveHeaders.contains(HTTP.SERVER_HEADER.toUpperCase())) {
                preserveHeaders.add(HTTP.SERVER_HEADER.toUpperCase());
            }

            if (getBooleanProperty(NhttpConstants.USER_AGENT_HEADER_PRESERVE, false)
                && !preserveHeaders.contains(HTTP.USER_AGENT.toUpperCase())) {
                preserveHeaders.add(HTTP.USER_AGENT.toUpperCase());
            }
        }
    }
}
