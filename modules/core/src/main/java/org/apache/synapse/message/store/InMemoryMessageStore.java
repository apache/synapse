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

package org.apache.synapse.message.store;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * InMemory Message store will store Failed Messages in the local memory
 */
public class InMemoryMessageStore extends AbstractMessageStore {

    private static final Log log = LogFactory.getLog(InMemoryMessageStore.class);

    /** The map that keeps the stored messages */
    private Queue<MessageContext> messageList = new ConcurrentLinkedQueue<MessageContext>();

    private Lock lock = new ReentrantLock();

    public boolean offer(MessageContext messageContext) {
        lock.lock();
        try {
            if (messageContext != null) {
                messageContext.getEnvelope().build();
                messageList.offer(messageContext);
                // Notify observers
                notifyMessageAddition(messageContext.getMessageID());
                if (log.isDebugEnabled()) {
                    log.debug("Message with id " + messageContext.getMessageID() + " stored");
                }
            }
        } finally {
            lock.unlock();
        }

        return true;
    }

    public MessageContext poll() {
        lock.lock();
        try {
            MessageContext context = messageList.poll();
            if (context != null) {
                // notify observers
                notifyMessageRemoval(context.getMessageID());
            }
            return context;
        } finally {
            lock.unlock();
        }
    }

    public MessageContext peek() {
        return messageList.peek();        
    }

    public MessageContext remove() throws NoSuchElementException {
        lock.lock();
        try {
            MessageContext msgCtx = messageList.remove();
            if (msgCtx != null) {
                notifyMessageRemoval(msgCtx.getMessageID());
            }
            return msgCtx;
        } finally {
            lock.unlock();
        }
    }

    public MessageContext get(int index) {
        lock.lock();
        try {
            if (index >= 0 && index < messageList.size()) {
                int i = 0;
                for (MessageContext msgCtx : messageList) {
                    if (index == i) {
                        return msgCtx;
                    }
                    i++;
                }
            }
            return null;
        } finally {
            lock.unlock();
        }
    }

    public MessageContext remove(String messageID) {
        lock.lock();
        try {
            if (messageID != null) {
                MessageContext removable = null;
                for (MessageContext msgCtx : messageList) {
                    if (msgCtx.getMessageID().equals(messageID)) {
                        removable = msgCtx;
                        break;
                    }
                }

                if (removable != null && messageList.remove(removable)) {
                    notifyMessageRemoval(messageID);
                }
            }
        } finally {
            lock.unlock();
        }
        return null;
    }

    public void clear() {
        lock.lock();
        try {
            while (!messageList.isEmpty()) {
                // We need to call remove() here because we need the notifications
                // to get fired properly for each removal
                remove();
            }
        } finally {
            lock.unlock();
        }
    }

    public List<MessageContext> getAll() {
        lock.lock();
        try {
            List<MessageContext> returnList = new ArrayList<MessageContext>();
            returnList.addAll(messageList);
            return returnList;
        } finally {
            lock.unlock();
        }
    }

    public MessageContext get(String messageId) {
        lock.lock();
        try {
            if (messageId != null) {
                for (MessageContext msgCtx : messageList) {
                    if (msgCtx.getMessageID().equals(messageId)) {
                        return msgCtx;
                    }
                }
            }
        } finally {
            lock.unlock();
        }
        return null;
    }

    public int size() {
        return messageList.size();
    }
}