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

package org.apache.synapse.util.concurrent;

import java.util.concurrent.*;

/**
 * This is the executor service that will be returned by the env
 */
public class SynapseThreadPool extends ThreadPoolExecutor {

    // default values
    private static final int SYNAPSE_CORE_THREADS  = 20;
    private static final int SYNAPSE_MAX_THREADS   = 100;
    private static final int SYNAPSE_KEEP_ALIVE     = 5;
    private static final int BLOCKING_QUEUE_LENGTH = -1;
    private static final String SYNAPSE_THREAD_GROUP = "synapse-thread-group";
    private static final String SYNAPSE_THREAD_ID_PREFIX = "SynapseWorker";

    // property keys
    private static final String SYN_THREAD_CORE     = "syn_t_core";
    private static final String SYN_THREAD_MAX      = "syn_t_max";
    private static final String SYN_THREAD_ALIVE    = "syn_alive_sec";
    private static final String SYN_THREAD_QLEN     = "syn_qlen";

    /**
     * Constructor for the Synapse thread poll
     * 
     * @param corePoolSize    - number of threads to keep in the pool, even if they are idle
     * @param maximumPoolSize - the maximum number of threads to allow in the pool
     * @param keepAliveTime   - this is the maximum time that excess idle threads will wait
     *  for new tasks before terminating.
     * @param unit            - the time unit for the keepAliveTime argument.
     * @param workQueue       - the queue to use for holding tasks before they are executed.
     */
    public SynapseThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime,
        TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
            new SynapseThreadFactory(
                new ThreadGroup(SYNAPSE_THREAD_GROUP), SYNAPSE_THREAD_ID_PREFIX));
    }

    /**
     * Default Constructor for the thread pool and will use all the values as default
     */
    public SynapseThreadPool() {
        this(SYNAPSE_CORE_THREADS, SYNAPSE_MAX_THREADS, SYNAPSE_KEEP_ALIVE,
            TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    }
}
