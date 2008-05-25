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

package org.apache.synapse.endpoints.dispatch;

import org.apache.synapse.MessageContext;
import org.apache.synapse.endpoints.Endpoint;

/**
 * Defines the behavior of session dispatchers. There can be two dispatcher types. Server initiated
 * session dispatchers and client initiated session dispatchers. In the former one, server generates
 * the session ID and sends it to the client in the first RESPONSE. In the later case, client should
 * generate the session ID and send it to the server in the first REQUEST. A dispatcher object will
 * be created for each session affinity load balance endpoint.
 */
public interface Dispatcher {

    /**
     * Dispatcher should check the session id pattern in the synapseMessageContext and return the
     * matching endpoint for that session id, if available. If the session id in the given
     * synapseMessageContext is not found it should return null.
     *
     * @param synCtx client -> esb message context.
     * @param dispatcherContext context for dispatching
     * @return Endpoint Endpoint associated with this session.
     */
    public Endpoint getEndpoint(MessageContext synCtx, DispatcherContext dispatcherContext);

    /**
     * Updates the session maps. This will be called in the first client -> synapse -> server flow
     * for client initiated sessions. For server initiated sessions, this will be called in the
     * first server -> synapse -> client flow.
     *
     * @param synCtx   SynapseMessageContext
     * @param dispatcherContext context for dispatching
     * @param endpoint Selected endpoint for this session.
     */
    public void updateSession(MessageContext synCtx, DispatcherContext dispatcherContext,
        Endpoint endpoint);

    /**
     * Removes the session belonging to the given message context.
     *
     * @param synCtx MessageContext containing an session ID.
     * @param dispatcherContext context for dispatching
     */
    public void unbind(MessageContext synCtx, DispatcherContext dispatcherContext);

    /**
     * Determine whether the session supported by the implementing dispatcher is initiated by the
     * server (e.g. soap session) or by the client. This can be used for optimizing session updates.
     *
     * @return true, if the session is initiated by the server. false, otherwise.
     */
    public boolean isServerInitiatedSession();
}
