/*
 * Copyright 2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.synapse.api;

import org.apache.synapse.SynapseMessage;
import org.apache.synapse.SynapseContext;

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
    public boolean mediate(SynapseContext synCtx);

    /**
     * This is used for debugging purposes and exposes the type of the current
     * mediator for logging and debugging purposes
     * @return a String representation of the mediator type
     */
    public String getType();
}