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

import org.apache.synapse.samples.framework.SynapseTestCase;
import org.apache.synapse.samples.framework.clients.StockQuoteSampleClient;

public class Sample707 extends SynapseTestCase {
    public Sample707() {
        super(707);
    }

    public void testResequenceMessageStore() throws InterruptedException {
        String trpUrl = "http://localhost:8280/";
        StockQuoteSampleClient client = getStockQuoteClient();
        log.info("Running test: Introduction to the script mediator using js scripts ");
        client.placeOrder(null, trpUrl, null, "WSO2-4");
        client.placeOrder(null, trpUrl, null, "WSO2-2");
        client.placeOrder(null, trpUrl, null, "WSO2-3");
        client.placeOrder(null, trpUrl, null, "WSO2-1");
        Thread.sleep(10000);
        assertEquals(4, getAxis2Server().getMessageCount("SimpleStockQuoteService", "placeOrder"));

    }
}
