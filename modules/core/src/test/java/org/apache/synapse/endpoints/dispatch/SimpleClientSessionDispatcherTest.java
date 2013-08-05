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

package org.apache.synapse.endpoints.dispatch;

import junit.framework.TestCase;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.endpoints.AddressEndpoint;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.mediators.TestUtils;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

public class SimpleClientSessionDispatcherTest extends TestCase {

    private static final QName CLIENT_ID= new QName("http://ws.apache.org/ns/synapse",
            "ClientID", "syn");

    public void testClientSessionDispatcher() throws Exception {
        AxisConfiguration axisConfiguration = new AxisConfiguration();
        ConfigurationContext configContext = new ConfigurationContext(axisConfiguration);
        SALSessions.getInstance().initialize(true, configContext);

        List<Endpoint> endpoints = new ArrayList<Endpoint>();
        AddressEndpoint endpoint = new AddressEndpoint();
        endpoint.setName("ep1");
        endpoints.add(endpoint);

        Dispatcher dispatcher = new SimpleClientSessionDispatcher();
        assertFalse(dispatcher.isServerInitiatedSession());

        // test session creation
        String clientId = "client0001";
        dispatcher.updateSession(getMessageContext(clientId, endpoints));
        assertNotNull(SALSessions.getInstance().getSession(clientId));

        // test session query
        SessionInformation session = dispatcher.getSession(getMessageContext(clientId,
                endpoints));
        assertNotNull(session);
        assertEquals(session.getEndpointList().get(0).getName(), "ep1");

        // test session removal
        dispatcher.unbind(getMessageContext(clientId, endpoints));
        session = dispatcher.getSession(getMessageContext(clientId, endpoints));
        assertNull(session);
        assertNull(SALSessions.getInstance().getSession(clientId));

        SALSessions.getInstance().reset();
    }

    private MessageContext getMessageContext(String clientId,
                                             List<Endpoint> endpoints) throws Exception {
        MessageContext msgContext = TestUtils.createLightweightSynapseMessageContext("<test/>");
        msgContext.setProperty(SynapseConstants.PROP_SAL_ENDPOINT_ENDPOINT_LIST, endpoints);
        org.apache.axis2.context.MessageContext axis2MsgContext =
                ((Axis2MessageContext) msgContext).getAxis2MessageContext();
        TestUtils.addSOAPHeaderBlock(axis2MsgContext, SimpleClientSessionDispatcherTest.CLIENT_ID,
                clientId);
        return msgContext;
    }
}
