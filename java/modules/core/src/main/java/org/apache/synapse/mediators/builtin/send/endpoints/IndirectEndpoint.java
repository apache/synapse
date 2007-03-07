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

public class IndirectEndpoint implements Endpoint {

    private String name = null;
    private String key = null;
    private boolean active = true;
    private Endpoint parentEndpoint = null;

    public void send(MessageContext synMessageContext) {
        // get the actual endpoint and send
        Endpoint endpoint = synMessageContext.getEndpoint(key);

        if (endpoint.isActive()) {
            endpoint.send(synMessageContext);
        } else {
            parentEndpoint.onChildEndpointFail(this, synMessageContext);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
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

    public void onChildEndpointFail(Endpoint endpoint, MessageContext synMessageContext) {
        endpoint.setActive(false);
        parentEndpoint.onChildEndpointFail(this, synMessageContext);
    }
}
