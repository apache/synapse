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

public class XMLAsyncMessageTestCase<C extends AsyncChannel<?>> extends AsyncMessageTestCase<C,XMLAsyncMessageSender<? super C>> {
    private final XMLMessageType xmlMessageType;
    private final MessageTestData data;
    private OMElement orgElement;
    
    public XMLAsyncMessageTestCase(C channel, XMLAsyncMessageSender<? super C> sender, XMLMessageType xmlMessageType, String baseName, ContentTypeMode contentTypeMode, String baseContentType, MessageTestData data) {
        super(channel, sender, baseName, contentTypeMode, baseContentType + "; charset=\"" + data.getCharset() + "\"");
        this.xmlMessageType = xmlMessageType;
        this.data = data;
    }
    
    @Override
    protected void buildName(NameBuilder name) {
        super.buildName(name);
        data.buildName(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        OMFactory factory = xmlMessageType.getOMFactory();
        orgElement = factory.createOMElement(new QName("root"));
        orgElement.setText(data.getText());
    }
    
    @Override
    protected void checkMessageData(MessageData messageData) throws Exception {
        SOAPEnvelope envelope = messageData.getEnvelope();
        OMElement element = envelope.getBody().getFirstElement();
        assertEquals(orgElement.getQName(), element.getQName());
        assertEquals(data.getText(), element.getText());
    }

    @Override
    protected void sendMessage(XMLAsyncMessageSender<? super C> sender, String endpointReference, String contentType) throws Exception {
        sender.sendMessage(getChannel(), endpointReference, contentType, data.getCharset(), xmlMessageType, orgElement);
    }
}