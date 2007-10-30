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

package org.apache.synapse.mediators.eip.aggregator;

import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.mediators.eip.EIPUtils;
import org.apache.synapse.mediators.eip.EIPConstants;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.jaxen.JaxenException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This mediator will aggregate the messages flowing in to this with the specified message types
 * and build a one message
 */
public class AggregateMediator extends AbstractMediator {

    private static final Log log = LogFactory.getLog(AggregateMediator.class);

    private static final Log trace = LogFactory.getLog(SynapseConstants.TRACE_LOGGER);

    /**
     * This will hold the maximum lifetime of an aggregate and if a particular aggregate does not
     * completed before its life time it will be invalidated and taken off from the activeAggregates
     * map and put in to the expiredAggregates map and the invalidate sequence will be called to
     * mediate the messages in the expired aggregate if there are any
     */
    private long timeToInvalidate = 0;

    /**
     * Messages comming to the aggregator will be examined for the existance of a node described
     * in this XPATH and if it contains the XPATH pick that, if not try to find the messageSequence
     * property for the corelation and if not pass the message through
     */
    private AXIOMXPath corelateExpression = null;

    /**
     * This will be used in the complete condition to complete the aggregation after waiting a
     * specified timeout and send the messages gatherd in the aggregate after aggregation
     * if there are any messages
     */
    private long completeTimeout = 0;

    /**
     * Minimum number of messages required to evaluate the complete condition to true unless the
     * aggregate has timedout with the provided timeout if there is a one
     */
    private int minMessagesToComplete = -1;

    /**
     * Maximum number of messages that can be contained in a particular aggregation
     */
    private int maxMessagesToComplete = -1;

    /**
     * This will hold the implementation of the aggregation algorithm and upon validating the
     * complete condition getAggregatedMessage method of the aggregator will be called to get
     * the aggregated message
     */
    private AXIOMXPath aggregationExpression = null;

    /**
     * Holds a String reference to the Named Sequence which will be called to mediate the invalid
     * messages coming in to the aggregator
     */
    private String invalidMsgSequenceRef = null;

    /**
     * Sequece which will be called to mediate the invalid messages comming in to aggregator
     */
    private SequenceMediator invalidMsgSequence = null;

    /**
     * This will be used to destroy the aggreagtes which were kept in the expiredAggregates map
     */
    private long invlidateToDestroyTime = 0;

    /**
     * This holds the reference sequence name of the
     */
    private String onCompleteSequenceRef = null;

    /**
     *
     */
    private SequenceMediator onCompleteSequence = null;

    /**
     * This will hold the map of active aggragates at any given time
     */
    private Map activeAggregates = new HashMap();

    /**
     * This will hold the expired aggregates at any given time, these will be cleaned by a timer
     * task time to time in order to ensure uncontroled growth
     */
    private Map expiredAggregates = new HashMap();

    private boolean isTimerSet = false;

    public AggregateMediator() {
        try {
            aggregationExpression = new AXIOMXPath("s11:Body/child::*[position()=1] | " +
                "s12:Body/child::*[position()=1]");
            aggregationExpression.addNamespace("s11", SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI);
            aggregationExpression.addNamespace("s12", SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI);
        } catch (JaxenException e) {
            if (log.isDebugEnabled()) {
                handleException("Unable to set the default " +
                    "aggregationExpression for the aggregation", e, null);
            }
        }
    }

    /**
     * This is the mediate method implementation of the AggregateMediator. And this will aggregate
     * the messages going through this mediator according to the corelation criteria and the
     * aggregation algorith specified to it
     *
     * @param synCtx - MessageContext to be mediated and aggregated
     * @return boolean true if the complete condition for the particular aggregate is validated
     *         false if not
     */
    public boolean mediate(MessageContext synCtx) {
        // tracing and debuggin related mediation initiation
        boolean traceOn = isTraceOn(synCtx);
        boolean traceOrDebugOn = isTraceOrDebugOn(traceOn);

        if (traceOrDebugOn) {
            traceOrDebug(traceOn, "Start : Aggregate mediator");

            if (traceOn && trace.isTraceEnabled()) {
                trace.trace("Message : " + synCtx.getEnvelope());
            }
        }

//        todo: revisit this         
//        if (!isTimerSet) {
//            synCtx.getConfiguration().getSynapseTimer()
//                    .schedule(new AggregateCollector(this), 5000);
//        }

        try {
            Aggregate aggregate = null;

            // if the corelate aggregationExpression is provided and there is a coresponding
            // element in the message corelate the messages on that
            if (this.corelateExpression != null
                    && this.corelateExpression.evaluate(synCtx.getEnvelope()) != null) {

                if (activeAggregates.containsKey(this.corelateExpression.toString())) {
                    Object o = activeAggregates.get(this.corelateExpression.toString());
                    if (o instanceof Aggregate) {
                        aggregate = (Aggregate) o;
                    } else {
                        handleException("Undefined aggregate type.", synCtx);
                    }
                } else {
                    aggregate = new Aggregate(this.corelateExpression.toString(),
                            this.completeTimeout, this.minMessagesToComplete,
                            this.maxMessagesToComplete);
                    activeAggregates.put(this.corelateExpression.toString(), aggregate);
                }

            // if the corelattion can not be found using the aggregationExpression try to find the
            // corelation on the default criteria which is through the aggregate corelation
            // property of the message
            } else if (synCtx.getProperty(EIPConstants.AGGREGATE_CORELATION) != null) {

                String corelation = synCtx.getProperty(
                    EIPConstants.AGGREGATE_CORELATION) instanceof String ? synCtx.getProperty(
                    EIPConstants.AGGREGATE_CORELATION).toString() : null;

                // check whether the message corelation name is in the expired aggregates
                if (expiredAggregates.containsKey(corelation)) {

                    if (traceOrDebugOn) {
                        traceOrDebug(traceOn, "Message with the corelation "
                                + corelation + " expired. Invalidating the message.");
                    }

                    invalidate(synCtx, traceOrDebugOn, traceOn);
                    return false;
                }

                if (corelation != null) {

                    if (activeAggregates.containsKey(corelation)) {

                        Object o = activeAggregates.get(corelation);
                        if (o instanceof Aggregate) {
                            aggregate = (Aggregate) o;
                        } else {
                            handleException("Undefined aggregate type.", synCtx);
                        }

                    } else {
                        aggregate = new Aggregate(corelation, this.completeTimeout,
                                this.minMessagesToComplete, this.maxMessagesToComplete);
                        activeAggregates.put(corelation, aggregate);
                    }

                } else {
                    if (traceOrDebugOn) {
                        traceOrDebug(traceOn,
                            "Error in getting corelation details. Skip the aggregator.");
                    }
                    return true;
                }
            } else {
                if (traceOrDebugOn) {
                    traceOrDebug(traceOn,
                        "Unable to find the aggregation corelation. Skip the aggregation");
                }
                return true;
            }

            // if there is an aggregate continue on aggregation
            if (aggregate != null) {

                // add the message to the aggregate and if the maximum count of the aggregate is
                // exceeded invalidate the message
                if (!aggregate.addMessage(synCtx)) {
                    if (traceOrDebugOn) {
                        traceOrDebug(traceOn, "Can not exceed aggregate " +
                                "max message count. Invalidating message");
                    }
                    invalidate(synCtx, traceOrDebugOn, traceOn);
                    return false;
                }

                // check the completeness of the aggregate and is completed aggregate the messages
                // if not completed return false and block the message sequence till it completes
                if (aggregate.isComplete()) {
                    return completeAggregate(aggregate);
                }

            // if the aggregation corelation can not be found then continue the message on the
            // normal path by returning true
            } else {
                if (traceOrDebugOn) {
                    traceOrDebug(traceOn, "Unable to find the aggregate. Skip the aggregation");
                }
                return true;
            }

        } catch (JaxenException e) {
            handleException("Unable to execute the XPATH over the message", e, synCtx);
        }

        // finalize tracing and debugging
        if (traceOrDebugOn) {
            traceOrDebug(traceOn, "End : Aggregate mediator");
        }

        return false;
    }

    private void invalidate(MessageContext synCtx, boolean traceOrDebugOn, boolean traceOn) {

        if (this.invalidMsgSequenceRef != null && synCtx.getConfiguration()
                .getSequence(invalidMsgSequenceRef) != null) {

            // use the sequence reference to get the sequence for mediation
            synCtx.getConfiguration().getSequence(invalidMsgSequenceRef).mediate(synCtx);

        } else if (this.invalidMsgSequence != null) {

            // use the sequence to mediate the invalidated messages
            invalidMsgSequence.mediate(synCtx);

        } else {
            if (traceOrDebugOn) {
                traceOrDebug(traceOn, "No invalid message sequence defined. Dropping the message");
            }
        }
    }

    public boolean completeAggregate(Aggregate aggregate) {

            MessageContext newSynCtx = getAggregatedMessage(aggregate);
            activeAggregates.remove(aggregate.getCorelation());

            if ((this.corelateExpression != null && !this.corelateExpression
                    .toString().equals(aggregate.getCorelation())) ||
                    this.corelateExpression == null) {

//                            aggregate.setExpireTime(
//                                    System.currentTimeMillis() + this.invlidateToDestroyTime);
                expiredAggregates.put(aggregate.getCorelation(),
                        new Long(System.currentTimeMillis() + this.invlidateToDestroyTime));

                if (this.onCompleteSequence != null) {
                    this.onCompleteSequence.mediate(newSynCtx);
                } else if (this.onCompleteSequenceRef != null
                        && newSynCtx.getSequence(this.onCompleteSequenceRef) != null) {
                    newSynCtx.getSequence(this.onCompleteSequenceRef).mediate(newSynCtx);
                } else {
                    handleException("Unable to find the sequence for the mediation " +
                            "of the aggregated message", newSynCtx);
                }
                return false;
            } else {
                return true;
            }
    }

    public MessageContext getAggregatedMessage(Aggregate aggregate) {
        MessageContext newCtx = null;
        Iterator itr = aggregate.getMessages().iterator();
        while (itr.hasNext()) {
            Object o = itr.next();
            if (o instanceof MessageContext) {
                MessageContext synCtx = (MessageContext) o;
                if (newCtx == null) {
                    newCtx = synCtx;
                } else {
                    try {
                        EIPUtils.enrichEnvelope(
                            newCtx.getEnvelope(), synCtx.getEnvelope(), this.aggregationExpression);
                    } catch (JaxenException e) {
                        handleException("Unable to get the aggreagated message", e, synCtx);
                    }
                }
            }
        }
        return newCtx;
    }

    public AXIOMXPath getCorelateExpression() {
        return corelateExpression;
    }

    public void setCorelateExpression(AXIOMXPath corelateExpression) {
        this.corelateExpression = corelateExpression;
    }

    public String getInvalidMsgSequenceRef() {
        return invalidMsgSequenceRef;
    }

    public void setInvalidMsgSequenceRef(String invalidMsgSequenceRef) {
        this.invalidMsgSequenceRef = invalidMsgSequenceRef;
    }

    public SequenceMediator getInvalidMsgSequence() {
        return invalidMsgSequence;
    }

    public void setInvalidMsgSequence(SequenceMediator invalidMsgSequence) {
        this.invalidMsgSequence = invalidMsgSequence;
    }

    public long getTimeToInvalidate() {
        return timeToInvalidate;
    }

    public void setTimeToInvalidate(long timeToInvalidate) {
        this.timeToInvalidate = timeToInvalidate;
    }

    public long getCompleteTimeout() {
        return completeTimeout;
    }

    public void setCompleteTimeout(long completeTimeout) {
        this.completeTimeout = completeTimeout;
    }

    public int getMinMessagesToComplete() {
        return minMessagesToComplete;
    }

    public void setMinMessagesToComplete(int minMessagesToComplete) {
        this.minMessagesToComplete = minMessagesToComplete;
    }

    public int getMaxMessagesToComplete() {
        return maxMessagesToComplete;
    }

    public void setMaxMessagesToComplete(int maxMessagesToComplete) {
        this.maxMessagesToComplete = maxMessagesToComplete;
    }

    public AXIOMXPath getAggregationExpression() {
        return aggregationExpression;
    }

    public void setAggregationExpression(AXIOMXPath aggregationExpression) {
        this.aggregationExpression = aggregationExpression;
    }

    public long getInvlidateToDestroyTime() {
        return invlidateToDestroyTime;
    }

    public void setInvlidateToDestroyTime(long invlidateToDestroyTime) {
        this.invlidateToDestroyTime = invlidateToDestroyTime;
    }

    public String getOnCompleteSequenceRef() {
        return onCompleteSequenceRef;
    }

    public void setOnCompleteSequenceRef(String onCompleteSequenceRef) {
        this.onCompleteSequenceRef = onCompleteSequenceRef;
    }

    public SequenceMediator getOnCompleteSequence() {
        return onCompleteSequence;
    }

    public void setOnCompleteSequence(SequenceMediator onCompleteSequence) {
        this.onCompleteSequence = onCompleteSequence;
    }

    public Map getExpiredAggregates() {
        return expiredAggregates;
    }

    public Map getActiveAggregates() {
        return activeAggregates;
    }
}
