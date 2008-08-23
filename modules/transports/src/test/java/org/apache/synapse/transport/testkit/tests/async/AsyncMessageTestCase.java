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
import org.apache.synapse.transport.testkit.client.ClientOptions;
import org.apache.synapse.transport.testkit.listener.AsyncChannel;
import org.apache.synapse.transport.testkit.listener.ContentTypeMode;
import org.apache.synapse.transport.testkit.server.AsyncEndpoint;
import org.apache.synapse.transport.testkit.server.AsyncEndpointFactory;
import org.apache.synapse.transport.testkit.tests.MessageTestCase;

public abstract class AsyncMessageTestCase<M,N> extends MessageTestCase {
    private final AsyncTestClient<M> client;
    private final String charset;
    private final AsyncEndpointFactory<N> endpointFactory;
    
    // TODO: maybe we don't need an explicit AsyncChannel
    public AsyncMessageTestCase(AsyncChannel channel, AsyncTestClient<M> client, AsyncEndpointFactory<N> endpointFactory, ContentTypeMode contentTypeMode, String contentType, String charset, Object... resources) {
        super(contentTypeMode, contentType, resources);
        this.client = client;
        this.endpointFactory = endpointFactory;
        this.charset = charset;
        addResource(channel);
        addResource(client);
        addResource(endpointFactory);
    }

    @Override
    protected void runTest() throws Throwable {
        AsyncEndpoint<N> endpoint = endpointFactory.createAsyncEndpoint(contentTypeMode == ContentTypeMode.SERVICE ? contentType : null);
        
        M message = prepareMessage();
        
        // Run the test.
        N messageData;
        try {
            ClientOptions options = new ClientOptions(charset);
//                    contentTypeMode == ContentTypeMode.TRANSPORT ? contentType : null);
            client.sendMessage(options, message);
            messageData = endpoint.waitForMessage(8000);
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
    protected abstract void checkMessageData(M message, N messageData) throws Exception;
}