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

import java.util.Map;
import java.util.Properties;

import javax.mail.Session;

import org.apache.axis2.description.AxisService;
import org.apache.synapse.transport.testkit.listener.AbstractChannel;
import org.apache.synapse.transport.testkit.listener.AsyncChannel;
import org.apache.synapse.transport.testkit.listener.RequestResponseChannel;

import com.icegreen.greenmail.user.GreenMailUser;

public class MailChannel extends AbstractChannel implements AsyncChannel, RequestResponseChannel {
    private GreenMailUser sender;
    private GreenMailUser recipient;
    private String protocol;
    private Map<String,String> senderInProperties;
    private Map<String,String> recipientInProperties;
    
    @SuppressWarnings("unused")
    private void setUp(MailTestEnvironment env) throws Exception {
        protocol = env.getProtocol();
        sender = env.getUser(0);
        senderInProperties = env.getInProperties(0);
        recipient = env.getUser(1);
        recipientInProperties = env.getInProperties(1);
    }

    public GreenMailUser getSender() {
        return sender;
    }

    public GreenMailUser getRecipient() {
        return recipient;
    }
    
    public Session getReplySession() {
        Properties props = new Properties();
        props.putAll(senderInProperties);
        return Session.getInstance(props);
    }

    @Override
    public void setupService(AxisService service) throws Exception {
        service.addParameter("transport.mail.Protocol", protocol);
        service.addParameter("transport.mail.Address", recipient.getEmail());
        service.addParameter("transport.PollInterval", "1");
        
        for (Map.Entry<String,String> prop : recipientInProperties.entrySet()) {
            service.addParameter(prop.getKey(), prop.getValue());
        }
    }
}
