/*
 *  Copyright 2013 Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.synapse.transport.passthru.logging;

import org.apache.http.nio.reactor.IOSession;
import org.apache.http.nio.NHttpClientHandler;
import org.apache.http.nio.NHttpServiceHandler;
import org.apache.http.nio.NHttpClientIOTarget;
import org.apache.http.nio.NHttpServerIOTarget;
import org.apache.http.nio.util.ByteBufferAllocator;
import org.apache.http.HttpResponseFactory;
import org.apache.http.HttpRequestFactory;
import org.apache.http.impl.nio.DefaultNHttpClientConnection;
import org.apache.http.impl.nio.DefaultNHttpServerConnection;
import org.apache.http.params.HttpParams;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class LoggingUtils {
    public final static String HEADER_LOG_ID = "org.wso2.carbon.transport.passthru.headers";
    public final static String WIRE_LOG_ID = "org.wso2.carbon.transport.passthru.wire";

    public static IOSession decorate(IOSession session, final String id) {
        Log log = LogFactory.getLog(session.getClass());
        Log wirelog = LogFactory.getLog(WIRE_LOG_ID);
        if (wirelog.isDebugEnabled() || log.isDebugEnabled()) {
            session = new LoggingIOSession(wirelog, session, id);
        }
        return session;
    }

    public static NHttpClientHandler decorate(NHttpClientHandler handler) {
        Log log = LogFactory.getLog(handler.getClass());
        if (log.isDebugEnabled()) {
            handler = new LoggingTargetHandler(handler);
        }
        return handler;
    }

    public static NHttpServiceHandler decorate(NHttpServiceHandler handler) {
        Log log = LogFactory.getLog(handler.getClass());
        if (log.isDebugEnabled()) {
            handler = new LoggingSourceHandler(handler);
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
            return new LoggingNHttpTargetConnection(
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
            return new LoggingNHttpSourceConnection(
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
