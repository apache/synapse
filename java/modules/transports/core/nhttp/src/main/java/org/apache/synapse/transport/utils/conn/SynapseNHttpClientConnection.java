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

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.config.MessageConstraints;
import org.apache.http.entity.ContentLengthStrategy;
import org.apache.http.impl.nio.DefaultNHttpClientConnection;
import org.apache.http.nio.NHttpClientEventHandler;
import org.apache.http.nio.NHttpMessageParserFactory;
import org.apache.http.nio.NHttpMessageWriterFactory;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.nio.util.ByteBufferAllocator;
import org.apache.synapse.transport.passthru.TargetHandler;

import java.io.IOException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

/**
 * An extension of the DefaultNHttpClientConnection class, that has some
 * additional stuff related to synapse NHttp transport implementation
 */
public class SynapseNHttpClientConnection extends DefaultNHttpClientConnection {

    private boolean markedForRelease = false;

    public SynapseNHttpClientConnection(IOSession session,
                                        int bufferSize,
                                        int fragmentSizeHint,
                                        ByteBufferAllocator allocator,
                                        CharsetDecoder charDecoder,
                                        CharsetEncoder charEncoder,
                                        MessageConstraints constraints,
                                        ContentLengthStrategy incomingContentStrategy,
                                        ContentLengthStrategy outgoingContentStrategy,
                                        NHttpMessageWriterFactory<HttpRequest> requestWriterFactory,
                                        NHttpMessageParserFactory<HttpResponse> responseParserFactory) {
        super(session, bufferSize, fragmentSizeHint, allocator,
              charDecoder, charEncoder, constraints, incomingContentStrategy,
              outgoingContentStrategy, requestWriterFactory, responseParserFactory);
    }

    @Override
    public void close() throws IOException {
        super.close();
    }

    @Override
    public void shutdown() throws IOException {
        super.shutdown();
    }

    @Override
    public void submitRequest(final HttpRequest request) throws IOException, HttpException {
        super.submitRequest(request);
    }

    @Override
    public void consumeInput(final NHttpClientEventHandler handler) {
        super.consumeInput(handler);
        if (markedForRelease) {
            if (handler instanceof TargetHandler) {
                resetState();
                ((TargetHandler) handler).getTargetConfiguration().getConnections().releaseConnection(this);
            }
        }
    }

    @Override
    public void produceOutput(final NHttpClientEventHandler handler) {
        super.produceOutput(handler);
    }

    /**
     * Mark this connection to be released to the pool.
     * <p>
     * This needs to be called after finishing work related to a particular request/response,
     * and only when keep-alive is enabled
     */
    public void markForRelease() {
        this.markedForRelease = true;
    }

    private void resetState() {
        markedForRelease = false;
    }
}
