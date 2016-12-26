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

package org.apache.synapse.transport.passthru.config;

import org.apache.synapse.transport.utils.config.HttpTransportConfiguration;

/**
 * This class encapsulates pass-through http transport tuning configurations specified via a
 * configurations file or system properties.
 */
public class PassThroughConfiguration extends HttpTransportConfiguration {

    /**
     * Default tuning parameter values
     */
    private static final int DEFAULT_WORKER_POOL_SIZE_CORE       = 40;
    private static final int DEFAULT_WORKER_POOL_SIZE_MAX        = 200;
    private static final int DEFAULT_WORKER_THREAD_KEEPALIVE_SEC = 60;
    private static final int DEFAULT_WORKER_POOL_QUEUE_LENGTH    = -1;
    private static final int DEFAULT_IO_THREADS_PER_REACTOR      = Runtime.getRuntime().availableProcessors();

    private static PassThroughConfiguration _instance = new PassThroughConfiguration();

    private PassThroughConfiguration() {
        super("passthru-http") ;
    }

    public static PassThroughConfiguration getInstance() {
        return _instance;
    }

    public int getWorkerPoolCoreSize() {
        return getIntProperty(PassThroughConfigPNames.WORKER_POOL_SIZE_CORE,
                DEFAULT_WORKER_POOL_SIZE_CORE);
    }

    public int getWorkerPoolMaxSize() {
        return getIntProperty(PassThroughConfigPNames.WORKER_POOL_SIZE_MAX,
                DEFAULT_WORKER_POOL_SIZE_MAX);
    }

    public int getWorkerThreadKeepaliveSec() {
        return getIntProperty(PassThroughConfigPNames.WORKER_THREAD_KEEP_ALIVE_SEC,
                DEFAULT_WORKER_THREAD_KEEPALIVE_SEC);
    }

    public int getWorkerPoolQueueLen() {
        return getIntProperty(PassThroughConfigPNames.WORKER_POOL_QUEUE_LENGTH,
                DEFAULT_WORKER_POOL_QUEUE_LENGTH);
    }

    protected int getThreadsPerReactor() {
        return getIntProperty(PassThroughConfigPNames.IO_THREADS_PER_REACTOR,
                DEFAULT_IO_THREADS_PER_REACTOR);
    }

    public String getPreserveHttpHeaders() {
        return getStringProperty(PassThroughConfigPNames.HTTP_HEADERS_PRESERVE, "");
    }

}
