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

package org.apache.synapse.transport.nhttp;

import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.synapse.transport.testkit.listener.AxisAsyncMessageSender;
import org.apache.synapse.transport.testkit.listener.AxisRequestResponseMessageSender;
import org.apache.synapse.transport.testkit.listener.ContentTypeMode;
import org.apache.synapse.transport.testkit.listener.ListenerTestSuite;
import org.apache.synapse.transport.testkit.listener.XMLAsyncMessageSender;

public class HttpCoreNIOListenerTest extends TestCase {
    public static TestSuite suite() {
        ListenerTestSuite suite = new ListenerTestSuite();
        HttpChannel channel = new HttpChannel();
        JavaNetSender javaNetSender = new JavaNetSender();
        List<XMLAsyncMessageSender<? super HttpChannel>> senders = new LinkedList<XMLAsyncMessageSender<? super HttpChannel>>();
        senders.add(javaNetSender);
        senders.add(new AxisAsyncMessageSender());
        for (XMLAsyncMessageSender<? super HttpChannel> sender : senders) {
            suite.addSOAPTests(channel, sender, ContentTypeMode.TRANSPORT);
            suite.addPOXTests(channel, sender, ContentTypeMode.TRANSPORT);
        }
//        suite.addPOXTests(channel, new AxisRequestResponseMessageSender(), ContentTypeMode.TRANSPORT);
        suite.addSwATests(channel, javaNetSender);
        suite.addTextPlainTests(channel, javaNetSender, ContentTypeMode.TRANSPORT);
        suite.addBinaryTest(channel, javaNetSender, ContentTypeMode.TRANSPORT);
        suite.addRESTTests(channel, javaNetSender);
        return suite;
    }
}
