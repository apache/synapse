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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Constants;
import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractListMediator;

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

    private static final Log log = LogFactory.getLog(SequenceMediator.class);
    private static final Log trace = LogFactory.getLog(Constants.TRACE_LOGGER);
    private String name = null;
    private String ref = null;
    private String errorHandler = null;

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
        log.debug("Sequence mediator <" + (name == null ? "anonymous" : name) + "> :: mediate()");
        boolean shouldTrace = shouldTrace(synCtx.getTracingState());
        if (ref == null) {
            try {
                if (shouldTrace) {
                    trace.trace("Start : Sequence <" + (name == null ? "anonymous" : name) + ">");
                }
                return super.mediate(synCtx);

            } catch (SynapseException e) {

                if (errorHandler != null) {
                    if (shouldTrace) {
                        trace.trace("Sequence " + name + " encountered an exception. " +
                            "Locating error handler sequence : " + errorHandler);
                    }
                    // set exception information to message context
                    Axis2MessageContext.setErrorInformation(synCtx, e);

                    Mediator errHandler = synCtx.getConfiguration().getNamedSequence(errorHandler);
                    if (errHandler == null) {
                        if (shouldTrace) {
                            trace.trace("Sequence " + name + "; error handler sequence named '" +
                                errorHandler + "' not found");
                        }
                        handleException("Error handler sequence mediator instance named " +
                                errorHandler + " cannot be found");
                    } else {
                        if (shouldTrace) {
                            trace.trace("Sequence " + name + "; Executing error handler sequence : "
                                + errorHandler);
                        }
                        return errHandler.mediate(synCtx);
                    }

                } else {
                    if (shouldTrace) {
                        trace.trace("Sequence " + name + " encountered an exception, but does " +
                            "not specify an error handler");
                    }
                    throw e;
                }
            } finally {
                if (shouldTrace) {
                    trace.trace("End : Sequence <" + (name == null ? "anonymous" : name) + ">");
                }
            }

        } else {
            Mediator m = synCtx.getConfiguration().getNamedSequence(ref);
            if (m == null) {
                if (shouldTrace) {
                    trace.trace("Sequence named " + ref + " cannot be found.");
                }
                handleException("Sequence named " + ref + " cannot be found.");
            } else {
                if (shouldTrace) {
                    trace.trace("Executing sequence named " + ref);
                }
                return m.mediate(synCtx);
            }
        }
        return false;
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public String getErrorHandler() {
        return errorHandler;
    }

    public void setErrorHandler(String errorHandler) {
        this.errorHandler = errorHandler;
    }
}
