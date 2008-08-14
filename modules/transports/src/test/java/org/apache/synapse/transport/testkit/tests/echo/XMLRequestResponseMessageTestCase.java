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

package org.apache.synapse.transport.testkit.tests.echo;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.synapse.transport.testkit.TestEnvironment;
import org.apache.synapse.transport.testkit.listener.ContentTypeMode;
import org.apache.synapse.transport.testkit.listener.MessageTestData;
import org.apache.synapse.transport.testkit.listener.RequestResponseChannel;
import org.apache.synapse.transport.testkit.listener.XMLRequestResponseMessageSender;
import org.apache.synapse.transport.testkit.message.XMLMessageType;
import org.apache.synapse.transport.testkit.name.DisplayName;
import org.apache.synapse.transport.testkit.name.NameComponent;
import org.apache.synapse.transport.testkit.server.Endpoint;
import org.apache.synapse.transport.testkit.server.EndpointFactory;
import org.apache.synapse.transport.testkit.tests.TransportTestCase;

@DisplayName("EchoXML")
public class XMLRequestResponseMessageTestCase<E extends TestEnvironment,C extends RequestResponseChannel<? super E>> extends TransportTestCase<E,C,XMLRequestResponseMessageSender<? super E,? super C>> {
    private final EndpointFactory<? super E,? super C> endpointFactory;
    private final XMLMessageType xmlMessageType;
    private final MessageTestData data;
    
    // TODO: realign order of arguments with XMLAsyncMessageTestCase constructor
    public XMLRequestResponseMessageTestCase(E env, C channel, XMLRequestResponseMessageSender<? super E,? super C> sender, EndpointFactory<? super E,? super C> endpointFactory, ContentTypeMode contentTypeMode, String contentType, XMLMessageType xmlMessageType, MessageTestData data) {
        super(env, channel, sender, endpointFactory.getServer(), contentTypeMode, contentType);
        this.endpointFactory = endpointFactory;
        this.xmlMessageType = xmlMessageType;
        this.data = data;
    }

    @NameComponent("messageType")
    public XMLMessageType getXmlMessageType() {
        return xmlMessageType;
    }

    @NameComponent("data")
    public MessageTestData getData() {
        return data;
    }

    @Override
    protected void runTest() throws Throwable {
        Endpoint endpoint = endpointFactory.createEchoEndpoint(env, channel, contentTypeMode == ContentTypeMode.SERVICE ? contentType : null);
        try {
            OMFactory factory = xmlMessageType.getOMFactory();
            OMElement orgElement = factory.createOMElement(new QName("root"));
            orgElement.setText(data.getText());
            OMElement element = sender.sendMessage(channel, endpoint.getEPR(), contentType, data.getCharset(), xmlMessageType, orgElement);
            assertEquals(orgElement.getQName(), element.getQName());
            assertEquals(orgElement.getText(), element.getText());
        } finally {
            Thread.sleep(1000);
            endpoint.remove();
        }
    }
}
