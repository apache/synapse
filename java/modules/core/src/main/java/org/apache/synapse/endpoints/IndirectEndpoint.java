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
import org.apache.synapse.SynapseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class represents the endpoints referred by keys. It does not store the actual referred
 * endpoint as a private variable as it could expire. Therefore, it only stores the key and gets the
 * actual endpoint from the synapse configuration.
 *
 * As this is also an instance of endpoint, this can be used any place, where a normal endpoint is used.
 */
public class IndirectEndpoint implements Endpoint {

    private static final Log log = LogFactory.getLog(IndirectEndpoint.class);

    private String name = null;
    private String key = null;
    private boolean active = true;
    private Endpoint parentEndpoint = null;

    /**
     * This should have a reference to the current message context as it gets the referred endpoint
     * from it.
     */
    private MessageContext currentMsgCtx = null;

    public void send(MessageContext synMessageContext) {
        // get the actual endpoint and send
        Endpoint endpoint = synMessageContext.getEndpoint(key);
        if (endpoint == null) {
            handleException("Reference to non-existent endpoint for key : " + key);
        }

        if (endpoint.isActive(synMessageContext)) {
            endpoint.send(synMessageContext);
        } else {
            parentEndpoint.onChildEndpointFail(this, synMessageContext);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name.trim();
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    /**
     * IndirectEndpoints are active if its referref endpoint is active and vise versa. Therefore,
     * this returns if its referred endpoint is active or not.
     *
     * @param synMessageContext MessageContext of the current message.
     *
     * @return true if the referred endpoint is active. false otherwise.
     */
    public boolean isActive(MessageContext synMessageContext) {
        Endpoint endpoint = synMessageContext.getEndpoint(key);
        if (endpoint == null) {
            handleException("Reference to non-existent endpoint for key : " + key);
        }

        return endpoint.isActive(synMessageContext);
    }

    /**
     * Activating or deactivating an IndirectEndpoint is the activating or deactivating its
     * referref endpoint. Therefore, this sets the active state of its referred endpoint.
     *
     * @param active true if active. false otherwise.
     *
     * @param synMessageContext MessageContext of the current message.
     */
    public void setActive(boolean active, MessageContext synMessageContext) {
        Endpoint endpoint = synMessageContext.getEndpoint(key);
        if (endpoint == null) {
            handleException("Reference to non-existent endpoint for key : " + key);
        }

        endpoint.setActive(active, synMessageContext);
    }

    public void setParentEndpoint(Endpoint parentEndpoint) {
        this.parentEndpoint = parentEndpoint;
    }

    public void onChildEndpointFail(Endpoint endpoint, MessageContext synMessageContext) {
        parentEndpoint.onChildEndpointFail(this, synMessageContext);
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }
}
