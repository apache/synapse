/*
 *  Copyright (c) 2005-2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.apache.synapse.registry;

import junit.framework.TestCase;
import org.apache.synapse.MessageContext;
import org.apache.synapse.Mediator;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.mediators.TestUtils;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.axiom.om.OMNode;

import java.util.Map;
import java.util.HashMap;

public class DynamicResourceTest extends TestCase {

    private static final String DYNAMIC_ENDPOINT_1 =
            "<endpoint xmlns=\"http://ws.apache.org/ns/synapse\">\n" +
            "    <address uri=\"http://localhost:9000/services/SimpleStockQuoteService\"/>\n" +
            "</endpoint>";

    private static final String DYNAMIC_SEQUENCE_1 =
            "<sequence xmlns=\"http://ws.apache.org/ns/synapse\" name=\"seq1\">\n" +
            "    <property name=\"foo\" value=\"bar\" />" +
            "</sequence>";

    private static final String DYNAMIC_SEQUENCE_2 =
            "<sequence xmlns=\"http://ws.apache.org/ns/synapse\" name=\"seq1\">\n" +
            "    <property name=\"foo\" value=\"baz\" />" +
            "</sequence>";

    private static final String KEY_DYNAMIC_SEQUENCE_1 = "dynamic_sequence_1";
    private static final String KEY_DYNAMIC_ENDPOINT_1 = "dynamic_endpoint_1";


    private SimpleInMemoryRegistry registry;
    private SynapseConfiguration config;

    public void setUp() {
        System.out.println("Initializing in-memory registry for dynamic resource tests...");

        Map<String, OMNode> data = new HashMap<String, OMNode>();
        data.put(KEY_DYNAMIC_ENDPOINT_1, TestUtils.createOMElement(DYNAMIC_ENDPOINT_1));
        data.put(KEY_DYNAMIC_SEQUENCE_1, TestUtils.createOMElement(DYNAMIC_SEQUENCE_1));

        registry = new SimpleInMemoryRegistry(data, 8000L);
        config = new SynapseConfiguration();
        config.setRegistry(registry);
    }

    public void testDynamicSequenceLookup() throws Exception {
        System.out.println("Testing dynamic sequence lookup...");

        // Phase 1
        System.out.println("Testing basic registry lookup functionality...");
        MessageContext synCtx = TestUtils.createLightweightSynapseMessageContext("<empty/>", config);
        Mediator seq1 = synCtx.getSequence(KEY_DYNAMIC_SEQUENCE_1);
        assertNotNull(seq1);
        assertTrue(((SequenceMediator) seq1).isInitialized());
        assertEquals(1, registry.getHitCount());
        seq1.mediate(synCtx);
        assertEquals("bar", synCtx.getProperty("foo"));

        // Phase 2
        System.out.println("Testing basic sequence caching...");
        synCtx = TestUtils.createLightweightSynapseMessageContext("<empty/>", config);
        Mediator seq2 = synCtx.getSequence(KEY_DYNAMIC_SEQUENCE_1);
        assertNotNull(seq2);
        assertTrue(((SequenceMediator) seq2).isInitialized());
        assertEquals(1, registry.getHitCount());
        seq2.mediate(synCtx);
        assertEquals("bar", synCtx.getProperty("foo"));
        assertTrue(seq1 == seq2);

        // Phase 3
        System.out.println("Testing advanced sequence caching...");
        synCtx = TestUtils.createLightweightSynapseMessageContext("<empty/>", config);
        System.out.println("Waiting for the cache to expire...");
        Thread.sleep(8500L);
        Mediator seq3 = synCtx.getSequence(KEY_DYNAMIC_SEQUENCE_1);
        assertNotNull(seq3);
        assertTrue(((SequenceMediator) seq3).isInitialized());
        assertEquals(1, registry.getHitCount());
        seq3.mediate(synCtx);
        assertEquals("bar", synCtx.getProperty("foo"));
        assertTrue(seq1 == seq3);

        // Phase 4
        System.out.println("Testing sequence reloading...");
        registry.updateResource(KEY_DYNAMIC_SEQUENCE_1, TestUtils.createOMElement(DYNAMIC_SEQUENCE_2));
        System.out.println("Waiting for the cache to expire...");
        Thread.sleep(8500L);
        synCtx = TestUtils.createLightweightSynapseMessageContext("<empty/>", config);
        Mediator seq4 = synCtx.getSequence(KEY_DYNAMIC_SEQUENCE_1);
        assertNotNull(seq4);
        assertTrue(((SequenceMediator) seq4).isInitialized());
        assertEquals(2, registry.getHitCount());
        seq4.mediate(synCtx);
        assertEquals("baz", synCtx.getProperty("foo"));
        assertTrue(seq1 != seq4);
        assertTrue(!((SequenceMediator) seq1).isInitialized());

        System.out.println("Dynamic sequence lookup tests were successful...");
    }
}
