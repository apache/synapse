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

package org.apache.synapse.transport.utils.logging;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.impl.nio.DefaultHttpClientIODispatch;
import org.apache.http.impl.nio.DefaultHttpServerIODispatch;
import org.apache.http.nio.NHttpClientEventHandler;
import org.apache.http.nio.NHttpServerEventHandler;
import org.apache.http.nio.reactor.ssl.SSLSetupHandler;

import javax.net.ssl.SSLContext;
import java.util.Map;

public class LoggingUtils {

    private static NHttpClientEventHandler decorate(NHttpClientEventHandler handler) {
        Log log = LogFactory.getLog(handler.getClass());
        if (log.isDebugEnabled()) {
            handler = new LoggingClientEventHandler(handler);
        }
        return handler;
    }

    private static NHttpServerEventHandler decorate(NHttpServerEventHandler handler) {
        Log log = LogFactory.getLog(handler.getClass());
        if (log.isDebugEnabled()) {
            handler = new LoggingServerEventHandler(handler);
        }
        return handler;
    }

    public static DefaultHttpServerIODispatch getServerIODispatch(final NHttpServerEventHandler handler,
                                                                  final ConnectionConfig config) {
        return new DefaultHttpServerIODispatch(decorate(handler),
                new LoggingNHttpServerConnectionFactory(config));
    }

    public static DefaultHttpServerIODispatch getServerIODispatch(final NHttpServerEventHandler handler,
                                                                  final ConnectionConfig config,
                                                                  final SSLContext sslContext,
                                                                  final SSLSetupHandler sslSetupHandler) {
        return new DefaultHttpServerIODispatch(decorate(handler),
                new LoggingNHttpSSLServerConnectionFactory(config, sslContext, sslSetupHandler));
    }

    public static DefaultHttpClientIODispatch getClientIODispatch(final NHttpClientEventHandler handler,
                                                                  final ConnectionConfig config) {
        return new DefaultHttpClientIODispatch(decorate(handler),
                new LoggingNHttpClientConnectionFactory(config));
    }

    public static DefaultHttpClientIODispatch getClientIODispatch(final NHttpClientEventHandler handler,
                                                                  final ConnectionConfig config,
                                                                  final SSLContext sslContext,
                                                                  final SSLSetupHandler sslSetupHandler,
                                                                  Map<String, SSLContext> customContexts) {
        return new DefaultHttpClientIODispatch(decorate(handler),
                new LoggingNHttpSSLClientConnectionFactory(config, sslContext, sslSetupHandler, customContexts));
    }
}
