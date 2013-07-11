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

import org.apache.synapse.*;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.statistics.StatisticsReporter;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.mediators.AbstractListMediator;
import org.apache.synapse.mediators.Value;
import org.apache.synapse.mediators.MediatorFaultHandler;

import java.util.Stack;

/**
 * The Sequence mediator either refers to a named Sequence mediator instance
 * or is a *Named* list/sequence of other (child) Mediators
 * <p/>
 * If this instance defines a sequence mediator, then the name is required, and
 * an errorHandler sequence name optional. If this instance refers to another (defined)
 * sequence mediator, the errorHandler will not have a meaning, and if an error in
 * encountered in the referred sequence, its errorHandler would execute.
 */
public class SequenceMediator extends AbstractListMediator implements Nameable {

    /** The name of the this sequence */
    private String name = null;
    /** The local registry key which is used to pick a sequence definition*/
    private Value key = null;
    /** The name of the error handler which is used to handle error during the mediation */
    private String errorHandler = null;
    /** is this definition dynamic */
    private boolean dynamic = false;
    /** flag to ensure that each and every sequence is initialized and destroyed atmost once */
    private boolean initialized = false;
    /** the registry key to load this definition if dynamic */
    private String registryKey = null;
    /** The name of the file where this sequence is defined */
    private String fileName;

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

        SynapseLog synLog = getLog(synCtx);

        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("Start : Sequence "
                    + (name == null ? (key == null ? "<anonymous" : "key=<" + key) : "<"
                    + name) + ">");

            if (synLog.isTraceTraceEnabled()) {
                synLog.traceTrace("Message : " + synCtx.getEnvelope());
            }
        }

        if (key == null) {

            // The onError sequence for handling errors which may occur during the
            // mediation through this sequence
            Mediator errorHandlerMediator = null;

            // Setting Required property to reportForComponent the sequence aspects

            try {
                if (isStatisticsEnable()) {
                    StatisticsReporter.reportForComponent(synCtx,
                            getAspectConfiguration(), ComponentType.SEQUENCE);
                }

                // push the errorHandler sequence into the current message as the fault handler
                if (errorHandler != null) {
                    errorHandlerMediator = synCtx.getSequence(errorHandler);

                    if (errorHandlerMediator != null) {
                        if (synLog.isTraceOrDebugEnabled()) {
                            synLog.traceOrDebug("Setting the onError handler : " +
                                    errorHandler + " for the sequence : " + name);
                        }
                        synCtx.pushFaultHandler(
                                new MediatorFaultHandler(errorHandlerMediator));
                    } else {
                        synLog.auditWarn("onError handler : " + errorHandler + " for sequence : " +
                                name + " cannot be found");
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

                if (synLog.isTraceOrDebugEnabled()) {
                    if (synLog.isTraceTraceEnabled()) {
                        synLog.traceTrace("Message : " + synCtx.getEnvelope());
                    }

                    synLog.traceOrDebug(
                            "End : Sequence <" + (name == null ? "anonymous" : name) + ">");
                }

                return result;

            } finally {

                if (isStatisticsEnable()) {
                    boolean shouldReport = Boolean.parseBoolean(
                            String.valueOf(synCtx.getProperty(SynapseConstants.OUT_ONLY)));
                    if (!shouldReport) {
                        shouldReport = !(Boolean.parseBoolean(String.valueOf(
                                synCtx.getProperty(SynapseConstants.SENDING_REQUEST))));
                    }
                    if (shouldReport) {
                        StatisticsReporter.reportForComponent(synCtx,
                                getAspectConfiguration(), ComponentType.SEQUENCE);
                    }
                }
            }

        } else {
            String sequenceKey = key.evaluateValue(synCtx);
            //Mediator m = synCtx.getSequence(key);
            Mediator m = synCtx.getSequence(sequenceKey);
            if (m == null) {
                handleException("Sequence named " + key + " cannot be found", synCtx);

            } else {
                if (synLog.isTraceOrDebugEnabled()) {
                    synLog.traceOrDebug("Executing with key " + key);
                }

                boolean result = m.mediate(synCtx);

                if (synLog.isTraceOrDebugEnabled()) {
                    synLog.traceOrDebug("End : Sequence key=<" + key + ">");
                }
                return result;
            }
        }

        return false;
    }

    /**
     * This method will ensure that each and every sequence wil only be initialized atmost once
     * @param se - enviorenment to be initialized
     */
    @Override
    public synchronized void init(SynapseEnvironment se) {
        if (!initialized) {
            super.init(se);
            initialized = true;
        }
    }

    @Override
    public synchronized void destroy() {
        if (initialized) {
            super.destroy();
            initialized = false;
        }
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
     * To get the key which is used to pick the sequence definition from the local registry
     * @return  return the key which is used to pick the sequence definition from the local registry
     */
    public Value getKey() {
        return key;
    }

    /**
     * To set the local registry key in order to pick the sequence definition
     * @param key the local registry key
     */
    public void setKey(Value key) {
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

    public String getAuditId() {
        return getName();
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public boolean isContentAware() {
        return false;
    }
}
