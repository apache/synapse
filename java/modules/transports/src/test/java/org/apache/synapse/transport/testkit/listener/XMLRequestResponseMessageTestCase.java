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
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.InOutAxisOperation;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.receivers.AbstractInOutMessageReceiver;

public class XMLRequestResponseMessageTestCase<C extends RequestResponseChannel<?>> extends ListenerTestCase<C,XMLRequestResponseMessageSender<? super C>> {
    private final XMLMessageType xmlMessageType;
    private final MessageTestData data;
    
    // TODO: realign order of arguments with XMLAsyncMessageTestCase constructor
    public XMLRequestResponseMessageTestCase(C channel, XMLRequestResponseMessageSender<? super C> sender, String name, ContentTypeMode contentTypeMode, String contentType, XMLMessageType xmlMessageType, MessageTestData data) {
        super(channel, sender, name, contentTypeMode, contentType);
        this.xmlMessageType = xmlMessageType;
        this.data = data;
    }

    @Override
    protected void runTest() throws Throwable {
        AxisService service = new AxisService("EchoService");
        AxisOperation operation = new InOutAxisOperation(DefaultOperationDispatcher.DEFAULT_OPERATION_NAME);
        operation.setMessageReceiver(new AbstractInOutMessageReceiver() {
            @Override
            public void invokeBusinessLogic(MessageContext inMessage, MessageContext outMessage) throws AxisFault {
                System.out.println(inMessage.getProperty(Constants.OUT_TRANSPORT_INFO));
                System.out.println(inMessage.getEnvelope());
                outMessage.setEnvelope(inMessage.getEnvelope());
            }
        });
        service.addOperation(operation);
        channel.setupService(service);
        if (contentTypeMode == ContentTypeMode.SERVICE) {
            channel.getSetup().setupContentType(service, contentType);
        }
        
        AxisConfiguration axisConfiguration = server.getAxisConfiguration();
        axisConfiguration.addService(service);
        try {
            OMFactory factory = xmlMessageType.getOMFactory();
            OMElement orgElement = factory.createOMElement(new QName("root"));
            orgElement.setText(data.getText());
            OMElement element = sender.sendMessage(channel, server.getEPR(service), contentType, data.getCharset(), xmlMessageType, orgElement);
            assertEquals(orgElement.getQName(), element.getQName());
            assertEquals(orgElement.getText(), element.getText());
        } finally {
            Thread.sleep(1000);
            axisConfiguration.removeService(service.getName());
        }
    }
}
