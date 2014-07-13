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

package org.apache.synapse.endpoints.algorithms;

import org.apache.synapse.MessageContext;
import org.apache.synapse.endpoints.Endpoint;

/**
 * All load balance algorithms must implement this interface. Implementations of this interface can
 * be registered in LoadbalanceManagers.
 */
public interface LoadbalanceAlgorithm {

    /**
     * This method returns the next node according to the algorithm implementation.
     *
     * @param synapseMessageContext SynapseMessageContext of the current message
     * @return Next node for directing the message
     */
    public Endpoint getNextEndpoint(MessageContext synapseMessageContext);

    /**
     * Resets the algorithm to its initial position. Initial position depends on the implementation.
     */
    public void reset();
}
