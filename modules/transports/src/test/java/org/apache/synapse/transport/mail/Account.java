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
package org.apache.synapse.transport.mail;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.mail.Flags;
import javax.mail.internet.InternetAddress;

public class Account {
    public static class Message {
        private final int uid;
        private final byte[] content;
        private final Flags flags = new Flags();
        
        public Message(int uid, byte[] content) {
            this.uid = uid;
            this.content = content;
        }

        public int getUid() {
            return uid;
        }

        public byte[] getContent() {
            return content;
        }
        
        public synchronized Flags getFlags() {
            return (Flags)flags.clone();
        }
        
        public synchronized void addFlags(Flags flags) {
            flags.add(flags);
        }
        
        public synchronized void removeFlags(Flags flags) {
            flags.remove(flags);
        }
    }
    
    private static final Map<InternetAddress,Account> accounts = new HashMap<InternetAddress,Account>();
    
    private final Map<Integer,Message> messageMap = new LinkedHashMap<Integer,Message>();
    private int nextUid;
    
    public static synchronized Account getAccount(InternetAddress address) {
        Account account = accounts.get(address);
        if (account == null) {
            account = new Account();
            accounts.put(address, account);
        }
        return account;
    }

    public synchronized void receive(byte[] content) {
        int uid = nextUid++;
        messageMap.put(uid, new Message(uid, content));
    }
    
    public synchronized Message[] getMessages() {
        Collection<Message> messages = messageMap.values();
        return messages.toArray(new Message[messages.size()]);
    }
    
    public synchronized void deleteMessage(int uid) {
        messageMap.remove(uid);
    }
}
