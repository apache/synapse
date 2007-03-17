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

import org.apache.http.impl.DefaultHttpRequestFactory;
import org.apache.http.impl.nio.DefaultNHttpServerConnection;
import org.apache.http.nio.NHttpServiceHandler;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.params.HttpParams;

public class PlainServerIOEventDispatch implements IOEventDispatch {

    private static final String NHTTP_CONN = "SYNAPSE.NHTTP_CONN";
    
    private final NHttpServiceHandler handler;
    private final HttpParams params;
    
    public PlainServerIOEventDispatch(final NHttpServiceHandler handler, final HttpParams params) {
        super();
        if (handler == null) {
            throw new IllegalArgumentException("HTTP service handler may not be null");
        }
        if (params == null) {
            throw new IllegalArgumentException("HTTP parameters may not be null");
        }
        // Decorate service handler with logging capabilities
        this.handler = new LoggingNHttpServiceHandler(handler);
        this.params = params;
    }
    
    public void connected(final IOSession session) {
        // Decorate I/O session with logging capabilities
        DefaultNHttpServerConnection conn = new DefaultNHttpServerConnection(
                new LoggingIOSession(session), 
                new DefaultHttpRequestFactory(),
                this.params); 
        session.setAttribute(NHTTP_CONN, conn);
        this.handler.connected(conn);
    }

    public void disconnected(final IOSession session) {
        DefaultNHttpServerConnection conn = (DefaultNHttpServerConnection) session.getAttribute(
                NHTTP_CONN);
        this.handler.closed(conn);
    }

    public void inputReady(final IOSession session) {
        DefaultNHttpServerConnection conn = (DefaultNHttpServerConnection) session.getAttribute(
                NHTTP_CONN);
        conn.consumeInput(this.handler);
    }

    public void outputReady(final IOSession session) {
        DefaultNHttpServerConnection conn = (DefaultNHttpServerConnection) session.getAttribute(
                NHTTP_CONN);
        conn.produceOutput(this.handler);
    }

    public void timeout(final IOSession session) {
        DefaultNHttpServerConnection conn = (DefaultNHttpServerConnection) session.getAttribute(
                NHTTP_CONN);
        this.handler.timeout(conn);
    }

}
