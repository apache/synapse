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
import org.apache.axis2.clustering.Member;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.endpoints.AddressEndpoint;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.mediators.TestUtils;

import java.util.*;

public class HttpSessionDispatcherTest extends TestCase {

    public void testHttpSessionDispatcher() throws Exception {
        AxisConfiguration axisConfiguration = new AxisConfiguration();
        ConfigurationContext configContext = new ConfigurationContext(axisConfiguration);
        SALSessions.getInstance().initialize(true, configContext);

        List<Endpoint> endpoints = new ArrayList<Endpoint>();
        AddressEndpoint endpoint = new AddressEndpoint();
        endpoint.setName("ep1");
        endpoints.add(endpoint);

        Dispatcher dispatcher = new HttpSessionDispatcher();
        assertTrue(dispatcher.isServerInitiatedSession());

        // test session creation
        String clientId = "JSESSIONID=760764CB72E96A7221506823748CF2AE";
        dispatcher.updateSession(getMessageContext(clientId + "; Path=/", true, endpoints));
        assertNotNull(SALSessions.getInstance().getSession(clientId));

        // test session query
        SessionInformation session = dispatcher.getSession(getMessageContext(clientId, false,
                endpoints));
        assertNotNull(session);
        assertNotNull(session.getMember());

        // test session removal
        dispatcher.unbind(getMessageContext(clientId, false, endpoints));
        session = dispatcher.getSession(getMessageContext(clientId, false, endpoints));
        assertNull(session);
        assertNull(SALSessions.getInstance().getSession(clientId));

        SALSessions.getInstance().reset();
    }

    private MessageContext getMessageContext(String clientId, boolean response,
                                             List<Endpoint> endpoints) throws Exception {
        MessageContext msgContext = TestUtils.createLightweightSynapseMessageContext("<test/>");
        msgContext.setProperty(SynapseConstants.PROP_SAL_ENDPOINT_ENDPOINT_LIST, endpoints);
        Member member = new Member("127.0.0.1", 9000);
        Properties props = new Properties();
        Map<String,String> hosts = new HashMap<String, String>();
        hosts.put("127.0.0.1", "foo");
        props.put(HttpSessionDispatcher.HOSTS, hosts);
        member.setProperties(props);
        msgContext.setProperty(SynapseConstants.PROP_SAL_ENDPOINT_CURRENT_MEMBER, member);
        org.apache.axis2.context.MessageContext axis2MsgContext =
                ((Axis2MessageContext) msgContext).getAxis2MessageContext();
        Map<String,String> headers = new HashMap<String, String>();
        if (response) {
            headers.put("Set-Cookie", clientId);
            msgContext.setResponse(true);
        } else {
            headers.put("Cookie", clientId);
            headers.put("Host", "127.0.0.1");
        }
        axis2MsgContext.setProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS, headers);
        return msgContext;
    }
}
