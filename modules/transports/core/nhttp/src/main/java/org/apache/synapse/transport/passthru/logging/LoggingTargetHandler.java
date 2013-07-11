/*
 *  Copyright 2013 Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.synapse.transport.passthru.logging;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.nio.NHttpClientHandler;
import org.apache.http.nio.NHttpClientConnection;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;

import java.io.IOException;

public class LoggingTargetHandler implements NHttpClientHandler {

    private final Log log;
    
    private final NHttpClientHandler handler;

    public LoggingTargetHandler(final NHttpClientHandler handler) {
        super();
        if (handler == null) {
            throw new IllegalArgumentException("HTTP client handler may not be null");
        }
        this.handler = handler;
        this.log = LogFactory.getLog(handler.getClass());
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
            this.log.debug("HTTP connection " + conn + ": InRequest ready" + getRequestMessageID(conn));
        }
        this.handler.requestReady(conn);
    }

    public void outputReady(final NHttpClientConnection conn, final ContentEncoder encoder) {
        if (this.log.isDebugEnabled()) {
            this.log.debug("HTTP connection " + conn + ": Output ready" + getRequestMessageID(conn));
        }
        this.handler.outputReady(conn, encoder);
        if (this.log.isDebugEnabled()) {
            this.log.debug("HTTP connection " + conn + ": Content encoder " + encoder);
        }
    }

    public void responseReceived(final NHttpClientConnection conn) {
        HttpResponse response = conn.getHttpResponse();
        if (this.log.isDebugEnabled()) {
            this.log.debug("HTTP connection " + conn + " : "
                    + response.getStatusLine() + getRequestMessageID(conn));
        }
        this.handler.responseReceived(conn);
    }

    public void inputReady(final NHttpClientConnection conn, final ContentDecoder decoder) {
        if (this.log.isDebugEnabled()) {
            this.log.debug("HTTP connection " + conn + ": Input ready" + getRequestMessageID(conn));
        }
        this.handler.inputReady(conn, decoder);
        if (this.log.isDebugEnabled()) {
            this.log.debug("HTTP connection " + conn + ": Content decoder " + decoder);
        }
    }

    public void timeout(final NHttpClientConnection conn) {
        if (this.log.isDebugEnabled()) {
            this.log.debug("HTTP connection " + conn + ": Timeout" + getRequestMessageID(conn));
        }
        this.handler.timeout(conn);
    }

    private static String getRequestMessageID(final NHttpClientConnection conn) {
        /*Axis2HttpRequest axis2Request = (Axis2HttpRequest)
                conn.getContext().getAttribute(ClientHandler.AXIS2_HTTP_REQUEST);
        if (axis2Request != null) {
            return " [InRequest Message ID : " + axis2Request.getMsgContext().getMessageID() + "]";
        }*/        
        return "";
    }
}
