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

package org.apache.synapse.transport.jms;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.synapse.transport.testkit.client.axis2.AxisAsyncTestClient;
import org.apache.synapse.transport.testkit.name.Key;

public class JMSAxisAsyncClient extends AxisAsyncTestClient {
    private final String jmsMessageType;
    
    public JMSAxisAsyncClient(String jmsMessageType) {
        this.jmsMessageType = jmsMessageType;
    }

    @Key(JMSConstants.JMS_MESSAGE_TYPE)
    public String getJmsMessageType() {
        return jmsMessageType;
    }

    @Override
    protected void setupRequestMessageContext(MessageContext msgContext) throws AxisFault {
        super.setupRequestMessageContext(msgContext);
        msgContext.setProperty(JMSConstants.JMS_MESSAGE_TYPE, jmsMessageType);
    }
}
