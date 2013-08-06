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

package org.apache.synapse.endpoints.algorithms;

import junit.framework.TestCase;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.synapse.MessageContext;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.endpoints.AddressEndpoint;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.mediators.TestUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class RoundRobinAlgorithmTest extends TestCase {

    private List<Endpoint> endpoints;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        endpoints = new ArrayList<Endpoint>();
    }

    @Override
    protected void tearDown() throws Exception {
        for (Endpoint endpoint : endpoints) {
            endpoint.destroy();
        }
    }

    public void testRoundRobin() throws Exception {
        AxisConfiguration axisConfig = new AxisConfiguration();
        ConfigurationContext configContext = new ConfigurationContext(axisConfig);
        SynapseConfiguration synapseConfig = new SynapseConfiguration();
        SynapseEnvironment env = new Axis2SynapseEnvironment(configContext, synapseConfig);

        endpoints.add(getEndpoint("ep1", env));
        endpoints.add(getEndpoint("ep2", env));
        endpoints.add(getEndpoint("ep3", env));
        RoundRobin roundRobin = new RoundRobin(endpoints);

        AlgorithmContext context = new AlgorithmContext(true, configContext, "ep");

        MessageContext messageContext = TestUtils.createLightweightSynapseMessageContext("<test/>");
        Endpoint endpoint = roundRobin.getNextEndpoint(messageContext, context);
        assertEquals("ep1", endpoint.getName());

        messageContext = TestUtils.createLightweightSynapseMessageContext("<test/>");
        endpoint = roundRobin.getNextEndpoint(messageContext, context);
        assertEquals("ep2", endpoint.getName());

        messageContext = TestUtils.createLightweightSynapseMessageContext("<test/>");
        endpoint = roundRobin.getNextEndpoint(messageContext, context);
        assertEquals("ep3", endpoint.getName());

        messageContext = TestUtils.createLightweightSynapseMessageContext("<test/>");
        endpoint = roundRobin.getNextEndpoint(messageContext, context);
        assertEquals("ep1", endpoint.getName());

        messageContext = TestUtils.createLightweightSynapseMessageContext("<test/>");
        endpoint = roundRobin.getNextEndpoint(messageContext, context);
        assertEquals("ep2", endpoint.getName());

        messageContext = TestUtils.createLightweightSynapseMessageContext("<test/>");
        endpoint = roundRobin.getNextEndpoint(messageContext, context);
        assertEquals("ep3", endpoint.getName());
    }

    public void testRoundRobinConcurrency() throws Exception {
        // This test case sets up the load balancer with 10 endpoints and sends request using
        // 10 concurrent threads. Each thread should get a separate endpoint.
        int concurrency = 10;

        AxisConfiguration axisConfig = new AxisConfiguration();
        ConfigurationContext configContext = new ConfigurationContext(axisConfig);
        SynapseConfiguration synapseConfig = new SynapseConfiguration();
        SynapseEnvironment env = new Axis2SynapseEnvironment(configContext, synapseConfig);

        for (int i = 0; i < concurrency; i++) {
            endpoints.add(getEndpoint("ep" + i, env));
        }
        RoundRobin roundRobin = new RoundRobin(endpoints);
        AlgorithmContext context = new AlgorithmContext(true, configContext, "ep");

        ExecutorService exec = Executors.newFixedThreadPool(concurrency);
        try {
            TestWorker[] workers = new TestWorker[concurrency];
            Future[]  futures = new Future[10];
            for (int i = 0; i < concurrency; i++) {
                workers[i] = new TestWorker(context, roundRobin);
                futures[i] = exec.submit(workers[i]);
            }
            Set<Integer> results = new HashSet<Integer>();
            for (int i = 0; i < concurrency; i++) {
                futures[i].get();
                if (workers[i].endpointIndex < 0) {
                    fail("At least one test worker finished abnormally");
                }
                results.add(workers[i].endpointIndex);
            }

            assertEquals(concurrency, results.size());
        } finally {
            exec.shutdownNow();
        }
    }

    private Endpoint getEndpoint(String name, SynapseEnvironment env) {
        AddressEndpoint endpoint = new AddressEndpoint();
        endpoint.setEnableMBeanStats(false);
        endpoint.setName(name);
        endpoint.init(env);
        return endpoint;
    }

    private static class TestWorker implements Runnable {

        AlgorithmContext context;
        RoundRobin roundRobin;
        int endpointIndex = -1;

        private TestWorker(AlgorithmContext context, RoundRobin roundRobin) {
            this.context = context;
            this.roundRobin = roundRobin;
        }

        public void run() {
            try {
                MessageContext messageContext = TestUtils.createLightweightSynapseMessageContext("<test/>");
                Endpoint ep = roundRobin.getNextEndpoint(messageContext, context);
                String epName = ep.getName();
                endpointIndex = Integer.parseInt(epName.substring(2));
            } catch (Exception ignored) {
            }
        }
    }
}
