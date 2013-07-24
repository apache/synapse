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

import javax.net.ssl.SSLContext;

import org.apache.http.HttpResponseFactory;
import org.apache.http.impl.nio.DefaultHttpClientIODispatch;
import org.apache.http.impl.nio.DefaultNHttpClientConnection;
import org.apache.http.impl.nio.SSLNHttpClientConnectionFactory;
import org.apache.http.nio.NHttpClientEventHandler;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.nio.reactor.ssl.SSLSetupHandler;
import org.apache.http.nio.util.ByteBufferAllocator;
import org.apache.http.params.HttpParams;

import java.util.Map;

/**
 * This custom SSLClientIOEventDispatch can keep a map of SSLContexts and use the correct
 * SSLContext when connecting to different servers. If a SSLContext cannot be found for a
 * particular server from the specified map it uses the default SSLContext.
 */
public class SSLClientIOEventDispatch 
    extends DefaultHttpClientIODispatch {

    private Map<String, SSLContext> contextMap;

    public SSLClientIOEventDispatch(
            final NHttpClientEventHandler handler,
            final SSLContext sslcontext,
            final SSLSetupHandler sslHandler,
            final HttpParams params) {
        super(LoggingUtils.decorate(handler), new SSLTargetConnectionFactory(sslcontext, sslHandler, params));
    }

    public void setContextMap(Map<String,SSLContext> contextMap) {
        this.contextMap = contextMap;
    }

//    protected SSLIOSession createSSLIOSession(IOSession ioSession, SSLContext sslContext,
//                                              SSLIOSessionHandler sslioSessionHandler) {
//
//        InetSocketAddress address = (InetSocketAddress) ioSession.getRemoteAddress();
//        String host = address.getHostName() + ":" + address.getPort();
//        SSLContext customContext = null;
//        if (contextMap != null) {
//            // See if there's a custom SSL profile configured for this server
//            customContext = contextMap.get(host);
//        }
//
//        if (customContext == null) {
//            customContext = sslContext;
//        }
//
//        return super.createSSLIOSession(ioSession, customContext, sslioSessionHandler);
//    }

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
            return LoggingUtils.createClientConnection(
                    session,
                    responseFactory,
                    allocator,
                    params);
        }
    }

}
