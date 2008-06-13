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

package org.apache.synapse.mediators.base;

import org.apache.synapse.SynapseConstants;
import org.apache.synapse.MessageContext;
import org.apache.synapse.statistics.StatisticsStack;
import org.apache.synapse.statistics.StatisticsUtils;
import org.apache.synapse.statistics.impl.SequenceStatisticsStack;
import org.apache.synapse.mediators.AbstractListMediator;

/**
 * The SynapseMediator is the "mainmediator" of the synapse engine. It is
 * given each message on arrival at the Synapse engine. The Synapse configuration
 * holds a reference to this special mediator instance. The SynapseMediator
 * holds the list of mediators supplied within the <rules> element of an XML
 * based Synapse configuration
 *
 * @see org.apache.synapse.config.SynapseConfiguration#getMainSequence()
 */
public class SynapseMediator extends AbstractListMediator {

    /**
     * Perform the mediation specified by the rule set
     *
     * @param synCtx the message context
     * @return as per standard mediate() semantics
     */
    public boolean mediate(MessageContext synCtx) {

        boolean traceOn = isTraceOn(synCtx);
        boolean traceOrDebugOn = isTraceOrDebugOn(traceOn);

        if (traceOrDebugOn) {
            traceOrDebug(traceOn, "Start : Mediation using '" + SynapseConstants.MAIN_SEQUENCE_KEY +
                "' sequence Message is a : " + (synCtx.isResponse() ? "response" : "request"));

            if (traceOn && trace.isTraceEnabled()) {
                trace.trace("Message : " + synCtx.getEnvelope());
            }
        }

        // If the message flow path is OUT, then process the satatistics
        if (synCtx.isResponse()) {
            StatisticsUtils.processAllSequenceStatistics(synCtx);
        }

        //put the required property for the collecttng statistics for the message mediation
        StatisticsStack sequenceStack = (StatisticsStack) synCtx.getProperty(
                SynapseConstants.SEQUENCE_STATS);
        if (sequenceStack == null) {
            sequenceStack = new SequenceStatisticsStack();
            synCtx.setProperty(SynapseConstants.SEQUENCE_STATS,sequenceStack);
        }
        String seqName = "MainSequence";
        boolean isFault = synCtx.getEnvelope().getBody().hasFault();
        sequenceStack.put(seqName,System.currentTimeMillis(),!synCtx.isResponse(),true,isFault);

        boolean result = super.mediate(synCtx);

        if (traceOrDebugOn) {
            if (traceOn && trace.isTraceEnabled()) {
                trace.trace("Message : " + synCtx.getEnvelope());
            }
            traceOrDebug(traceOn, "End : Mediation using '" +
                SynapseConstants.MAIN_SEQUENCE_KEY + "' sequence");
        }
        return result;        
    }
}
