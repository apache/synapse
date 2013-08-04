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

public class MessageStoreMediatorSerializationTest extends AbstractTestCase {

    private MessageStoreMediatorFactory factory;
    private MessageStoreMediatorSerializer serializer;

    public MessageStoreMediatorSerializationTest() {
        super(MessageStoreMediatorSerializationTest.class.getName());
        factory = new MessageStoreMediatorFactory();
        serializer = new MessageStoreMediatorSerializer();
    }

    public void testStoreMediatorSerializationScenarioOne() throws Exception {
        String inputXml = "<store xmlns=\"http://ws.apache.org/ns/synapse\" " +
                "messageStore=\"foo\"/>";
        assertTrue(serialization(inputXml, factory, serializer));
        assertTrue(serialization(inputXml, serializer));
    }

    public void testStoreMediatorSerializationScenarioTwo() throws Exception {
        String inputXml = "<store xmlns=\"http://ws.apache.org/ns/synapse\" " +
                "messageStore=\"foo\" sequence=\"bar\"/>";
        assertTrue(serialization(inputXml, factory, serializer));
        assertTrue(serialization(inputXml, serializer));
    }
}
