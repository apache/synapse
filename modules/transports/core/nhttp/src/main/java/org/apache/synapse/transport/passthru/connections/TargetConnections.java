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

package org.apache.synapse.transport.passthru.connections;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.nio.NHttpClientConnection;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.synapse.transport.passthru.ConnectCallback;
import org.apache.synapse.transport.passthru.PassThroughConstants;
import org.apache.synapse.transport.passthru.TargetContext;
import org.apache.synapse.transport.passthru.config.TargetConfiguration;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.net.InetSocketAddress;

/**
 * Manages the connection from transport to the back end servers. It keeps track of the
 * connections for host:port pair. 
 */
public class TargetConnections {

    private static final Log log = LogFactory.getLog(TargetConnections.class);

    /** map to hold the ConnectionPools. The key is host:port */
    private final Map<String, HostConnections> poolMap =
            new ConcurrentHashMap<String, HostConnections>();

    /** max connections per host:port pair. At the moment all the host:ports can
     * have the same max */
    private int maxConnections;

    /** io-reactor to use for creating connections */
    private ConnectingIOReactor ioReactor;

    /** callback invoked when a connection is made */
    private ConnectCallback callback = null;

    /**
     * Create a TargetConnections with the given IO-Reactor
     *
     * @param ioReactor the IO-Reactor
     * @param targetConfiguration the configuration of the sender
     * @param callback the callback
     */
    public TargetConnections(ConnectingIOReactor ioReactor,
                             TargetConfiguration targetConfiguration,
                             ConnectCallback callback) {

        this.maxConnections = targetConfiguration.getMaxConnections();
        this.ioReactor = ioReactor;
        this.callback = callback;
    }

    /**
     * Return a connection to the host:port pair. If a connection is not available
     * return <code>null</code>. If the particular host:port allows to create more connections
     * this method will try to connect asynchronously. If the connection is successful it will
     * be notified in a separate thread.
     *
     * @param host host
     * @param port port
     * @return Either returns a connection if already available or returns null and notifies
     *         the delivery agent when the connection is available
     */
    public NHttpClientConnection getConnection(String host, int port) {
        if (log.isDebugEnabled()) {
            log.debug("Trying to get a connection to the " + host + ":" + port);
        }

        HostConnections pool = getConnectionPool(host, port);

        // trying to get an existing connection
        NHttpClientConnection connection = pool.getConnection();
        if (connection == null) {
            if (pool.canHaveMoreConnections()) {
                ioReactor.connect(new InetSocketAddress(host, port), null, pool, callback);
            } else {
                log.warn("Connection pool reached maximum allowed connections for: "
                        + host + ":" + port + ". Target server may have become slow");
            }
        } else {
            return connection;
        }
        return null;
    }

    /**
     * Return an existing connection to the host:port pair from connection pool.
     * If a connection is not available, return <code>null</code>
     *
     * @param host host
     * @param port port
     * @return returns a connection if already available in the pool
     */
    public NHttpClientConnection getExistingConnection(String host, int port) {
        if (log.isDebugEnabled()) {
            log.debug("Trying to get an existing connection to the " + host + ":" + port);
        }

        HostConnections pool = getConnectionPool(host, port);
        return pool.getConnection();
    }

    /**
     * This connection is no longer needed. So we need to close connection.
     *
     * @param conn connection to shutdownConnection
     */
    public void closeConnection(NHttpClientConnection conn) {
        HostConnections pool = (HostConnections) conn.getContext().getAttribute(
                PassThroughConstants.CONNECTION_POOL);

        TargetContext.get(conn).reset();

        if (pool != null) {
            pool.forget(conn);
        } else {
            // we shouldn't get here
            log.fatal("Connection without a pool. Something wrong. Need to fix.");
        }

        try {
            conn.close();
        } catch (IOException ignored) {
        }
    }

    /**
     * This connection is no longer valid. So we need to shutdown connection.
     *
     * @param conn connection to shutdownConnection
     */
    public void shutdownConnection(NHttpClientConnection conn) {
        shutdownConnection(conn, false);
    }

    /**
     * This connection is no longer valid. So we need to shutdown connection.
     *
     * @param conn connection to shutdownConnection
     * @param isError whether an error is causing this shutdown of the connection.
     *                It is very important to set this flag correctly.
     *                When an error causing the shutdown of the connections we should not
     *                release associated writer buffer to the pool as it might lead into
     *                situations like same buffer is getting released to both source and target
     *                buffer factories
     */
    public void shutdownConnection(NHttpClientConnection conn, boolean isError) {
        HostConnections pool = (HostConnections) conn.getContext().getAttribute(
                PassThroughConstants.CONNECTION_POOL);

        TargetContext.get(conn).reset(isError);

        if (pool != null) {
            pool.forget(conn);
        } else {
            // we shouldn't get here
            log.fatal("Connection without a pool. Something wrong. Need to fix.");
        }

        try {
            conn.shutdown();
        } catch (IOException ignored) {
        }
    }

    /**
     * Release an active connection to the pool
     *
     * @param conn connection to be released
     */
    public void releaseConnection(NHttpClientConnection conn) {
        HostConnections pool = (HostConnections) conn.getContext().getAttribute(
                PassThroughConstants.CONNECTION_POOL);

        TargetContext.get(conn).reset();

        if (pool != null) {
            pool.release(conn);
        } else {
            // we shouldn't get here
            log.fatal("Connection without a pool. Something wrong. Need to fix.");
        }
    }

    /**
     * This method is called when a new connection is made.
     *
     * @param conn connection to the target server     
     */
    public void addConnection(NHttpClientConnection conn) {
        HostConnections pool = (HostConnections) conn.getContext().getAttribute(
                PassThroughConstants.CONNECTION_POOL);
        if (pool != null) {
            pool.addConnection(conn);
        } else {
            // we shouldn't get here
            log.fatal("Connection without a pool. Something wrong. Need to fix.");            
        }
    }

    private HostConnections getConnectionPool(String host, int port) {
        String key = host + ":" + port;
        if (!poolMap.containsKey(key)) {
            synchronized (poolMap) {
                // see weather a pool already exists for this host:port
                if (!poolMap.containsKey(key)) {
                    HostConnections pool = new HostConnections(host, port, maxConnections);
                    poolMap.put(key, pool);
                }
            }
        }
        return poolMap.get(key);
    }

}
