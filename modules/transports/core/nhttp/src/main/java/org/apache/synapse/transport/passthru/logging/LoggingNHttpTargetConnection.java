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

package org.apache.synapse.transport.passthru.logging;

import org.apache.commons.logging.Log;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.nio.reactor.SessionOutputBuffer;
import org.apache.http.nio.reactor.SessionInputBuffer;
import org.apache.http.nio.util.ByteBufferAllocator;
import org.apache.http.nio.NHttpClientHandler;
import org.apache.http.nio.NHttpMessageWriter;
import org.apache.http.nio.NHttpMessageParser;
import org.apache.http.*;
import org.apache.http.impl.nio.DefaultNHttpClientConnection;
import org.apache.http.params.HttpParams;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;

public class LoggingNHttpTargetConnection extends DefaultNHttpClientConnection {
    private final Log log;
    private final Log headerLog;

    public LoggingNHttpTargetConnection(
            final Log log,
            final Log headerlog,
            final IOSession iosession,
            final HttpResponseFactory responseFactory,
            final ByteBufferAllocator allocator,
            final HttpParams params) {
        super(iosession, responseFactory, allocator, params);
        this.log = log;
        this.headerLog = headerlog;
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
    public void consumeInput(final NHttpClientHandler handler) {
        this.log.debug("Consume input");
        super.consumeInput(handler);
    }

    @Override
    public void produceOutput(final NHttpClientHandler handler) {
        this.log.debug("Produce output");
        super.produceOutput(handler);
    }

    @Override
    protected NHttpMessageWriter createRequestWriter(
            final SessionOutputBuffer buffer,
            final HttpParams params) {
        return new LoggingNHttpMessageWriter(
                super.createRequestWriter(buffer, params));
    }

    @Override
    protected NHttpMessageParser createResponseParser(
            final SessionInputBuffer buffer,
            final HttpResponseFactory responseFactory,
            final HttpParams params) {
        return new LoggingNHttpMessageParser(
                super.createResponseParser(buffer, responseFactory, params));
    }

    class LoggingNHttpMessageWriter implements NHttpMessageWriter {

        private final NHttpMessageWriter writer;

        public LoggingNHttpMessageWriter(final NHttpMessageWriter writer) {
            super();
            this.writer = writer;
        }

        public void reset() {
            this.writer.reset();
        }

        public void write(final HttpMessage message) throws IOException, HttpException {
            if (message != null && headerLog.isDebugEnabled()) {
                HttpRequest request = (HttpRequest) message;
                headerLog.debug(">> " + request.getRequestLine().toString());
                Header[] headers = request.getAllHeaders();
                for (Header header : headers) {
                    headerLog.debug(">> " + header.toString());
                }
            }
            this.writer.write(message);
        }

    }

    class LoggingNHttpMessageParser implements NHttpMessageParser {

        private final NHttpMessageParser parser;

        public LoggingNHttpMessageParser(final NHttpMessageParser parser) {
            super();
            this.parser = parser;
        }

        public void reset() {
            this.parser.reset();
        }

        public int fillBuffer(final ReadableByteChannel channel) throws IOException {
            return this.parser.fillBuffer(channel);
        }

        public HttpMessage parse() throws IOException, HttpException {
            HttpMessage message = this.parser.parse();
            if (message != null && headerLog.isDebugEnabled()) {
                HttpResponse response = (HttpResponse) message;
                headerLog.debug("<< " + response.getStatusLine().toString());
                Header[] headers = response.getAllHeaders();
                for (Header header : headers) {
                    headerLog.debug("<< " + header.toString());
                }
            }
            return message;
        }

    }
}
