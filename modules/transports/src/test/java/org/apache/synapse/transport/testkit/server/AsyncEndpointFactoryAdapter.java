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

package org.apache.synapse.transport.testkit.server;

import org.apache.synapse.transport.testkit.Adapter;
import org.apache.synapse.transport.testkit.listener.AsyncChannel;
import org.apache.synapse.transport.testkit.message.MessageConverter;

public class AsyncEndpointFactoryAdapter<C extends AsyncChannel<?>,M,N> implements AsyncEndpointFactory<C,M>, Adapter {
    private final AsyncEndpointFactory<C,N> targetFactory;
    private final MessageConverter<N,M> converter;
    
    public AsyncEndpointFactoryAdapter(AsyncEndpointFactory<C,N> targetFactory, MessageConverter<N,M> converter) {
        this.targetFactory = targetFactory;
        this.converter = converter;
    }
    
    public AsyncEndpointFactory<C,N> getTarget() {
        return targetFactory;
    }

    public Server<?> getServer() {
        return targetFactory.getServer();
    }

    public AsyncEndpoint<M> createAsyncEndpoint(C channel, String contentType) throws Exception {
        final AsyncEndpoint<N> targetEndpoint = targetFactory.createAsyncEndpoint(channel, contentType);
        final MessageConverter<N,M> converter = this.converter;
        return new AsyncEndpoint<M>() {
            public String getEPR() throws Exception {
                return targetEndpoint.getEPR();
            }

            public M waitForMessage(int timeout) throws Throwable {
                N message = targetEndpoint.waitForMessage(timeout);
                return message == null ? null : converter.convert(null, message);
            }

            public void remove() throws Exception {
                targetEndpoint.remove();
            }
        };
    }
}
