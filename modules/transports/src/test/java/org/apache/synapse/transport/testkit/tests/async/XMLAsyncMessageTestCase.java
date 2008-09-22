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
import org.apache.synapse.transport.testkit.MessageTestData;
import org.apache.synapse.transport.testkit.channel.AsyncChannel;
import org.apache.synapse.transport.testkit.client.AsyncTestClient;
import org.apache.synapse.transport.testkit.message.XMLMessage;
import org.apache.synapse.transport.testkit.name.Key;
import org.apache.synapse.transport.testkit.name.Name;
import org.apache.synapse.transport.testkit.name.Named;
import org.apache.synapse.transport.testkit.server.AsyncEndpoint;

@Name("AsyncXML")
public class XMLAsyncMessageTestCase extends AsyncMessageTestCase<XMLMessage> {
    private final XMLMessage.Type xmlMessageType;
    private final MessageTestData data;
    
    public XMLAsyncMessageTestCase(AsyncChannel channel, AsyncTestClient<XMLMessage> client, AsyncEndpoint<XMLMessage> endpoint, XMLMessage.Type xmlMessageType, MessageTestData data, Object... resources) {
        super(channel, client, endpoint, xmlMessageType.getContentType(), data.getCharset(), resources);
        this.xmlMessageType = xmlMessageType;
        this.data = data;
    }
    
    @Key("messageType")
    public XMLMessage.Type getXmlMessageType() {
        return xmlMessageType;
    }

    @Named
    public MessageTestData getData() {
        return data;
    }

    @Override
    protected XMLMessage prepareMessage() throws Exception {
        OMFactory factory = OMAbstractFactory.getOMFactory();
        OMElement orgElement = factory.createOMElement(new QName("root"));
        orgElement.setText(data.getText());
        return new XMLMessage(orgElement, xmlMessageType);
    }

    @Override
    protected void checkMessageData(XMLMessage expected, XMLMessage actual) throws Exception {
        OMElement element = actual.getPayload();
        OMElement orgElement = expected.getPayload();
        assertEquals(orgElement.getQName(), element.getQName());
        assertEquals(data.getText(), element.getText());
    }
}