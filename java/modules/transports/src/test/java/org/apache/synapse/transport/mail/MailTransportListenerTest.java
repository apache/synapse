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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.util.ByteArrayDataSource;

import junit.framework.TestSuite;

import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportInDescription;
import org.apache.synapse.transport.TransportListenerTestTemplate;

public class MailTransportListenerTest extends TransportListenerTestTemplate {
    public static class TestStrategyImpl extends TestStrategy {
        private static final String ADDRESS = "test-account@localhost";
        
        @Override
        protected TransportInDescription createTransportInDescription() {
            TransportInDescription trpInDesc
                = new TransportInDescription(MailConstants.TRANSPORT_NAME);
            trpInDesc.setReceiver(new MailTransportListener());
            return trpInDesc;
        }
    
        @Override
        protected List<Parameter> getServiceParameters(String contentType) throws Exception {
            List<Parameter> parameters = new ArrayList<Parameter>();
            parameters.add(new Parameter("transport.mail.Protocol", "test-store"));
            parameters.add(new Parameter("transport.mail.Address", ADDRESS));
            parameters.add(new Parameter("transport.PollInterval", "1"));
            // TODO: logically, this should be mail.test-store.user and mail.test-store.password
            parameters.add(new Parameter("mail.pop3.user", ADDRESS));
            parameters.add(new Parameter("mail.pop3.password", "dummy"));
            return parameters;
        }
    
        @Override
        protected void sendMessage(String endpointReference,
                                   String contentType,
                                   byte[] content) throws Exception {
            Properties props = new Properties();
            props.put("mail.smtp.class", TestTransport.class.getName());
            Session session = Session.getInstance(props);
            MimeMessage msg = new MimeMessage(session);
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(ADDRESS));
            msg.setFrom(new InternetAddress("test-sender@localhost"));
            msg.setSentDate(new Date());
            msg.setDataHandler(new DataHandler(new ByteArrayDataSource(content, contentType)));
            Transport.send(msg);
        }
    }
    
    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        TestStrategy strategy = new TestStrategyImpl();
        addSOAPTests(strategy, suite);
        // TODO: POX tests don't work yet for mail transport
        // addPOXTests(strategy, suite);
        // Temporarily skip this test until we know why it fails.
        // addSwATests(strategy, suite);
        // Temporarily skip the following tests until SYNAPSE-359 is solved
        // addTextPlainTests(strategy, suite);
        // addBinaryTest(strategy, suite);
        return suite;
    }
}
