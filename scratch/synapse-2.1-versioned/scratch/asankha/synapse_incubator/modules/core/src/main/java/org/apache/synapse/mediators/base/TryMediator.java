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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This is a ListMediator which is similar to a Java try-catch-finally but with a catch-all
 * <p/>
 * If any of the child mediators throws an exception during execution, this mediator
 * invokes the specified error handler sequence
 */
public class TryMediator extends AbstractListMediator {

    private static final Log log = LogFactory.getLog(TryMediator.class);
    private static final Log trace = LogFactory.getLog(Constants.TRACE_LOGGER);

    private List errorHandlerMediators = new ArrayList();

    private List finallyMediators = new ArrayList();

    public boolean mediate(MessageContext synCtx) {
        log.debug("Try mediator :: mediate()");
        boolean shouldTrace = shouldTrace(synCtx.getTracingState());
        try {
            if (shouldTrace) {
                trace.trace("Start : Try Mediator");
            }
            return super.mediate(synCtx);

        } catch (SynapseException e) {
            try {
                if (shouldTrace) {
                    trace.trace("Encountered an exception, executing the 'onError' mediators");
                }
                // set exception information to message context
                Axis2MessageContext.setErrorInformation(synCtx, e);
                saveAndSetTraceState(synCtx);
                Iterator it = errorHandlerMediators.iterator();
                while (it.hasNext()) {
                    Mediator m = (Mediator) it.next();
                    if (!m.mediate(synCtx)) {
                        return false;
                    }
                }
            } finally {
                restoreTracingState(synCtx);
                if (shouldTrace) {
                    trace.trace("End executing 'onError' mediators");
                }
            }
        } finally {
            try {
                if (shouldTrace) {
                    trace.trace("Encountered an exception, executing the 'finally' mediators");
                }
                saveAndSetTraceState(synCtx);
                Iterator it = finallyMediators.iterator();
                while (it.hasNext()) {
                    Mediator m = (Mediator) it.next();
                    if (!m.mediate(synCtx)) {
                        return false;
                    }
                }
            } finally {
                restoreTracingState(synCtx);
                if (shouldTrace) {
                    trace.trace("End executing 'finally' mediators");
                }
            }
            if (shouldTrace) {
                trace.trace("End : Try mediator");
            }
        }
        return true;
    }

    public List getErrorHandlerMediators() {
        return errorHandlerMediators;
    }

    public void setErrorHandlerMediators(List errorHandlerMediators) {
        this.errorHandlerMediators = errorHandlerMediators;
    }

    public List getFinallyMediators() {
        return finallyMediators;
    }

    public void setFinallyMediators(List finallyMediators) {
        this.finallyMediators = finallyMediators;
    }
}
