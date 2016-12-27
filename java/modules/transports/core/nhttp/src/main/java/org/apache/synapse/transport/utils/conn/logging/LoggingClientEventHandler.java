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
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.NHttpClientConnection;
import org.apache.http.nio.NHttpClientEventHandler;

import java.io.IOException;

/**
 * A decorator (wrapper) for NHttpClientEventHandler instances. This decorator
 * logs additional debug information regarding each of the events triggered on the
 * actual NHttpClientEventHandler instance. Most events are logged 'before' they are
 * dispatched to the wrapped NHttpClientEventHandler, but this implementation does
 * not modify the event arguments by any means. In that sense this decorator is
 * read-only and safe. This implementation does not log the exception event. It is
 * expected that the actual NHttpClientEventHandler will take the necessary steps to
 * log exceptions.
 */
public class LoggingClientEventHandler implements NHttpClientEventHandler {

    private final Log log;

    private final NHttpClientEventHandler handler;

    /**
     * Create a new instance of the decorator.
     *
     * @param handler The instance of NHttpClientEventHandler to be decorated (wrapped)
     */
    public LoggingClientEventHandler(final NHttpClientEventHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("HTTP client handler must not be null");
        }
        this.handler = handler;
        this.log = LogFactory.getLog(handler.getClass());
    }

    public void connected(final NHttpClientConnection conn, final Object attachment) throws IOException, HttpException {
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

    public void endOfInput(NHttpClientConnection conn) throws IOException {
        if (this.log.isDebugEnabled()) {
            this.log.debug("HTTP connection " + conn + ": Closed at remote end");
        }
        this.handler.endOfInput(conn);
    }

    public void exception(NHttpClientConnection conn, Exception ex) {
        // Do not log errors at this level - Actual handler implementation should do that
        this.handler.exception(conn, ex);
    }

    public void requestReady(final NHttpClientConnection conn) throws IOException, HttpException {
        if (this.log.isDebugEnabled()) {
            this.log.debug("HTTP connection " + conn + ": InRequest ready");
        }
        this.handler.requestReady(conn);
    }

    public void outputReady(final NHttpClientConnection conn, final ContentEncoder encoder) throws IOException, HttpException {
        if (this.log.isDebugEnabled()) {
            this.log.debug("HTTP connection " + conn + ": Output ready");
        }
        this.handler.outputReady(conn, encoder);
        if (this.log.isDebugEnabled()) {
            this.log.debug("HTTP connection " + conn + ": Content encoder " + encoder);
        }
    }

    public void responseReceived(final NHttpClientConnection conn) throws IOException, HttpException {
        HttpResponse response = conn.getHttpResponse();
        if (this.log.isDebugEnabled()) {
            this.log.debug("HTTP connection " + conn + " : " + response.getStatusLine());
        }
        this.handler.responseReceived(conn);
    }

    public void inputReady(final NHttpClientConnection conn, final ContentDecoder decoder) throws IOException, HttpException {
        if (this.log.isDebugEnabled()) {
            this.log.debug("HTTP connection " + conn + ": Input ready");
        }
        this.handler.inputReady(conn, decoder);
        if (this.log.isDebugEnabled()) {
            this.log.debug("HTTP connection " + conn + ": Content decoder " + decoder);
        }
    }

    public void timeout(final NHttpClientConnection conn) throws IOException, HttpException {
        if (this.log.isDebugEnabled()) {
            this.log.debug("HTTP connection " + conn + ": Timeout");
        }
        this.handler.timeout(conn);
    }
}
