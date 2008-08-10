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

package org.apache.synapse.transport.testkit.listener;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.synapse.transport.testkit.message.MessageData;
import org.apache.synapse.transport.testkit.message.XMLMessage;
import org.apache.synapse.transport.testkit.message.XMLMessageType;
import org.apache.synapse.transport.testkit.name.DisplayName;
import org.apache.synapse.transport.testkit.server.AsyncEndpointFactory;

@DisplayName("AsyncXML")
public class XMLAsyncMessageTestCase<C extends AsyncChannel<?>> extends AsyncMessageTestCase<C,XMLMessage,MessageData> {
    private final XMLMessageType xmlMessageType;
    private final MessageTestData data;
    
    public XMLAsyncMessageTestCase(C channel, AsyncMessageSender<? super C,XMLMessage> sender, AsyncEndpointFactory<? super C,MessageData> endpointFactory, XMLMessageType xmlMessageType, ContentTypeMode contentTypeMode, String baseContentType, MessageTestData data) {
        super(channel, sender, endpointFactory, contentTypeMode, baseContentType + "; charset=\"" + data.getCharset() + "\"", data.getCharset());
        this.xmlMessageType = xmlMessageType;
        this.data = data;
    }
    
    @Override
    protected void buildName(NameBuilder name) {
        super.buildName(name);
        data.buildName(name);
    }

    @Override
    protected XMLMessage prepareMessage() throws Exception {
        OMFactory factory = xmlMessageType.getOMFactory();
        OMElement orgElement = factory.createOMElement(new QName("root"));
        orgElement.setText(data.getText());
        return new XMLMessage(contentType, orgElement, xmlMessageType);
    }

    @Override
    protected void checkMessageData(XMLMessage message, MessageData messageData) throws Exception {
        SOAPEnvelope envelope = messageData.getEnvelope();
        OMElement element = envelope.getBody().getFirstElement();
        OMElement orgElement = message.getPayload();
        assertEquals(orgElement.getQName(), element.getQName());
        assertEquals(data.getText(), element.getText());
    }
}