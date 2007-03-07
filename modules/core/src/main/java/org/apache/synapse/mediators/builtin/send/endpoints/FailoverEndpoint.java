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

package org.apache.synapse.mediators.builtin.send.endpoints;

import org.apache.synapse.MessageContext;

import java.util.ArrayList;

public class FailoverEndpoint implements Endpoint {

    private String name = null;
    private boolean active = true;
    private ArrayList endpoints = null;
    private Endpoint currentEndpoint = null;
    private Endpoint parentEndpoint = null;

    public void send(MessageContext synMessageContext) {

        if (currentEndpoint.isActive()) {
            currentEndpoint.send(synMessageContext);
        } else {

            Endpoint liveEndpoint = null;
            boolean foundEndpoint = false;
            for (int i = 0; i < endpoints.size(); i++) {
                liveEndpoint = (Endpoint) endpoints.get(i);
                if (liveEndpoint.isActive()) {
                    foundEndpoint = true;
                    currentEndpoint = liveEndpoint;
                    currentEndpoint.send(synMessageContext);
                    break;
                }
            }

            if (!foundEndpoint) {
                if (parentEndpoint != null) {
                    parentEndpoint.onChildEndpointFail(this, synMessageContext);
                }
            }
        }
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isActive() {
        return this.active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }   

    public ArrayList getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(ArrayList endpoints) {
        this.endpoints = endpoints;
        if (endpoints.size() > 0) {
            currentEndpoint = (Endpoint) endpoints.get(0);
        }
    }

    public void onChildEndpointFail(Endpoint endpoint, MessageContext synMessageContext) {
        endpoint.setActive(false);
        send(synMessageContext);
    }

    public void setParentEndpoint(Endpoint parentEndpoint) {
        this.parentEndpoint = parentEndpoint;
    }
}
