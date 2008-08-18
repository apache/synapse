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

import org.apache.synapse.transport.testkit.Adapter;
import org.apache.synapse.transport.testkit.message.MessageConverter;

public class RequestResponseTestClientAdapter<M,N,O,P> implements RequestResponseTestClient<M,O>, Adapter {
    private final RequestResponseTestClient<N,P> target;
    private final MessageConverter<M,N> requestConverter;
    private final MessageConverter<P,O> responseConverter;

    public RequestResponseTestClientAdapter(RequestResponseTestClient<N,P> target,
                                            MessageConverter<M,N> requestConverter,
                                            MessageConverter<P,O> responseConverter) {
        this.target = target;
        this.requestConverter = requestConverter;
        this.responseConverter = responseConverter;
    }
    
    public RequestResponseTestClient<N,P> getTarget() {
        return target;
    }

    public O sendMessage(ClientOptions options, M message) throws Exception {
        return responseConverter.convert(options, target.sendMessage(options, requestConverter.convert(options, message)));
    }
}
