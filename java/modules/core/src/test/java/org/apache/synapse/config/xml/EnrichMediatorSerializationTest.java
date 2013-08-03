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

public class EnrichMediatorSerializationTest extends AbstractTestCase {

    private EnrichMediatorFactory factory;
    private EnrichMediatorSerializer serializer;

    public EnrichMediatorSerializationTest() {
        super(EnrichMediatorSerializationTest.class.getName());
        factory = new EnrichMediatorFactory();
        serializer = new EnrichMediatorSerializer();
    }

    public void testEnrichMediatorSerializationScenarioOne() throws Exception {
        String inputXml = "<enrich xmlns=\"http://ws.apache.org/ns/synapse\">" +
                "<source type=\"envelope\"/>" +
                "<target type=\"body\"/>" +
                "</enrich>";
        assertTrue(serialization(inputXml, factory, serializer));
        assertTrue(serialization(inputXml, serializer));
    }

    public void testEnrichMediatorSerializationScenarioTwo() throws Exception {
        String inputXml = "<enrich xmlns=\"http://ws.apache.org/ns/synapse\">" +
                "<source type=\"property\" property=\"p1\"/>" +
                "<target type=\"body\"/>" +
                "</enrich>";
        assertTrue(serialization(inputXml, factory, serializer));
        assertTrue(serialization(inputXml, serializer));
    }

    public void testEnrichMediatorSerializationScenarioThree() throws Exception {
        String inputXml = "<enrich xmlns=\"http://ws.apache.org/ns/synapse\">" +
                "<source type=\"property\" property=\"p1\"/>" +
                "<target type=\"property\" property=\"p2\"/>" +
                "</enrich>";
        assertTrue(serialization(inputXml, factory, serializer));
        assertTrue(serialization(inputXml, serializer));
    }

    public void testEnrichMediatorSerializationScenarioFour() throws Exception {
        String inputXml = "<enrich xmlns=\"http://ws.apache.org/ns/synapse\">" +
                "<source type=\"envelope\" clone=\"false\"/>" +
                "<target type=\"body\"/>" +
                "</enrich>";
        assertTrue(serialization(inputXml, factory, serializer));
        assertTrue(serialization(inputXml, serializer));
    }

    public void testEnrichMediatorSerializationScenarioFive() throws Exception {
        String inputXml = "<enrich xmlns=\"http://ws.apache.org/ns/synapse\">" +
                "<source xpath=\"//foo/bar\"/>" +
                "<target type=\"body\"/>" +
                "</enrich>";
        assertTrue(serialization(inputXml, factory, serializer));
        assertTrue(serialization(inputXml, serializer));
    }

    public void testEnrichMediatorSerializationScenarioSix() throws Exception {
        String inputXml = "<enrich xmlns=\"http://ws.apache.org/ns/synapse\">" +
                "<source type=\"property\" property=\"p1\"/>" +
                "<target xpath=\"//foo/bar\"/>" +
                "</enrich>";
        assertTrue(serialization(inputXml, factory, serializer));
        assertTrue(serialization(inputXml, serializer));
    }

    public void testEnrichMediatorSerializationScenarioSeven() throws Exception {
        String inputXml = "<enrich xmlns=\"http://ws.apache.org/ns/synapse\">" +
                "<source type=\"inline\"><foo><bar>text</bar></foo></source>" +
                "<target type=\"body\"/>" +
                "</enrich>";
        assertTrue(serialization(inputXml, factory, serializer));
        assertTrue(serialization(inputXml, serializer));
    }

    public void testEnrichMediatorSerializationScenarioEight() throws Exception {
        String inputXml = "<enrich xmlns=\"http://ws.apache.org/ns/synapse\">" +
                "<source type=\"inline\">foo</source>" +
                "<target type=\"body\"/>" +
                "</enrich>";
        assertTrue(serialization(inputXml, factory, serializer));
        assertTrue(serialization(inputXml, serializer));
    }

    public void testEnrichMediatorSerializationScenarioNine() throws Exception {
        String inputXml = "<enrich xmlns=\"http://ws.apache.org/ns/synapse\">" +
                "<source type=\"inline\" key=\"k1\"/>" +
                "<target type=\"body\"/>" +
                "</enrich>";
        assertTrue(serialization(inputXml, factory, serializer));
        assertTrue(serialization(inputXml, serializer));
    }

    public void testEnrichMediatorSerializationScenarioTen() throws Exception {
        String inputXml = "<enrich xmlns=\"http://ws.apache.org/ns/synapse\">" +
                "<source type=\"property\" property=\"p1\"/>" +
                "<target action=\"child\" xpath=\"//foo/bar\"/>" +
                "</enrich>";
        assertTrue(serialization(inputXml, factory, serializer));
        assertTrue(serialization(inputXml, serializer));
    }

    public void testEnrichMediatorSerializationScenarioEleven() throws Exception {
        String inputXml = "<enrich xmlns=\"http://ws.apache.org/ns/synapse\">" +
                "<source type=\"property\" property=\"p1\"/>" +
                "<target action=\"sibling\" xpath=\"//foo/bar\"/>" +
                "</enrich>";
        assertTrue(serialization(inputXml, factory, serializer));
        assertTrue(serialization(inputXml, serializer));
    }
}
