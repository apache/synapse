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

import org.apache.http.impl.nio.DefaultNHttpServerConnection;
import org.apache.http.nio.NHttpServerEventHandler;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.nio.reactor.SessionOutputBuffer;
import org.apache.http.nio.reactor.SessionInputBuffer;
import org.apache.http.nio.util.ByteBufferAllocator;
import org.apache.http.nio.NHttpMessageWriter;
import org.apache.http.nio.NHttpMessageParser;
import org.apache.http.*;
import org.apache.http.params.HttpParams;
import org.apache.commons.logging.Log;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;

public class LoggingNHttpSourceConnection extends DefaultNHttpServerConnection {
    private final Log log;
    private final Log headerLog;

    public LoggingNHttpSourceConnection(
            final Log log,
            final Log headerLog,
            final IOSession session,
            final HttpRequestFactory requestFactory,
            final ByteBufferAllocator allocator,
            final HttpParams params) {
        super(session, requestFactory, allocator, params);
        this.log = log;
        this.headerLog = headerLog;
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
    public void submitResponse(final HttpResponse response) throws IOException, HttpException {
        if (this.log.isDebugEnabled()) {
            this.log.debug("HTTP connection " + this + ": "  + response.getStatusLine().toString());
        }
        super.submitResponse(response);
    }

    @Override
    public void consumeInput(final NHttpServerEventHandler handler) {
        this.log.debug("Consume input");
        super.consumeInput(handler);
    }

    @Override
    public void produceOutput(final NHttpServerEventHandler handler) {
        this.log.debug("Produce output");
        super.produceOutput(handler);
    }

    @Override
    protected NHttpMessageWriter<HttpResponse> createResponseWriter(
            final SessionOutputBuffer buffer,
            final HttpParams params) {
        return new LoggingNHttpMessageWriter(
                super.createResponseWriter(buffer, params));
    }

    @Override
    protected NHttpMessageParser<HttpRequest> createRequestParser(
            final SessionInputBuffer buffer,
            final HttpRequestFactory requestFactory,
            final HttpParams params) {
        return new LoggingNHttpMessageParser(
                super.createRequestParser(buffer, requestFactory, params));
    }

    class LoggingNHttpMessageWriter implements NHttpMessageWriter<HttpResponse> {

        private final NHttpMessageWriter<HttpResponse> writer;

        public LoggingNHttpMessageWriter(final NHttpMessageWriter<HttpResponse> writer) {
            super();
            this.writer = writer;
        }

        public void reset() {
            this.writer.reset();
        }

        public void write(final HttpResponse response) throws IOException, HttpException {
            if (response != null && headerLog.isDebugEnabled()) {
                headerLog.debug("<< " + response.getStatusLine().toString());
                Header[] headers = response.getAllHeaders();
                for (Header header : headers) {
                    headerLog.debug("<< " + header.toString());
                }
            }
            this.writer.write(response);
        }

    }

    class LoggingNHttpMessageParser implements NHttpMessageParser<HttpRequest> {

        private final NHttpMessageParser<HttpRequest> parser;

        public LoggingNHttpMessageParser(final NHttpMessageParser<HttpRequest> parser) {
            super();
            this.parser = parser;
        }

        public void reset() {
            this.parser.reset();
        }

        public int fillBuffer(final ReadableByteChannel channel) throws IOException {
            return this.parser.fillBuffer(channel);
        }

        public HttpRequest parse() throws IOException, HttpException {
            HttpRequest request = this.parser.parse();
            if (request != null && headerLog.isDebugEnabled()) {
                headerLog.debug(">> " + request.getRequestLine().toString());
                Header[] headers = request.getAllHeaders();
                for (Header header : headers) {
                    headerLog.debug(">> " + header.toString());
                }
            }
            return request;
        }
    }
}

