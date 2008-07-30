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

package org.apache.synapse.transport.jms;

import java.io.StringWriter;

import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMOutputFormat;
import org.apache.synapse.transport.base.BaseConstants;
import org.apache.synapse.transport.testkit.listener.AbstractMessageSender;
import org.apache.synapse.transport.testkit.listener.XMLAsyncMessageSender;
import org.apache.synapse.transport.testkit.listener.XMLMessageType;

public class JMSTextMessageSender extends AbstractMessageSender<JMSAsyncChannel> implements XMLAsyncMessageSender<JMSAsyncChannel> {
    public JMSTextMessageSender() {
        super("TextMessage");
    }

    public void sendMessage(JMSAsyncChannel channel,
            String endpointReference, String contentType, String charset,
            XMLMessageType xmlMessageType, OMElement payload) throws Exception {
        Session session = channel.createSession();
        TextMessage message = session.createTextMessage();
        if (contentType != null) {
            message.setStringProperty(BaseConstants.CONTENT_TYPE, contentType);
        }
        OMOutputFormat format = new OMOutputFormat();
        format.setIgnoreXMLDeclaration(true);
        StringWriter sw = new StringWriter();
        xmlMessageType.getMessage(payload).serializeAndConsume(sw, format);
        message.setText(sw.toString());
        channel.send(session, message);
    }
}