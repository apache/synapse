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

import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;
import javax.mail.internet.InternetAddress;

public class TestStore extends Store {
    private Account account;
    
    public TestStore(Session session, URLName urlname) {
        super(session, urlname);
    }

    @Override
    protected boolean protocolConnect(String host, int port, String user,
            String password) throws MessagingException {
        account = Account.getAccount(new InternetAddress(user));
        return true;
    }

    @Override
    public Folder getDefaultFolder() throws MessagingException {
        return new TestFolder(this, account);
    }

    @Override
    public Folder getFolder(String name) throws MessagingException {
        return getDefaultFolder();
    }

    @Override
    public Folder getFolder(URLName url) throws MessagingException {
        return getDefaultFolder();
    }
}
