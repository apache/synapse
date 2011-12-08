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

import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.samples.framework.SampleClientResult;
import org.apache.synapse.samples.framework.SynapseTestCase;
import org.apache.synapse.samples.framework.clients.StockQuoteSampleClient;

public class Sample53 extends SynapseTestCase {

    private static final Log log = LogFactory.getLog(Sample53.class);

    private SampleClientResult result;
    private StockQuoteSampleClient client;
    private String addUrl;

    public Sample53() {
        super(53);
        client = getStockQuoteClient();
    }


    public void testFailOver() {
        String expectedError = "COULDN'T SEND THE MESSAGE TO THE SERVER";
        addUrl = "http://localhost:8280/services/LBService1";
        log.info("Running test: Failover sending among 3 endpoints");

        // Send some messages and check
        Thread t = new Thread(new Runnable() {
            public void run() {
                result = client.sessionlessClient(addUrl, null, 10);
            }
        });
        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {

        }
        assertResponseReceived(result);

        // Stop BE server 1
        getBackendServerControllers().get(0).stop();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {

        }

        // Send another burst of messages and check
        t = new Thread(new Runnable() {
            public void run() {
                result = client.sessionlessClient(addUrl, null, 10);
            }
        });
        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {

        }
        assertResponseReceived(result);

        // Stop BE server 2
        getBackendServerControllers().get(1).stop();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {

        }

        // Send some more messages and check
        t = new Thread(new Runnable() {
            public void run() {
                result = client.sessionlessClient(addUrl, null, 10);
            }
        });
        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {

        }
        assertResponseReceived(result);

        // Stop BE server 3
        getBackendServerControllers().get(2).stop();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {

        }

        // Send another message - Should fail
        result = client.sessionlessClient(addUrl, null, 1);
        Exception resultEx = result.getException();
        assertNotNull("Did not receive expected error", resultEx);
        log.info("Got an error as expected: " + resultEx.getMessage());
        assertTrue("Did not receive expected error", resultEx instanceof AxisFault);
        assertTrue("Did not receive expected error", resultEx.getMessage().contains(expectedError));
    }

}
