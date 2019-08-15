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

import org.apache.http.nio.NHttpClientConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This stores connections for a particular host + port.
 */
public class HostConnections {

    private static final Log log = LogFactory.getLog(HostConnections.class);
    // host
    private String host;
    // port
    private int port;
    // maximum number of connections allowed for this host + port
    private int maxSize;
    // number of awaiting connections
    private int pendingConnections;
    // list of free connections available
    private List<NHttpClientConnection> freeConnections = new ArrayList<NHttpClientConnection>();
    // list of connections in use
    private List<NHttpClientConnection> busyConnections = new ArrayList<NHttpClientConnection>();

    private Lock lock = new ReentrantLock();

    public HostConnections(String host, int port, int maxSize) {
        if (log.isDebugEnabled()) {
            log.debug("Creating new connection pool to the host: " + host + ", port: " + port);
        }
        this.host = host;
        this.port = port;
        this.maxSize = maxSize;
    }

    /**
     * Get a connection for the host:port
     *
     * @return a connection
     */
    public NHttpClientConnection getConnection() {
        lock.lock();
        try {
            if (freeConnections.size() > 0) {
                if (log.isDebugEnabled()) {
                    log.debug("Returning an existing free connection to " + host + ":" + port);
                }
                NHttpClientConnection conn = freeConnections.get(0);
                freeConnections.remove(conn);
                busyConnections.add(conn);
                return conn;
            }
        } finally {
            lock.unlock();
        }
        return null;
    }

    public void release(NHttpClientConnection conn) {
        conn.getMetrics().reset();
        HttpContext ctx = conn.getContext();
        ctx.removeAttribute(HttpCoreContext.HTTP_REQUEST);
        ctx.removeAttribute(HttpCoreContext.HTTP_RESPONSE);

        lock.lock();
        try {
            if (busyConnections.remove(conn)) {
                freeConnections.add(conn);
            } else {
                log.error("Attempted to releaseConnection connection not in the busy list");
            }
        } finally {
            lock.unlock();
        }
    }

    public void forget(NHttpClientConnection conn) {
        lock.lock();
        try {
            if (!freeConnections.remove(conn)) {
                busyConnections.remove(conn);
            }
        } finally {
            lock.unlock();
        }
    }

    public void addConnection(NHttpClientConnection conn) {
        if (log.isDebugEnabled()) {
            log.debug("New connection to " + host + ":" + port + " is added to the free list");
        }
        lock.lock();
        try {
            // Adding to busyConnections to make sure the first requester get it.
            // Otherwise someone else might acquire it.
            busyConnections.add(conn);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Indicates that a connection has been successfully established with a remote server
     * as notified by the session request call back.
     */
    public synchronized void pendingConnectionSucceeded() {
        lock.lock();
        try {
            pendingConnections--;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Keep track of the number of times connections to this host:port has failed
     * consecutively
     */
    public void pendingConnectionFailed() {
        lock.lock();
        try {
            pendingConnections--;
        } finally {
            lock.unlock();
        }
    }    

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public boolean canHaveMoreConnections() {
        return busyConnections.size() + pendingConnections < maxSize;
    }
}
