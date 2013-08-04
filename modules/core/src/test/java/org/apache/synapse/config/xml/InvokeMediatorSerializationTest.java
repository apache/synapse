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

public class InvokeMediatorSerializationTest extends AbstractTestCase {

    private InvokeMediatorFactory factory;
    private InvokeMediatorSerializer serializer;

    public InvokeMediatorSerializationTest() {
        super(InvokeMediatorSerializationTest.class.getName());
        factory = new InvokeMediatorFactory();
        serializer = new InvokeMediatorSerializer();
    }

    public void testInvokeMediatorSerializationScenarioOne() throws Exception {
        String inputXml = "<call-template xmlns=\"http://ws.apache.org/ns/synapse\" " +
                "target=\"foo\"/>";
        assertTrue(serialization(inputXml, factory, serializer));
        assertTrue(serialization(inputXml, serializer));
    }

    public void testInvokeMediatorSerializationScenarioTwo() throws Exception {
        String inputXml = "<call-template xmlns=\"http://ws.apache.org/ns/synapse\" " +
                "target=\"foo\"><with-param name=\"bar\" value=\"bar_value\"/></call-template>";
        assertTrue(serialization(inputXml, factory, serializer));
        assertTrue(serialization(inputXml, serializer));
    }

    public void testInvokeMediatorSerializationScenarioThree() throws Exception {
        String inputXml = "<call-template xmlns=\"http://ws.apache.org/ns/synapse\" " +
                "target=\"foo\"><with-param name=\"bar\" value=\"bar_value\"/>" +
                "<with-param name=\"bar2\" value=\"bar2_value\"/>" +
                "<with-param name=\"bar3\" value=\"bar3_value\"/></call-template>";
        System.out.println(inputXml);
        System.out.println(serializer.serializeSpecificMediator(factory.createSpecificMediator(createOMElement(inputXml), null)));
        assertTrue(serialization(inputXml, factory, serializer));
        assertTrue(serialization(inputXml, serializer));
    }
}
