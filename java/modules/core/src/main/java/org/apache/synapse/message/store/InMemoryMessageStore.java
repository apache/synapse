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

import java.util.*;

/**
 * InMemory Message store will store Failed Messages in the local memory
 */
public class InMemoryMessageStore extends AbstractMessageStore {

    private static final Log log = LogFactory.getLog(InMemoryMessageStore.class);

    /** The map that keeps the stored messages */
    private Map<String, StorableMessage> messageList = new HashMap<String, StorableMessage>();

    public void store(StorableMessage storableMessage) {
        if (storableMessage != null) {
            mediateSequence(storableMessage.getMessageContext());
            messageList.put(storableMessage.getMessageContext().getMessageID(), storableMessage);
            if (log.isDebugEnabled()) {
                log.debug("Message " + storableMessage.getMessageContext().getMessageID() +
                        " has been stored");
            }
        }
    }

    public StorableMessage unstore(String messageID) {
        if (messageID != null) {
            return messageList.remove(messageID);
        }
        return null;
    }

    public List<StorableMessage> unstoreAll() {
        List<StorableMessage> returnList = new ArrayList<StorableMessage>();
        for (String k : messageList.keySet()) {
            returnList.add(messageList.remove(k));
        }
        return returnList;
    }

    public List<StorableMessage> getAllMessages() {
        List<StorableMessage> returnlist = new ArrayList<StorableMessage>();

        Iterator<String> it = messageList.keySet().iterator();
        while (it.hasNext()) {
            returnlist.add(messageList.get(it.next()));
        }

        return returnlist;
    }

    public StorableMessage getMessage(String messageId) {
        if (messageId != null) {
            return messageList.get(messageId);
        }

        return null;
    }

    public int getSize() {
        return messageList.size();
    }
}