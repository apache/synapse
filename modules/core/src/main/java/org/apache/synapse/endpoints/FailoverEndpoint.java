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
import org.apache.synapse.SynapseConstants;

/**
 * FailoverEndpoint can have multiple child endpoints. It will always try to send messages to
 * current endpoint. If the current endpoint is failing, it gets another active endpoint from the
 * list and make it the current endpoint. Then the message is sent to the current endpoint and if
 * it fails, above procedure repeats until there are no active endpoints. If all endpoints are
 * failing and parent endpoint is available, this will delegate the problem to the parent endpoint.
 * If parent endpoint is not available it will pop the next FaultHandler and delegate the problem
 * to that.
 */
public class FailoverEndpoint extends AbstractEndpoint {

    /** Endpoint for which is currently used */
    private Endpoint currentEndpoint = null;

    public void send(MessageContext synCtx) {

        if (log.isDebugEnabled()) {
            log.debug("Failover Endpoint : " + getName());
        }

        boolean isARetry = false;
        if (synCtx.getProperty(SynapseConstants.LAST_ENDPOINT) == null) {
            // If not yet a retry, we have to build the envelope since we need to support failover
            synCtx.getEnvelope().build();
        } else {
            isARetry = true;
        }

        if (currentEndpoint == null) {
            currentEndpoint = getChildren().get(0);
        }

        if (currentEndpoint.readyToSend()) {
            if (isARetry) {
                metricsMBean.reportSendingFault(SynapseConstants.ENDPOINT_FO_FAIL_OVER);
            }
            synCtx.pushFaultHandler(this);
            currentEndpoint.send(synCtx);

        } else {
            boolean foundEndpoint = false;
            for (Endpoint endpoint : getChildren()) {
                if (endpoint.readyToSend()) {
                    foundEndpoint = true;
                    currentEndpoint = endpoint;
                    if (isARetry) {
                        metricsMBean.reportSendingFault(SynapseConstants.ENDPOINT_FO_FAIL_OVER);
                    }
                    synCtx.pushFaultHandler(this);
                    currentEndpoint.send(synCtx);
                    break;
                }
            }

            if (!foundEndpoint) {
                // if this is not a retry
                if (synCtx.getProperty(SynapseConstants.LAST_ENDPOINT) == null) {
                    synCtx.setProperty(SynapseConstants.ERROR_CODE, SynapseConstants.ENDPOINT_FO_NONE_READY);
                    synCtx.setProperty(SynapseConstants.ERROR_MESSAGE,
                        "Failover endpoint : " + getName() + " - no ready child endpoints");
                    synCtx.setProperty(SynapseConstants.ERROR_DETAIL,
                        "Failover endpoint : " + getName() + " - no ready child endpoints");
                    synCtx.setProperty(SynapseConstants.ERROR_EXCEPTION, null);
                }
                super.onFault(synCtx);
            }
        }
    }

    public void onChildEndpointFail(Endpoint endpoint, MessageContext synMessageContext) {
        send(synMessageContext);
    }

    public boolean readyToSend() {
        for (Endpoint endpoint : getChildren()) {
            if (endpoint.readyToSend()) {
                currentEndpoint = endpoint;
                return true;
            }
        }
        return false;
    }        
}
