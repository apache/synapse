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
import java.util.Properties;

import javax.mail.Session;

import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.transport.mail.MailConstants;
import org.apache.synapse.transport.testkit.axis2.AxisServiceConfigurator;
import org.apache.synapse.transport.testkit.axis2.client.AxisTestClientSetup;
import org.apache.synapse.transport.testkit.channel.AsyncChannel;
import org.apache.synapse.transport.testkit.channel.RequestResponseChannel;

public class MailChannel implements AsyncChannel, RequestResponseChannel, AxisTestClientSetup, AxisServiceConfigurator {
    private MailTestEnvironment env;
    private MailTestEnvironment.Account sender;
    private MailTestEnvironment.Account recipient;
    
    @SuppressWarnings("unused")
    private void setUp(MailTestEnvironment env) throws Exception {
        this.env = env;
        sender = env.allocateAccount();
        recipient = env.allocateAccount();
    }
    
    @SuppressWarnings("unused")
    private void tearDown() {
        env.freeAccount(sender);
        env.freeAccount(recipient);
        sender = null;
        recipient = null;
    }

    public MailTestEnvironment.Account getSender() {
        return sender;
    }

    public MailTestEnvironment.Account getRecipient() {
        return recipient;
    }
    
    public Session getReplySession() {
        Properties props = new Properties();
        props.putAll(env.getInProperties(sender));
        return Session.getInstance(props);
    }

    public EndpointReference getEndpointReference() throws Exception {
        return new EndpointReference("mailto:" + recipient.getAddress());
    }

    public void setupService(AxisService service, boolean isClientSide) throws Exception {
        MailTestEnvironment.Account account = isClientSide ? sender : recipient;
        service.addParameter("transport.mail.Protocol", env.getProtocol());
        service.addParameter("transport.mail.Address", account.getAddress());
        service.addParameter("transport.PollInterval", "50ms");
        
        for (Map.Entry<String,String> prop : env.getInProperties(account).entrySet()) {
            service.addParameter(prop.getKey(), prop.getValue());
        }
    }

    public void setupRequestMessageContext(MessageContext msgContext) {
        Map<String,String> trpHeaders = new HashMap<String,String>();
        trpHeaders.put(MailConstants.MAIL_HEADER_FROM, sender.getAddress());
        msgContext.setProperty(MessageContext.TRANSPORT_HEADERS, trpHeaders);
    }
}
