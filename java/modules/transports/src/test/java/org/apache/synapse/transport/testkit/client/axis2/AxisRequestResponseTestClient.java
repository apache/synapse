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

package org.apache.synapse.transport.testkit.client.axis2;

import org.apache.axis2.client.OperationClient;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.synapse.transport.testkit.client.ClientOptions;
import org.apache.synapse.transport.testkit.client.RequestResponseTestClient;
import org.apache.synapse.transport.testkit.message.AxisMessage;

public class AxisRequestResponseTestClient extends AxisTestClient implements RequestResponseTestClient<AxisMessage,AxisMessage> {
    public AxisRequestResponseTestClient(AxisTestClientSetup setup) {
        super(setup);
    }

    public AxisRequestResponseTestClient() {
        super();
    }

    public AxisMessage sendMessage(ClientOptions options, AxisMessage message) throws Exception {
        OperationClient mepClient = createClient(options, message, ServiceClient.ANON_OUT_IN_OP);
        mepClient.execute(true);
        return new AxisMessage(mepClient.getMessageContext(WSDLConstants.MESSAGE_LABEL_IN_VALUE));
    }
}
