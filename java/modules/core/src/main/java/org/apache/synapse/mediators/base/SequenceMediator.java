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

import org.apache.synapse.Mediator;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.AbstractListMediator;
import org.apache.synapse.mediators.MediatorFaultHandler;
import org.apache.synapse.statistics.StatisticsUtils;
import org.apache.synapse.statistics.StatisticsStack;
import org.apache.synapse.statistics.impl.SequenceStatisticsStack;

import java.util.Stack;

/**
 * The Sequence mediator either refers to a named Sequence mediator instance
 * or is a *Named* list/sequence of other (child) Mediators
 * <p/>
 * If this instance defines a sequence mediator, then the name is required, and
 * an errorHandler sequence name optional. If this instance refers to another (defined)
 * sequence mediator, the errorHandler will not have a meaning, and if an error in
 * encountered in the reffered sequence, its errorHandler would execute.
 */
public class SequenceMediator extends AbstractListMediator {

    /** The name of the this sequence */
    private String name = null;
    /** The local registry key which is used to pick a sequence definition*/
    private String key = null;
    /** The name of the error handler which is used to handle error during the mediation */
    private String errorHandler = null;
    /** is this definition dynamic */
    private boolean dynamic = false;
    /** the registry key to load this definition if dynamic */
    private String registryKey = null;

    /** To decide to whether statistics should have collected or not  */
    private int statisticsState = SynapseConstants.STATISTICS_UNSET;

    /**
     * If this mediator refers to another named Sequence, execute that. Else
     * execute the list of mediators (children) contained within this. If a referenced
     * named sequence mediator instance cannot be found at runtime, an exception is
     * thrown. This may occur due to invalid configuration of an erroneous runtime
     * change of the synapse configuration. It is the responsibility of the
     * SynapseConfiguration builder to ensure that dead references are not present.
     *
     * @param synCtx the synapse message
     * @return as per standard mediator result
     */
    public boolean mediate(MessageContext synCtx) {

        boolean statsOn = SynapseConstants.STATISTICS_ON == statisticsState;
        boolean traceOn = isTraceOn(synCtx);
        boolean traceOrDebugOn = isTraceOrDebugOn(traceOn);

        if (traceOrDebugOn) {
            traceOrDebug(traceOn, "Start : Sequence <"
                + (name == null ? (key == null ? "anonymous" : key) : name) + ">");

            if (traceOn && trace.isTraceEnabled()) {
                trace.trace("Message : " + synCtx.getEnvelope());
            }
        }

        if (key == null) {

            // The onError sequence for handling errors which may occur during the
            // mediation through this sequence
            Mediator errorHandlerMediator = null;

            // Setting Required property to collect the sequence statistics
            if (statsOn) {
                StatisticsStack sequenceStack = (StatisticsStack)
                    synCtx.getProperty(SynapseConstants.SEQUENCE_STATS);
                if (sequenceStack == null) {
                    sequenceStack = new SequenceStatisticsStack();
                    synCtx.setProperty(SynapseConstants.SEQUENCE_STATS, sequenceStack);
                }
                String seqName = (name == null ? SynapseConstants.ANONYMOUS_SEQUENCE : name);
                boolean isFault = synCtx.getEnvelope().getBody().hasFault();
                sequenceStack.put(seqName, System.currentTimeMillis(),
                        !synCtx.isResponse(), statsOn, isFault);
            }
            try {

                // push the errorHandler sequence into the current message as the fault handler
                if (errorHandler != null) {
                    errorHandlerMediator = synCtx.getSequence(errorHandler);

                    if (errorHandlerMediator != null) {
                        if (traceOrDebugOn) {
                            traceOrDebug(traceOn, "Setting the onError handler : " +
                                errorHandler + " for the sequence : " + name);
                        }
                        synCtx.pushFaultHandler(
                                new MediatorFaultHandler(errorHandlerMediator));
                    } else {
                        auditWarn("onError handler : " + errorHandler + " for sequence : " +
                            name + " cannot be found", synCtx);
                    }
                }

                boolean result = super.mediate(synCtx);

                // if we pushed an error handler, pop it from the fault stack
                // before we exit normally without an exception
                if (errorHandlerMediator != null) {
                    Stack faultStack = synCtx.getFaultStack();
                    if (faultStack != null && !faultStack.isEmpty()) {
                        Object o = faultStack.peek();

                        if (o instanceof MediatorFaultHandler &&
                            errorHandlerMediator.equals(
                                ((MediatorFaultHandler) o).getFaultMediator())) {
                            faultStack.pop();
                        }
                    }
                }

                if (traceOrDebugOn) {
                    if (traceOn && trace.isTraceEnabled()) {
                        trace.trace("Message : " + synCtx.getEnvelope());
                    }

                    traceOrDebug(traceOn,
                        "End : Sequence <" + (name == null ? "anonymous" : name) + ">");
                }

                return result;

            } finally {

                //If this sequence is finished it's task normally
                if (statsOn) {
                    StatisticsUtils.processSequenceStatistics(synCtx);
                }
                //If this sequence is a IN or OUT sequence of a proxy service
                StatisticsUtils.processProxyServiceStatistics(synCtx);
            }

        } else {

            Mediator m = synCtx.getSequence(key);
            if (m == null) {
                handleException("Sequence named " + key + " cannot be found", synCtx);

            } else {
                if (traceOrDebugOn) {
                    traceOrDebug(traceOn, "Executing sequence named " + key);
                }

                boolean result = m.mediate(synCtx);

                if (traceOrDebugOn) {
                    traceOrDebug(traceOn, "End : Sequence <" + key + ">");
                }
                return result;
            }
        }

        return false;
    }

    /**
     * To get the name of the sequence
     * @return the name of the sequence
     */
    public String getName() {
        return name;
    }

    /**
     * setting the name of the sequence
     * @param name the name of the this sequence
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * To get the key which which is used to fick the sequence definition from the local registry
     * @return  return the key which is used to fick the sequence definition from the local registry
     */
    public String getKey() {
        return key;
    }

    /**
     * To set the local registry key in order to pick the sequence definition
     * @param key the local registry key
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     *
     * @return  Returns the errorhandler sequence name
     */
    public String getErrorHandler() {
        return errorHandler;
    }

    /**
     *
     * @param errorHandler to used handle error will appear during the
     *        mediation through this sequence
     */
    public void setErrorHandler(String errorHandler) {
        this.errorHandler = errorHandler;
    }

    /**
     * To check whether statistics should have collected or not
     *
     * @return Returns the int value that indicate statistics is enabled or not.
     */
    public int getStatisticsState() {
        return statisticsState;
    }

    /**
     * To set the statistics enable variable value
     *
     * @param statisticsState  To indicate statistics collecting state
     */
    public void setStatisticsState(int statisticsState) {
        this.statisticsState = statisticsState;
    }

    /**
     * Is this a dynamic sequence?
     * @return true if dynamic
     */
    public boolean isDynamic() {
        return dynamic;
    }

    /**
     * Mark this as a dynamic sequence
     * @param dynamic true if this is a dynamic sequence
     */
    public void setDynamic(boolean dynamic) {
        this.dynamic = dynamic;
    }

    /**
     * Return the registry key used to load this sequence dynamically
     * @return  registry key
     */
    public String getRegistryKey() {
        return registryKey;
    }

    /**
     * To get the registry key used to load this sequence dynamically
     * @param registryKey  returns the registry key which point to this sequence
     */
    public void setRegistryKey(String registryKey) {
        this.registryKey = registryKey;
    }
}
