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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.URLName;
import javax.mail.internet.InternetAddress;

public class TestTransport extends Transport {
    public TestTransport(Session session, URLName urlname) {
        super(session, urlname);
    }

    @Override
    protected boolean protocolConnect(String host, int port, String user,
            String password) throws MessagingException {
        return true;
    }

    @Override
    public void sendMessage(Message msg, Address[] addresses) throws MessagingException {
        System.out.println("Message to " + Arrays.asList(addresses));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            msg.writeTo(baos);
        } catch (IOException ex) {
            throw new MessagingException("Unable to serialize message", ex);
        }
        byte[] content = baos.toByteArray();
        for (Address address : addresses) {
            Account.getAccount((InternetAddress)address).receive(content);
        }
    }
}
