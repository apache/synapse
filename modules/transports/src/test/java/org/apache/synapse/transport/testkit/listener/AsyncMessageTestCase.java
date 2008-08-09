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

import java.util.concurrent.TimeUnit;

import org.apache.synapse.transport.testkit.server.AsyncEndpoint;

public abstract class AsyncMessageTestCase<C extends AsyncChannel<?>,M> extends ListenerTestCase<C,AsyncMessageSender<? super C,M>> {
    private final String charset;
    
    public AsyncMessageTestCase(C channel, AsyncMessageSender<? super C,M> sender, String name, ContentTypeMode contentTypeMode, String contentType, String charset) {
        super(channel, sender, name, contentTypeMode, contentType);
        this.charset = charset;
    }

    @Override
    protected void runTest() throws Throwable {
        AsyncEndpoint endpoint = channel.getServer().createAsyncEndpoint(contentTypeMode == ContentTypeMode.SERVICE ? contentType : null);
        
        M message = prepareMessage();
        
        // Run the test.
        MessageData messageData;
        try {
            SenderOptions options = new SenderOptions(endpoint.getEPR(), charset);
//                    contentTypeMode == ContentTypeMode.TRANSPORT ? contentType : null);
            sender.sendMessage(channel, options, message);
            messageData = endpoint.waitForMessage(8, TimeUnit.SECONDS);
            if (messageData == null) {
                fail("Failed to get message");
            }
        }
        finally {
            endpoint.remove();
        }
        
        checkMessageData(message, messageData);
    }
    
    protected abstract M prepareMessage() throws Exception;
    protected abstract void checkMessageData(M message, MessageData messageData) throws Exception;
}