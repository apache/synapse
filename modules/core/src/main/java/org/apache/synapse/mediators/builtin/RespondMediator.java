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

package org.apache.synapse.mediators.builtin;

import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.core.axis2.Axis2Sender;
import org.apache.synapse.mediators.AbstractMediator;

/**
 * Halts further processing/mediation of the current message and return the current message back to client
 * This Mediator reduce the number of configuration element need to build an echo service from ESB.
 */
public class RespondMediator extends AbstractMediator {

    /**
     * Halts further processing/mediation of the current message and return the current message back to client.
     *
     * @param synCtx the current message for mediation
     * @return false
     */
    public boolean mediate(MessageContext synCtx) {
        SynapseLog synapseLog = getLog(synCtx);

        boolean isTraceOrDebugEnabled = synapseLog.isTraceOrDebugEnabled();
        if (isTraceOrDebugEnabled) {
            synapseLog.traceOrDebug("Start : Respond mediator");

            if (synapseLog.isTraceTraceEnabled()) {
                synapseLog.traceOrDebug("Message : " + synCtx.getEnvelope());
            }
        }

        synCtx.setTo(null);
        synCtx.setResponse(true);
        Axis2Sender.sendBack(synCtx);

        if (isTraceOrDebugEnabled) {
            synapseLog.traceOrDebug("End : Respond Mediator");
        }
        return false;
    }

    /**
     * Since this Mediator does not touch the content of the message Content Aware property is set into False.
     *
     * @return false
     */
    @Override
    public boolean isContentAware() {
        return false;
    }
}
