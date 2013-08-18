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

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.ParameterInclude;
import org.apache.axis2.transport.base.threads.WorkerPool;
import org.apache.axis2.transport.base.threads.WorkerPoolFactory;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.util.HeapByteBufferAllocator;
import org.apache.http.protocol.HttpProcessor;
import org.apache.synapse.transport.passthru.jmx.PassThroughTransportMetricsCollector;
import org.apache.synapse.transport.passthru.util.BufferFactory;

/**
 * This class has common configurations for both sender and receiver.
 */
public abstract class BaseConfiguration {

    /**
     * Configurations given by axis2.xml
     */
    protected ParameterInclude parameters = null;

    /** The thread pool for executing the messages passing through */
    private WorkerPool workerPool = null;

    /** The Axis2 ConfigurationContext */
    private ConfigurationContext configurationContext = null;

    private BufferFactory bufferFactory = null;

    private PassThroughTransportMetricsCollector metrics = null;

    private HttpProcessor httpProcessor;

    protected PassThroughConfiguration conf = PassThroughConfiguration.getInstance();

    public BaseConfiguration(ConfigurationContext configurationContext,
                             ParameterInclude parameters,
                             WorkerPool workerPool) {
        this.parameters = parameters;
        this.configurationContext = configurationContext;
        if (workerPool == null) {
            this.workerPool = WorkerPoolFactory.getWorkerPool(
                    conf.getWorkerPoolCoreSize(),
                    conf.getWorkerPoolMaxSize(),
                    conf.getWorkerThreadKeepaliveSec(),
                    conf.getWorkerPoolQueueLen(),
                    "Pass-through Message Processing Thread Group",
                    "PassThroughMessageProcessor");
        } else {
            this.workerPool = workerPool;
        }

        int bufferSize = conf.getIntProperty(PassThroughConfigPNames.IO_BUFFER_SIZE, 1024 * 8);
        bufferFactory = new BufferFactory(bufferSize, HeapByteBufferAllocator.INSTANCE, 512);
        httpProcessor = initHttpProcessor();
    }

    abstract protected HttpProcessor initHttpProcessor();

    public IOReactorConfig getReactorConfig(boolean listener) {
        if (listener) {
            return conf.getListeningReactorConfig();
        } else {
            return conf.getConnectingReactorConfig();
        }
    }

    public ConnectionConfig getConnectionConfig() {
        return conf.getConnectionConfig();
    }

    public WorkerPool getWorkerPool() {
        return workerPool;
    }

    public ConfigurationContext getConfigurationContext() {
        return configurationContext;
    }

    public BufferFactory getBufferFactory() {
        return bufferFactory;
    }

    public HttpProcessor getHttpProcessor() {
        return httpProcessor;
    }

    public PassThroughTransportMetricsCollector getMetrics() {
        return metrics;
    }

    public void setMetrics(PassThroughTransportMetricsCollector metrics) {
        this.metrics = metrics;
    }
}
