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

import java.util.List;

/**
 * FailoverEndpoint can have multiple child endpoints. It will always try to send messages to current
 * endpoint. If the current endpoint is failing, it gets another active endpoint from the list and
 * make it the current endpoint. Then the message is sent to the current endpoint and if it fails, above
 * procedure repeats until there are no active endpoints. If all endpoints are failing and parent
 * endpoint is available, this will delegate the problem to the parent endpoint. If parent endpoint
 * is not available it will pop the next FaultHandler and delegate the problem to that.
 */
public class FailoverEndpoint implements Endpoint {

    /**
     * Name of the endpoint. Used for named endpoints which can be referred using the key attribute
     * of indirect endpoints.
     */
    private String name = null;

    /**
     * Determine whether this endpoint is active or not. This is active iff all child endpoints of
     * this endpoint is active. This is always loaded from the memory as it could be accessed from
     * multiple threads simultaneously.
     */
    private volatile boolean active = true;

    /**
     * List of child endpoints. Failover sending is done among these. Any object implementing the
     * Endpoint interface can be a child.
     */
    private List endpoints = null;

    /**
     * Endpoint for which currently sending the SOAP traffic.
     */
    private Endpoint currentEndpoint = null;

    /**
     * Parent endpoint of this endpoint if this used inside another endpoint. Possible parents are
     * LoadbalanceEndpoint, SALoadbalanceEndpoint and FailoverEndpoint objects. But use of
     * SALoadbalanceEndpoint as the parent is the logical scenario.
     */
    private Endpoint parentEndpoint = null;

    public void send(MessageContext synMessageContext) {

        // We have to build the envelop if we are supporting failover.
        // Failover should sent the original message multiple times if failures occur. So we have to
        // access the envelop multiple times.        
        synMessageContext.getEnvelope().build();

        if (currentEndpoint.isActive(synMessageContext)) {
            currentEndpoint.send(synMessageContext);
        } else {

            Endpoint liveEndpoint = null;
            boolean foundEndpoint = false;
            for (int i = 0; i < endpoints.size(); i++) {
                liveEndpoint = (Endpoint) endpoints.get(i);
                if (liveEndpoint.isActive(synMessageContext)) {
                    foundEndpoint = true;
                    currentEndpoint = liveEndpoint;
                    currentEndpoint.send(synMessageContext);
                    break;
                }
            }

            if (!foundEndpoint) {
                // there are no active child endpoints. so mark this endpoint as failed.
                setActive(false, synMessageContext);

                if (parentEndpoint != null) {
                    parentEndpoint.onChildEndpointFail(this, synMessageContext);
                } else {
                    Object o = synMessageContext.getFaultStack().pop();
                    if (o != null) {
                        ((FaultHandler) o).handleFault(synMessageContext);
                    }
                }
            }
        }
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name.trim();
    }

    /**
     * If this endpoint is in inactive state, checks if all immediate child endpoints are still
     * failed. If so returns false. If at least one child endpoint is in active state, sets this
     * endpoint's state to active and returns true.
     *
     * @param synMessageContext MessageContext of the current message. This is not used here.
     *
     * @return true if active. false otherwise.
     */
    public boolean isActive(MessageContext synMessageContext) {

        if (!active) {
            for (int i = 0; i < endpoints.size(); i++) {
                Endpoint endpoint = (Endpoint) endpoints.get(i);
                if (endpoint.isActive(synMessageContext)) {
                    active = true;

                    // don't break the loop though we found one active endpoint. calling isActive()
                    // on all child endpoints will update their active state. so this is a good
                    // time to do that.
                }
            }
        }

        return active;
    }

    public void setActive(boolean active, MessageContext synMessageContext) {
        // setting a volatile boolean value is thread safe.
        this.active = active;
    }

    public List getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(List endpoints) {
        this.endpoints = endpoints;
        if (endpoints.size() > 0) {
            currentEndpoint = (Endpoint) endpoints.get(0);
        }
    }

    public void onChildEndpointFail(Endpoint endpoint, MessageContext synMessageContext) {
        send(synMessageContext);
    }

    public void setParentEndpoint(Endpoint parentEndpoint) {
        this.parentEndpoint = parentEndpoint;
    }
}
