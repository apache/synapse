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

package org.apache.synapse.mediators.builtin.send.algorithms;

import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.mediators.builtin.send.endpoints.Endpoint;

import java.util.ArrayList;

public class RoundRobin implements LoadbalanceAlgorithm {

    private ArrayList endpoints = null;
    private int currentEPR = 0;

    public RoundRobin(ArrayList endpoints) {
        this.endpoints = endpoints;
    }

    /**
     * Choose an active endpoint using the round robin algorithm.
     *
     * @param synapseMessageContext
     * @return endpoint to send the next message
     */
    public Endpoint getNextEndpoint(MessageContext synapseMessageContext) {

        Endpoint nextEndpoint = null;
        int attempts = 0;

        do {
            nextEndpoint = (Endpoint) endpoints.get(currentEPR);

            if(currentEPR == endpoints.size() - 1) {
                currentEPR = 0;
            } else {
                currentEPR++;
            }

            attempts++;
            if (attempts > endpoints.size()) {
                throw new SynapseException("All endpoints have failed.");
            }

        } while (!nextEndpoint.isActive());

        return nextEndpoint;
    }

    public void reset() {
        currentEPR = 0;
    }
}
