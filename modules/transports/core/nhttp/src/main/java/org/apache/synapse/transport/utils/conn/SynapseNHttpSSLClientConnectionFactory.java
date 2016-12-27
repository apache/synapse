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

package org.apache.synapse.transport.utils.conn;

import org.apache.http.config.ConnectionConfig;
import org.apache.http.impl.nio.DefaultNHttpClientConnection;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.nio.reactor.ssl.SSLIOSession;
import org.apache.http.nio.reactor.ssl.SSLMode;
import org.apache.http.nio.reactor.ssl.SSLSetupHandler;

import javax.net.ssl.SSLContext;
import java.net.InetSocketAddress;
import java.util.Map;

/**
 * The SSL-enabled version of the SynapseNHttpClientConnectionFactory. Identical in behavior
 * to the parent class, but wraps IOSession instances with SSLIOSession instances. This
 * implementation also supports using different SSLContext instances for different target
 * I/O sessions.
 */
public class SynapseNHttpSSLClientConnectionFactory extends SynapseNHttpClientConnectionFactory {

    private SSLContext sslContext;
    private SSLSetupHandler sslSetupHandler;
    private Map<String,SSLContext> customContexts;

    public SynapseNHttpSSLClientConnectionFactory(ConnectionConfig config,
                                                  SSLContext sslContext,
                                                  SSLSetupHandler sslSetupHandler,
                                                  Map<String, SSLContext> customContexts) {
        super(config);
        this.sslContext = sslContext;
        this.sslSetupHandler = sslSetupHandler;
        this.customContexts = customContexts;
    }

    @Override
    public DefaultNHttpClientConnection createConnection(IOSession session) {
        final SSLIOSession ssliosession = new SSLIOSession(
                session,
                SSLMode.CLIENT,
                getSSLContext(session),
                sslSetupHandler);
        session.setAttribute(SSLIOSession.SESSION_KEY, ssliosession);
        return super.createConnection(ssliosession);
    }

    private SSLContext getSSLContext(IOSession session) {
        InetSocketAddress address = (InetSocketAddress) session.getRemoteAddress();
        String host = address.getHostName() + ":" + address.getPort();
        SSLContext customContext = null;
        if (customContexts != null) {
            // See if there's a custom SSL profile configured for this server
            customContext = customContexts.get(host);
        }

        if (customContext == null) {
            customContext = sslContext;
        }
        return customContext;
    }
}
