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

import org.apache.axiom.om.OMElement;
import org.apache.axis2.client.OperationClient;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.synapse.transport.testkit.message.XMLMessageType;

public class AxisRequestResponseMessageSender extends AxisMessageSender<RequestResponseChannel<?>> implements XMLRequestResponseMessageSender<RequestResponseChannel<?>> {
    public AxisRequestResponseMessageSender() {
        super("axis");
    }

    public OMElement sendMessage(RequestResponseChannel<?> channel,
            String endpointReference, String contentType, String charset,
            XMLMessageType xmlMessageType, OMElement payload) throws Exception {
        
        OperationClient mepClient = createClient(endpointReference, ServiceClient.ANON_OUT_IN_OP, xmlMessageType, payload, charset);
        mepClient.execute(true);
        
        MessageContext response = mepClient.getMessageContext(WSDLConstants.MESSAGE_LABEL_IN_VALUE);
        return response.getEnvelope().getBody().getFirstElement();
    }
}
