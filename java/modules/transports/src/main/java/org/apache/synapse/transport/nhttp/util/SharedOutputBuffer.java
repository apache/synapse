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

package org.apache.synapse.transport.nhttp.util;

import org.apache.http.nio.util.ExpandableBuffer;
import org.apache.http.nio.util.ByteBufferAllocator;
import org.apache.http.nio.util.ContentOutputBuffer;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.ContentEncoder;

import java.io.IOException;
import java.io.InterruptedIOException;

/**
 * A copy of the SharedOutputBuffer implementation of Apache HttpComponents - HttpCore/NIO
 * found at http://svn.apache.org/repos/asf/httpcomponents/httpcore/trunk/module-nio/
 *  src/main/java/org/apache/http/nio/util/SharedOutputBuffer.java
 *
 * To include the fix described here : https://issues.apache.org/jira/browse/HTTPCORE-172
 * with the HttpCore version 4.0-beta1
 *
 * TODO : This class to be removed as soon as we update the HttpCore dependency from 4.0-beta1
 */
public class SharedOutputBuffer extends ExpandableBuffer implements ContentOutputBuffer {

    private final IOControl ioctrl;
    private final Object mutex;

    private volatile boolean shutdown = false;
    private volatile boolean endOfStream = false;

    public SharedOutputBuffer(int buffersize, final IOControl ioctrl, final ByteBufferAllocator allocator) {
        super(buffersize, allocator);
        if (ioctrl == null) {
            throw new IllegalArgumentException("I/O content control may not be null");
        }
        this.ioctrl = ioctrl;
        this.mutex = new Object();
    }

    public void reset() {
        if (this.shutdown) {
            return;
        }
        synchronized (this.mutex) {
            clear();
            this.endOfStream = false;
        }
    }

    public int produceContent(final ContentEncoder encoder) throws IOException {
        if (this.shutdown) {
            return -1;
        }
        synchronized (this.mutex) {
            setOutputMode();
            int bytesWritten = 0;
            if (hasData()) {
                bytesWritten = encoder.write(this.buffer);
                if (encoder.isCompleted()) {
                    this.endOfStream = true;
                }
            }
            if (!hasData()) {
                // No more buffered content
                // If at the end of the stream, terminate
                if (this.endOfStream && !encoder.isCompleted()) {
                    encoder.complete();
                }
                if (!this.endOfStream) {
                    // suspend output events
                    this.ioctrl.suspendOutput();
                }
            }
            this.mutex.notifyAll();
            return bytesWritten;
        }
    }

    public void close() {
        shutdown();
    }

    public void shutdown() {
        if (this.shutdown) {
            return;
        }
        this.shutdown = true;
        synchronized (this.mutex) {
            this.mutex.notifyAll();
        }
    }

    public void write(final byte[] b, int off, int len) throws IOException {
        if (b == null) {
            return;
        }
        synchronized (this.mutex) {
            if (this.shutdown || this.endOfStream) {
                throw new IllegalStateException("Buffer already closed for writing");
            }
            setInputMode();
            int remaining = len;
            while (remaining > 0) {
                if (!this.buffer.hasRemaining()) {
                    flushContent();
                    setInputMode();
                }
                int chunk = Math.min(remaining, this.buffer.remaining());
                this.buffer.put(b, off, chunk);
                remaining -= chunk;
                off += chunk;
            }
        }
    }

    public void write(final byte[] b) throws IOException {
        if (b == null) {
            return;
        }
        write(b, 0, b.length);
    }

    public void write(int b) throws IOException {
        synchronized (this.mutex) {
            if (this.shutdown || this.endOfStream) {
                throw new IllegalStateException("Buffer already closed for writing");
            }
            setInputMode();
            if (!this.buffer.hasRemaining()) {
                flushContent();
                setInputMode();
            }
            this.buffer.put((byte)b);
        }
    }

    public void flush() throws IOException {
    }

    private void flushContent() throws IOException {
        synchronized (this.mutex) {
            try {
                while (hasData()) {
                    if (this.shutdown) {
                        throw new InterruptedIOException("Output operation aborted");
                    }
                    this.ioctrl.requestOutput();
                    this.mutex.wait();
                }
            } catch (InterruptedException ex) {
                throw new IOException("Interrupted while flushing the content buffer");
            }
        }
    }

    public void writeCompleted() throws IOException {
        synchronized (this.mutex) {
            if (this.endOfStream) {
                return;
            }
            this.endOfStream = true;
            this.ioctrl.requestOutput();
        }
    }

}