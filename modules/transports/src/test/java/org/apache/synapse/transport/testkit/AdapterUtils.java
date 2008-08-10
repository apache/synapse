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

package org.apache.synapse.transport.testkit;

import org.apache.synapse.transport.testkit.listener.AsyncChannel;
import org.apache.synapse.transport.testkit.listener.AsyncMessageSender;
import org.apache.synapse.transport.testkit.listener.AsyncMessageSenderAdapter;
import org.apache.synapse.transport.testkit.message.MessageConverter;
import org.apache.synapse.transport.testkit.server.AsyncEndpointFactory;
import org.apache.synapse.transport.testkit.server.AsyncEndpointFactoryAdapter;

public class AdapterUtils {
    public static <C extends AsyncChannel<?>,M,N> AsyncMessageSender<C,M> adapt(AsyncMessageSender<C,N> parent, MessageConverter<M,N> converter) {
        return new AsyncMessageSenderAdapter<C,M,N>(parent, converter);
    }

    public static <C extends AsyncChannel<?>,M,N> AsyncEndpointFactory<C,M> adapt(AsyncEndpointFactory<C,N> targetFactory, MessageConverter<N,M> converter) {
        return new AsyncEndpointFactoryAdapter<C,M,N>(targetFactory, converter);
    }
}
