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

import java.util.Arrays;
import java.util.Random;

import org.apache.synapse.transport.testkit.TestEnvironment;
import org.apache.synapse.transport.testkit.listener.AsyncChannel;
import org.apache.synapse.transport.testkit.listener.AsyncMessageSender;
import org.apache.synapse.transport.testkit.listener.AsyncMessageTestCase;
import org.apache.synapse.transport.testkit.listener.ContentTypeMode;
import org.apache.synapse.transport.testkit.message.ByteArrayMessage;
import org.apache.synapse.transport.testkit.name.DisplayName;
import org.apache.synapse.transport.testkit.server.AsyncEndpointFactory;

@DisplayName("AsyncBinary")
public class BinaryTestCase<E extends TestEnvironment,C extends AsyncChannel<? super E>> extends AsyncMessageTestCase<E,C,ByteArrayMessage,ByteArrayMessage> {
    private static final Random random = new Random();
    
    public BinaryTestCase(E env, C channel, AsyncMessageSender<? super E,? super C,ByteArrayMessage> sender, AsyncEndpointFactory<? super E,? super C,ByteArrayMessage> endpointFactory, ContentTypeMode contentTypeMode) {
        super(env, channel, sender, endpointFactory, contentTypeMode, "application/octet-stream", null);
    }
    
    @Override
    protected ByteArrayMessage prepareMessage() throws Exception {
        byte[] content = new byte[8192];
        random.nextBytes(content);
        return new ByteArrayMessage("application/octet-stream", content);
    }

    @Override
    protected void checkMessageData(ByteArrayMessage message, ByteArrayMessage messageData) throws Exception {
        assertTrue(Arrays.equals(message.getContent(), messageData.getContent()));
    }
}
