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
import org.apache.synapse.SynapseException;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.statistics.impl.EndPointStatisticsStack;
import org.apache.synapse.config.EndpointDefinition;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class represents an actual endpoint to send the message. It is resposible for sending the
 * message, performing reries if a failure occured and informing the parent endpoint if a failure
 * couldn't be recovered.
 */
public class AddressEndpoint extends FaultHandler implements Endpoint {

    private static final Log log = LogFactory.getLog(AddressEndpoint.class);

    private String name;
    private boolean active = true;
    private EndpointDefinition endpoint = null;
    private Endpoint parentEndpoint = null;

    public EndpointDefinition getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(EndpointDefinition endpoint) {
        this.endpoint = endpoint;
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

    public void send(MessageContext synCtx) {

        String eprAddress = null;
        if (endpoint.getAddress() != null) {
            eprAddress = endpoint.getAddress().toString();

            if (endpoint.isForcePOX()) {
                synCtx.setDoingPOX(true);
            } else if (endpoint.isForceSOAP()) {
                synCtx.setDoingPOX(false);
            }

            if (endpoint.isUseMTOM()) {
                synCtx.setDoingMTOM(true);
            } else if (endpoint.isUseSwa()) {
                synCtx.setDoingSWA(true);
            }

            if (endpoint.isUseSeparateListener()) {
                synCtx.setProperty(Constants.OUTFLOW_USE_SEPARATE_LISTENER, Boolean.TRUE);
            }

            String endPointName = this.getName();

            // Setting Required property to collect the End Point statistics
            boolean statisticsEnable = (org.apache.synapse.Constants.STATISTICS_ON == endpoint.getStatisticsEnable());
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
            if (endpoint.isReliableMessagingOn()) {
                synCtx.setProperty(Constants.OUTFLOW_ADDRESSING_ON, Boolean.TRUE);
                synCtx.setProperty(Constants.OUTFLOW_RM_ON, Boolean.TRUE);
                if (endpoint.getWsRMPolicyKey() != null) {
                    synCtx.setProperty(Constants.OUTFLOW_RM_POLICY,
                            endpoint.getWsRMPolicyKey());
                }
            }

            // if WS Security is specified
            if (endpoint.isSecurityOn()) {
                synCtx.setProperty(Constants.OUTFLOW_ADDRESSING_ON, Boolean.TRUE);
                synCtx.setProperty(Constants.OUTFLOW_SECURITY_ON, Boolean.TRUE);
                if (endpoint.getWsSecPolicyKey() != null) {
                    synCtx.setProperty(Constants.OUTFLOW_SEC_POLICY,
                            endpoint.getWsSecPolicyKey());
                }
            }

            // if WS Addressing is specified
            if (endpoint.isAddressingOn()) {
                synCtx.setProperty(Constants.OUTFLOW_ADDRESSING_ON, Boolean.TRUE);
            }

            synCtx.pushFault(this);
            synCtx.getEnvironment().send(endpoint, synCtx);
        }
    }

    public void onChildEndpointFail(Endpoint endpoint, MessageContext synMessageContext) {
        // nothing to do as this is a leaf level endpoint
    }

    public void setParentEndpoint(Endpoint parentEndpoint) {
        this.parentEndpoint = parentEndpoint;
    }

    public void onFault(MessageContext synCtx) throws SynapseException {
        // perform retries here

        // if this endpoint has actually failed, inform the parent.
        if (parentEndpoint != null) {
            parentEndpoint.onChildEndpointFail(this, synCtx);
        } else {
            Object o = synCtx.getFaultStack().pop();
            ((FaultHandler) o).handleFault(synCtx);
        }
    }
}
