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

package org.apache.synapse.mediators;

import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.SynapseEnvironment;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This is the base class for all List mediators
 *
 * @see ListMediator
 */
public abstract class AbstractListMediator extends AbstractMediator
    implements ListMediator, ManagedLifecycle {

    /** the list of child mediators held. These are executed sequentially */
    protected List mediators = new ArrayList();

    public boolean mediate(MessageContext synCtx) {

        int parentsEffectiveTraceState = synCtx.getTracingState();
        // if I have been explicitly asked to enable or disable tracing, set it to the message
        // to pass it on; else, do nothing -> i.e. let the parents state flow
        setEffectiveTraceState(synCtx);
        int myEffectiveTraceState = synCtx.getTracingState();

        try {
            if (isTraceOrDebugOn(isTraceOn(synCtx))) {
                traceOrDebug(isTraceOn(synCtx), "Sequence <" + getType() + "> :: mediate()");
            }

            for (Iterator it = mediators.iterator(); it.hasNext();) {
                Mediator m = (Mediator) it.next();

                // ensure correct trace state after each invocation of a mediator
                synCtx.setTracingState(myEffectiveTraceState);
                if (!m.mediate(synCtx)) {
                    return false;
                }
            }
        } finally {
            synCtx.setTracingState(parentsEffectiveTraceState);
        }
        return true;
    }

    public List getList() {
        return mediators;
    }

    public boolean addChild(Mediator m) {
        return mediators.add(m);
    }

    public boolean addAll(List c) {
        return mediators.addAll(c);
    }

    public Mediator getChild(int pos) {
        return (Mediator) mediators.get(pos);
    }

    public boolean removeChild(Mediator m) {
        return mediators.remove(m);
    }

    public Mediator removeChild(int pos) {
        return (Mediator) mediators.remove(pos);
    }

    /**
     * Initialize child mediators recursively
     * @param se synapse environment
     */
    public void init(SynapseEnvironment se) {
        if (log.isDebugEnabled()) {
            log.debug("Initializing child mediators");
        }

        for (Iterator it = mediators.iterator(); it.hasNext();) {
            Mediator m = (Mediator) it.next();

            if (m instanceof ManagedLifecycle) {
            	((ManagedLifecycle) m).init(se);
            }
        } 
    }

    /**
     * Destroy child mediators recursively
     */
    public void destroy() {
        if (log.isDebugEnabled()) {
            log.debug("Destroying child mediators");
        }

        for (Iterator it = mediators.iterator(); it.hasNext();) {
            Mediator m = (Mediator) it.next();

            if (m instanceof ManagedLifecycle) {
            	((ManagedLifecycle) m).destroy();
            }
        } 
    }
}
