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

import org.apache.axiom.om.OMElement;
import org.apache.synapse.message.store.MessageStore;

import java.util.Properties;

/**
 * Class <code>MessageStoreSerializationTest</code> implements the test cases for XML serialization
 * scenarios for the synapse message store
 */
public class MessageStoreSerializationTest extends AbstractTestCase {

    public MessageStoreSerializationTest() {
        super(MessageStoreSerializationTest.class.getName());
    }

    /**
     * Test case for InMemory Message Store Configuration Factory and serializer with no parameters.
     */
    public void testMessageStoreSerialization() {
        String messageStoreConfiguration = "<syn:messageStore xmlns:syn=\"" +
                "http://ws.apache.org/ns/synapse\"" +
                " name=\"foo\" >" +
                "</syn:messageStore>";

        OMElement messageStoreElement = createOMElement(messageStoreConfiguration);
        MessageStore messageStore = MessageStoreFactory.createMessageStore(messageStoreElement,
                new Properties());
        OMElement serializedElement = MessageStoreSerializer.serializeMessageStore(null,
                messageStore);

        assertTrue(compare(messageStoreElement, serializedElement));

    }

    /**
     * Test case for InMemory Message Store configuration Factory and serializer with parameters
     */
    public void testMessageStoreSerializationWithParameters() {
        String messageStoreConfiguration = "<syn:messageStore xmlns:syn=\"" +
                "http://ws.apache.org/ns/synapse\"" +
                " name=\"foo\" >" +
                "<syn:parameter name=\"testName1\">testValue1</syn:parameter>" +
                "<syn:parameter name=\"testName2\">testValue2</syn:parameter>" +
                "</syn:messageStore>";

        OMElement messageStoreElement = createOMElement(messageStoreConfiguration);
        MessageStore messageStore = MessageStoreFactory.createMessageStore(messageStoreElement,
                new Properties());
        OMElement serializedElement = MessageStoreSerializer.serializeMessageStore(null,
                messageStore);

        assertTrue(compare(messageStoreElement, serializedElement));

    }



}
