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

import java.util.HashMap;
import java.util.Map;

import org.apache.synapse.transport.testkit.TestEnvironment;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;

public class MailTestEnvironment extends TestEnvironment {
    private static final String ADDRESS = "test-account";
    private static final String PASSWORD = "password";
    
    private static final ServerSetup SMTP =
                new ServerSetup(7025, "127.0.0.1", ServerSetup.PROTOCOL_SMTP);
    
    private static final ServerSetup POP3 =
                new ServerSetup(7110, "127.0.0.1", ServerSetup.PROTOCOL_POP3);
    
    private GreenMail greenMail;
    
    @Override
    public void setUp() throws Exception {
        greenMail = new GreenMail(new ServerSetup[] { SMTP, POP3 });
        greenMail.setUser(ADDRESS, PASSWORD);
        greenMail.start();
    }

    @Override
    public void tearDown() throws Exception {
        greenMail.stop();
        greenMail = null;
    }
    
    public String getProtocol() {
        return "pop3";
    }
    
    public String getAddress() {
        return ADDRESS;
    }
    
    public Map<String,String> getInProperties() {
        Map<String,String> props = new HashMap<String,String>();
        props.put("mail.pop3.host", "localhost");
        props.put("mail.pop3.port", String.valueOf(POP3.getPort()));
        props.put("mail.pop3.user", ADDRESS);
        props.put("mail.pop3.password", PASSWORD);
        return props;
    }
    
    public Map<String,String> getOutProperties() {
        Map<String,String> props = new HashMap<String,String>();
        props.put("mail.smtp.host", "localhost");
        props.put("mail.smtp.port", String.valueOf(SMTP.getPort()));
        return props;
    }
}
