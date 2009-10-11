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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpRequestFactory;
import org.apache.http.HttpResponseFactory;
import org.apache.http.impl.nio.DefaultNHttpClientConnection;
import org.apache.http.impl.nio.DefaultNHttpServerConnection;
import org.apache.http.nio.NHttpClientHandler;
import org.apache.http.nio.NHttpClientIOTarget;
import org.apache.http.nio.NHttpServerIOTarget;
import org.apache.http.nio.NHttpServiceHandler;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.nio.util.ByteBufferAllocator;
import org.apache.http.params.HttpParams;

class LoggingUtils {
    
    public final static String HEADER_LOG_ID = "org.apache.synapse.transport.nhttp.headers"; 
    public final static String WIRE_LOG_ID = "org.apache.synapse.transport.nhttp.wire"; 

    public static IOSession decorate(IOSession session, final String id) {
        Log log = LogFactory.getLog(session.getClass());
        Log wirelog = LogFactory.getLog(WIRE_LOG_ID);
        if (wirelog.isDebugEnabled() || log.isDebugEnabled()) {
            session = new LoggingIOSession(log, wirelog, session, id);
        }
        return session;
    }
    
    public static NHttpClientHandler decorate(NHttpClientHandler handler) {
        Log log = LogFactory.getLog(handler.getClass());
        if (log.isDebugEnabled()) {
            handler = new LoggingNHttpClientHandler(log, handler);
        }
        return handler;
    }

    public static NHttpServiceHandler decorate(NHttpServiceHandler handler) {
        Log log = LogFactory.getLog(handler.getClass());
        if (log.isDebugEnabled()) {
            handler = new LoggingNHttpServiceHandler(log, handler);
        }
        return handler;
    }

    public static NHttpClientIOTarget createClientConnection(
            final IOSession iosession,
            final HttpResponseFactory responseFactory,
            final ByteBufferAllocator allocator,
            final HttpParams params) {
        Log log = LogFactory.getLog(DefaultNHttpClientConnection.class);
        Log headerlog = LogFactory.getLog(HEADER_LOG_ID);        
        if (headerlog.isDebugEnabled() || log.isDebugEnabled()) {
            return new LoggingNHttpClientConnection(
                    log, 
                    headerlog, 
                    iosession, 
                    responseFactory,
                    allocator,
                    params);
        } else {
            return new DefaultNHttpClientConnection(
                    iosession, 
                    responseFactory,
                    allocator,
                    params);
        }
    }

    public static NHttpServerIOTarget createServerConnection(
            final IOSession iosession,
            final HttpRequestFactory requestFactory,
            final ByteBufferAllocator allocator,
            final HttpParams params) {
        Log log = LogFactory.getLog(DefaultNHttpClientConnection.class);
        Log headerlog = LogFactory.getLog(HEADER_LOG_ID);        
        if (headerlog.isDebugEnabled() || log.isDebugEnabled()) {
            return new LoggingNHttpServerConnection(
                    log, 
                    headerlog, 
                    iosession, 
                    requestFactory,
                    allocator,
                    params);
        } else {
            return new DefaultNHttpServerConnection(
                    iosession, 
                    requestFactory,
                    allocator,
                    params);
        }
    }
    
}