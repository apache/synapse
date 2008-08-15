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

package org.apache.synapse.transport.testkit.message;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;

import javax.activation.DataHandler;
import javax.mail.internet.ContentType;

import junit.framework.Assert;

import org.apache.axiom.attachments.ByteArrayDataSource;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.OMOutputFormat;
import org.apache.axiom.om.OMText;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.synapse.transport.base.BaseConstants;
import org.apache.synapse.transport.testkit.client.ClientOptions;

public interface MessageConverter<T,U> {
    MessageConverter<MessageData,ByteArrayMessage> AXIS_TO_BYTE =
        new MessageConverter<MessageData,ByteArrayMessage>() {

        public ByteArrayMessage convert(ClientOptions options, MessageData message) throws Exception {
            SOAPEnvelope envelope = message.getEnvelope();
            OMElement wrapper = envelope.getBody().getFirstElement();
            Assert.assertEquals(BaseConstants.DEFAULT_BINARY_WRAPPER, wrapper.getQName());
            OMNode child = wrapper.getFirstOMChild();
            Assert.assertTrue(child instanceof OMText);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ((DataHandler)((OMText)child).getDataHandler()).writeTo(baos);
            return new ByteArrayMessage(null, baos.toByteArray());
        }
    };
    
    MessageConverter<MessageData,StringMessage> AXIS_TO_STRING =
        new MessageConverter<MessageData,StringMessage>() {

        public StringMessage convert(ClientOptions options, MessageData message) throws Exception {
            SOAPEnvelope envelope = message.getEnvelope();
            OMElement wrapper = envelope.getBody().getFirstElement();
            Assert.assertEquals(BaseConstants.DEFAULT_TEXT_WRAPPER, wrapper.getQName());
            return new StringMessage(null, wrapper.getText());
        }
    };
    
    MessageConverter<XMLMessage,ByteArrayMessage> XML_TO_BYTE =
        new MessageConverter<XMLMessage,ByteArrayMessage>() {

        public ByteArrayMessage convert(ClientOptions options, XMLMessage message) throws Exception {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            OMOutputFormat outputFormat = new OMOutputFormat();
            outputFormat.setCharSetEncoding(options.getCharset());
            outputFormat.setIgnoreXMLDeclaration(true);
            message.getXmlMessageType().getMessage(message.getPayload()).serializeAndConsume(baos, outputFormat);
            return new ByteArrayMessage(message.getContentType(), baos.toByteArray());
        }
    };
    
    MessageConverter<XMLMessage,StringMessage> XML_TO_STRING =
        new MessageConverter<XMLMessage,StringMessage>() {

        public StringMessage convert(ClientOptions options, XMLMessage message) throws Exception {
            OMOutputFormat format = new OMOutputFormat();
            format.setIgnoreXMLDeclaration(true);
            StringWriter sw = new StringWriter();
            message.getXmlMessageType().getMessage(message.getPayload()).serializeAndConsume(sw, format);
            return new StringMessage(message.getContentType(), sw.toString());
        }
    };
    
    MessageConverter<ByteArrayMessage,XMLMessage> BINARY_WRAPPER =
        new MessageConverter<ByteArrayMessage,XMLMessage>() {

        public XMLMessage convert(ClientOptions options, ByteArrayMessage message) throws Exception {
            OMFactory omFactory = XMLMessageType.SOAP11.getOMFactory();
            OMElement wrapper = omFactory.createOMElement(BaseConstants.DEFAULT_BINARY_WRAPPER);
            DataHandler dataHandler = new DataHandler(new ByteArrayDataSource(message.getContent()));
            wrapper.addChild(omFactory.createOMText(dataHandler, true));
            return new XMLMessage(message.getContentType(), wrapper, XMLMessageType.SOAP11);
        }
    };
    
    MessageConverter<StringMessage,XMLMessage> TEXT_WRAPPER =
        new MessageConverter<StringMessage,XMLMessage>() {

        public XMLMessage convert(ClientOptions options, StringMessage message) throws Exception {
            OMFactory omFactory = XMLMessageType.SOAP11.getOMFactory();
            OMElement wrapper = omFactory.createOMElement(BaseConstants.DEFAULT_TEXT_WRAPPER);
            wrapper.addChild(omFactory.createOMText(message.getContent()));
            return new XMLMessage(message.getContentType(), wrapper, XMLMessageType.SOAP11);
        }
    };
    
    MessageConverter<StringMessage,ByteArrayMessage> STRING_TO_BYTE =
        new MessageConverter<StringMessage,ByteArrayMessage>() {

        public ByteArrayMessage convert(ClientOptions options, StringMessage message) throws Exception {
            return new ByteArrayMessage(message.getContentType(), message.getContent().getBytes(options.getCharset()));
        }
    };
    
    MessageConverter<ByteArrayMessage,StringMessage> BYTE_TO_STRING =
        new MessageConverter<ByteArrayMessage,StringMessage>() {

        public StringMessage convert(ClientOptions options, ByteArrayMessage message) throws Exception {
            ContentType contentType = new ContentType(message.getContentType());
            String charset = contentType.getParameter("charset");
            return new StringMessage(message.getContentType(), new String(message.getContent(), charset));
        }
    };
    
    U convert(ClientOptions options, T message) throws Exception;
}
