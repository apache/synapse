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

import org.apache.synapse.MessageContext;
import org.apache.synapse.Constants;
import org.apache.synapse.FaultHandler;
import org.apache.synapse.endpoints.utils.EndpointDefinition;
import org.apache.synapse.statistics.impl.EndPointStatisticsStack;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Stack;

/**
 * WSDLEndpoint represents the endpoints built using a wsdl document. It stores the details about
 * the endpoint in a EndpointDefinition object. Once the WSDLEndpoint object is contructed, it should
 * not access the wsdl document at runtime to obtain endpoint information. If it is neccessary to
 * create an endpoint using a dynamic wsdl, store the endpoint configuration in the registry and
 * create a dynamic wsdl endpoint using that registry key.
 *
 * TODO: This should allow variuos policies to be applied on fine grained level (e.g. operations).
 */
public class WSDLEndpoint extends FaultHandler implements Endpoint {

    private static final Log log = LogFactory.getLog(AddressEndpoint.class);

    private String name;
    private boolean active = true;
    private Endpoint parentEndpoint = null;
    private EndpointDefinition endpointDefinition = null;

    public void send(MessageContext synCtx) {

        String eprAddress = null;
        if (endpointDefinition.getAddress() != null) {
            eprAddress = endpointDefinition.getAddress().toString();

            if (endpointDefinition.isForcePOX()) {
                synCtx.setDoingPOX(true);
            } else if (endpointDefinition.isForceSOAP()) {
                synCtx.setDoingPOX(false);
            }

            if (endpointDefinition.isUseMTOM()) {
                synCtx.setDoingMTOM(true);
                // fix / workaround for AXIS2-1798
                ((Axis2MessageContext) synCtx).getAxis2MessageContext().setProperty(
                    org.apache.axis2.Constants.Configuration.ENABLE_MTOM,
                    org.apache.axis2.Constants.VALUE_TRUE);
            } else if (endpointDefinition.isUseSwa()) {
                synCtx.setDoingSWA(true);
                // fix / workaround for AXIS2-1798
                ((Axis2MessageContext) synCtx).getAxis2MessageContext().setProperty(
                    org.apache.axis2.Constants.Configuration.ENABLE_MTOM,
                    org.apache.axis2.Constants.VALUE_TRUE);
            }

            if (endpointDefinition.isUseSeparateListener()) {
                synCtx.setProperty(Constants.OUTFLOW_USE_SEPARATE_LISTENER, Boolean.TRUE);
            }

            String endPointName = this.getName();

            // Setting Required property to collect the End Point statistics
            boolean statisticsEnable = (org.apache.synapse.Constants.STATISTICS_ON == endpointDefinition.getStatisticsEnable());
            if (endPointName != null && statisticsEnable) {
                EndPointStatisticsStack endPointStatisticsStack = new EndPointStatisticsStack();
                boolean isFault =synCtx.getEnvelope().getBody().hasFault();
                endPointStatisticsStack.put(endPointName, System.currentTimeMillis(), !synCtx.isResponse(), statisticsEnable,isFault);
                synCtx.setProperty(org.apache.synapse.Constants.ENDPOINT_STATISTICS_STACK, endPointStatisticsStack);
            }
            synCtx.setTo(new EndpointReference(eprAddress));

            if (log.isDebugEnabled()) {
                log.debug("Sending message to endpoint :: name = " +
                        endPointName + " resolved address = " + eprAddress);
                log.debug("Sending To: " + (synCtx.getTo() != null ?
                        synCtx.getTo().getAddress() : "null"));
                log.debug("SOAPAction: " + (synCtx.getWSAAction() != null ?
                        synCtx.getWSAAction() : "null"));
                log.debug("Body : \n" + synCtx.getEnvelope());
            }

            // if RM is turned on
            if (endpointDefinition.isReliableMessagingOn()) {
                synCtx.setProperty(Constants.OUTFLOW_ADDRESSING_ON, Boolean.TRUE);
                synCtx.setProperty(Constants.OUTFLOW_RM_ON, Boolean.TRUE);
                if (endpointDefinition.getWsRMPolicyKey() != null) {
                    synCtx.setProperty(Constants.OUTFLOW_RM_POLICY,
                            endpointDefinition.getWsRMPolicyKey());
                }
            }

            // if WS Security is specified
            if (endpointDefinition.isSecurityOn()) {
                synCtx.setProperty(Constants.OUTFLOW_ADDRESSING_ON, Boolean.TRUE);
                synCtx.setProperty(Constants.OUTFLOW_SECURITY_ON, Boolean.TRUE);
                if (endpointDefinition.getWsSecPolicyKey() != null) {
                    synCtx.setProperty(Constants.OUTFLOW_SEC_POLICY,
                            endpointDefinition.getWsSecPolicyKey());
                }
            }

            // if WS Addressing is specified
            if (endpointDefinition.isAddressingOn()) {
                synCtx.setProperty(Constants.OUTFLOW_ADDRESSING_ON, Boolean.TRUE);
            }

            synCtx.pushFaultHandler(this);
            synCtx.getEnvironment().send(endpointDefinition, synCtx);
        }
    }

    public void onFault(MessageContext synCtx) {
         // perform retries here

        // if this endpoint has actually failed, inform the parent.
        if (parentEndpoint != null) {
            parentEndpoint.onChildEndpointFail(this, synCtx);
        } else {
            Stack faultStack = synCtx.getFaultStack();
            if (!faultStack.isEmpty()) {
                ((FaultHandler) faultStack.pop()).handleFault(synCtx);
            }
        }
    }

    public void onChildEndpointFail(Endpoint endpoint, MessageContext synMessageContext) {
        // WSDLEndpoint does not contain any child endpoints. So this method will never be called.
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setParentEndpoint(Endpoint parentEndpoint) {
        this.parentEndpoint = parentEndpoint;
    }

    public EndpointDefinition getEndpointDefinition() {
        return endpointDefinition;
    }

    public void setEndpointDefinition(EndpointDefinition endpointDefinition) {
        this.endpointDefinition = endpointDefinition;
    }
}
