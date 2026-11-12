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

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.config.MessageConstraints;
import org.apache.http.entity.ContentLengthStrategy;
import org.apache.http.impl.nio.DefaultNHttpServerConnection;
import org.apache.http.nio.NHttpMessageParserFactory;
import org.apache.http.nio.NHttpMessageWriterFactory;
import org.apache.http.nio.NHttpServerEventHandler;
import org.apache.http.nio.reactor.EventMask;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.nio.util.ByteBufferAllocator;

import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

/**
 * Restores the {@link EventMask#WRITE} clearing that httpcore 4.4 dropped from
 * {@code DefaultNHttpServerConnection.produceOutput} (kept armed for pipelining, which the
 * nhttp listener does not use). Left armed, the dispatcher thread re-enters {@code produceOutput}
 * even when idle and can race a worker thread's {@code commitResponse} on the same
 * non-thread-safe session output buffer, corrupting its position/limit state.
 * <p>
 * Mirrors the client-side fix in {@link SynapseNHttpClientConnection}.
 */
public class SynapseNHttpServerConnection extends DefaultNHttpServerConnection {

    public SynapseNHttpServerConnection(IOSession session,
                                        int bufferSize,
                                        int fragmentSizeHint,
                                        ByteBufferAllocator allocator,
                                        CharsetDecoder charDecoder,
                                        CharsetEncoder charEncoder,
                                        MessageConstraints constraints,
                                        ContentLengthStrategy incomingContentStrategy,
                                        ContentLengthStrategy outgoingContentStrategy,
                                        NHttpMessageParserFactory<HttpRequest> requestParserFactory,
                                        NHttpMessageWriterFactory<HttpResponse> responseWriterFactory) {
        super(session, bufferSize, fragmentSizeHint, allocator,
                charDecoder, charEncoder, constraints, incomingContentStrategy,
                outgoingContentStrategy, requestParserFactory, responseWriterFactory);
    }

    @Override
    public void produceOutput(final NHttpServerEventHandler handler) {
        super.produceOutput(handler);
        if (!this.outbuf.hasData() && this.contentEncoder == null && this.status != CLOSED) {
            this.session.clearEvent(EventMask.WRITE);
        }
    }

}
