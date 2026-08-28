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
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.samples.framework.tests.advanced;

import org.apache.synapse.samples.framework.SampleClientResult;
import org.apache.synapse.samples.framework.SynapseTestCase;
import org.apache.synapse.samples.framework.clients.StockQuoteSampleClient;

public class Sample706 extends SynapseTestCase {
    public Sample706() {
        super(706);
    }

    public void testJDBCMessageStore() throws InterruptedException {
        String trpUrl = "http://localhost:8280/";
        StockQuoteSampleClient client = getStockQuoteClient();
        log.info("Running test: Introduction to JDBC message store ");
        SampleClientResult result = client.placeOrder(null, trpUrl, null, "WSO2-1");
        assertTrue("Client received response successfully ", result.responseReceived());
        Thread.sleep(7000);
        assertEquals(1, getAxis2Server().getMessageCount("SimpleStockQuoteService", "placeOrder"));
    }
}
