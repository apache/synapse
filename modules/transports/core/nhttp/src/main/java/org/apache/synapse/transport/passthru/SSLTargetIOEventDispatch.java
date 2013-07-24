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
import org.apache.http.impl.nio.DefaultHttpClientIODispatch;
import org.apache.http.impl.nio.DefaultNHttpClientConnection;
import org.apache.http.impl.nio.SSLNHttpClientConnectionFactory;
import org.apache.http.nio.NHttpClientEventHandler;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.nio.reactor.ssl.SSLSetupHandler;
import org.apache.http.nio.util.ByteBufferAllocator;
import org.apache.http.params.HttpParams;
import org.apache.synapse.transport.passthru.logging.LoggingUtils;

import javax.net.ssl.SSLContext;
import java.util.Map;

public class SSLTargetIOEventDispatch extends DefaultHttpClientIODispatch {

    private Map<String, SSLContext> contextMap;

    public SSLTargetIOEventDispatch(NHttpClientEventHandler handler,
                                    SSLContext sslcontext,
                                    SSLSetupHandler sslHandler,
                                    HttpParams params) {
        super(handler, new SSLTargetConnectionFactory(sslcontext, sslHandler, params));
    }

    public void setContextMap(Map<String,SSLContext> contextMap) {
        this.contextMap = contextMap;
    }

    private static class SSLTargetConnectionFactory extends SSLNHttpClientConnectionFactory {

        public SSLTargetConnectionFactory(SSLContext sslcontext,
                                          SSLSetupHandler sslHandler, HttpParams params) {
            super(sslcontext, sslHandler, params);
        }

        @Override
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
    }

    /*protected SSLIOSession createSSLIOSession(IOSession session,
                                              SSLContext sslcontext,
                                              SSLSetupHandler sslHandler) {

        InetSocketAddress address = (InetSocketAddress) session.getRemoteAddress();
        String host = address.getHostName() + ":" + address.getPort();
        SSLContext customContext = null;
        if (contextMap != null) {
            // See if there's a custom SSL profile configured for this server
            customContext = contextMap.get(host);
        }

        if (customContext == null) {
            customContext = sslcontext;
        }

        return super.createSSLIOSession(session, customContext, sslHandler);
    }*/
}
