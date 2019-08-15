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

package org.apache.synapse.config.xml;

public class SamplingThrottleMediatorSerializationTest extends AbstractTestCase {

    private SamplingThrottleMediatorFactory factory;
    private SamplingThrottleMediatorSerializer serializer;

    public SamplingThrottleMediatorSerializationTest() {
        super(SamplingThrottleMediatorSerializationTest.class.getName());
        factory = new SamplingThrottleMediatorFactory();
        serializer = new SamplingThrottleMediatorSerializer();
    }

    public void testSamplerMediatorSerializationScenarioOne() throws Exception {
        String inputXml = "<sampler xmlns=\"http://ws.apache.org/ns/synapse\">" +
                "<target sequence=\"foo\"/></sampler>";
        assertTrue(serialization(inputXml, factory, serializer));
        assertTrue(serialization(inputXml, serializer));
    }

    public void testSamplerMediatorSerializationScenarioTwo() throws Exception {
        String inputXml = "<sampler xmlns=\"http://ws.apache.org/ns/synapse\" rate=\"10\" unitTime=\"60000\">" +
                "<target sequence=\"foo\"/></sampler>";
        assertTrue(serialization(inputXml, factory, serializer));
        assertTrue(serialization(inputXml, serializer));
    }

    public void testSamplerMediatorSerializationScenarioThree() throws Exception {
        String inputXml = "<sampler xmlns=\"http://ws.apache.org/ns/synapse\" id=\"bar\">" +
                "<target sequence=\"foo\"/></sampler>";
        assertTrue(serialization(inputXml, factory, serializer));
        assertTrue(serialization(inputXml, serializer));
    }

    public void testSamplerMediatorSerializationScenarioFour() throws Exception {
        String inputXml = "<sampler xmlns=\"http://ws.apache.org/ns/synapse\">" +
                "<messageQueue class=\"org.apache.synapse.mediators.eip.sample.UnboundedMessageQueue\"/>" +
                "<target sequence=\"foo\"/></sampler>";
        assertTrue(serialization(inputXml, factory, serializer));
        assertTrue(serialization(inputXml, serializer));
    }

    public void testSamplerMediatorSerializationScenarioFive() throws Exception {
        String inputXml = "<sampler xmlns=\"http://ws.apache.org/ns/synapse\" id=\"test\" rate=\"10\" unitTime=\"60000\">" +
                "<messageQueue class=\"org.apache.synapse.mediators.eip.sample.UnboundedMessageQueue\"/>" +
                "<target sequence=\"foo\"/></sampler>";
        assertTrue(serialization(inputXml, factory, serializer));
        assertTrue(serialization(inputXml, serializer));
    }

    public void testSamplerMediatorSerializationScenarioSix() throws Exception {
        String inputXml = "<sampler xmlns=\"http://ws.apache.org/ns/synapse\" id=\"test\" rate=\"10\" unitTime=\"60000\">" +
                "<messageQueue class=\"org.apache.synapse.mediators.eip.sample.UnboundedMessageQueue\"/>" +
                "<target><sequence><log/></sequence></target></sampler>";
        assertTrue(serialization(inputXml, factory, serializer));
        assertTrue(serialization(inputXml, serializer));
    }
}
