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

import org.apache.synapse.transport.testkit.client.AsyncTestClient;
import org.apache.synapse.transport.testkit.listener.AsyncChannel;
import org.apache.synapse.transport.testkit.listener.ContentTypeMode;
import org.apache.synapse.transport.testkit.listener.MessageTestData;
import org.apache.synapse.transport.testkit.message.StringMessage;
import org.apache.synapse.transport.testkit.name.Name;
import org.apache.synapse.transport.testkit.name.Named;
import org.apache.synapse.transport.testkit.server.AsyncEndpointFactory;

@Name("AsyncTextPlain")
public class TextPlainTestCase extends AsyncMessageTestCase<StringMessage,StringMessage> {
    private final MessageTestData data;
    
    public TextPlainTestCase(AsyncChannel channel, AsyncTestClient<StringMessage> client, AsyncEndpointFactory<StringMessage> endpointFactory, ContentTypeMode contentTypeMode, MessageTestData data, Object... resources) {
        super(channel, client, endpointFactory, contentTypeMode, "text/plain; charset=\"" + data.getCharset() + "\"", data.getCharset(), resources);
        this.data = data;
    }
    
    @Named
    public MessageTestData getData() {
        return data;
    }
    
    @Override
    protected StringMessage prepareMessage() throws Exception {
        return new StringMessage(contentType, data.getText());
    }

    @Override
    protected void checkMessageData(StringMessage message, StringMessage messageData) throws Exception {
        // Some transport protocols add a newline at the end of the payload. Therefore trim the
        // strings before comparison.
        // TODO: investigate this a bit further
        assertEquals(message.getContent().trim(), messageData.getContent().trim());
    }
}
