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
import org.apache.synapse.message.processors.AbstractMessageProcessor;
import org.apache.synapse.message.processors.MessageProcessor;

/**
 * Class <code>MessageProcessorSerializationTest</code> implements test cases for XML Serialization
 * Scenarios for Synapse message processor.
 */
public class MessageProcessorSerializationTest extends AbstractTestCase {

    public MessageProcessorSerializationTest() {
        super(MessageProcessorSerializationTest.class.getName());
    }

    /**
     * Test the Message Processor Creation and Serialization
     * For a Basic Message processor with No parameters.
     */
    public void testMessageProcessorSerialization() {
        String messageProcessorConfig = "<syn:messageProcessor xmlns:syn=\"" +
                "http://ws.apache.org/ns/synapse\"" +
                " name=\"foo\" " +
                "class=\"org.apache.synapse.config.xml.MessageProcessorSerializationTest$TestMessageProcessor\" messageStore=\"bar\">" +
                "</syn:messageProcessor>";

        OMElement messageProcessorElement = createOMElement(messageProcessorConfig);
        MessageProcessor messageProcessor = MessageProcessorFactory.createMessageProcessor(messageProcessorElement);
        OMElement serializedElement = MessageProcessorSerializer.serializeMessageProcessor(null,
                messageProcessor);

        assertTrue(compare(messageProcessorElement, serializedElement));
    }

    /**
     * Test the Message Processor Creation and Serialization
     * For a Basic Message processor with parameters.
     */
    public void testMesssageProcessorSerializationWithParameters() {
        String messageProcessorConfig = "<syn:messageProcessor xmlns:syn=\"" +
                "http://ws.apache.org/ns/synapse\"" +
                " name=\"foo\" " +
                "class=\"org.apache.synapse.config.xml.MessageProcessorSerializationTest$TestMessageProcessor\" messageStore=\"bar\">" +
                "<syn:parameter name=\"testName1\">testValue1</syn:parameter>" +
                "<syn:parameter name=\"testName2\">testValue2</syn:parameter>" +
                "</syn:messageProcessor>";

        OMElement messageProcessorElement = createOMElement(messageProcessorConfig);
        MessageProcessor messageProcessor = MessageProcessorFactory.createMessageProcessor(messageProcessorElement);
        OMElement serializedElement = MessageProcessorSerializer.serializeMessageProcessor(null,
                messageProcessor);

        assertTrue(compare(messageProcessorElement, serializedElement));
    }

    /**
     * Test the Message Processor Creation and Serialization
     * For a Basic Message processor with expressions.
     */
    public void testMesssageProcessorSerializationWithExpressions() {
        String messageProcessorConfig = "<syn:messageProcessor xmlns:syn=\"" +
                                        "http://ws.apache.org/ns/synapse\"" +
                                        " name=\"foo\" " +
                                        "class=\"org.apache.synapse.config.xml.MessageProcessorSerializationTest$TestMessageProcessor\" messageStore=\"bar\">" +
                                        "<syn:parameter name=\"testName0\" xmlns:ns1=\"http://namespace1.synapse.org\" expression=\"//ns1:section/ns1:subSection\"/>"+
                                        "<syn:parameter name=\"testName1\">testValue1</syn:parameter>" +
                                        "<syn:parameter name=\"testName2\">testValue2</syn:parameter>" +
                                        "</syn:messageProcessor>";

        OMElement messageProcessorElement = createOMElement(messageProcessorConfig);
        MessageProcessor messageProcessor = MessageProcessorFactory.createMessageProcessor(messageProcessorElement);
        OMElement serializedElement = MessageProcessorSerializer.serializeMessageProcessor(null,
                                                                                           messageProcessor);

        assertTrue(compare(messageProcessorElement, serializedElement));
    }

    /**
     * This is a Test Message Processor implementation used only to test the XML Serialization
     */
    @SuppressWarnings("unused")
    public static class TestMessageProcessor extends AbstractMessageProcessor {


        @Override
        public void start() {
            //DO NOTHING
        }

        @Override
        public void stop() {
            //DO NOTHING
        }

        @Override
        public void destroy() {
            //DO NOTHING
        }
    }
}
