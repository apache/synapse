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

public class PayloadFactoryMediatorSerializationTest extends AbstractTestCase {

    private PayloadFactoryMediatorFactory payloadFactoryMediatorFactory;
    private PayloadFactoryMediatorSerializer payloadFactoryMediatorSerializer;

    public PayloadFactoryMediatorSerializationTest() {
        super(AbstractTestCase.class.getName());
        payloadFactoryMediatorFactory = new PayloadFactoryMediatorFactory();
        payloadFactoryMediatorSerializer = new PayloadFactoryMediatorSerializer();
    }

    public void testPayloadFactoryMediatorStaticArgsSerialization() throws Exception {

        String inputXml = "<payloadFactory xmlns=\"http://ws.apache.org/ns/synapse\">" +
                "<format><foo><bar>$1</bar><batz>X</batz></foo></format><args><arg value=\"One\"/></args>" +
                "</payloadFactory>";

        assertTrue(serialization(inputXml, payloadFactoryMediatorFactory, payloadFactoryMediatorSerializer));
        assertTrue(serialization(inputXml, payloadFactoryMediatorSerializer));
    }

    public void testPayloadFactoryMediatorExpressionArgsSerialization() throws Exception {

        String inputXml = "<payloadFactory xmlns=\"http://ws.apache.org/ns/synapse\">" +
                "<format><foo><bar>$1</bar><batz>X</batz></foo></format>" +
                "<args><arg expression=\"get-property('foo')\"/><arg value=\"Two\"/></args></payloadFactory>";

        assertTrue(serialization(inputXml, payloadFactoryMediatorFactory, payloadFactoryMediatorSerializer));
        assertTrue(serialization(inputXml, payloadFactoryMediatorSerializer));
    }

    public void testPayloadFactoryMediatorFormatWithNamespaceSerialization() throws Exception {

        String inputXml = "<payloadFactory xmlns=\"http://ws.apache.org/ns/synapse\">" +
                "<format><m:getQuote xmlns:m=\"http://services.samples\"><m:request><m:symbol>$1</m:symbol>" +
                "</m:request></m:getQuote></format><args><arg xmlns:m0=\"http://services.samples\" " +
                "expression=\"//m0:Code\"/></args></payloadFactory>";

        assertTrue(serialization(inputXml, payloadFactoryMediatorFactory, payloadFactoryMediatorSerializer));
        assertTrue(serialization(inputXml, payloadFactoryMediatorSerializer));
    }

    public void testPayloadFactoryMediatorMultiArgsSerialization() throws Exception {

        String inputXml = "<payloadFactory xmlns=\"http://ws.apache.org/ns/synapse\">" +
                "<format><m:CheckPriceResponse xmlns:m=\"http://services.samples/xsd\"><m:Code>$1</m:Code>" +
                "<m:Price>$2</m:Price></m:CheckPriceResponse></format><args>" +
                "<arg xmlns:m0=\"http://services.samples/xsd\" expression=\"//m0:symbol\"/>" +
                "<arg xmlns:m0=\"http://services.samples/xsd\" expression=\"//m0:last\"/></args></payloadFactory>";

        assertTrue(serialization(inputXml, payloadFactoryMediatorFactory, payloadFactoryMediatorSerializer));
        assertTrue(serialization(inputXml, payloadFactoryMediatorSerializer));
    }

}
