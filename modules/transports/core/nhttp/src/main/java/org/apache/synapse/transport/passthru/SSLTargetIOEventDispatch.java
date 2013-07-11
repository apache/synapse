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

package org.apache.synapse.transport.passthru;

import org.apache.http.impl.nio.reactor.SSLIOSession;
import org.apache.http.impl.nio.reactor.SSLSetupHandler;
import org.apache.http.nio.NHttpClientHandler;
import org.apache.http.nio.NHttpClientIOTarget;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.params.HttpParams;
import org.apache.synapse.transport.passthru.logging.LoggingUtils;

import javax.net.ssl.SSLContext;
import java.net.InetSocketAddress;
import java.util.Map;

public class SSLTargetIOEventDispatch extends org.apache.http.impl.nio.ssl.SSLClientIOEventDispatch {

    private Map<String, SSLContext> contextMap;

    private HttpParams params = null;

    public SSLTargetIOEventDispatch(NHttpClientHandler handler,
                                    SSLContext sslcontext,
                                    SSLSetupHandler sslHandler,
                                    HttpParams params) {
        super(handler, sslcontext, sslHandler, params);
        this.params = params;
    }

    public void setContextMap(Map<String,SSLContext> contextMap) {
        this.contextMap = contextMap;
    }

    @Override
    protected SSLIOSession createSSLIOSession(IOSession session,
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
    }

    @Override
    protected NHttpClientIOTarget createConnection(IOSession session) {
        session = LoggingUtils.decorate(session, "sslclient");
        return LoggingUtils.createClientConnection(
                session,
                createHttpResponseFactory(),
                createByteBufferAllocator(),
                params);
    }
}
