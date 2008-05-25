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

package org.apache.synapse.endpoints;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.FaultHandler;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.endpoints.utils.EndpointDefinition;
import org.apache.synapse.statistics.impl.EndPointStatisticsStack;

import java.util.Stack;

/**
 * This class represents an endpoint with epr as addressing to header of the message. It is
 * responsible for sending the message to the epr specified in the message To header, performing
 * retries if a failure occurred and informing the parent endpoint if a failure couldn't be
 * recovered.
 */
public class DefaultEndpoint extends FaultHandler implements Endpoint {

    protected Log log;
    
    protected static final Log trace = LogFactory.getLog(SynapseConstants.TRACE_LOGGER);

    /**
     * Name of the endpoint. Used for named endpoints which can be referred using the key attribute
     * of indirect endpoints.
     */
    private String name = null;

    /**
     * Stores the endpoint details for this endpoint. Details include EPR, WS-Addressing
     * information, WS-Security information, etc.
     */
    private EndpointDefinition endpoint = null;

    /**
     * Parent endpoint of this endpoint if this used inside another endpoint. Possible parents are
     * LoadbalanceEndpoint, SALoadbalanceEndpoint and FailoverEndpoint objects.
     */
    private Endpoint parentEndpoint = null;

    public DefaultEndpoint() {
        log = LogFactory.getLog(this.getClass());
    }

    public EndpointDefinition getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(EndpointDefinition endpoint) {
        this.endpoint = endpoint;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name.trim();
    }

    /**
     * This will always return true, because the endpoint epr is dependent on the message being
     * processed
     *
     * @param synMessageContext not being used
     * @return true
     */
    public boolean isActive(MessageContext synMessageContext) {
        return true;
    }

    /**
     * since this is a virtual representation of an endpoint and the epr changes from message
     * to message setting active state doesn't have a meaning
     *
     * @param active            not being used
     * @param synMessageContext not being used
     */
    public synchronized void setActive(boolean active, MessageContext synMessageContext) {
        // no implementation according to the behavior
    }

    /**
     * Sends the message through this endpoint. This method just handles statistics related
     * functions and gives the message to the Synapse environment to send. It does not add any
     * endpoint specific details to the message context. These details are added only to the cloned
     * message context by the Axis2FlexibleMepClient. So that we can reuse the original message
     * context for resending through different endpoints.
     *
     * @param synCtx MessageContext sent by client to Synapse
     */
    public void send(MessageContext synCtx) {

        boolean traceOn = isTraceOn(synCtx);
        boolean traceOrDebugOn = isTraceOrDebugOn(traceOn);

        if (traceOrDebugOn) {
            traceOrDebug(traceOn, "Start : Default Endpoint");

            if (traceOn && trace.isTraceEnabled()) {
                trace.trace("Message : " + synCtx.getEnvelope());
            }
        }

        String endPointName = this.getName();
        if (endPointName == null) {
            endPointName = SynapseConstants.ANONYMOUS_ENDPOINT;
        }

        // Setting Required property to collect the End Point statistics
        boolean statisticsEnable
                = (SynapseConstants.STATISTICS_ON == endpoint.getStatisticsState());
        if (statisticsEnable) {
            EndPointStatisticsStack endPointStatisticsStack = null;
            Object statisticsStackObj =
                    synCtx.getProperty(org.apache.synapse.SynapseConstants.ENDPOINT_STATS);
            if (statisticsStackObj == null) {
                endPointStatisticsStack = new EndPointStatisticsStack();
                synCtx.setProperty(org.apache.synapse.SynapseConstants.ENDPOINT_STATS,
                        endPointStatisticsStack);
            } else if (statisticsStackObj instanceof EndPointStatisticsStack) {
                endPointStatisticsStack = (EndPointStatisticsStack) statisticsStackObj;
            }
            if (endPointStatisticsStack != null) {
                boolean isFault = synCtx.getEnvelope().getBody().hasFault();
                endPointStatisticsStack.put(endPointName, System.currentTimeMillis(),
                        !synCtx.isResponse(), statisticsEnable, isFault);
            }
        }

        if (synCtx.getTo() != null && synCtx.getTo().getAddress() != null) {
            if (traceOrDebugOn) {
                traceOrDebug(traceOn, "Sending message to endpoint : " +
                        endPointName + " resolves to address = " + synCtx.getTo().getAddress());
                traceOrDebug(traceOn, "SOAPAction: " + (synCtx.getSoapAction() != null ?
                        synCtx.getSoapAction() : "null"));
                traceOrDebug(traceOn, "WSA-Action: " + (synCtx.getWSAAction() != null ?
                        synCtx.getWSAAction() : "null"));

                if (traceOn && trace.isTraceEnabled()) {
                    trace.trace("Envelope : \n" + synCtx.getEnvelope());
                }
            }
        }

        // register this as the immediate fault handler for this message.
        synCtx.pushFaultHandler(this);

        // add this as the last endpoint to process this message. it is used by statistics code.
        synCtx.setProperty(SynapseConstants.PROCESSED_ENDPOINT, this);

        synCtx.getEnvironment().send(endpoint, synCtx);
    }

    public void onChildEndpointFail(Endpoint endpoint, MessageContext synMessageContext) {
        // nothing to do as this is a leaf level endpoint
    }

    public void setParentEndpoint(Endpoint parentEndpoint) {
        this.parentEndpoint = parentEndpoint;
    }

    public void onFault(MessageContext synCtx) {
        // perform retries here

        if (parentEndpoint != null) {
            parentEndpoint.onChildEndpointFail(this, synCtx);
        } else {
            Stack faultStack = synCtx.getFaultStack();
            if (!faultStack.isEmpty()) {
                ((FaultHandler) faultStack.pop()).handleFault(synCtx);
            }
        }
    }

    /**
     * Should this mediator perform tracing? True if its explicitly asked to
     * trace, or its parent has been asked to trace and it does not reject it
     *
     * @param msgCtx the current message
     * @return true if tracing should be performed
     */
    protected boolean isTraceOn(MessageContext msgCtx) {
        return (endpoint.getTraceState() == SynapseConstants.TRACING_ON) ||
                (endpoint.getTraceState() == SynapseConstants.TRACING_UNSET &&
                        msgCtx.getTracingState() == SynapseConstants.TRACING_ON);
    }

    /**
     * Is tracing or debug logging on?
     *
     * @param isTraceOn is tracing known to be on?
     * @return true, if either tracing or debug logging is on
     */
    protected boolean isTraceOrDebugOn(boolean isTraceOn) {
        return isTraceOn || log.isDebugEnabled();
    }

    /**
     * Perform Trace and Debug logging of a message @INFO (trace) and DEBUG (log)
     *
     * @param traceOn is runtime trace on for this message?
     * @param msg     the message to log/trace
     */
    protected void traceOrDebug(boolean traceOn, String msg) {
        if (traceOn) {
            trace.info(msg);
        }
        if (log.isDebugEnabled()) {
            log.debug(msg);
        }
    }
}
