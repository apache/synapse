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

package org.apache.synapse.transport.testkit.axis2.client;

import javax.mail.internet.ContentType;

import junit.framework.Assert;

import org.apache.axis2.client.OperationClient;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.synapse.transport.testkit.axis2.MessageContextValidator;
import org.apache.synapse.transport.testkit.client.ClientOptions;
import org.apache.synapse.transport.testkit.client.RequestResponseTestClient;
import org.apache.synapse.transport.testkit.message.AxisMessage;
import org.apache.synapse.transport.testkit.message.IncomingMessage;

public class AxisRequestResponseTestClient extends AxisTestClient implements RequestResponseTestClient<AxisMessage,AxisMessage> {
    private MessageContextValidator[] validators;
    
    @SuppressWarnings("unused")
    private void setUp(MessageContextValidator[] validators) {
        this.validators = validators;
    }
    
    public IncomingMessage<AxisMessage> sendMessage(ClientOptions options, ContentType contentType, AxisMessage message) throws Exception {
        OperationClient mepClient = createClient(options, message, ServiceClient.ANON_OUT_IN_OP);
        mepClient.execute(true);
        MessageContext responseMsgContext = mepClient.getMessageContext(WSDLConstants.MESSAGE_LABEL_IN_VALUE);
        Assert.assertFalse(responseMsgContext.isServerSide());
        for (MessageContextValidator validator : validators) {
            validator.validate(responseMsgContext, true);
        }
        return new IncomingMessage<AxisMessage>(null, new AxisMessage(responseMsgContext));
    }
}
