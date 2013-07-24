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

import org.apache.http.HttpResponseFactory;
import org.apache.http.impl.nio.DefaultHttpClientIODispatch;
import org.apache.http.impl.nio.DefaultNHttpClientConnection;
import org.apache.http.impl.nio.DefaultNHttpClientConnectionFactory;
import org.apache.http.nio.NHttpClientEventHandler;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.nio.util.ByteBufferAllocator;
import org.apache.http.params.HttpParams;

public class PlainClientIOEventDispatch extends DefaultHttpClientIODispatch {

    public PlainClientIOEventDispatch(final NHttpClientEventHandler handler, final HttpParams params) {
        super(LoggingUtils.decorate(handler), new TargetConnectionFactory(params));
    }

    private static class TargetConnectionFactory extends DefaultNHttpClientConnectionFactory {

        public TargetConnectionFactory(HttpParams params) {
            super(params);
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
