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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;

import javax.activation.DataHandler;
import javax.mail.internet.ContentType;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import junit.framework.Assert;

import org.apache.axiom.attachments.ByteArrayDataSource;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.OMOutputFormat;
import org.apache.axiom.om.OMText;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.util.StAXUtils;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.impl.builder.StAXSOAPModelBuilder;
import org.apache.synapse.transport.base.BaseConstants;
import org.apache.synapse.transport.testkit.client.ClientOptions;

public interface MessageConverter<T,U> {
    MessageConverter<AxisMessage,ByteArrayMessage> AXIS_TO_BYTE =
        new MessageConverter<AxisMessage,ByteArrayMessage>() {

        public ByteArrayMessage convert(ClientOptions options, AxisMessage message) throws Exception {
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
    
    MessageConverter<AxisMessage,StringMessage> AXIS_TO_STRING =
        new MessageConverter<AxisMessage,StringMessage>() {

        public StringMessage convert(ClientOptions options, AxisMessage message) throws Exception {
            SOAPEnvelope envelope = message.getEnvelope();
            OMElement wrapper = envelope.getBody().getFirstElement();
            Assert.assertEquals(BaseConstants.DEFAULT_TEXT_WRAPPER, wrapper.getQName());
            return new StringMessage(null, wrapper.getText());
        }
    };
    
    MessageConverter<XMLMessage,AxisMessage> XML_TO_AXIS =
        new MessageConverter<XMLMessage,AxisMessage>() {

        public AxisMessage convert(ClientOptions options, XMLMessage message) throws Exception {
            XMLMessage.Type type = message.getType();
            AxisMessage result = new AxisMessage();
            SOAPFactory factory;
            if (type == XMLMessage.Type.SOAP12) {
                factory = OMAbstractFactory.getSOAP12Factory();
            } else {
                factory = OMAbstractFactory.getSOAP11Factory();
            }
            result.setMessageType(type.getContentType());
            SOAPEnvelope envelope = factory.getDefaultEnvelope();
            envelope.getBody().addChild(message.getPayload());
            result.setEnvelope(envelope);
            return result;
        }
    };
    
    MessageConverter<AxisMessage,XMLMessage> AXIS_TO_XML =
        new MessageConverter<AxisMessage,XMLMessage>() {

        public XMLMessage convert(ClientOptions options, AxisMessage message) throws Exception {
            XMLMessage.Type type = null;
            for (XMLMessage.Type candidate : XMLMessage.Type.values()) {
                if (candidate.getContentType().equals(message.getMessageType())) {
                    type = candidate;
                    break;
                }
            }
            if (type == null) {
                // TODO: make this an error later
                type = XMLMessage.Type.POX;
//                throw new UnsupportedOperationException("Content type " + message.getMessageType() + " not supported");
            }
            return new XMLMessage(message.getMessageType(), message.getEnvelope().getBody().getFirstElement(), type);
        }
    };
    
    MessageConverter<XMLMessage,ByteArrayMessage> XML_TO_BYTE =
        new MessageConverter<XMLMessage,ByteArrayMessage>() {

        public ByteArrayMessage convert(ClientOptions options, XMLMessage message) throws Exception {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            OMOutputFormat outputFormat = new OMOutputFormat();
            outputFormat.setCharSetEncoding(options.getCharset());
            outputFormat.setIgnoreXMLDeclaration(true);
            message.getMessageElement().serializeAndConsume(baos, outputFormat);
            return new ByteArrayMessage(new ContentType(message.getContentType()), baos.toByteArray());
        }
    };
    
    MessageConverter<ByteArrayMessage,XMLMessage> BYTE_TO_XML =
        new MessageConverter<ByteArrayMessage,XMLMessage>() {

        public XMLMessage convert(ClientOptions options, ByteArrayMessage message) throws Exception {
            ContentType contentType = message.getContentType();
            String baseType = contentType.getBaseType();
            String charset = contentType.getParameter("charset");
            XMLStreamReader reader = StAXUtils.createXMLStreamReader(new ByteArrayInputStream(message.getContent()), charset);
            XMLMessage.Type type = null;
            for (XMLMessage.Type candidate : XMLMessage.Type.values()) {
                if (candidate.getContentType().equals(baseType)) {
                    type = candidate;
                    break;
                }
            }
            if (type == null) {
                throw new Exception("Unrecognized content type " + baseType);
            }
            OMElement payload;
            if (type == XMLMessage.Type.POX) {
                payload = new StAXOMBuilder(reader).getDocumentElement();
            } else {
                payload = new StAXSOAPModelBuilder(reader).getSOAPEnvelope().getBody().getFirstElement();
            }
            return new XMLMessage(contentType.toString(), payload, type);
        }
    };
    
    MessageConverter<XMLMessage,StringMessage> XML_TO_STRING =
        new MessageConverter<XMLMessage,StringMessage>() {

        public StringMessage convert(ClientOptions options, XMLMessage message) throws Exception {
            OMOutputFormat format = new OMOutputFormat();
            format.setIgnoreXMLDeclaration(true);
            StringWriter sw = new StringWriter();
            message.getMessageElement().serializeAndConsume(sw, format);
            return new StringMessage(message.getContentType(), sw.toString());
        }
    };
    
    MessageConverter<ByteArrayMessage,AxisMessage> BINARY_WRAPPER =
        new MessageConverter<ByteArrayMessage,AxisMessage>() {

        public AxisMessage convert(ClientOptions options, ByteArrayMessage message) throws Exception {
            AxisMessage result = new AxisMessage();
            result.setMessageType("application/octet-stream");
            SOAPFactory factory = OMAbstractFactory.getSOAP11Factory();
            SOAPEnvelope envelope = factory.getDefaultEnvelope();
            OMElement wrapper = factory.createOMElement(BaseConstants.DEFAULT_BINARY_WRAPPER);
            DataHandler dataHandler = new DataHandler(new ByteArrayDataSource(message.getContent()));
            wrapper.addChild(factory.createOMText(dataHandler, true));
            envelope.getBody().addChild(wrapper);
            result.setEnvelope(envelope);
            return result;
        }
    };
    
    MessageConverter<StringMessage,AxisMessage> TEXT_WRAPPER =
        new MessageConverter<StringMessage,AxisMessage>() {

        public AxisMessage convert(ClientOptions options, StringMessage message) throws Exception {
            AxisMessage result = new AxisMessage();
            result.setMessageType("application/octet-stream");
            SOAPFactory factory = OMAbstractFactory.getSOAP11Factory();
            SOAPEnvelope envelope = factory.getDefaultEnvelope();
            OMElement wrapper = factory.createOMElement(BaseConstants.DEFAULT_TEXT_WRAPPER);
            wrapper.addChild(factory.createOMText(message.getContent()));
            envelope.getBody().addChild(wrapper);
            result.setEnvelope(envelope);
            return result;
        }
    };
    
    MessageConverter<StringMessage,ByteArrayMessage> STRING_TO_BYTE =
        new MessageConverter<StringMessage,ByteArrayMessage>() {

        public ByteArrayMessage convert(ClientOptions options, StringMessage message) throws Exception {
            return new ByteArrayMessage(new ContentType(message.getContentType()), message.getContent().getBytes(options.getCharset()));
        }
    };
    
    MessageConverter<ByteArrayMessage,StringMessage> BYTE_TO_STRING =
        new MessageConverter<ByteArrayMessage,StringMessage>() {

        public StringMessage convert(ClientOptions options, ByteArrayMessage message) throws Exception {
            ContentType contentType = message.getContentType();
            String charset = contentType.getParameter("charset");
            return new StringMessage(contentType.toString(), new String(message.getContent(), charset));
        }
    };
    
    U convert(ClientOptions options, T message) throws Exception;
}
