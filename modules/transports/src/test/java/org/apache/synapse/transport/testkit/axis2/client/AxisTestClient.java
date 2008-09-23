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
import javax.xml.namespace.QName;

import org.apache.axiom.attachments.Attachments;
import org.apache.axis2.Constants;
import org.apache.axis2.client.OperationClient;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.MessageContext;
import org.apache.synapse.transport.testkit.channel.Channel;
import org.apache.synapse.transport.testkit.client.ClientOptions;
import org.apache.synapse.transport.testkit.client.TestClient;
import org.apache.synapse.transport.testkit.message.AxisMessage;
import org.apache.synapse.transport.testkit.name.Name;
import org.apache.synapse.transport.testkit.util.ContentTypeUtil;

@Name("axis")
public class AxisTestClient implements TestClient {
    private AxisTestClientSetup[] setups;
    protected ServiceClient serviceClient;
    protected Options axisOptions;
    
    @SuppressWarnings("unused")
    private void setUp(AxisTestClientContext context, Channel channel, AxisTestClientSetup[] setups) throws Exception {
        this.setups = setups;
        serviceClient = new ServiceClient(context.getConfigurationContext(), null);
        axisOptions = new Options();
        axisOptions.setTo(channel.getEndpointReference());
        serviceClient.setOptions(axisOptions);
    }
    
    @SuppressWarnings("unused")
    private void tearDown() throws Exception {
        serviceClient.cleanup();
        setups = null;
        serviceClient = null;
        axisOptions = null;
    }

    public ContentType getContentType(ClientOptions options, ContentType contentType) {
        // TODO: this may be incorrect in some cases
        String charset = options.getCharset();
        if (charset == null) {
            return contentType;
        } else {
            return ContentTypeUtil.addCharset(contentType, options.getCharset());
        }
    }

    protected OperationClient createClient(ClientOptions options, AxisMessage message, QName operationQName) throws Exception {
        OperationClient mepClient = serviceClient.createClient(operationQName);
        MessageContext mc = new MessageContext();
        mc.setProperty(Constants.Configuration.MESSAGE_TYPE, message.getMessageType());
        mc.setEnvelope(message.getEnvelope());
        Attachments attachments = message.getAttachments();
        if (attachments != null) {
            mc.setAttachmentMap(attachments);
            mc.setDoingSwA(true);
            mc.setProperty(Constants.Configuration.ENABLE_SWA, true);
        }
        for (AxisTestClientSetup setup : setups) {
            setup.setupRequestMessageContext(mc);
        }
        mc.setProperty(Constants.Configuration.CHARACTER_SET_ENCODING, options.getCharset());
        mc.setServiceContext(serviceClient.getServiceContext());
        mepClient.addMessageContext(mc);
        
        return mepClient;
    }
}
