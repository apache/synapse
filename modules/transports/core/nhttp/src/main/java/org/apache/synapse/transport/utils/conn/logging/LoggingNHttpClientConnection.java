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

package org.apache.synapse.transport.utils.conn.logging;

import org.apache.commons.logging.Log;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.config.MessageConstraints;
import org.apache.http.entity.ContentLengthStrategy;
import org.apache.http.nio.NHttpClientEventHandler;
import org.apache.http.nio.NHttpMessageParserFactory;
import org.apache.http.nio.NHttpMessageWriterFactory;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.nio.util.ByteBufferAllocator;
import org.apache.synapse.transport.utils.conn.SynapseNHttpClientConnection;

import java.io.IOException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

/**
 * An extension of the DefaultNHttpClientConnection class, that provides some
 * additional logging features. This implementation enhances the default connection
 * class by logging all the major events that occur on the connection instance.
 */
public class LoggingNHttpClientConnection extends SynapseNHttpClientConnection {

    private final Log log;

    public LoggingNHttpClientConnection(IOSession session,
                                        int bufferSize,
                                        int fragmentSizeHint,
                                        ByteBufferAllocator allocator,
                                        CharsetDecoder charDecoder,
                                        CharsetEncoder charEncoder,
                                        MessageConstraints constraints,
                                        ContentLengthStrategy incomingContentStrategy,
                                        ContentLengthStrategy outgoingContentStrategy,
                                        NHttpMessageWriterFactory<HttpRequest> requestWriterFactory,
                                        NHttpMessageParserFactory<HttpResponse> responseParserFactory,
                                        Log log) {
        super(session, bufferSize, fragmentSizeHint, allocator,
                charDecoder, charEncoder, constraints, incomingContentStrategy,
                outgoingContentStrategy, requestWriterFactory, responseParserFactory);
        this.log = log;
    }

    @Override
    public void close() throws IOException {
        this.log.debug("Close connection");
        super.close();
    }

    @Override
    public void shutdown() throws IOException {
        this.log.debug("Shutdown connection");
        super.shutdown();
    }

    @Override
    public void submitRequest(final HttpRequest request) throws IOException, HttpException {
        if (this.log.isDebugEnabled()) {
            this.log.debug("HTTP connection " + this + ": "  + request.getRequestLine().toString());
        }
        super.submitRequest(request);
    }

    @Override
    public void consumeInput(final NHttpClientEventHandler handler) {
        this.log.debug("Consume input");
        super.consumeInput(handler);
    }

    @Override
    public void produceOutput(final NHttpClientEventHandler handler) {
        this.log.debug("Produce output");
        super.produceOutput(handler);
    }
}
