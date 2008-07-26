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

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.synapse.transport.testkit.listener.AxisMessageSender;
import org.apache.synapse.transport.testkit.listener.ContentTypeMode;
import org.apache.synapse.transport.testkit.listener.ListenerTestSetup;
import org.apache.synapse.transport.testkit.listener.ListenerTestSuite;
import org.apache.synapse.transport.testkit.listener.XMLMessageSender;

public class HttpCoreNIOListenerTest extends TestCase {
    public static TestSuite suite() {
        ListenerTestSuite suite = new ListenerTestSuite();
        ListenerTestSetup setup = new HttpCoreNIOListenerSetup();
        JavaNetSender javaNetSender = new JavaNetSender();
        for (XMLMessageSender sender : new XMLMessageSender[] { javaNetSender, new AxisMessageSender() }) {
            suite.addSOAPTests(setup, sender, ContentTypeMode.TRANSPORT);
            suite.addPOXTests(setup, sender, ContentTypeMode.TRANSPORT);
        }
        suite.addSwATests(setup, javaNetSender);
        suite.addTextPlainTests(setup, javaNetSender, ContentTypeMode.TRANSPORT);
        suite.addBinaryTest(setup, javaNetSender, ContentTypeMode.TRANSPORT);
        suite.addRESTTests(setup, javaNetSender);
        return suite;
    }
}
