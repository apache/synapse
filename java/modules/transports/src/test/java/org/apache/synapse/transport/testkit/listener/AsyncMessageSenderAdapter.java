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

import org.apache.synapse.transport.testkit.message.MessageConverter;

public class AsyncMessageSenderAdapter<C extends AsyncChannel<?>,M,N> implements AsyncMessageSender<C,M> {
    private final AsyncMessageSender<C,N> parent;
    private final MessageConverter<M,N> converter;

    public AsyncMessageSenderAdapter(AsyncMessageSender<C,N> parent, MessageConverter<M,N> converter) {
        this.parent = parent;
        this.converter = converter;
    }
    
    public void sendMessage(C channel, SenderOptions options, M message) throws Exception {
        parent.sendMessage(channel, options, converter.convert(options, message));
    }

    public void buildName(NameBuilder nameBuilder) {
        parent.buildName(nameBuilder);
    }

    public void setUp(C channel) throws Exception {
        parent.setUp(channel);
    }

    public void tearDown() throws Exception {
        parent.tearDown();
    }
}
