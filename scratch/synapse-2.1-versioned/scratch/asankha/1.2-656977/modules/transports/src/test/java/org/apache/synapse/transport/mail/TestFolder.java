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

import java.util.LinkedList;
import java.util.List;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Store;

public class TestFolder extends Folder {
    private final Account account;
    private TestMimeMessage[] messages;
    private boolean open;
    
    public TestFolder(Store store, Account account) {
        super(store);
        this.account = account;
    }
    
    private void refresh() throws MessagingException {
        Account.Message[] messages = account.getMessages();
        this.messages = new TestMimeMessage[messages.length];
        for (int i=0; i<messages.length; i++) {
            this.messages[i] = new TestMimeMessage(this, messages[i], i+1);
        }
    }
    
    @Override
    public void open(int mode) throws MessagingException {
        refresh();
        open = true;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public int getMessageCount() throws MessagingException {
        return messages.length;
    }

    @Override
    public Message getMessage(int msgnum) throws MessagingException {
        return messages[msgnum-1];
    }
    
    private Message[] doExpunge() throws MessagingException {
        List<Message> expunged = new LinkedList<Message>();
        for (TestMimeMessage message : messages) {
            if (message.isSet(Flags.Flag.DELETED)) {
                expunged.add(message);
                account.deleteMessage(message.getUid());
            }
        }
        return expunged.toArray(new Message[expunged.size()]);
    }
    
    @Override
    public Message[] expunge() throws MessagingException {
        Message[] expunged = doExpunge();
        refresh();
        return expunged;
    }

    @Override
    public void close(boolean expunge) throws MessagingException {
        if (expunge) {
            doExpunge();
        }
        open = false;
        messages = null;
    }

    
    
    @Override
    public void appendMessages(Message[] msgs) throws MessagingException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean create(int type) throws MessagingException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean delete(boolean recurse) throws MessagingException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean exists() throws MessagingException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Folder getFolder(String name) throws MessagingException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getFullName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Folder getParent() throws MessagingException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Flags getPermanentFlags() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public char getSeparator() throws MessagingException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getType() throws MessagingException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean hasNewMessages() throws MessagingException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Folder[] list(String pattern) throws MessagingException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean renameTo(Folder f) throws MessagingException {
        // TODO Auto-generated method stub
        return false;
    }

}
