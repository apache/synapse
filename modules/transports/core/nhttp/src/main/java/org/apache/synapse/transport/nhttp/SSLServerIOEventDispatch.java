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
import org.apache.http.nio.NHttpServerIOTarget;
import org.apache.http.nio.NHttpServiceHandler;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.params.HttpParams;

public class SSLServerIOEventDispatch 
    extends org.apache.http.impl.nio.SSLServerIOEventDispatch {

    public SSLServerIOEventDispatch(
            final NHttpServiceHandler handler,
            final SSLContext sslcontext,
            final SSLIOSessionHandler sslHandler,
            final HttpParams params) {
        super(LoggingUtils.decorate(handler), sslcontext, sslHandler, params);
    }
    
    public SSLServerIOEventDispatch(
            final NHttpServiceHandler handler,
            final SSLContext sslcontext,
            final HttpParams params) {
        this(handler, sslcontext, null, params);
    }
    
    @Override
    protected NHttpServerIOTarget createConnection(IOSession session) {
        session = LoggingUtils.decorate(session, "sslserver");
        return LoggingUtils.createServerConnection(
                session, 
                createHttpRequestFactory(), 
                createByteBufferAllocator(), 
                this.params);
    }
    
}
