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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * InMemory Message store will store Failed Messages in the local memory
 */
public class InMemoryMessageStore extends AbstractMessageStore {

    private static final Log log = LogFactory.getLog(InMemoryMessageStore.class);

    /** The map that keeps the stored messages */
    private Map<String, MessageContext> messageList = new ConcurrentHashMap<String, MessageContext>();

    private Lock lock = new ReentrantLock();

    public boolean offer(MessageContext messageContext) {
        lock.lock();
        try {
            if (messageContext != null) {
                messageContext.getEnvelope().build();
                messageList.put(messageContext.getMessageID(), messageContext);
                /** Notify observers */
                notifyMessageAddition(messageContext.getMessageID());
                if (log.isDebugEnabled()) {
                    log.debug("Message with id " + messageContext.getMessageID() +
                            " stored");
                }
            }
        } finally {
            lock.unlock();
        }

        return true;
    }

    public MessageContext poll() {
        lock.lock();
        MessageContext context;
        try {
            context = peek();
            if(context !=null) {
                messageList.remove(context.getMessageID());
                /** Notify observers */
                notifyMessageRemoval(context.getMessageID());
            }
        } finally {
            lock.unlock();
        }
        return context;
    }

    public MessageContext peek() {
        if (messageList.size() > 0) {
            return (MessageContext) messageList.values().toArray()[0];
        }

        return null;
    }

    public MessageContext remove() throws NoSuchElementException {
        MessageContext context = poll();
        if(context == null) {
            throw  new NoSuchElementException();
        }

        return context;

    }

    public MessageContext get(int index) {
        if(index >=0 && index < messageList.size()) {
            return (MessageContext) messageList.values().toArray()[index];
        }
        return null;
    }

    public MessageContext remove(String messageID) {
        lock.lock();
        try {
            if (messageID != null) {
               if(messageList.remove(messageID) != null) {
                   /** Notify observers */
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

            for (String k : messageList.keySet()) {
                messageList.remove(k);
                /** Notify observers */
                notifyMessageRemoval(k);
            }
        } finally {
            lock.unlock();
        }
    }

    public List<MessageContext> getAll() {
        lock.lock();
        try {
            List<MessageContext> returnList = new ArrayList<MessageContext>();
            for (Map.Entry<String, MessageContext> entry : messageList.entrySet()) {
                returnList.add(entry.getValue());
            }
            return returnList;
        } finally {
            lock.unlock();
        }
    }

    public MessageContext get(String messageId) {
        lock.lock();
        try {
            if (messageId != null) {
                return messageList.get(messageId);
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