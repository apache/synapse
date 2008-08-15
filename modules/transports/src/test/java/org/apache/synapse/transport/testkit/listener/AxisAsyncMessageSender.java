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

import org.apache.axis2.client.ServiceClient;
import org.apache.synapse.transport.testkit.TestEnvironment;
import org.apache.synapse.transport.testkit.TransportDescriptionFactory;
import org.apache.synapse.transport.testkit.message.XMLMessage;
import org.apache.synapse.transport.testkit.name.DisplayName;

@DisplayName("axis")
public class AxisAsyncMessageSender extends AxisMessageSender<AsyncChannel<?>> implements AsyncMessageSender<TestEnvironment,AsyncChannel<?>,XMLMessage> {
    public AxisAsyncMessageSender(TransportDescriptionFactory tdf) {
        super(tdf);
    }

    public void sendMessage(AsyncChannel<?> channel, SenderOptions options, XMLMessage message) throws Exception {
        createClient(options.getEndpointReference(), ServiceClient.ANON_OUT_ONLY_OP, message.getXmlMessageType(), message.getPayload(), options.getCharset()).execute(false);
    }
}
