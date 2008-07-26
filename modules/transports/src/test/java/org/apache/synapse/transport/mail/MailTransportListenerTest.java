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
import java.util.Properties;

import javax.activation.DataHandler;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.TransportInDescription;
import org.apache.synapse.transport.testkit.listener.ContentTypeMode;
import org.apache.synapse.transport.testkit.listener.ListenerTestSetup;
import org.apache.synapse.transport.testkit.listener.ListenerTestSuite;
import org.apache.synapse.transport.testkit.listener.BinaryPayloadSender;

public class MailTransportListenerTest extends TestCase {
    private static final String ADDRESS = "test-account@localhost";
    
    public static class TestStrategyImpl extends ListenerTestSetup {
        @Override
        public TransportInDescription createTransportInDescription() {
            TransportInDescription trpInDesc
                = new TransportInDescription(MailConstants.TRANSPORT_NAME);
            trpInDesc.setReceiver(new MailTransportListener());
            return trpInDesc;
        }
    
        @Override
        public void setupService(AxisService service) throws Exception {
            service.addParameter("transport.mail.Protocol", "test-store");
            service.addParameter("transport.mail.Address", ADDRESS);
            service.addParameter("transport.PollInterval", "1");
            // TODO: logically, this should be mail.test-store.user and mail.test-store.password
            service.addParameter("mail.pop3.user", ADDRESS);
            service.addParameter("mail.pop3.password", "dummy");
        }
    }
    
    private static abstract class MailSender extends BinaryPayloadSender {
        @Override
        public void sendMessage(ListenerTestSetup setup, String endpointReference, String contentType, byte[] content) throws Exception {
            Properties props = new Properties();
            props.put("mail.smtp.class", TestTransport.class.getName());
            Session session = Session.getInstance(props);
            MimeMessage msg = new MimeMessage(session);
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(ADDRESS));
            msg.setFrom(new InternetAddress("test-sender@localhost"));
            msg.setSentDate(new Date());
            DataHandler dh = new DataHandler(new ByteArrayDataSource(content, contentType));
            setupMessage(msg, dh);
            Transport.send(msg);
        }
        
        protected abstract void setupMessage(MimeMessage msg, DataHandler dh) throws Exception;
    }
    
    private static class MimeSender extends MailSender {
        @Override
        protected void setupMessage(MimeMessage msg, DataHandler dh) throws Exception {
            msg.setDataHandler(dh);
        }
    }
    
    private static class MultipartSender extends MailSender {
        @Override
        protected void setupMessage(MimeMessage msg, DataHandler dh) throws Exception {
            MimeMultipart multipart = new MimeMultipart();
            MimeBodyPart part1 = new MimeBodyPart();
            part1.setContent("This is an automated message.", "text/plain");
            multipart.addBodyPart(part1);
            MimeBodyPart part2 = new MimeBodyPart();
            part2.setDataHandler(dh);
            multipart.addBodyPart(part2);
            msg.setContent(multipart);
        }
    }
    
    public static TestSuite suite() {
        ListenerTestSuite suite = new ListenerTestSuite();
        ListenerTestSetup setup = new TestStrategyImpl();
        for (BinaryPayloadSender sender : new BinaryPayloadSender[] { new MimeSender(), new MultipartSender() }) {
            // TODO: SOAP 1.2 tests don't work yet for mail transport
            suite.addSOAP11Test(setup, sender, ContentTypeMode.TRANSPORT, ListenerTestSuite.ASCII_TEST_DATA);
            suite.addSOAP11Test(setup, sender, ContentTypeMode.TRANSPORT, ListenerTestSuite.UTF8_TEST_DATA);
            // TODO: this test fails when using multipart
            if (sender instanceof MimeSender) {
                suite.addSOAP11Test(setup, sender, ContentTypeMode.TRANSPORT, ListenerTestSuite.LATIN1_TEST_DATA);
            }
            // addSOAPTests(strategy, suite);
            // TODO: POX tests don't work yet for mail transport
            // addPOXTests(strategy, suite);
            // Temporarily skip this test until we know why it fails.
            // addSwATests(strategy, suite);
            // Temporarily skip the following tests until SYNAPSE-359 is solved
            // addTextPlainTests(strategy, suite);
            // addBinaryTest(strategy, suite);
        }
        return suite;
    }
}
