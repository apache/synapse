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
package org.apache.axis2.transport.nhttp;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.nio.reactor.SessionBufferStatus;

/**
 * Decorator class intended to transparently extend an {@link IOSession} 
 * with basic event logging capabilities using Commons Logging. 
 */
public class LoggingIOSession implements IOSession {

    private static int COUNT = 0;
    
    private final Log log;
    private final IOSession session;
    private final ByteChannel channel;
    private final int id;
    
    public LoggingIOSession(final IOSession session) {
        super();
        if (session == null) {
            throw new IllegalArgumentException("I/O session may not be null");
        }
        this.session = session;
        this.channel = new LoggingByteChannel();
        this.id = ++COUNT;
        this.log = LogFactory.getLog(session.getClass());
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

    public void setEventMask(int ops) {
        if (this.log.isDebugEnabled()) {
            this.log.debug("I/O session " + this.id + " " + this.session + ": Set event mask " 
                    + ops);
        }
        this.session.setEventMask(ops);
    }

    public void setEvent(int op) {
        if (this.log.isDebugEnabled()) {
            this.log.debug("I/O session " + this.id + " " + this.session + ": Set event " 
                    + op);
        }
        this.session.setEvent(op);
    }

    public void clearEvent(int op) {
        if (this.log.isDebugEnabled()) {
            this.log.debug("I/O session " + this.id + " " + this.session + ": Clear event " 
                    + op);
        }
        this.session.clearEvent(op);
    }

    public void close() {
        if (this.log.isDebugEnabled()) {
            this.log.debug("I/O session " + this.id + " " + this.session + ": Close");
        }
        this.session.close();
    }

    public boolean isClosed() {
        return this.session.isClosed();
    }

    public int getSocketTimeout() {
        return this.session.getSocketTimeout();
    }

    public void setSocketTimeout(int timeout) {
        if (this.log.isDebugEnabled()) {
            this.log.debug("I/O session " + this.id + " " + this.session + ": Set timeout " 
                    + timeout);
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
        if (this.log.isDebugEnabled()) {
            this.log.debug("I/O session " + this.id + " " + this.session + ": Set attribute " 
                    + name);
        }
        this.session.setAttribute(name, obj);
    }

    public Object removeAttribute(final String name) {
        if (this.log.isDebugEnabled()) {
            this.log.debug("I/O session " + this.id + " " + this.session + ": Remove attribute " 
                    + name);
        }
        return this.session.removeAttribute(name);
    }

    class LoggingByteChannel implements ByteChannel {

        public int read(final ByteBuffer dst) throws IOException {
            int bytesRead = session.channel().read(dst);
            if (log.isDebugEnabled()) {
                log.debug("I/O session " + id + " " + session + ": " + bytesRead + " bytes read");
            }
            return bytesRead;
        }

        public int write(final ByteBuffer src) throws IOException {
            int byteWritten = session.channel().write(src);
            if (log.isDebugEnabled()) {
                log.debug("I/O session " + id + " " + session + ": " + byteWritten + " bytes written");
            }
            return byteWritten;
        }

        public void close() throws IOException {
            if (log.isDebugEnabled()) {
                log.debug("I/O session " + id + " " + session + ": Channel close");
            }
            session.channel().close();
        }

        public boolean isOpen() {
            return session.channel().isOpen();
        }
        
    }    
    
}