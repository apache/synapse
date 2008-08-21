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

import org.apache.synapse.transport.testkit.client.ClientOptions;
import org.apache.synapse.transport.testkit.client.RequestResponseTestClient;
import org.apache.synapse.transport.testkit.listener.ContentTypeMode;
import org.apache.synapse.transport.testkit.listener.RequestResponseChannel;
import org.apache.synapse.transport.testkit.name.NameComponent;
import org.apache.synapse.transport.testkit.server.Endpoint;
import org.apache.synapse.transport.testkit.server.EndpointFactory;
import org.apache.synapse.transport.testkit.tests.MessageTestCase;

public abstract class RequestResponseMessageTestCase<M,N> extends MessageTestCase {
    private final RequestResponseTestClient<M,N> client;
    private final String charset;
    private final EndpointFactory endpointFactory;

    // TODO: maybe we don't need an explicit RequestResponseChannel
    public RequestResponseMessageTestCase(RequestResponseChannel channel, RequestResponseTestClient<M,N> client, EndpointFactory endpointFactory, ContentTypeMode contentTypeMode, String contentType, String charset, Object... resources) {
        super(contentTypeMode, contentType, resources);
        this.client = client;
        this.endpointFactory = endpointFactory;
        this.charset = charset;
        addResource(channel);
        addResource(client);
        addResource(endpointFactory);
    }
    
    @NameComponent("client")
    public RequestResponseTestClient<M,N> getClient() {
        return client;
    }

    @Override
    protected void runTest() throws Throwable {
        Endpoint endpoint = endpointFactory.createEchoEndpoint(contentTypeMode == ContentTypeMode.SERVICE ? contentType : null);
        M request = prepareRequest();
        N response;
        try {
            response = client.sendMessage(new ClientOptions(charset), request);
        } finally {
            endpoint.remove();
        }
        checkResponse(request, response);
    }

    protected abstract M prepareRequest() throws Exception;
    protected abstract void checkResponse(M request, N response) throws Exception;
}
