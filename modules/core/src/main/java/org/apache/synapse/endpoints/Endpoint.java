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

/**
 * Endpoint defines the bahavior common to all synapse endpoints. Synapse endpoints should be able
 * to send the given synapse message context, rather than just providing the information for sending
 * the message. The task a particuler endpoint does in its send(...) methis is specific to the endpoint.
 * For example a loadbalance endpoint may choose another endpoint using its loadbalance policy and
 * call its send(...) method while an address endpoint (leaf level) may send the message to a actual
 * endpoint url. Endpoints may contain zero or more endpoints in them and build up a heirachycal
 * structure of endpoints.
 */
public interface Endpoint {

    /**
     * Sends the message context according to an endpoint specific behavior.
     *
     * @param synMessageContext MessageContext to be sent.
     */
    public void send(MessageContext synMessageContext);

    /**
     * Endpoints that contain other endpoints should implement this method. It will be called if a
     * child endpoint causes an exception. Action to be taken on such failure is upto the implementation.
     * But it is good practice to first try addressing the issue. If it can't be addressed propagate the
     * exception to parent endpoint by calling parent endpoint's onChildEndpointFail(...) method.
     *
     * @param endpoint The child endpoint which caused the exception.
     * @param synMessageContext MessageContext that was used in the failed attempt.
     */
    public void onChildEndpointFail(Endpoint endpoint, MessageContext synMessageContext);

    /**
     * Sets the parent endpoint for the current endpoint.
     *
     * @param parentEndpoint parent endpoint containing this endpoint. It should handle the onChildEndpointFail(...)
     * callback.
     */
    public void setParentEndpoint(Endpoint parentEndpoint);

    /**
     * Returns the name of the endpoint.
     *
     * @return Endpoint name.
     */
    public String getName();

    /**
     * Sets the name of the endpoint. Local registry use this name as the key for storing the
     * endpoint.
     *
     * @param name Name for the endpoint.
     */
    public void setName(String name);

    /**
     * Returns if the endpoint is currently active or not. Messages should not be sent to inactive
     * endpoints.
     *
     * @param synMessageContext MessageContext for the current message. This is required for
     * IndirectEndpoints where the actual endpoint is retrieved from the MessageContext. Other
     * Endpoint implementations may ignore this parameter.
     *
     * @return true if the endpoint is in active state. false otherwise.
     */
    public boolean isActive(MessageContext synMessageContext);

    /**
     * Sets the endpoint as active or inactive. If an endpoint is detected as failed, it should be
     * set as inactive. But endpoints may be eventually set as active by the endpoint refresher to
     * avoid ignoring endpoints forever.
     *
     * @param active true if active. false otherwise.
     *
     * @param synMessageContext MessageContext for the current message. This is required for
     * IndirectEndpoints where the actual endpoint is retrieved from the MessageContext. Other
     * Endpoint implementations may ignore this parameter.
     */
    public void setActive(boolean active, MessageContext synMessageContext);
}
