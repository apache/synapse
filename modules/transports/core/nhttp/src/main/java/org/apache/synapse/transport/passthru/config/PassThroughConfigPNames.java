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

/**
 * Defines configuration parameter names for Pass-through HTTP Transport.
 */
public interface PassThroughConfigPNames {

    /**
     * Defines the core size (number of threads) of the worker thread pool.
     */
    public String WORKER_POOL_SIZE_CORE = "worker_pool_size_core";

    /**
     * Defines the maximum size (number of threads) of the worker thread pool.
     */
    public String WORKER_POOL_SIZE_MAX = "worker_pool_size_max";

    /**
     * Defines the keep-alive time for extra threads in the worker pool.
     */
    public String WORKER_THREAD_KEEP_ALIVE_SEC = "worker_thread_keepalive_sec";

    /**
     * Defines the length of the queue that is used to hold Runnable tasks to be executed by the
     * worker pool.
     */
    public String WORKER_POOL_QUEUE_LENGTH = "worker_pool_queue_length";

    /**
     * Defines the number of IO dispatcher threads used per reactor
     */
    public String IO_THREADS_PER_REACTOR = "io_threads_per_reactor";

    /**
     * Defines the IO buffer size
     */
    public String IO_BUFFER_SIZE = "io_buffer_size";

    /**
     * Defines whether ESB needs to preserve the original User-Agent header.
     */
    public String USER_AGENT_HEADER_PRESERVE = "http.user.agent.preserve";

    /**
     * Defines whether ESB needs to preserve the original Server header.
     */
    public String SERVER_HEADER_PRESERVE = "http.server.preserve";

    /**
     * Defines whether ESB needs to preserve the original Http header.
     */
    public String HTTP_HEADERS_PRESERVE = "http.headers.preserve";

    /**
     * Defines whether HTTP keep-alive is disabled
     */
    public String DISABLE_KEEPALIVE = "http.connection.disable.keepalive";

    /**
     * Defines the maximum number of connections per target (host:port pair)
     */
    public String MAX_CONNECTION_PER_TARGET = "http.max.connection.per.target";

    /**
     * Determines the value of the User-Agent header sent by the transport, when sending
     * requests to a backend endpoint.
     */
    public String USER_AGENT_HEADER_VALUE = "http.user.agent.value";

    /**
     * Determines the value of the Server header sent by the transport, when sending
     * responses to a client.
     */
    public String SERVER_HEADER_VALUE = "http.server.value";
}
