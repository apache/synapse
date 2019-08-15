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
package org.apache.synapse.transport.passthru.util;

import org.apache.http.nio.util.ByteBufferAllocator;
import org.apache.http.nio.util.HeapByteBufferAllocator;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BufferFactory {

    private volatile ControlledByteBuffer [] buffers;

    private volatile int marker = -1;

    private ByteBufferAllocator allocator = null;

    private Lock lock = new ReentrantLock();

    private int bufferSize = 1024 * 8;

    public BufferFactory(int bufferSize, ByteBufferAllocator allocator, int size) {
        this.bufferSize = bufferSize;
        if (allocator != null) {
            this.allocator = allocator;
        } else {
            this.allocator = HeapByteBufferAllocator.INSTANCE;
        }

        buffers = new ControlledByteBuffer[size];
    }

    public ControlledByteBuffer getBuffer() {
        if (marker == -1) {
            return new ControlledByteBuffer(allocator.allocate(bufferSize));
        } else {
            try {
                lock.lock();
                if (marker >= 0) {
                    ControlledByteBuffer controlledByteBuffer = buffers[marker];
                    controlledByteBuffer.clear();
                    controlledByteBuffer.forceSetInputMode();
                    buffers[marker] = null;
                    marker--;
                    return controlledByteBuffer;
                }
            } finally {
                lock.unlock();
            }
        }
        return new ControlledByteBuffer(allocator.allocate(bufferSize));
    }

    public void release(ControlledByteBuffer buffer) {
        try {
            lock.lock();
            if (marker < buffers.length - 1) {
                buffer.clear();
                buffer.forceSetInputMode();
                buffers[++marker] = buffer;
            }
        } finally {
            lock.unlock();
        }
    }
}
