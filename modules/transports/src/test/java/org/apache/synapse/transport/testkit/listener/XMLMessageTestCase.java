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

public abstract class XMLMessageTestCase extends ListenerTestCase<XMLMessageSender> {
    private final MessageTestData data;
    private OMElement orgElement;
    protected OMFactory factory;
    
    public XMLMessageTestCase(ListenerTestSetup setup, XMLMessageSender sender, String baseName, ContentTypeMode contentTypeMode, String baseContentType, MessageTestData data) {
        super(setup, sender, baseName, contentTypeMode, baseContentType + "; charset=\"" + data.getCharset() + "\"");
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
        factory = getOMFactory();
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
    protected void sendMessage(XMLMessageSender sender, String endpointReference, String contentType) throws Exception {
        sender.sendMessage(getSetup(), endpointReference, contentType, data.getCharset(), getMessage(orgElement));
    }
    
    protected abstract OMFactory getOMFactory();
    protected abstract OMElement getMessage(OMElement payload);
}