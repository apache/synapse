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
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.nio.reactor.SessionBufferStatus;
import org.apache.http.nio.reactor.ssl.SSLIOSession;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.SelectionKey;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A decorator (wrapper) for IOSession instances. This decorator logs additional
 * debug information regarding each of the events triggered on the actual IOSession
 * instance. Most events are logged 'before' they are dispatched to the wrapped
 * IOSession, but this implementation does not modify the event arguments by any means.
 * In that sense this decorator is read-only and safe. This implementation also facilitates
 * intercepting and logging HTTP messages at wire-level.
 */
public class LoggingIOSession implements IOSession {

    private static AtomicLong COUNT = new AtomicLong(0);

    private final Log sessionLog;
    private final Wire wireLog;
    private final IOSession session;
    private final ByteChannel channel;
    private final String id;

    /**
     * Create a new instance of the decorator.
     *
     * @param sessionLog Log instance used to log IOSession events.
     * @param wireLog Log instance used to log wire-level HTTP messages.
     * @param session IOSession to be decorated.
     * @param id An identifier (name) that will be attached to the IOSession for the logging
     *           purposes.
     */
    public LoggingIOSession(
            final Log sessionLog,
            final Log wireLog,
            final IOSession session,
            final String id) {
        if (session == null) {
            throw new IllegalArgumentException("I/O session must not be null");
        }
        this.session = session;
        this.channel = new LoggingByteChannel();
        this.id = id + "-" + COUNT.incrementAndGet();
        this.sessionLog = sessionLog;
        this.wireLog = new Wire(wireLog);
    }

    public int getStatus() {
        return this.session.getStatus();
    }

    public ByteChannel channel() {
        return this.channel;
    }

    public SocketAddress getLocalAddress() {
        return this.session.getLocalAddress();
    }

    public SocketAddress getRemoteAddress() {
        return this.session.getRemoteAddress();
    }

    public int getEventMask() {
        return this.session.getEventMask();
    }

    private static String formatOps(int ops) {
        StringBuilder buffer = new StringBuilder(6);
        buffer.append('[');
        if ((ops & SelectionKey.OP_READ) > 0) {
            buffer.append('r');
        }
        if ((ops & SelectionKey.OP_WRITE) > 0) {
            buffer.append('w');
        }
        if ((ops & SelectionKey.OP_ACCEPT) > 0) {
            buffer.append('a');
        }
        if ((ops & SelectionKey.OP_CONNECT) > 0) {
            buffer.append('c');
        }
        buffer.append(']');
        return buffer.toString();
    }

    private String getPreamble() {
        String preamble = "I/O session " + this.id + " " + this.session;
        if (this.session instanceof SSLIOSession) {
            return "SSL " + preamble;
        } else {
            return preamble;
        }
    }

    public void setEventMask(int ops) {
        if (sessionLog.isDebugEnabled()) {
            sessionLog.debug(getPreamble() + ": Set event mask " + formatOps(ops));
        }
        this.session.setEventMask(ops);
    }

    public void setEvent(int op) {
        if (sessionLog.isDebugEnabled()) {
            sessionLog.debug(getPreamble() + ": Set event " + formatOps(op));
        }
        this.session.setEvent(op);
    }

    public void clearEvent(int op) {
        if (sessionLog.isDebugEnabled()) {
            sessionLog.debug(getPreamble() + ": Clear event " + formatOps(op));
        }
        this.session.clearEvent(op);
    }

    public void close() {
        if (sessionLog.isDebugEnabled()) {
            sessionLog.debug(getPreamble() + ": Close");
        }
        this.session.close();
    }

    public boolean isClosed() {
        return this.session.isClosed();
    }

    public void shutdown() {
        if (sessionLog.isDebugEnabled()) {
            sessionLog.debug(getPreamble() + ": Shutdown");
        }
        this.session.shutdown();
    }

    public int getSocketTimeout() {
        return this.session.getSocketTimeout();
    }

    public void setSocketTimeout(int timeout) {
        if (sessionLog.isDebugEnabled()) {
            sessionLog.debug(getPreamble() + ": Set timeout " + timeout);
        }
        this.session.setSocketTimeout(timeout);
    }

    public void setBufferStatus(final SessionBufferStatus status) {
        this.session.setBufferStatus(status);
    }

    public boolean hasBufferedInput() {
        return this.session.hasBufferedInput();
    }

    public boolean hasBufferedOutput() {
        return this.session.hasBufferedOutput();
    }

    public Object getAttribute(final String name) {
        return this.session.getAttribute(name);
    }

    public void setAttribute(final String name, final Object obj) {
        if (sessionLog.isDebugEnabled()) {
            sessionLog.debug(getPreamble() + ": Set attribute " + name);
        }
        this.session.setAttribute(name, obj);
    }

    public Object removeAttribute(final String name) {
        if (sessionLog.isDebugEnabled()) {
            sessionLog.debug(getPreamble() + ": Remove attribute " + name);
        }
        return this.session.removeAttribute(name);
    }

    class LoggingByteChannel implements ByteChannel {

        public int read(final ByteBuffer dst) throws IOException {
            int bytesRead = session.channel().read(dst);
            if (sessionLog.isDebugEnabled()) {
                sessionLog.debug(getPreamble() + ": " + bytesRead + " bytes read");
            }
            if (bytesRead > 0 && wireLog.isEnabled()) {
                ByteBuffer b = dst.duplicate();
                int p = b.position();
                b.limit(p);
                b.position(p - bytesRead);
                wireLog.input(b);
            }
            return bytesRead;
        }

        public int write(final ByteBuffer src) throws IOException {
            int byteWritten = session.channel().write(src);
            if (sessionLog.isDebugEnabled()) {
                sessionLog.debug(getPreamble() + ": " + byteWritten + " bytes written");
            }
            if (byteWritten > 0 && wireLog.isEnabled()) {
                ByteBuffer b = src.duplicate();
                int p = b.position();
                b.limit(p);
                b.position(p - byteWritten);
                wireLog.output(b);
            }
            return byteWritten;
        }

        public void close() throws IOException {
            if (sessionLog.isDebugEnabled()) {
                sessionLog.debug(getPreamble() + ": Channel close");
            }
            session.channel().close();
        }

        public boolean isOpen() {
            return session.channel().isOpen();
        }

    }
}
