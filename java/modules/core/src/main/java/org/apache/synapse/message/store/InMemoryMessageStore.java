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

/**
 * InMemory Message store will store Failed Messages in the local memory
 */
public class InMemoryMessageStore extends AbstractMessageStore {

    private static final Log log = LogFactory.getLog(InMemoryMessageStore.class);

    /** The map that keeps the stored messages */
    private Map<String, MessageContext> messageList = new HashMap<String, MessageContext>();

    public void store(MessageContext messageContext) {
        if (messageContext != null) {
            mediateSequence(messageContext);
            messageList.put(messageContext.getMessageID(), messageContext);

            /**
             * If the associated processor is not started we start it. When storing the message
             */
            if(!processor.isStarted()) {
                processor.start();
            }
            if (log.isDebugEnabled()) {
                log.debug("Message " + messageContext.getMessageID() +
                        " has been stored");
            }
        }
    }

    public MessageContext unstore(String messageID) {
        if (messageID != null) {
            return messageList.remove(messageID);
        }
        return null;
    }

    public List<MessageContext> unstoreAll() {
        List<MessageContext> returnList = new ArrayList<MessageContext>();
        for (String k : messageList.keySet()) {
            returnList.add(messageList.remove(k));
        }
        return returnList;
    }

    public List<MessageContext> unstore(int maxNumberOfMessages) {
        List<MessageContext> returnList = new ArrayList<MessageContext>();
        Iterator<String> it = messageList.keySet().iterator();
        while (it.hasNext() && maxNumberOfMessages > 0) {
            returnList.add(messageList.get(it.next()));
            maxNumberOfMessages--;
        }

        return returnList;
    }

    public List<MessageContext> unstore(int from, int to) {
        List<MessageContext> returnlist = new ArrayList<MessageContext>();
        if (from <= to && (from <= messageList.size() && to <= messageList.size()) && messageList.size() > 0) {

            String[] keys = messageList.keySet().toArray(new String[messageList.keySet().size()]);

            for (int i = from; i <= to; i++) {
                returnlist.add(messageList.remove(keys[i]));
            }
        }
        return returnlist;
    }

    public List<MessageContext> getMessages(int from, int to) {
        List<MessageContext> returnList = new ArrayList<MessageContext>();
        if (from <= to && (from <= messageList.size() && to <= messageList.size()) && messageList.size() > 0) {
            String[] keys = messageList.keySet().toArray(new String[messageList.keySet().size()]);

            for (int i = from; i <= to; i++) {
                returnList.add(messageList.get(keys[i]));
            }
        }
        return returnList;
    }

    public List<MessageContext> getAllMessages() {
        List<MessageContext> returnList = new ArrayList<MessageContext>();
        for (Map.Entry<String ,MessageContext> entry :messageList.entrySet()) {
            returnList.add(entry.getValue());
        }
        return returnList;
    }

    public MessageContext getMessage(String messageId) {
        if (messageId != null) {
            return messageList.get(messageId);
        }

        return null;
    }

    public List<MessageContext> getMessages(int maxNumberOfMessages) {
        List<MessageContext> returnList = new ArrayList<MessageContext>();

        Iterator<String> it = messageList.keySet().iterator();
        while (it.hasNext() && maxNumberOfMessages > 0) {
            returnList.add(messageList.get(it.next()));
            maxNumberOfMessages--;
        }

        return returnList;
    }

    public int getSize() {
        return messageList.size();
    }
}