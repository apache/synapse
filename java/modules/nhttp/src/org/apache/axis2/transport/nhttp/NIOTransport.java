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

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.TransportType;
import org.apache.mina.common.ExceptionMonitor;
import org.apache.mina.io.IoFilter;
import org.apache.mina.io.filter.IoLoggingFilter;
import org.apache.mina.io.filter.IoThreadPoolFilter;
import org.apache.mina.io.filter.SSLFilter;
import org.apache.mina.io.socket.SocketAcceptor;
import org.apache.mina.registry.Service;
import org.apache.mina.registry.ServiceRegistry;
import org.apache.mina.registry.SimpleServiceRegistry;
import org.safehaus.asyncweb.container.ServiceContainer;
import org.safehaus.asyncweb.transport.Transport;
import org.safehaus.asyncweb.transport.TransportException;
import org.safehaus.asyncweb.transport.nio.HttpIOHandler;


/**
 * A <code>Transport</code> implementation which receives requests and sends
 * responses using non-blocking selector based IO.
 *
 */
public class NIOTransport implements Transport {

    private static final Log LOG = LogFactory.getLog(NIOTransport.class);

    private static final String SERVICE_NAME = "HTTP_NIO_TRANSPORT";
    private static final int DEFAULT_PORT = 9012;
    private static final int DEFAULT_IO_WORKERS = 2;

    private ServiceRegistry registry;
    private int port = DEFAULT_PORT;
    private int ioWorkerCount = DEFAULT_IO_WORKERS;
    private HttpIOHandler httpIOHandler;
    private boolean isLoggingTraffic = false;
    private ServiceContainer container;

    /**
     * Sets the port this transport will listen on
     *
     * @param port The port
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Sets the number of worker threads employed by this transport.
     * This should typically be a small number (2 is a good choice) -
     * and is not tied to the number of concurrent connections you wish to
     * support
     *
     * @param ioWorkerCount The number of worker threads to employ
     */
    public void setIoWorkerCount(int ioWorkerCount) {
        this.ioWorkerCount = ioWorkerCount;
    }

    /**
     * Sets whether traffic received through this transport is
     * logged (off by default)
     *
     * @param isLoggingTraffic <code>true</code> iff traffic should be logged
     */
    public void setIsLoggingTraffic(boolean isLoggingTraffic) {
        this.isLoggingTraffic = isLoggingTraffic;
    }

    /**
     * Sets the <code>ServiceContainer</code> to which we issue requests
     *
     * @param container Our associated <code>ServiceContainer</code>
     */
    public void setServiceContainer(ServiceContainer container) {
        this.container = container;
    }

    /**
     * Sets the <code>HttpIOHandler</code> to be employed by this transport
     *
     * @param httpIOHandler The handler to be employed by this transport
     */
    public void setHttpIOHandler(HttpIOHandler httpIOHandler) {
        this.httpIOHandler = httpIOHandler;
    }

    /**
     * Starts this transport
     *
     * @throws TransportException If the transport can not be started
     */
    public void start() throws TransportException {
        initIOHandler();
        registry = new SimpleServiceRegistry();

        try {
            Service service = new Service(SERVICE_NAME, TransportType.SOCKET, port);
            registry.bind(service, httpIOHandler);
            SocketAcceptor acceptor = (SocketAcceptor) registry.getIoAcceptor(TransportType.SOCKET);
            configureFilters(acceptor);
            acceptor.setExceptionMonitor(new LoggingExceptionMonitor());
            LOG.info("NIO HTTP Transport bound on port " + port);
        } catch (IOException e) {
            throw new TransportException("NIOTransport Failed to bind to port " + port, e);
        }
    }

    /**
     * Stops this transport
     */
    public void stop() throws TransportException {
        registry.unbindAll();
    }

    /**
     * @return A string representation of this transport
     */
    public String toString() {
        return "NIOTransport [port=" + port + "]";
    }

    /**
     * Configures the filters to be employed for a given acceptor
     *
     * @param acceptor The acceptor
     */
    private void configureFilters(SocketAcceptor acceptor) {
        LOG.info("Configuring " + ioWorkerCount + " IO workers");
        IoThreadPoolFilter threadPoolFilter = (IoThreadPoolFilter) acceptor.getFilterChain().getChild("threadPool");
        threadPoolFilter.setMaximumPoolSize(ioWorkerCount);
        if (isLoggingTraffic) {
            LOG.info("Configuring traffic logging filter");
            IoFilter filter = new IoLoggingFilter();
            acceptor.getFilterChain().addLast("LoggingFilter", filter);
        }

        //acceptor.getFilterChain().addLast("ThreadPoolFilter",
        //new IoThreadPoolFilter("NIO-ThreadPool"));

        //acceptor.getFilterChain().addLast("SSL",
        //    new SSLFilter(new ()));
    }

    /**
     * Initialises our handler - creating a new (default) handler if none has
     * been specified
     *
     * @throws IllegalStateException If we have not yet been associated with a
     *                               container
     */
    private void initIOHandler() {
        if (httpIOHandler == null) {
            LOG.info("No http IO Handler associated - using defaults");
            httpIOHandler = new HttpIOHandler();
        }
        if (container == null) {
            throw new IllegalStateException("Transport not associated with a container");
        }
        httpIOHandler.setContainer(container);
    }

    class LoggingExceptionMonitor implements ExceptionMonitor {

        public void exceptionCaught(Object source, Throwable e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("NIOTransport encountered exception on source: " + source, e);
            }
        }
    }

}
