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

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.ParameterInclude;
import org.apache.axis2.transport.base.threads.WorkerPool;
import org.apache.axis2.transport.base.threads.WorkerPoolFactory;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.params.NIOReactorPNames;
import org.apache.http.nio.params.NIOReactorParams;
import org.apache.http.nio.util.HeapByteBufferAllocator;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
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
    protected ConfigurationContext configurationContext = null;

    /** Default http parameters */
    protected HttpParams httpParameters = null;

    protected BufferFactory bufferFactory = null;

    private PassThroughTransportMetricsCollector metrics = null;

    private int iOThreadsPerReactor;

    protected PassThroughConfiguration conf = PassThroughConfiguration.getInstance();

    public BaseConfiguration(ConfigurationContext configurationContext,
                             ParameterInclude parameters,
                             WorkerPool workerPool) {
        this.parameters = parameters;
        this.workerPool = workerPool;
        this.configurationContext = configurationContext;
    }

    public void build() throws AxisFault {
        iOThreadsPerReactor = conf.getIOThreadsPerReactor();

        if (workerPool == null) {
            workerPool = WorkerPoolFactory.getWorkerPool(
                            conf.getWorkerPoolCoreSize(),
                            conf.getWorkerPoolMaxSize(),
                            conf.getWorkerThreadKeepaliveSec(),
                            conf.getWorkerPoolQueueLen(),
                            "Pass-through Message Processing Thread Group",
                            "PassThroughMessageProcessor");
        }

        httpParameters = retrieveHttpParameters();
        bufferFactory = new BufferFactory(conf.getIOBufferSize(), new HeapByteBufferAllocator(), 512);
    }

    public IOReactorConfig getReactorConfig() {
        IOReactorConfig config = new IOReactorConfig();
        config.setIoThreadCount(iOThreadsPerReactor);
        config.setSoTimeout(
                conf.getIntProperty(HttpConnectionParams.SO_TIMEOUT, 60000));
        config.setConnectTimeout(
                conf.getIntProperty(HttpConnectionParams.CONNECTION_TIMEOUT, 0));
        config.setRcvBufSize(
                conf.getIntProperty(HttpConnectionParams.SOCKET_BUFFER_SIZE, 8 * 1024));
        config.setSndBufSize(
                conf.getIntProperty(HttpConnectionParams.SOCKET_BUFFER_SIZE, 8 * 1024));
        config.setTcpNoDelay(
                conf.getBooleanProperty(HttpConnectionParams.TCP_NODELAY, true));
        config.setInterestOpQueued(
                conf.getBooleanProperty(NIOReactorParams.INTEREST_OPS_QUEUEING, false));

        if (conf.getIntProperty(HttpConnectionParams.SO_LINGER) != null) {
            config.setSoLinger(conf.getIntProperty(HttpConnectionParams.SO_LINGER));
        }

        if (conf.getBooleanProperty(HttpConnectionParams.SO_REUSEADDR) != null) {
            config.setSoReuseAddress(conf.getBooleanProperty(HttpConnectionParams.SO_REUSEADDR));
        }

        if (conf.getIntProperty(NIOReactorPNames.SELECT_INTERVAL) != null) {
            config.setSelectInterval(conf.getIntProperty(NIOReactorPNames.SELECT_INTERVAL));
        }

        return config;
    }

    public WorkerPool getWorkerPool() {
        return workerPool;
    }

    public ConfigurationContext getConfigurationContext() {
        return configurationContext;
    }

    protected HttpParams retrieveHttpParameters() throws AxisFault {
        HttpParams params = new BasicHttpParams();
        params.
            setIntParameter(HttpConnectionParams.SO_TIMEOUT,
                    conf.getIntProperty(HttpConnectionParams.SO_TIMEOUT, 60000)).
            setIntParameter(HttpConnectionParams.CONNECTION_TIMEOUT,
                    conf.getIntProperty(HttpConnectionParams.CONNECTION_TIMEOUT, 0)).
            setIntParameter(HttpConnectionParams.SOCKET_BUFFER_SIZE,
                    conf.getIntProperty(HttpConnectionParams.SOCKET_BUFFER_SIZE, 8 * 1024)).
            setBooleanParameter(HttpConnectionParams.STALE_CONNECTION_CHECK,
                    conf.getBooleanProperty(HttpConnectionParams.STALE_CONNECTION_CHECK, false)).
            setBooleanParameter(HttpConnectionParams.TCP_NODELAY,
                    conf.getBooleanProperty(HttpConnectionParams.TCP_NODELAY, true)).
            setBooleanParameter(NIOReactorPNames.INTEREST_OPS_QUEUEING,
                    conf.getBooleanProperty(NIOReactorParams.INTEREST_OPS_QUEUEING, false)).
            setParameter(HttpProtocolParams.ORIGIN_SERVER,
                    conf.getStringProperty(HttpProtocolParams.ORIGIN_SERVER, "Synapse-PassThrough-HTTP"));

        /* Set advanced tuning params only if they are explicitly set so that we are not loosing
           internal defaults of HttpCore-NIO */
        if (conf.getIntProperty(HttpConnectionParams.SO_LINGER) != null) {
            HttpConnectionParams.setLinger(params,
                    conf.getIntProperty(HttpConnectionParams.SO_LINGER));
        }

        if (conf.getBooleanProperty(HttpConnectionParams.SO_REUSEADDR) != null) {
            HttpConnectionParams.setSoReuseaddr(params,
                    conf.getBooleanProperty(HttpConnectionParams.SO_REUSEADDR));
        }

        if (conf.getIntProperty(NIOReactorPNames.SELECT_INTERVAL) != null) {
            NIOReactorParams.setSelectInterval(params,
                    conf.getIntProperty(NIOReactorPNames.SELECT_INTERVAL));
        }

        return params;
    }

    public BufferFactory getBufferFactory() {
        return bufferFactory;
    }

    public PassThroughTransportMetricsCollector getMetrics() {
        return metrics;
    }

    public void setMetrics(PassThroughTransportMetricsCollector metrics) {
        this.metrics = metrics;
    }
}
