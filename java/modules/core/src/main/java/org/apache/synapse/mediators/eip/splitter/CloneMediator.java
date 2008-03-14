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

package org.apache.synapse.mediators.eip.splitter;

import org.apache.synapse.MessageContext;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.util.MessageHelper;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.mediators.eip.Target;
import org.apache.synapse.mediators.eip.EIPConstants;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.context.OperationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

/**
 * This mediator will clone the message into multiple messages and mediate as specified in the
 * target elements. A target specifies or refers to a sequence or an endpoint, and optionally
 * specifies an Action and/or To address to be set to the cloned message. The number of cloned
 * messages created is the number of targets specified
 */
public class CloneMediator extends AbstractMediator implements ManagedLifecycle {

    /**
     * Continue processing the parent message or not?
     * (i.e. message which is subjected to cloning)
     */
    private boolean continueParent = false;

    /** the list of targets to which cloned copies of the message will be given for mediation */
    private List<Target> targets = new ArrayList<Target>();

    /**
     * This will implement the mediate method of the Mediator interface and will provide the
     * functionality of cloning message into the specified targets and mediation
     *
     * @param synCtx - MessageContext which is subjected to the cloning
     * @return boolean true if this needs to be further mediated (continueParent=true)
     */
    public boolean mediate(MessageContext synCtx) {

        boolean traceOn = isTraceOn(synCtx);
        boolean traceOrDebugOn = isTraceOrDebugOn(traceOn);

        if (traceOrDebugOn) {
            traceOrDebug(traceOn, "Start : Clone mediator");

            if (traceOn && trace.isTraceEnabled()) {
                trace.trace("Message : " + synCtx.getEnvelope());
            }
        }

        // get the targets list, clone the message for the number of targets and then
        // mediate the cloned messages using the targets
        Iterator<Target> iter = targets.iterator();
        int i = 0;
        while (iter.hasNext()) {
            if (traceOrDebugOn) {
                traceOrDebug(traceOn, "Submitting " + (i+1) + " of " + targets.size() +
                    " messages for processing in parallel");
            }

            iter.next().mediate(getClonedMessageContext(synCtx, i++, targets.size()));
        }

        // if the continuation of the parent message is stopped from here set the RESPONSE_WRITTEN
        // property to SKIP to skip the blank http response 
        OperationContext opCtx
            = ((Axis2MessageContext) synCtx).getAxis2MessageContext().getOperationContext();
        if (!continueParent && opCtx != null) {
            opCtx.setProperty(Constants.RESPONSE_WRITTEN, "SKIP");
        }

        // finalize tracing and debugging
        if (traceOrDebugOn) {
            traceOrDebug(traceOn, "End : Clone mediator");
        }

        // if continue parent is true mediators after the clone will be called for the further
        // mediation of the message which is subjected for clonning (parent message)
        return continueParent;
    }

    /**
     * clone the provided message context as a new message, and mark as the messageSequence'th
     * message context of a total of messageCount messages
     *
     * @param synCtx          - MessageContext which is subjected to the cloning
     * @param messageSequence - the position of this message of the cloned set
     * @param messageCount    - total of cloned copies
     * @return MessageContext the cloned message context
     */
    private MessageContext getClonedMessageContext(MessageContext synCtx, int messageSequence,
        int messageCount) {

        MessageContext newCtx = null;
        try {
            newCtx = MessageHelper.cloneMessageContext(synCtx);

            // set the property MESSAGE_SEQUENCE to the MC for aggregation purposes
            newCtx.setProperty(EIPConstants.MESSAGE_SEQUENCE,
                String.valueOf(messageSequence) + EIPConstants.MESSAGE_SEQUENCE_DELEMITER +
                messageCount);            
        } catch (AxisFault axisFault) {
            handleException("Error cloning the message context", axisFault, synCtx);
        }

        return newCtx;
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    //                        Getters and Setters                                        //
    ///////////////////////////////////////////////////////////////////////////////////////

    public boolean isContinueParent() {
        return continueParent;
    }

    public void setContinueParent(boolean continueParent) {
        this.continueParent = continueParent;
    }

    public List<Target> getTargets() {
        return targets;
    }

    public void setTargets(List<Target> targets) {
        this.targets = targets;
    }

    public void addTarget(Target target) {
        this.targets.add(target);
    }

    public void init(SynapseEnvironment se) {

        for (Target target : targets) {
            SequenceMediator seq = target.getSequence();
            if (seq != null) {
                seq.init(se);
            }
        }
    }

    public void destroy() {
        
        for (Target target : targets) {
            SequenceMediator seq = target.getSequence();
            if (seq != null) {
                seq.destroy();
            }
        }
    }

}
