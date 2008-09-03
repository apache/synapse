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

package org.apache.synapse.transport.testkit.client;

import javax.mail.internet.ContentType;

import org.apache.synapse.transport.testkit.Adapter;
import org.apache.synapse.transport.testkit.message.MessageEncoder;

public class AsyncTestClientAdapter<M,N> implements AsyncTestClient<M>, Adapter {
    private final AsyncTestClient<N> target;
    private final MessageEncoder<M,N> encoder;

    public AsyncTestClientAdapter(AsyncTestClient<N> target, MessageEncoder<M,N> encoder) {
        this.target = target;
        this.encoder = encoder;
    }
    
    public AsyncTestClient<N> getTarget() {
        return target;
    }

    public ContentType getContentType(ClientOptions options, ContentType contentType) throws Exception {
        return target.getContentType(options, encoder.getContentType(options, contentType));
    }

    public void sendMessage(ClientOptions options, ContentType contentType, M message) throws Exception {
        target.sendMessage(options, encoder.getContentType(options, contentType), encoder.encode(options, message));
    }
}
