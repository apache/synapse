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

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.NHttpClientConnection;
import org.apache.http.nio.NHttpClientHandler;

/**
 * Decorator class intended to transparently extend an {@link NHttpClientHandler} 
 * with basic event logging capabilities using Commons Logging. 
 */
public class LoggingNHttpClientHandler implements NHttpClientHandler {

    private final Log log;
    private final Log headerlog;
    private final NHttpClientHandler handler;
    
    public LoggingNHttpClientHandler(
            final Log log, 
            final Log headerlog, 
            final NHttpClientHandler handler) {
        super();
        if (handler == null) {
            throw new IllegalArgumentException("HTTP client handler may not be null");
        }
        this.handler = handler;
        if (log != null) {
            this.log = log;
        } else {
            this.log = LogFactory.getLog(handler.getClass());
        }
        if (log != null) {
            this.headerlog = headerlog;
        } else {
            this.headerlog = LogFactory.getLog(LoggingUtils.HEADER_LOG_ID);
        }
    }
    
    public void connected(final NHttpClientConnection conn, final Object attachment) {
        if (this.log.isDebugEnabled()) {
            this.log.debug("HTTP connection " + conn + ": Connected (" + attachment + ")");
        }
        this.handler.connected(conn, attachment);
    }

    public void closed(final NHttpClientConnection conn) {
        if (this.log.isDebugEnabled()) {
            this.log.debug("HTTP connection " + conn + ": Closed");
        }
        this.handler.closed(conn);
    }

    public void exception(final NHttpClientConnection conn, final IOException ex) {
        this.log.error("HTTP connection " + conn + ": " + ex.getMessage(), ex);
        this.handler.exception(conn, ex);
    }

    public void exception(final NHttpClientConnection conn, final HttpException ex) {
        this.log.error("HTTP connection " + conn + ": " + ex.getMessage(), ex);
        this.handler.exception(conn, ex);
    }

    public void requestReady(final NHttpClientConnection conn) {
        if (this.log.isDebugEnabled()) {
            this.log.debug("HTTP connection " + conn + ": Request ready");
        }
        this.handler.requestReady(conn);
    }

    public void outputReady(final NHttpClientConnection conn, final ContentEncoder encoder) {
        if (this.log.isDebugEnabled()) {
            this.log.debug("HTTP connection " + conn + ": Output ready");
        }
        this.handler.outputReady(conn, encoder);
        if (this.log.isDebugEnabled()) {
            this.log.debug("HTTP connection " + conn + ": Content encoder " + encoder);
        }
    }

    public void responseReceived(final NHttpClientConnection conn) {
        HttpResponse response = conn.getHttpResponse();
        if (this.log.isDebugEnabled()) {
            this.log.debug("HTTP connection " + conn + ": " + response.getStatusLine());
        }
        this.handler.responseReceived(conn);
        if (this.headerlog.isDebugEnabled()) {
            this.headerlog.debug("<< " + response.getStatusLine().toString());
            Header[] headers = response.getAllHeaders();
            for (int i = 0; i < headers.length; i++) {
                this.headerlog.debug("<< " + headers[i].toString());
            }
        }
    }

    public void inputReady(final NHttpClientConnection conn, final ContentDecoder decoder) {
        if (this.log.isDebugEnabled()) {
            this.log.debug("HTTP connection " + conn + ": Input ready");
        }
        this.handler.inputReady(conn, decoder);
        if (this.log.isDebugEnabled()) {
            this.log.debug("HTTP connection " + conn + ": Content decoder " + decoder);
        }
    }

    public void timeout(final NHttpClientConnection conn) {
        if (this.log.isDebugEnabled()) {
            this.log.debug("HTTP connection " + conn + ": Timeout");
        }
        this.handler.timeout(conn);
    }

}