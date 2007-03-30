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

import java.util.ArrayList;

/**
 * This is the implementation of the round robin load balancing algorithm. It simply iterates through
 * the endpoint list one by one for until an active endpoint is found.
 */
public class RoundRobin implements LoadbalanceAlgorithm {

    private ArrayList endpoints = null;
    private int currentEPR = 0;

    public RoundRobin(ArrayList endpoints) {
        this.endpoints = endpoints;
    }

    /**
     * Choose an active endpoint using the round robin algorithm. If there are no active endpoints
     * available, returns null.
     *
     * @param synapseMessageContext
     * @return endpoint to send the next message
     */
    public Endpoint getNextEndpoint(MessageContext synapseMessageContext) {

        Endpoint nextEndpoint = null;
        int attempts = 0;

        do {
            // two successive clients could get the same endpoint if not synchronized.
            synchronized(this) {
                nextEndpoint = (Endpoint) endpoints.get(currentEPR);

                if(currentEPR == endpoints.size() - 1) {
                    currentEPR = 0;
                } else {
                    currentEPR++;
                }
            }

            attempts++;
            if (attempts > endpoints.size()) {
                return null;
            }

        } while (!nextEndpoint.isActive(synapseMessageContext));

        return nextEndpoint;
    }

    public void reset() {
        currentEPR = 0;
    }
}
