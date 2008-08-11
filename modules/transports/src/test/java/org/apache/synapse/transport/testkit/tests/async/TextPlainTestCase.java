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

import org.apache.synapse.transport.testkit.TestEnvironment;
import org.apache.synapse.transport.testkit.listener.AsyncChannel;
import org.apache.synapse.transport.testkit.listener.AsyncMessageSender;
import org.apache.synapse.transport.testkit.listener.AsyncMessageTestCase;
import org.apache.synapse.transport.testkit.listener.ContentTypeMode;
import org.apache.synapse.transport.testkit.listener.MessageTestData;
import org.apache.synapse.transport.testkit.message.StringMessage;
import org.apache.synapse.transport.testkit.name.DisplayName;
import org.apache.synapse.transport.testkit.name.NameComponent;
import org.apache.synapse.transport.testkit.server.AsyncEndpointFactory;

@DisplayName("AsyncTextPlain")
public class TextPlainTestCase<E extends TestEnvironment,C extends AsyncChannel<? super E>> extends AsyncMessageTestCase<E,C,StringMessage,StringMessage> {
    private final MessageTestData data;
    
    public TextPlainTestCase(E env, C channel, AsyncMessageSender<? super C,StringMessage> sender, AsyncEndpointFactory<? super E,? super C,StringMessage> endpointFactory, ContentTypeMode contentTypeMode, MessageTestData data) {
        super(env, channel, sender, endpointFactory, contentTypeMode, "text/plain; charset=\"" + data.getCharset() + "\"", data.getCharset());
        this.data = data;
    }
    
    @NameComponent("data")
    public MessageTestData getData() {
        return data;
    }
    
    @Override
    protected StringMessage prepareMessage() throws Exception {
        return new StringMessage(contentType, data.getText());
    }

    @Override
    protected void checkMessageData(StringMessage message, StringMessage messageData) throws Exception {
        assertEquals(message.getContent(), messageData.getContent());
    }
}
