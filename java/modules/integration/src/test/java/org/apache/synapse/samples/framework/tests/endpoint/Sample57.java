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
package org.apache.synapse.samples.framework.tests.endpoint;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.samples.framework.SampleClientResult;
import org.apache.synapse.samples.framework.SynapseTestCase;
import org.apache.synapse.samples.framework.clients.StockQuoteSampleClient;

public class Sample57 extends SynapseTestCase {

    private static final Log log = LogFactory.getLog(Sample57.class);
    SampleClientResult result;
    StockQuoteSampleClient client;
    String addUrl;

    public Sample57() {
        super(57);
        client = getStockQuoteClient();
    }

    public void testDynamicLB() {
        addUrl = "http://localhost:8280/services/LBService1";

        log.info("Running test: Dynamic load balancing between 3 nodes");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {

        }
        new Thread(new Runnable() {
            public void run() {
                result = client.sessionlessClient(addUrl, null, 500);
            }
        }).start();
//            try {
//                Thread.sleep(2000);
//            } catch (InterruptedException e) {
//
//            }
//            getbESControllers().get("SampleServer0").stop();
//
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {

        }
        new Thread(new Runnable() {
            public void run() {
                result = client.sessionlessClient(addUrl, null, 500);
            }
        }).start();
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {

        }

        assertTrue("Did not receive a response", result.gotResponse());
    }
}
