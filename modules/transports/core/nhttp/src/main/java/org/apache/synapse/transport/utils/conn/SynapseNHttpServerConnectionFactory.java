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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.MessageConstraints;
import org.apache.http.impl.ConnSupport;
import org.apache.http.impl.entity.StrictContentLengthStrategy;
import org.apache.http.impl.nio.DefaultNHttpServerConnection;
import org.apache.http.impl.nio.codecs.DefaultHttpRequestParser;
import org.apache.http.impl.nio.codecs.DefaultHttpResponseWriter;
import org.apache.http.nio.*;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.nio.reactor.SessionInputBuffer;
import org.apache.http.nio.reactor.SessionOutputBuffer;
import org.apache.http.nio.util.HeapByteBufferAllocator;
import org.apache.synapse.transport.nhttp.AccessHandler;
import org.apache.synapse.transport.utils.conn.logging.LoggingConstants;
import org.apache.synapse.transport.utils.conn.logging.LoggingIOSession;
import org.apache.synapse.transport.utils.conn.logging.LoggingNHttpServerConnection;

import java.io.IOException;

/**
 * A connection factory implementation for DefaultNHttpServerConnection instances.
 * Based on the current logging configuration, this factory decides whether to create
 * regular DefaultNHttpServerConnection objects or to create LoggingNHttpServerConnection
 * objects. Also, depending on the logging configuration, this factory may choose to
 * wrap IOSession instances in LoggingIOSession objects.
 */
public class SynapseNHttpServerConnectionFactory implements NHttpConnectionFactory<DefaultNHttpServerConnection> {

    private static final Log sourceConnLog = LogFactory.getLog(
            LoggingConstants.SOURCE_CONNECTION_LOG_ID);
    private static final Log sourceHeaderLog = LogFactory.getLog(
            LoggingConstants.SOURCE_HEADER_LOG_ID);
    private static final Log sourceSessionLog = LogFactory.getLog(
            LoggingConstants.SOURCE_SESSION_LOG_ID);
    private static final Log sourceWireLog = LogFactory.getLog(
            LoggingConstants.SOURCE_WIRE_LOG_ID);

    private static final NHttpMessageParserFactory<HttpRequest> requestParserFactory =
            new LoggingNHttpRequestParserFactory();
    private static final NHttpMessageWriterFactory<HttpResponse> responseWriterFactory =
            new LoggingNHttpResponseWriterFactory();

    private final ConnectionConfig config;

    public SynapseNHttpServerConnectionFactory(ConnectionConfig config) {
        this.config = config;
    }

    public DefaultNHttpServerConnection createConnection(IOSession session) {
        if (sourceSessionLog.isDebugEnabled() || sourceWireLog.isDebugEnabled()) {
            session = new LoggingIOSession(sourceSessionLog, sourceWireLog,
                                           session, "http-listener");
        }

        if (sourceConnLog.isDebugEnabled()) {
            return new LoggingNHttpServerConnection(
                    session,
                    config.getBufferSize(),
                    config.getFragmentSizeHint(),
                    HeapByteBufferAllocator.INSTANCE,
                    ConnSupport.createDecoder(config),
                    ConnSupport.createEncoder(config),
                    config.getMessageConstraints(),
                    StrictContentLengthStrategy.INSTANCE,
                    StrictContentLengthStrategy.INSTANCE,
                    requestParserFactory,
                    responseWriterFactory,
                    sourceConnLog);
        } else {
            return new DefaultNHttpServerConnection(
                    session,
                    config.getBufferSize(),
                    config.getFragmentSizeHint(),
                    HeapByteBufferAllocator.INSTANCE,
                    ConnSupport.createDecoder(config),
                    ConnSupport.createEncoder(config),
                    config.getMessageConstraints(),
                    StrictContentLengthStrategy.INSTANCE,
                    StrictContentLengthStrategy.INSTANCE,
                    requestParserFactory,
                    responseWriterFactory);
        }
    }

    static class LoggingNHttpRequestParserFactory implements NHttpMessageParserFactory<HttpRequest> {
        public NHttpMessageParser<HttpRequest> create(SessionInputBuffer sessionBuffer,
                                                      MessageConstraints messageConstraints) {
            return new LoggingNHttpRequestParser(sessionBuffer, messageConstraints);
        }
    }

    static class LoggingNHttpResponseWriterFactory implements NHttpMessageWriterFactory<HttpResponse> {
        public NHttpMessageWriter<HttpResponse> create(SessionOutputBuffer sessionBuffer) {
            return new LoggingNHttpResponseWriter(sessionBuffer);
        }
    }

    static class LoggingNHttpRequestParser extends DefaultHttpRequestParser {

        public LoggingNHttpRequestParser(SessionInputBuffer buffer, MessageConstraints constraints) {
            super(buffer, constraints);
        }

        public HttpRequest parse() throws IOException, HttpException {
            HttpRequest request = super.parse();
            if (request != null) {
                if (sourceHeaderLog.isDebugEnabled()) {
                    sourceHeaderLog.debug(">> " + request.getRequestLine().toString());
                    Header[] headers = request.getAllHeaders();
                    for (Header header : headers) {
                        sourceHeaderLog.debug(">> " + header.toString());
                    }
                }

                if (AccessHandler.getAccessLog().isInfoEnabled()) {
                    AccessHandler.getAccess().addAccessToQueue(request);
                }
            }
            return request;
        }
    }

    static class LoggingNHttpResponseWriter extends DefaultHttpResponseWriter {

        public LoggingNHttpResponseWriter(SessionOutputBuffer buffer) {
            super(buffer);
        }

        public void write(final HttpResponse response) throws IOException, HttpException {
            if (response != null) {
                if (response != null && sourceHeaderLog.isDebugEnabled()) {
                    sourceHeaderLog.debug("<< " + response.getStatusLine().toString());
                    Header[] headers = response.getAllHeaders();
                    for (Header header : headers) {
                        sourceHeaderLog.debug("<< " + header.toString());
                    }
                }

                if (AccessHandler.getAccessLog().isInfoEnabled()) {
                    AccessHandler.getAccess().addAccessToQueue(response);
                }
            }
            super.write(response);
        }
    }
}
