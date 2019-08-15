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
import org.apache.http.impl.nio.DefaultNHttpClientConnection;
import org.apache.http.impl.nio.codecs.DefaultHttpRequestWriter;
import org.apache.http.impl.nio.codecs.DefaultHttpResponseParser;
import org.apache.http.nio.*;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.nio.reactor.SessionInputBuffer;
import org.apache.http.nio.reactor.SessionOutputBuffer;
import org.apache.http.nio.util.HeapByteBufferAllocator;
import org.apache.synapse.transport.nhttp.AccessHandler;
import org.apache.synapse.transport.utils.conn.SynapseNHttpClientConnection;
import org.apache.synapse.transport.utils.conn.logging.LoggingConstants;
import org.apache.synapse.transport.utils.conn.logging.LoggingIOSession;
import org.apache.synapse.transport.utils.conn.logging.LoggingNHttpClientConnection;

import java.io.IOException;

/**
 * A connection factory implementation for DefaultNHttpClientConnection instances.
 * Based on the current logging configuration, this factory decides whether to create
 * regular SynapseNHttpClientConnection objects or to create LoggingNHttpClientConnection
 * objects. Also, depending on the logging configuration, this factory may choose to
 * wrap IOSession instances in LoggingIOSession objects.
 */
public class SynapseNHttpClientConnectionFactory implements NHttpConnectionFactory<DefaultNHttpClientConnection> {

    private static final Log targetConnLog = LogFactory.getLog(
            LoggingConstants.TARGET_CONNECTION_LOG_ID);
    private static final Log targetHeaderLog = LogFactory.getLog(
            LoggingConstants.TARGET_HEADER_LOG_ID);
    private static final Log targetSessionLog = LogFactory.getLog(
            LoggingConstants.TARGET_SESSION_LOG_ID);
    private static final Log targetWireLog = LogFactory.getLog(
            LoggingConstants.TARGET_WIRE_LOG_ID);

    private static final NHttpMessageWriterFactory<HttpRequest> requestWriterFactory =
            new LoggingNHttpRequestWriterFactory();
    private static final NHttpMessageParserFactory<HttpResponse> responseParserFactory =
            new LoggingNHttpResponseParserFactory();

    private final ConnectionConfig config;

    public SynapseNHttpClientConnectionFactory(ConnectionConfig config) {
        this.config = config;
    }

    public DefaultNHttpClientConnection createConnection(IOSession session) {
        if (targetSessionLog.isDebugEnabled() || targetWireLog.isDebugEnabled()) {
            session = new LoggingIOSession(targetSessionLog, targetWireLog,
                                           session, "http-sender");
        }

        if (targetConnLog.isDebugEnabled() || targetHeaderLog.isDebugEnabled()) {
            return new LoggingNHttpClientConnection(
                    session,
                    config.getBufferSize(),
                    config.getFragmentSizeHint(),
                    HeapByteBufferAllocator.INSTANCE,
                    ConnSupport.createDecoder(config),
                    ConnSupport.createEncoder(config),
                    config.getMessageConstraints(),
                    StrictContentLengthStrategy.INSTANCE,
                    StrictContentLengthStrategy.INSTANCE,
                    requestWriterFactory,
                    responseParserFactory,
                    targetConnLog);
        } else {
            return new SynapseNHttpClientConnection(
                    session,
                    config.getBufferSize(),
                    config.getFragmentSizeHint(),
                    HeapByteBufferAllocator.INSTANCE,
                    ConnSupport.createDecoder(config),
                    ConnSupport.createEncoder(config),
                    config.getMessageConstraints(),
                    StrictContentLengthStrategy.INSTANCE,
                    StrictContentLengthStrategy.INSTANCE,
                    requestWriterFactory,
                    responseParserFactory);
        }
    }

    static class LoggingNHttpRequestWriterFactory implements NHttpMessageWriterFactory<HttpRequest> {
        public NHttpMessageWriter<HttpRequest> create(SessionOutputBuffer sessionBuffer) {
            return new LoggingNHttpRequestWriter(sessionBuffer);
        }
    }

    static class LoggingNHttpResponseParserFactory implements NHttpMessageParserFactory<HttpResponse> {
        public NHttpMessageParser<HttpResponse> create(SessionInputBuffer sessionBuffer,
                                                      MessageConstraints messageConstraints) {
            return new LoggingNHttpResponseParser(sessionBuffer, messageConstraints);
        }
    }

    static class LoggingNHttpRequestWriter extends DefaultHttpRequestWriter {

        public LoggingNHttpRequestWriter(SessionOutputBuffer buffer) {
            super(buffer);
        }

        public void write(final HttpRequest request) throws IOException, HttpException {
            if (request != null) {
                if (targetHeaderLog.isDebugEnabled()) {
                    targetHeaderLog.debug(">> " + request.getRequestLine().toString());
                    Header[] headers = request.getAllHeaders();
                    for (Header header : headers) {
                        targetHeaderLog.debug(">> " + header.toString());
                    }
                }

                if (AccessHandler.getAccessLog().isInfoEnabled()) {
                    AccessHandler.getAccess().addAccessToQueue(request);
                }
            }
            super.write(request);
        }

    }

    static class LoggingNHttpResponseParser extends DefaultHttpResponseParser {

        public LoggingNHttpResponseParser(SessionInputBuffer buffer, MessageConstraints constraints) {
            super(buffer, constraints);
        }

        public HttpResponse parse() throws IOException, HttpException {
            HttpResponse response = super.parse();
            if (response != null) {
                if (targetHeaderLog.isDebugEnabled()) {
                    targetHeaderLog.debug("<< " + response.getStatusLine().toString());
                    Header[] headers = response.getAllHeaders();
                    for (Header header : headers) {
                        targetHeaderLog.debug("<< " + header.toString());
                    }
                }

                if (AccessHandler.getAccessLog().isInfoEnabled()) {
                    AccessHandler.getAccess().addAccessToQueue(response);
                }
            }
            return response;
        }

    }
}
