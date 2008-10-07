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
import org.apache.axis2.context.ConfigurationContext;

import java.util.List;

/**
 * Endpoint defines the behavior common to all Synapse endpoints. Synapse endpoints should be able
 * to send the given Synapse message context, rather than just providing the information for sending
 * the message. The task a particular endpoint does in its send(...) method is specific to the endpoint.
 * For example a loadbalance endpoint may choose another endpoint using its load balance policy and
 * call its send(...) method while an address endpoint (leaf level) may send the message to an actual
 * endpoint url. Endpoints may contain zero or more endpoints in them and build up a hierarchical
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
     * child endpoint causes an exception. Action to be taken on such failure is up to the implementation.
     * But it is good practice to first try addressing the issue. If it can't be addressed propagate the
     * exception to parent endpoint by calling parent endpoint's onChildEndpointFail(...) method.
     *
     * @param endpoint          The child endpoint which caused the exception.
     * @param synMessageContext MessageContext that was used in the failed attempt.
     */
    public void onChildEndpointFail(Endpoint endpoint, MessageContext synMessageContext);

    /**
     * Sets the parent endpoint for the current endpoint.
     *
     * @param parentEndpoint parent endpoint containing this endpoint. It should handle the onChildEndpointFail(...)
     *                       callback.
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
     * An event notification whenever endpoint invocation is successful
     * Can be used to clear a timeout status etc
     */
    public void onSuccess();

    /**
     * Returns true to indicate that the endpoint is ready to service requests
     * @return true if endpoint is ready to service requests
     */
    public boolean readyToSend();

    /**
     * Initialize the endpoint, using this configuration context
     * @param cc the axis2 configuration context
     */
    public void init(ConfigurationContext cc);

    /**
     * Has this Endpoint initialized?
     * @return true if the endpoint is initialized
     */
    public boolean isInitialized();

    /**
     * Get the EndpointContext that has the run-time state of this endpoint
     * @return the runtime context
     */
    public EndpointContext getContext();

    /**
     * Get the children of this endpoint
     * @return the child endpoints
     */
    public List<Endpoint> getChildren();

    /**
     * Get a reference to the metrics MBean for this endpoint
     * @return
     */
    public EndpointView getMetricsMBean();
}
