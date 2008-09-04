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

import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.mail.Flags;

import org.apache.synapse.transport.testkit.name.Name;
import org.apache.synapse.transport.testkit.util.LogManager;

import com.icegreen.greenmail.store.FolderListener;
import com.icegreen.greenmail.store.MailFolder;
import com.icegreen.greenmail.store.StoredMessage;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;

@Name("greenmail")
public class GreenMailTestEnvironment extends MailTestEnvironment {
    private static final ServerSetup SMTP =
            new ServerSetup(7025, "127.0.0.1", ServerSetup.PROTOCOL_SMTP);
    
    private static final ServerSetup POP3 =
            new ServerSetup(7110, "127.0.0.1", ServerSetup.PROTOCOL_POP3);

    private LogManager logManager;
    private GreenMail greenMail;
    private int accountNumber;
    private List<Account> unallocatedAccounts;

    @SuppressWarnings("unused")
    private void setUp(LogManager logManager) throws Exception {
        this.logManager = logManager;
        greenMail = new GreenMail(new ServerSetup[] { SMTP, POP3 });
        greenMail.start();
        unallocatedAccounts = new LinkedList<Account>();
    }

    @SuppressWarnings("unused")
    private void tearDown() throws Exception {
        greenMail.stop();
        greenMail = null;
        accountNumber = 1;
        unallocatedAccounts = null;
        logManager = null;
    }
    
    @Override
    public String getProtocol() {
        return "pop3";
    }
    
    @Override
    public Account allocateAccount() throws Exception {
        if (unallocatedAccounts.isEmpty()) {
            GreenMailUser user = greenMail.setUser("test" + accountNumber++, "password");
            final MailFolder inbox = greenMail.getManagers().getImapHostManager().getInbox(user);
            inbox.addListener(new FolderListener() {
                public void added(int msn) {
                    StoredMessage storedMessage = (StoredMessage)inbox.getMessages().get(msn-1);
                    try {
                        OutputStream out = logManager.createLog("greenmail");
                        try {
                            storedMessage.getMimeMessage().writeTo(out);
                        } finally {
                            out.close();
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }

                public void expunged(int msn) {}
                public void flagsUpdated(int msn, Flags flags, Long uid) {}
                public void mailboxDeleted() {}
            });
            return new Account(user.getEmail(), user.getLogin(), user.getPassword());
        } else {
            return unallocatedAccounts.remove(0);
        }
    }

    @Override
    public void freeAccount(Account account) {
        unallocatedAccounts.add(account);
    }

    @Override
    public Map<String,String> getInProperties(Account account) {
        Map<String,String> props = new HashMap<String,String>();
        props.put("mail.pop3.host", "localhost");
        props.put("mail.pop3.port", String.valueOf(POP3.getPort()));
        props.put("mail.pop3.user", account.getLogin());
        props.put("mail.pop3.password", account.getPassword());
        return props;
    }
    
    @Override
    public Map<String,String> getOutProperties() {
        Map<String,String> props = new HashMap<String,String>();
        props.put("mail.smtp.host", "localhost");
        props.put("mail.smtp.port", String.valueOf(SMTP.getPort()));
        return props;
    }
}
