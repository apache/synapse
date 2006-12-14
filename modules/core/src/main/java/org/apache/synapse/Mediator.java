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

package org.apache.synapse;

import org.apache.synapse.MessageContext;

/**
 * All Synapse mediators must implement this Mediator interface. As a message passes
 * through the synapse system, each mediator's mediate() method is invoked in the
 * sequence/order defined in the SynapseConfiguration.
 */
public interface Mediator {

    /**
     * Invokes the mediator passing the current message for mediation. Each
     * mediator performs its mediation action, and returns true if mediation
     * should continue, or false if further mediation should be aborted.
     *
     * @param synCtx the current message for mediation
     * @return true if further mediation should continue
     */
    public boolean mediate(MessageContext synCtx);

    /**
     * This is used for debugging purposes and exposes the type of the current
     * mediator for logging and debugging purposes
     * @return a String representation of the mediator type
     */
    public String getType();

    /**
     * This is used to check whether the tracing should be enabled on the current mediator or not
     * @return value that indicate whether tracing is on, off or unset
     */
    public int getTraceState();

    /**
     * This is used to set the value of tracing enable variable
     * @param traceState Set whether the tracing is enabled or not
     */
    public void setTraceState(int traceState);
}
