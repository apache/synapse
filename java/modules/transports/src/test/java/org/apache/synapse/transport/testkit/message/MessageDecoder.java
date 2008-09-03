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

import javax.activation.DataHandler;
import javax.mail.internet.ContentType;
import javax.xml.stream.XMLStreamReader;

import junit.framework.Assert;

import org.apache.axiom.attachments.Attachments;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.OMText;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.util.StAXUtils;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.impl.builder.StAXSOAPModelBuilder;
import org.apache.synapse.transport.base.BaseConstants;

public interface MessageDecoder<T,U> {
    MessageDecoder<AxisMessage,byte[]> AXIS_TO_BYTE =
        new MessageDecoder<AxisMessage,byte[]>() {
    
        public byte[] decode(ContentType contentType, AxisMessage message) throws Exception {
            SOAPEnvelope envelope = message.getEnvelope();
            OMElement wrapper = envelope.getBody().getFirstElement();
            Assert.assertEquals(BaseConstants.DEFAULT_BINARY_WRAPPER, wrapper.getQName());
            OMNode child = wrapper.getFirstOMChild();
            Assert.assertTrue(child instanceof OMText);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ((DataHandler)((OMText)child).getDataHandler()).writeTo(baos);
            return baos.toByteArray();
        }
    };
    
    MessageDecoder<AxisMessage,String> AXIS_TO_STRING =
        new MessageDecoder<AxisMessage,String>() {
    
        public String decode(ContentType contentType, AxisMessage message) throws Exception {
            SOAPEnvelope envelope = message.getEnvelope();
            OMElement wrapper = envelope.getBody().getFirstElement();
            Assert.assertEquals(BaseConstants.DEFAULT_TEXT_WRAPPER, wrapper.getQName());
            return wrapper.getText();
        }
    };
    
    MessageDecoder<AxisMessage,XMLMessage> AXIS_TO_XML =
        new MessageDecoder<AxisMessage,XMLMessage>() {

        public XMLMessage decode(ContentType contentType, AxisMessage message) throws Exception {
            Attachments attachments = message.getAttachments();
            XMLMessage.Type type;
            if (attachments != null) {
                type = XMLMessage.Type.SWA;
            } else {
                type = null;
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
            }
            return new XMLMessage(message.getEnvelope().getBody().getFirstElement(), type, attachments);
        }
    };

    MessageDecoder<byte[],XMLMessage> BYTE_TO_XML =
        new MessageDecoder<byte[],XMLMessage>() {
    
        public XMLMessage decode(ContentType contentType, byte[] message) throws Exception {
            String baseType = contentType.getBaseType();
            String charset = contentType.getParameter("charset");
            XMLStreamReader reader = StAXUtils.createXMLStreamReader(new ByteArrayInputStream(message), charset);
            XMLMessage.Type type = null;
            for (XMLMessage.Type candidate : XMLMessage.Type.values()) {
                if (candidate.getContentType().getBaseType().equals(baseType)) {
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
            return new XMLMessage(payload, type);
        }
    };

    MessageDecoder<byte[],String> BYTE_TO_STRING =
        new MessageDecoder<byte[],String>() {
    
        public String decode(ContentType contentType, byte[] message) throws Exception {
            String charset = contentType.getParameter("charset");
            return new String(message, charset);
        }
    };

    U decode(ContentType contentType, T message) throws Exception;
}
