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

import java.util.Date;
import java.util.Map;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.util.ByteArrayDataSource;

import org.apache.synapse.transport.testkit.client.AsyncTestClient;
import org.apache.synapse.transport.testkit.client.ClientOptions;
import org.apache.synapse.transport.testkit.message.ByteArrayMessage;

public abstract class MailClient implements AsyncTestClient<ByteArrayMessage> {
    private Map<String,String> outProperties;
    private MailChannel channel;
    
    @SuppressWarnings("unused")
    private void setUp(MailTestEnvironment env, MailChannel channel) throws Exception {
        outProperties = env.getOutProperties();
        this.channel = channel;
    }

    public void sendMessage(ClientOptions options, ByteArrayMessage message) throws Exception {
        Properties props = new Properties();
        props.putAll(outProperties);
        Session session = Session.getInstance(props);
        MimeMessage msg = new MimeMessage(session);
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(channel.getAddress()));
        msg.setFrom(new InternetAddress("test-sender@localhost"));
        msg.setSentDate(new Date());
        DataHandler dh = new DataHandler(new ByteArrayDataSource(message.getContent(), message.getContentType()));
        setupMessage(msg, dh);
        Transport.send(msg);
    }
    
    protected abstract void setupMessage(MimeMessage msg, DataHandler dh) throws Exception;
}
