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

import java.io.ByteArrayInputStream;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

public class TestMimeMessage extends MimeMessage {
    private final Account.Message message;
    
    public TestMimeMessage(Folder folder, Account.Message message, int msgnum) throws MessagingException {
        super(folder, new ByteArrayInputStream(message.getContent()), msgnum);
        this.message = message;
        flags = message.getFlags();
    }

    @Override
    public synchronized void setFlags(Flags flags, boolean set) throws MessagingException {
        super.setFlags(flags, set);
        if (set) {
            message.addFlags(flags);
        } else {
            message.removeFlags(flags);
        }
    }
    
    public int getUid() {
        return message.getUid();
    }
}
