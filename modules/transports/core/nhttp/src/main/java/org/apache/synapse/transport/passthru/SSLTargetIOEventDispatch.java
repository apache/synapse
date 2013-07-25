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

package org.apache.synapse.transport.passthru;

import org.apache.http.HttpResponseFactory;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.nio.DefaultHttpClientIODispatch;
import org.apache.http.impl.nio.DefaultNHttpClientConnection;
import org.apache.http.nio.NHttpClientEventHandler;
import org.apache.http.nio.NHttpConnectionFactory;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.nio.reactor.ssl.SSLIOSession;
import org.apache.http.nio.reactor.ssl.SSLMode;
import org.apache.http.nio.reactor.ssl.SSLSetupHandler;
import org.apache.http.nio.util.ByteBufferAllocator;
import org.apache.http.nio.util.HeapByteBufferAllocator;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.synapse.transport.passthru.logging.LoggingUtils;

import javax.net.ssl.SSLContext;
import java.net.InetSocketAddress;
import java.util.Map;

public class SSLTargetIOEventDispatch extends DefaultHttpClientIODispatch {

    public SSLTargetIOEventDispatch(NHttpClientEventHandler handler,
                                    SSLContext sslcontext,
                                    Map<String,SSLContext> customContexts,
                                    SSLSetupHandler sslHandler,
                                    HttpParams params) {
        super(handler,
                new SSLTargetConnectionFactory(sslcontext, customContexts, sslHandler, params));
    }

    /**
     * Custom NHttpClientConnectionFactory implementation. Most of this code has been borrowed
     * from the SSLNHttpClientConnectionFactory class of HttpCore-NIO. This custom implementation
     * allows using different SSLContext instances for different target endpoints (custom SSL
     * profiles feature). Hopefully a future HttpCore-NIO API will provide an easier way to
     * customize the way SSLIOSession instances are created and we will be able to get rid of this.
     */
    private static class SSLTargetConnectionFactory
            implements NHttpConnectionFactory<DefaultNHttpClientConnection> {

        private final HttpResponseFactory responseFactory;
        private final ByteBufferAllocator allocator;
        private final SSLContext sslcontext;
        private final SSLSetupHandler sslHandler;
        private final HttpParams params;
        private final Map<String,SSLContext> contextMap;

        public SSLTargetConnectionFactory(
                final SSLContext sslcontext,
                final Map<String,SSLContext> contextMap,
                final SSLSetupHandler sslHandler,
                final HttpParams params) {

            if (params == null) {
                throw new IllegalArgumentException("HTTP parameters may not be null");
            }
            this.sslcontext = sslcontext;
            this.contextMap = contextMap;
            this.sslHandler = sslHandler;
            this.responseFactory = new DefaultHttpResponseFactory();
            this.allocator = new HeapByteBufferAllocator();
            this.params = params;
        }

        protected DefaultNHttpClientConnection createConnection(IOSession session,
                                                                HttpResponseFactory responseFactory,
                                                                ByteBufferAllocator allocator,
                                                                HttpParams params) {
            session = LoggingUtils.decorate(session, "sslclient");
            return LoggingUtils.createClientConnection(
                    session,
                    responseFactory,
                    allocator,
                    params);
        }

        private SSLContext getDefaultSSLContext() {
            SSLContext sslcontext;
            try {
                sslcontext = SSLContext.getInstance("TLS");
                sslcontext.init(null, null, null);
            } catch (Exception ex) {
                throw new IllegalStateException("Failure initializing default SSL context", ex);
            }
            return sslcontext;
        }

        private SSLContext getSSLContext(IOSession session) {
            InetSocketAddress address = (InetSocketAddress) session.getRemoteAddress();
            String host = address.getHostName() + ":" + address.getPort();
            SSLContext customContext = null;
            if (contextMap != null) {
                // See if there's a custom SSL profile configured for this server
                customContext = contextMap.get(host);
            }

            if (customContext == null) {
                customContext = this.sslcontext != null ? this.sslcontext : getDefaultSSLContext();
            }
            return customContext;
        }

        public DefaultNHttpClientConnection createConnection(final IOSession session) {
            SSLContext sslcontext = getSSLContext(session);
            SSLIOSession ssliosession = new SSLIOSession(session, SSLMode.CLIENT,
                    sslcontext, this.sslHandler);
            session.setAttribute(SSLIOSession.SESSION_KEY, ssliosession);
            DefaultNHttpClientConnection conn = createConnection(
                    ssliosession, this.responseFactory, this.allocator, this.params);
            int timeout = HttpConnectionParams.getSoTimeout(this.params);
            conn.setSocketTimeout(timeout);
            return conn;
        }
    }
}
