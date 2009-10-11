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

import org.apache.http.impl.nio.reactor.SSLIOSessionHandler;
import org.apache.http.impl.nio.reactor.SSLIOSession;
import org.apache.http.nio.NHttpClientHandler;
import org.apache.http.nio.NHttpClientIOTarget;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.params.HttpParams;

import java.net.InetSocketAddress;
import java.util.Map;

/**
 * This custom SSLClientIOEventDispatch can keep a map of SSLContexts and use the correct
 * SSLContext when connecting to different servers. If a SSLContext cannot be found for a
 * particular server from the specified map it uses the default SSLContext.
 */
public class SSLClientIOEventDispatch 
    extends org.apache.http.impl.nio.SSLClientIOEventDispatch {

    private Map<String, SSLContext> contextMap;

    public SSLClientIOEventDispatch(
            final NHttpClientHandler handler,
            final SSLContext sslcontext,
            final SSLIOSessionHandler sslHandler,
            final HttpParams params) {
        super(LoggingUtils.decorate(handler), sslcontext, sslHandler, params);
    }

    public void setContextMap(Map<String,SSLContext> contextMap) {
        this.contextMap = contextMap;
    }

    protected SSLIOSession createSSLIOSession(IOSession ioSession, SSLContext sslContext,
                                              SSLIOSessionHandler sslioSessionHandler) {

        InetSocketAddress address = (InetSocketAddress) ioSession.getRemoteAddress();
        String host = address.getHostName() + ":" + address.getPort();
        SSLContext customContext = null;
        if (contextMap != null) {
            // See if there's a custom SSL profile configured for this server
            customContext = contextMap.get(host);
        }

        if (customContext == null) {
            customContext = sslContext;
        }
        
        return super.createSSLIOSession(ioSession, customContext, sslioSessionHandler);
    }

    protected NHttpClientIOTarget createConnection(IOSession session) {
        session = LoggingUtils.decorate(session, "sslclient");
        return LoggingUtils.createClientConnection(
                session, 
                createHttpResponseFactory(), 
                createByteBufferAllocator(), 
                this.params);
    }

}
