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

package org.apache.synapse.transport.base;

/**
 * Collects metrics related to a transport that has metrics support enabled
 */
public class MetricsCollector {

    private long messagesReceived;
    private long faultsReceiving;
    private long timeoutsReceiving;
    private long bytesReceived;

    private long messagesSent;
    private long faultsSending;
    private long timeoutsSending;
    private long bytesSent;

    public void reset() {
        messagesReceived = 0;
        faultsReceiving = 0;
        timeoutsReceiving = 0;
        bytesReceived = 0;
        messagesSent = 0;
        faultsSending = 0;
        timeoutsSending = 0;
        bytesSent = 0;
    }

    public long getMessagesReceived() {
        return messagesReceived;
    }

    public long getFaultsReceiving() {
        return faultsReceiving;
    }

    public long getTimeoutsReceiving() {
        return timeoutsReceiving;
    }

    public long getBytesReceived() {
        return bytesReceived;
    }

    public long getMessagesSent() {
        return messagesSent;
    }

    public long getFaultsSending() {
        return faultsSending;
    }

    public long getTimeoutsSending() {
        return timeoutsSending;
    }

    public long getBytesSent() {
        return bytesSent;
    }

    public synchronized void incrementMessagesReceived() {
        messagesReceived++;
    }

    public synchronized void incrementFaultsReceiving() {
        faultsReceiving++;
    }

    public synchronized void incrementTimeoutsReceiving() {
        timeoutsReceiving++;
    }

    public synchronized void incrementBytesReceived(long size) {
        bytesReceived += size;
    }

    public synchronized void incrementMessagesSent() {
        messagesSent++;
    }

    public synchronized void incrementFaultsSending() {
        faultsSending++;
    }

    public synchronized void incrementTimeoutsSending() {
        timeoutsSending++;
    }

    public synchronized void incrementBytesSent(long size) {
        bytesSent += size;
    }
}
