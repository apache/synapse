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

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.synapse.transport.testkit.client.ClientOptions;
import org.apache.synapse.transport.testkit.client.RequestResponseTestClient;
import org.apache.synapse.transport.testkit.listener.ContentTypeMode;
import org.apache.synapse.transport.testkit.listener.MessageTestData;
import org.apache.synapse.transport.testkit.listener.RequestResponseChannel;
import org.apache.synapse.transport.testkit.message.XMLMessage;
import org.apache.synapse.transport.testkit.name.DisplayName;
import org.apache.synapse.transport.testkit.name.NameComponent;
import org.apache.synapse.transport.testkit.server.Endpoint;
import org.apache.synapse.transport.testkit.server.EndpointFactory;
import org.apache.synapse.transport.testkit.tests.TransportTestCase;

@DisplayName("EchoXML")
public class XMLRequestResponseMessageTestCase extends TransportTestCase {
    private final RequestResponseTestClient<XMLMessage,XMLMessage> client;
    private final EndpointFactory endpointFactory;
    private final XMLMessage.Type xmlMessageType;
    private final MessageTestData data;
    
    // TODO: realign order of arguments with XMLAsyncMessageTestCase constructor
    // TODO: maybe we don't need an explicit RequestResponseChannel
    public XMLRequestResponseMessageTestCase(RequestResponseChannel channel, RequestResponseTestClient<XMLMessage,XMLMessage> client, EndpointFactory endpointFactory, ContentTypeMode contentTypeMode, String contentType, XMLMessage.Type xmlMessageType, MessageTestData data, Object... resources) {
        super(contentTypeMode, contentType, resources);
        this.client = client;
        this.endpointFactory = endpointFactory;
        this.xmlMessageType = xmlMessageType;
        this.data = data;
        addResource(channel);
        addResource(client);
        addResource(endpointFactory);
    }

    @NameComponent("client")
    public RequestResponseTestClient<XMLMessage,XMLMessage> getClient() {
        return client;
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
    protected void runTest() throws Throwable {
        Endpoint endpoint = endpointFactory.createEchoEndpoint(contentTypeMode == ContentTypeMode.SERVICE ? contentType : null);
        try {
            OMFactory factory = OMAbstractFactory.getOMFactory();
            OMElement orgElement = factory.createOMElement(new QName("root"));
            orgElement.setText(data.getText());
            OMElement element = client.sendMessage(new ClientOptions(endpoint.getEPR(), data.getCharset()), new XMLMessage(contentType, orgElement, xmlMessageType)).getPayload();
            assertEquals(orgElement.getQName(), element.getQName());
            assertEquals(orgElement.getText(), element.getText());
        } finally {
            Thread.sleep(1000);
            endpoint.remove();
        }
    }
}
