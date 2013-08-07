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

package org.apache.synapse.endpoints;

import junit.framework.TestCase;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.synapse.FaultHandler;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.mediators.TestUtils;

public class BasicEndpointTest extends TestCase {

    private static final int CUSTOM_ERROR = 911911;

    public void testDefaultTimeoutErrorHandling() throws Exception {
        AddressEndpoint endpoint = new AddressEndpoint();
        EndpointDefinition def = new EndpointDefinition();
        endpoint.setDefinition(def);

        MessageContext messageContext = TestUtils.createLightweightSynapseMessageContext("<test/>");
        messageContext.setProperty(SynapseConstants.ERROR_CODE,
                SynapseConstants.NHTTP_CONNECTION_TIMEOUT);
        assertTrue(endpoint.isTimeout(messageContext));

        messageContext.setProperty(SynapseConstants.ERROR_CODE,
                SynapseConstants.NHTTP_CONNECTION_CLOSED);
        assertTrue(endpoint.isTimeout(messageContext));
    }

    public void testCustomTimeoutErrorHandling() throws Exception {
        AddressEndpoint endpoint = new AddressEndpoint();
        EndpointDefinition def = new EndpointDefinition();
        def.addTimeoutErrorCode(SynapseConstants.NHTTP_CONNECTION_TIMEOUT);
        def.addTimeoutErrorCode(CUSTOM_ERROR);
        endpoint.setDefinition(def);

        MessageContext messageContext = TestUtils.createLightweightSynapseMessageContext("<test/>");
        messageContext.setProperty(SynapseConstants.ERROR_CODE,
                SynapseConstants.NHTTP_CONNECTION_TIMEOUT);
        assertTrue(endpoint.isTimeout(messageContext));

        messageContext.setProperty(SynapseConstants.ERROR_CODE, CUSTOM_ERROR);
        assertTrue(endpoint.isTimeout(messageContext));

        messageContext.setProperty(SynapseConstants.ERROR_CODE,
                SynapseConstants.NHTTP_CONNECTION_CLOSED);
        assertFalse(endpoint.isTimeout(messageContext));
    }

    public void testDefaultSuspendErrorHandling() throws Exception {
        AddressEndpoint endpoint = new AddressEndpoint();
        EndpointDefinition def = new EndpointDefinition();
        endpoint.setDefinition(def);

        MessageContext messageContext = TestUtils.createLightweightSynapseMessageContext("<test/>");
        messageContext.setProperty(SynapseConstants.ERROR_CODE,
                SynapseConstants.NHTTP_CONNECTION_TIMEOUT);
        assertTrue(endpoint.isSuspendFault(messageContext));

        messageContext.setProperty(SynapseConstants.ERROR_CODE, CUSTOM_ERROR);
        assertTrue(endpoint.isSuspendFault(messageContext));
    }

    public void testCustomSuspendErrorHandling() throws Exception {
        AddressEndpoint endpoint = new AddressEndpoint();
        EndpointDefinition def = new EndpointDefinition();
        def.addSuspendErrorCode(CUSTOM_ERROR);
        endpoint.setDefinition(def);

        MessageContext messageContext = TestUtils.createLightweightSynapseMessageContext("<test/>");
        messageContext.setProperty(SynapseConstants.ERROR_CODE,
                SynapseConstants.NHTTP_CONNECTION_TIMEOUT);
        assertFalse(endpoint.isSuspendFault(messageContext));

        messageContext.setProperty(SynapseConstants.ERROR_CODE, CUSTOM_ERROR);
        assertTrue(endpoint.isSuspendFault(messageContext));
    }

    public void testDefaultRetryErrorHandling() throws Exception {
        AddressEndpoint endpoint = new AddressEndpoint();
        EndpointDefinition def = new EndpointDefinition();
        endpoint.setDefinition(def);

        MessageContext messageContext = TestUtils.createLightweightSynapseMessageContext("<test/>");
        messageContext.setProperty(SynapseConstants.ERROR_CODE,
                SynapseConstants.NHTTP_CONNECTION_TIMEOUT);
        assertFalse(endpoint.isRetryDisabled(messageContext));

        messageContext.setProperty(SynapseConstants.ERROR_CODE, CUSTOM_ERROR);
        assertFalse(endpoint.isRetryDisabled(messageContext));
    }

    public void testCustomRetryErrorHandling() throws Exception {
        AddressEndpoint endpoint = new AddressEndpoint();
        EndpointDefinition def = new EndpointDefinition();
        def.addRetryDisabledErrorCode(CUSTOM_ERROR);
        endpoint.setDefinition(def);

        MessageContext messageContext = TestUtils.createLightweightSynapseMessageContext("<test/>");
        messageContext.setProperty(SynapseConstants.ERROR_CODE,
                SynapseConstants.NHTTP_CONNECTION_TIMEOUT);
        assertFalse(endpoint.isRetryDisabled(messageContext));

        messageContext.setProperty(SynapseConstants.ERROR_CODE, CUSTOM_ERROR);
        assertTrue(endpoint.isRetryDisabled(messageContext));
    }

    public void testFaultHandlerInvocation() throws Exception {
        AddressEndpoint endpoint = new AddressEndpoint();
        EndpointDefinition def = new EndpointDefinition();
        endpoint.setDefinition(def);

        MessageContext messageContext = TestUtils.createLightweightSynapseMessageContext("<test/>");
        TestFaultHandler faultHandler = new TestFaultHandler();
        messageContext.pushFaultHandler(faultHandler);

        endpoint.onFault(messageContext);
        assertTrue(faultHandler.invoked);

        faultHandler.invoked = false;
        messageContext = TestUtils.createLightweightSynapseMessageContext("<test/>");
        messageContext.pushFaultHandler(faultHandler);
        endpoint.informFailure(messageContext, CUSTOM_ERROR, "Custom Error");
        assertTrue(faultHandler.invoked);
        assertEquals(String.valueOf(CUSTOM_ERROR),
                messageContext.getProperty(SynapseConstants.ERROR_CODE));
        assertEquals("Custom Error",
                messageContext.getProperty(SynapseConstants.ERROR_MESSAGE));
    }

    public void testSend() throws Exception {
        SynapseConfiguration synapseConfig = new SynapseConfiguration();
        AxisConfiguration axisConfig = new AxisConfiguration();
        ConfigurationContext configContext = new ConfigurationContext(axisConfig);
        SynapseEnvironment env = new Axis2SynapseEnvironment(configContext, synapseConfig) {
            @Override
            public void send(EndpointDefinition endpoint, MessageContext synCtx) {
                synCtx.setProperty("__test__", "__success__");
            }
        };

        AddressEndpoint endpoint = new AddressEndpoint();
        EndpointDefinition def = new EndpointDefinition();
        def.setAddress("http://foo.com");
        endpoint.setDefinition(def);
        endpoint.init(env);

        MessageContext messageContext = TestUtils.createLightweightSynapseMessageContext("<test/>");
        messageContext.setEnvironment(env);
        endpoint.send(messageContext);

        assertEquals("__success__", messageContext.getProperty("__test__"));
        assertTrue(messageContext.getFaultStack().peek() == endpoint);
        assertTrue(messageContext.getProperty(SynapseConstants.LAST_ENDPOINT) == endpoint);
        endpoint.destroy();
    }

    private static class TestFaultHandler extends FaultHandler {

        boolean invoked = false;

        @Override
        public void onFault(MessageContext synCtx) {
            invoked = true;
        }
    }
}
