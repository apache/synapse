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

import org.apache.http.nio.NHttpClientConnection;
import org.apache.http.protocol.HttpContext;
import org.apache.http.HttpHost;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.protocol.HttpCoreContext;

import java.util.*;
import java.io.IOException;

public class ConnectionPool {

    private static final Log log = LogFactory.getLog(ConnectionPool.class);

    /** A map of available connections for reuse. The key selects the host+port of the
     * connection and the value contains a List of available connections to destination
     */
    private static Map<String,List<NHttpClientConnection>> connMap =
            Collections.synchronizedMap(new HashMap<String,List<NHttpClientConnection>>());

    public static NHttpClientConnection getConnection(String host, int port) {

        String key = host + ":" + Integer.toString(port);
        List<NHttpClientConnection> connections = connMap.get(key);

        if (connections == null || connections.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("No connections available for reuse");
            }
            return null;

        } else {
            NHttpClientConnection conn;
            synchronized (connections) {
                while (!connections.isEmpty()) {
                    conn = connections.remove(0);

                    if (conn.isOpen() && !conn.isStale()) {
                        if (log.isDebugEnabled()) {
                            log.debug("A connection to host : " + host + " on port : " +
                                port + " is available in the pool, and will be reused");
                        }
                        conn.requestInput(); // asankha - make sure keep alives work properly when reused with throttling
                        return conn;
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("closing stale connection to : " + host + ":" + port);
                        }
                        try {
                            conn.close();
                        } catch (IOException ignore) {
                        }
                    }
                }
            }
            return null;
        }
    }

    public static void release(NHttpClientConnection conn) {

        HttpHost host = (HttpHost) conn.getContext().getAttribute(
            HttpCoreContext.HTTP_TARGET_HOST);
        String key = host.getHostName() + ":" + Integer.toString(host.getPort());

        List<NHttpClientConnection> connections = connMap.get(key);
        if (connections == null) {
            synchronized(connMap) {
                // use double locking to make sure
                connections = connMap.get(key);
                if (connections == null) {
                    connections = Collections.synchronizedList(new LinkedList<NHttpClientConnection>());
                    connMap.put(key, connections);
                }
            }
        }

        cleanConnectionReferences(conn);
        connections.add(conn);

        if (log.isDebugEnabled()) {
            log.debug("Released a connection to host: " + host.getHostName() + " on port : " +
                    host.getPort() + " to the connection pool of current size : " + connections.size());
        }
    }

    private static void cleanConnectionReferences(NHttpClientConnection conn) {

        HttpContext ctx = conn.getContext();        
        Axis2HttpRequest axis2Req =
            (Axis2HttpRequest) ctx.getAttribute(ClientHandler.AXIS2_HTTP_REQUEST);
        axis2Req.clear();   // this is linked via the selection key attachment and will free itself
                            // on timeout of the keep alive connection. Till then minimize the
                            // memory usage to a few bytes 

        ctx.removeAttribute(ClientHandler.AXIS2_HTTP_REQUEST);
        ctx.removeAttribute(ClientHandler.OUTGOING_MESSAGE_CONTEXT);
        ctx.removeAttribute(ClientHandler.REQUEST_SOURCE_BUFFER);
        ctx.removeAttribute(ClientHandler.RESPONSE_SINK_BUFFER);

        ctx.removeAttribute(HttpCoreContext.HTTP_REQUEST);
        ctx.removeAttribute(HttpCoreContext.HTTP_RESPONSE);

        conn.resetOutput();
    }

    public static void forget(NHttpClientConnection conn) {

        HttpHost host = (HttpHost) conn.getContext().getAttribute(
            HttpCoreContext.HTTP_TARGET_HOST);
        String key = host.getHostName() + ":" + Integer.toString(host.getPort());

        List<NHttpClientConnection> connections = connMap.get(key);
        if (connections != null) {
            synchronized(connections) {
                connections.remove(conn);
            }
        }
    }
}
