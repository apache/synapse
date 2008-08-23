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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.axiom.om.OMElement;
import org.apache.synapse.transport.testkit.client.AsyncTestClient;
import org.apache.synapse.transport.testkit.listener.AsyncChannel;
import org.apache.synapse.transport.testkit.listener.ContentTypeMode;
import org.apache.synapse.transport.testkit.message.AxisMessage;
import org.apache.synapse.transport.testkit.message.RESTMessage;
import org.apache.synapse.transport.testkit.message.RESTMessage.Parameter;
import org.apache.synapse.transport.testkit.name.Name;
import org.apache.synapse.transport.testkit.server.AsyncEndpointFactory;

@Name("REST")
public class RESTTestCase extends AsyncMessageTestCase<RESTMessage,AxisMessage> {
    private final RESTMessage message;
    
    public RESTTestCase(AsyncChannel channel, AsyncTestClient<RESTMessage> client, AsyncEndpointFactory<AxisMessage> endpointFactory, RESTMessage message, Object... resources) {
        super(channel, client, endpointFactory, ContentTypeMode.TRANSPORT, null, null, resources);
        this.message = message;
    }
    
    @Override
    protected RESTMessage prepareMessage() throws Exception {
        return message;
    }

    @Override
    protected void checkMessageData(RESTMessage message, AxisMessage messageData) throws Exception {
        OMElement content = messageData.getEnvelope().getBody().getFirstElement();
        Set<Parameter> expected = new HashSet<Parameter>(Arrays.asList(message.getParameters()));
        for (Iterator<?> it = content.getChildElements(); it.hasNext(); ) {
            OMElement child = (OMElement)it.next();
            assertTrue(expected.remove(new Parameter(child.getLocalName(), child.getText())));
        }
        assertTrue(expected.isEmpty());
    }
}
