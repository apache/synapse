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

import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.MessageContext;

public interface Dispatcher {

    /**
     * Dispatcher should check the session id pattern in the synapseMessageContext and return the
     * matching endpoint for that session id, if availabale. If the session id in the given
     * synapseMessageContext is not found it should return null.
     *
     * @param synCtx client -> esb message context.
     * @return Endpoint Endpoint associated with this session.
     */
    public Endpoint getEndpoint(MessageContext synCtx);
    
    /**
     * Updates the session maps for client initiated sessions. This should be called in
     * client -> esb -> server flow.
     *
     * @param synCtx SynapseMessageContext
     * @param endpoint Selected endpoint for this session.
     */
    public void updateSession(MessageContext synCtx, Endpoint endpoint);

    /**
     * Determine whether the session supported by the implementing dispatcher is intiated by the
     * server (e.g. soap session) or by the client. This can be used for optimizing session updates.
     *
     * @return true, if the session is initiated by the server. false, otherwise.
     */
    public boolean isServerInitiatedSession();
}
