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

import javax.jms.BytesMessage;

import org.apache.synapse.transport.base.BaseConstants;
import org.apache.synapse.transport.testkit.client.AsyncTestClient;
import org.apache.synapse.transport.testkit.client.ClientOptions;
import org.apache.synapse.transport.testkit.message.ByteArrayMessage;
import org.apache.synapse.transport.testkit.name.DisplayName;

@DisplayName("BytesMessage")
public class JMSBytesMessageClient extends JMSClient implements AsyncTestClient<ByteArrayMessage> {
    public void sendMessage(ClientOptions options,
                            ByteArrayMessage message) throws Exception {
        BytesMessage jmsMessage = session.createBytesMessage();
        if (message.getContentType() != null) {
            jmsMessage.setStringProperty(BaseConstants.CONTENT_TYPE, message.getContentType());
        }
        jmsMessage.writeBytes(message.getContent());
        producer.send(jmsMessage);
    }
}