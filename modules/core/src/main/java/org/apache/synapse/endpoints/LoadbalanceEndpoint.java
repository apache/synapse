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

import org.apache.synapse.FaultHandler;
import org.apache.synapse.MessageContext;
import org.apache.synapse.endpoints.algorithms.LoadbalanceAlgorithm;

import java.util.ArrayList;

/**
 * Load balance endpoint can have multiple endpoints. It will route messages according to the
 * specified loadbalance algorithm. This will assume that all immediate child endpoints are identical
 * in state (state is replicated) or state is not maintained at those endpoints. If an endpoint is
 * failing, the failed endpoint is marked as inactive and the message to the next endpoint obtained
 * using the loadbalance algorithm. If all the endpoints have failed and the parent endpoint is
 * available, onChildEndpointFail(...) methos of parent endpoint is called. If parent is not
 * avialable, this will call next FaultHandler for the message context.
 */
public class LoadbalanceEndpoint implements Endpoint {

    private ArrayList endpoints = null;
    private long abandonTime = 0;
    private LoadbalanceAlgorithm algorithm = null;
    private int maximumRetries = 1;
    private long retryInterval = 30000;
    private String name = null;
    private boolean active = true;
    private Endpoint parentEndpoint = null;

    public void send(MessageContext synMessageContext) {

        Endpoint endpoint = algorithm.getNextEndpoint(synMessageContext);
        if (endpoint != null) {
            endpoint.send(synMessageContext);
        } else {
            if (parentEndpoint != null) {
                parentEndpoint.onChildEndpointFail(this, synMessageContext);
            } else {
                Object o = synMessageContext.getFaultStack().pop();
                ((FaultHandler) o).handleFault(synMessageContext);
            }
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LoadbalanceAlgorithm getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(LoadbalanceAlgorithm algorithm) {
        this.algorithm = algorithm;
    }

    public int getMaximumRetries() {
        return maximumRetries;
    }

    public void setMaximumRetries(int maximumRetries) {
        this.maximumRetries = maximumRetries;
    }

    public long getRetryInterval() {
        return retryInterval;
    }

    public void setRetryInterval(long retryInterval) {
        this.retryInterval = retryInterval;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public ArrayList getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(ArrayList endpoints) {
        this.endpoints = endpoints;
    }

    public long getAbandonTime() {
        return abandonTime;
    }

    public void setAbandonTime(long abandonTime) {
        this.abandonTime = abandonTime;
    }

    public void setParentEndpoint(Endpoint parentEndpoint) {
        this.parentEndpoint = parentEndpoint;
    }

    public void onChildEndpointFail(Endpoint endpoint, MessageContext synMessageContext) {
        endpoint.setActive(false);
        send(synMessageContext);
    }
}
