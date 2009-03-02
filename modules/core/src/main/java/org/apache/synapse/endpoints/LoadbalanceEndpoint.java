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

import org.apache.axis2.context.ConfigurationContext;
import org.apache.synapse.FaultHandler;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.endpoints.algorithms.AlgorithmContext;
import org.apache.synapse.endpoints.algorithms.LoadbalanceAlgorithm;

/**
 * A Load balance endpoint contains multiple child endpoints. It routes messages according to the
 * specified load balancing algorithm. This will assume that all immediate child endpoints are
 * identical in state (state is replicated) or state is not maintained at those endpoints. If an
 * endpoint is failing, the failed endpoint is marked as inactive and the message sent to the next
 * endpoint obtained using the load balancing algorithm. If all the endpoints have failed and a
 * parent endpoint is available, onChildEndpointFail(...) method of parent endpoint is called. If
 * a parent is not available, this will call next FaultHandler for the message context.
 */
public class LoadbalanceEndpoint extends AbstractEndpoint {

    /** Should this load balancer fail over as well? */
    private boolean failover = true;
    /** The algorithm used for selecting the next endpoint */
    private LoadbalanceAlgorithm algorithm = null;
    /** The algorithm context to hold runtime state related to the load balance algorithm */
    private AlgorithmContext algorithmContext = null;

    @Override
    public void init(SynapseEnvironment synapseEnvironment) {
        ConfigurationContext cc =
                ((Axis2SynapseEnvironment) synapseEnvironment).getAxis2ConfigurationContext();
        if (!initialized) {
            super.init(synapseEnvironment);
            if (algorithmContext == null) {
                algorithmContext = new AlgorithmContext(isClusteringEnabled, cc, getName());
            }
        }
    }

    public void send(MessageContext synCtx) {

        if (log.isDebugEnabled()) {
            log.debug("Load-balance Endpoint :  " + getName());
        }

        Endpoint endpoint = getNextChild(synCtx); 

        if (endpoint != null) {
            // if this is not a retry
            if (synCtx.getProperty(SynapseConstants.LAST_ENDPOINT) == null) {
                // We have to build the envelop when we are supporting failover, as we
                // may have to retry this message for failover support
                if (failover) {
                    synCtx.getEnvelope().build();
                }
            } else {
                if (metricsMBean != null) {
                    // this is a retry, where we are now failing over to an active node
                    metricsMBean.reportSendingFault(SynapseConstants.ENDPOINT_LB_FAIL_OVER);
                }
            }
            synCtx.pushFaultHandler(this);
            endpoint.send(synCtx);

        } else {
            // if this is not a retry
            informFailure(synCtx, SynapseConstants.ENDPOINT_LB_NONE_READY, "Loadbalance endpoint : " +
                    getName() + " - no ready child endpoints");
        }
    }

    /**
     * If this endpoint is in inactive state, checks if all immediate child endpoints are still
     * failed. If so returns false. If at least one child endpoint is in active state, sets this
     * endpoint's state to active and returns true. As this a sessionless load balancing endpoint
     * having one active child endpoint is enough to consider this as active.
     *
     * @return true if active. false otherwise.
     */
    public boolean readyToSend() {
        for (Endpoint endpoint : getChildren()) {
            if (endpoint.readyToSend()) {
                if (log.isDebugEnabled()) {
                    log.debug("Endpoint : " + getName() + " has at least one ready endpoint");
                }
                return true;
            }
        }

        log.warn("Endpoint : " + getName() + " has no ready endpoints to process message");

        return false;
    }

    @Override
    public void onChildEndpointFail(Endpoint endpoint, MessageContext synMessageContext) {

        logOnChildEndpointFail(endpoint, synMessageContext);
        // resend (to a different endpoint) only if we support failover
        if (failover) {
            send(synMessageContext);
        } else {
            // we are not informing this to the parent endpoint as the failure of this loadbalance
            // endpoint. there can be more active endpoints under this, and current request has
            // failed only because the currently selected child endpoint has failed AND failover is
            // turned off in this load balance endpoint. so just call the next fault handler.
            Object o = synMessageContext.getFaultStack().pop();
            if (o != null) {
                ((FaultHandler) o).handleFault(synMessageContext);
            }
        }
    }

    public boolean isFailover() {
        return failover;
    }

    public void setFailover(boolean failover) {
        this.failover = failover;
    }

    public LoadbalanceAlgorithm getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(LoadbalanceAlgorithm algorithm) {
        if (log.isDebugEnabled()) {
            log.debug("Endpoint : " + getName() + " will be using the "
                + algorithm.getName() + " for load distribution");
        }
        this.algorithm = algorithm;
    }

    protected Endpoint getNextChild(MessageContext synCtx) {
        return algorithm.getNextEndpoint(synCtx, algorithmContext);
    }
}
