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

import org.apache.http.HttpRequestFactory;
import org.apache.http.impl.nio.DefaultHttpServerIODispatch;

import org.apache.http.impl.nio.DefaultNHttpServerConnection;
import org.apache.http.impl.nio.DefaultNHttpServerConnectionFactory;
import org.apache.http.nio.NHttpServerEventHandler;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.nio.util.ByteBufferAllocator;
import org.apache.http.params.HttpParams;
import org.apache.synapse.transport.passthru.logging.LoggingUtils;

/**
 * This is a factory for creating the logging sessions or non-logging sessions.
 */
public class SourceIOEventDispatch extends DefaultHttpServerIODispatch {

    public SourceIOEventDispatch(final NHttpServerEventHandler handler, final HttpParams params) {
        super(LoggingUtils.decorate(handler), new SourceConnectionFactory(params));
    }

    private static class SourceConnectionFactory extends DefaultNHttpServerConnectionFactory {

        public SourceConnectionFactory(HttpParams params) {
            super(params);
        }

        @Override
        protected DefaultNHttpServerConnection createConnection(IOSession session,
                                                                HttpRequestFactory requestFactory,
                                                                ByteBufferAllocator allocator,
                                                                HttpParams params) {
            session = LoggingUtils.decorate(session, "server");
            return LoggingUtils.createServerConnection(
                    session,
                    requestFactory,
                    allocator,
                    params);
        }
    }

}
