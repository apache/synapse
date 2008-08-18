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

package org.apache.synapse.transport.testkit.tests.async;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.synapse.transport.testkit.client.AsyncTestClient;
import org.apache.synapse.transport.testkit.listener.AsyncChannel;
import org.apache.synapse.transport.testkit.listener.AsyncMessageTestCase;
import org.apache.synapse.transport.testkit.listener.ContentTypeMode;
import org.apache.synapse.transport.testkit.listener.MessageTestData;
import org.apache.synapse.transport.testkit.message.AxisMessage;
import org.apache.synapse.transport.testkit.message.XMLMessage;
import org.apache.synapse.transport.testkit.name.DisplayName;
import org.apache.synapse.transport.testkit.name.NameComponent;
import org.apache.synapse.transport.testkit.server.AsyncEndpointFactory;

@DisplayName("AsyncXML")
public class XMLAsyncMessageTestCase extends AsyncMessageTestCase<XMLMessage,AxisMessage> {
    private final XMLMessage.Type xmlMessageType;
    private final MessageTestData data;
    
    public XMLAsyncMessageTestCase(AsyncChannel channel, AsyncTestClient<XMLMessage> client, AsyncEndpointFactory<AxisMessage> endpointFactory, XMLMessage.Type xmlMessageType, ContentTypeMode contentTypeMode, String baseContentType, MessageTestData data, Object... resources) {
        super(channel, client, endpointFactory, contentTypeMode, baseContentType + "; charset=\"" + data.getCharset() + "\"", data.getCharset(), resources);
        this.xmlMessageType = xmlMessageType;
        this.data = data;
    }
    
    @NameComponent("messageType")
    public XMLMessage.Type getXmlMessageType() {
        return xmlMessageType;
    }

    @NameComponent("data")
    public MessageTestData getData() {
        return data;
    }

    @Override
    protected XMLMessage prepareMessage() throws Exception {
        OMFactory factory = OMAbstractFactory.getOMFactory();
        OMElement orgElement = factory.createOMElement(new QName("root"));
        orgElement.setText(data.getText());
        return new XMLMessage(contentType, orgElement, xmlMessageType);
    }

    @Override
    protected void checkMessageData(XMLMessage message, AxisMessage messageData) throws Exception {
        SOAPEnvelope envelope = messageData.getEnvelope();
        OMElement element = envelope.getBody().getFirstElement();
        OMElement orgElement = message.getPayload();
        assertEquals(orgElement.getQName(), element.getQName());
        assertEquals(data.getText(), element.getText());
    }
}