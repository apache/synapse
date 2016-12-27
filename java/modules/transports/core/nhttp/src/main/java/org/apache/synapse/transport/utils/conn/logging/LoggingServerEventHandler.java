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
import org.apache.http.HttpRequest;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.NHttpServerConnection;
import org.apache.http.nio.NHttpServerEventHandler;

import java.io.IOException;

/**
 * A decorator (wrapper) for NHttpServerEventHandler instances. This decorator
 * logs additional debug information regarding each of the events triggered on the
 * actual NHttpServerEventHandler instance. Most events are logged 'before' they are
 * dispatched to the wrapped NHttpServerEventHandler, but this implementation does
 * not modify the event arguments by any means. In that sense this decorator is
 * read-only and safe. This implementation does not log the exception event. It is
 * expected that the actual NHttpServerEventHandler will take the necessary steps to
 * log exceptions.
 */
public class LoggingServerEventHandler implements NHttpServerEventHandler {

    private final Log log;

    private final NHttpServerEventHandler handler;

    /**
     * Create a new instance of the decorator.
     *
     * @param handler The instance of NHttpServerEventHandler to be decorated (wrapped)
     */
    public LoggingServerEventHandler(final NHttpServerEventHandler handler) {
        super();
        if (handler == null) {
            throw new IllegalArgumentException("HTTP service handler must not be null");
        }
        this.handler = handler;
        this.log = LogFactory.getLog(handler.getClass());
    }

    public void connected(final NHttpServerConnection conn) throws IOException, HttpException {
        if (this.log.isDebugEnabled()) {
            this.log.debug("HTTP connection " + conn + ": Connected");
        }
        this.handler.connected(conn);
    }

    public void closed(final NHttpServerConnection conn) {
        if (this.log.isDebugEnabled()) {
            this.log.debug("HTTP connection " + conn + ": Closed");
        }
        this.handler.closed(conn);
    }

    public void endOfInput(NHttpServerConnection conn) throws IOException {
        if (this.log.isDebugEnabled()) {
            this.log.debug("HTTP connection " + conn + ": Closed at the remote end");
        }
        this.handler.endOfInput(conn);
    }

    public void exception(NHttpServerConnection conn, Exception ex) {
        // No need to log errors at this level - Actual handler implementation
        // should take care of that
        this.handler.exception(conn, ex);
    }

    public void requestReceived(final NHttpServerConnection conn) throws IOException, HttpException {
        HttpRequest request = conn.getHttpRequest();
        if (this.log.isDebugEnabled()) {
            this.log.debug("HTTP InRequest Received on connection " + conn + ": "
                    + request.getRequestLine());
        }
        this.handler.requestReceived(conn);
    }

    public void outputReady(final NHttpServerConnection conn, final ContentEncoder encoder) throws IOException, HttpException {
        if (this.log.isDebugEnabled()) {
            this.log.debug("HTTP connection " + conn + ": Output ready");
        }
        this.handler.outputReady(conn, encoder);
        if (this.log.isDebugEnabled()) {
            this.log.debug("HTTP connection " + conn + ": Content encoder " + encoder);
        }
    }

    public void responseReady(final NHttpServerConnection conn) throws IOException, HttpException {
        if (this.log.isDebugEnabled()) {
            this.log.debug("HTTP connection " + conn + ": Response ready");
        }
        this.handler.responseReady(conn);
    }

    public void inputReady(final NHttpServerConnection conn, final ContentDecoder decoder) throws IOException, HttpException {
        if (this.log.isDebugEnabled()) {
            this.log.debug("HTTP connection " + conn + ": Input ready");
        }
        this.handler.inputReady(conn, decoder);
        if (this.log.isDebugEnabled()) {
            this.log.debug("HTTP connection " + conn + ": Content decoder " + decoder);
        }
    }

    public void timeout(final NHttpServerConnection conn) throws IOException {
        if (this.log.isDebugEnabled()) {
            this.log.debug("HTTP connection " + conn + ": Timeout");
        }
        this.handler.timeout(conn);
    }
}
