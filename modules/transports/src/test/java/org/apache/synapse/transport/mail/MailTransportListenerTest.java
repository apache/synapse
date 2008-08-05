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

import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.synapse.transport.testkit.listener.Adapter;
import org.apache.synapse.transport.testkit.listener.AsyncMessageSender;
import org.apache.synapse.transport.testkit.listener.ContentTypeMode;
import org.apache.synapse.transport.testkit.listener.ListenerTestSuite;
import org.apache.synapse.transport.testkit.listener.XMLMessage;

public class MailTransportListenerTest extends TestCase {
    public static TestSuite suite() {
        ListenerTestSuite suite = new ListenerTestSuite();
        MailChannel channel = new MailChannel("test-account@localhost");
        List<MailSender> senders = new LinkedList<MailSender>();
        senders.add(new MimeSender());
        senders.add(new MultipartSender());
        for (MailSender sender : senders) {
            AsyncMessageSender<MailChannel,XMLMessage> xmlSender = new Adapter<MailChannel>(sender);
            // TODO: SOAP 1.2 tests don't work yet for mail transport
            suite.addSOAP11Test(channel, xmlSender, ContentTypeMode.TRANSPORT, ListenerTestSuite.ASCII_TEST_DATA);
            suite.addSOAP11Test(channel, xmlSender, ContentTypeMode.TRANSPORT, ListenerTestSuite.UTF8_TEST_DATA);
            // TODO: this test fails when using multipart
            if (sender instanceof MimeSender) {
                suite.addSOAP11Test(channel, xmlSender, ContentTypeMode.TRANSPORT, ListenerTestSuite.LATIN1_TEST_DATA);
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
