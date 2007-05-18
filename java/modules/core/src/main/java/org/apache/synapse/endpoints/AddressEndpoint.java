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

import org.apache.synapse.MessageContext;
import org.apache.synapse.Constants;
import org.apache.synapse.FaultHandler;
import org.apache.synapse.endpoints.utils.EndpointDefinition;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.statistics.impl.EndPointStatisticsStack;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Stack;

/**
 * This class represents an actual endpoint to send the message. It is resposible for sending the
 * message, performing reries if a failure occured and informing the parent endpoint if a failure
 * couldn't be recovered.
 */
public class AddressEndpoint extends FaultHandler implements Endpoint {

    private static final Log log = LogFactory.getLog(AddressEndpoint.class);

    /**
     * Name of the endpoint. Used for named endpoints which can be referred using the key attribute
     * of indirect endpoints.
     */
    private String name = null;

    /**
     * Determines if this endpoint is active or not. This variable have to be loaded always from the
     * memory as multiple threads could access it.
     */
    private volatile boolean active = true;

    /**
     * Stores the endpoint details for this endpoint. Details include EPR, WS-Addressing information,
     * WS-Security information, etc.
     */
    private EndpointDefinition endpoint = null;

    /**
     * Parent endpoint of this endpoint if this used inside another endpoint. Possible parents are
     * LoadbalanceEndpoint, SALoadbalanceEndpoint and FailoverEndpoint objects.
     */
    private Endpoint parentEndpoint = null;

    /**
     * Leaf level endpoints will be suspended for the specified time by this variable, after a
     * failure. If this is not explicitly set, it is set to -1, which causes endpoints to suspended forever.
     */
    private long suspendOnFailDuration = -1;

    /**
     * Time to recover a failed endpoint. Value of this is calculated when endpoint is set as
     * failed by adding suspendOnFailDuration to current time.
     */
    private long recoverOn = Long.MAX_VALUE;

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
     * Checks if the endpoint is active (failed or not). If endpoint is in failed state and
     * suspendOnFailDuration has elapsed, it will be set to active.
     *
     * @param synMessageContext MessageContext of the current message. This is not used here.
     *
     * @return true if endpoint is active. false otherwise.
     */
    public boolean isActive(MessageContext synMessageContext) {

        if (!active) {
            if (System.currentTimeMillis() > recoverOn) {
                active = true;
                recoverOn = 0;
            }
        }

        return active;
    }

    /**
     * Sets if endpoint active or not. if endpoint is set as failed (active = false), the recover on
     * time is calculated so that it will be activated after the recover on time.
     *
     * @param active true if active. false otherwise.
     *
     * @param synMessageContext MessageContext of the current message. This is not used here.
     */
    public synchronized void setActive(boolean active, MessageContext synMessageContext) {

        // this is synchronized as recoverOn can be set to unpredictable values if two threads call
        // this method simultaneously.

        if (!active) {
            if (suspendOnFailDuration != -1) {
                recoverOn = System.currentTimeMillis() + suspendOnFailDuration;
            } else {
                recoverOn = Long.MAX_VALUE;
            }
        }

        this.active = active;
    }

    /**
     * Sends the message through this endpoint. This method just handles statistics related functions
     * and gives the message to the Synapse environment to send. It does not add any endpoint
     * specific details to the message context. These details are added only to the cloned message
     * context by the Axis2FlexibleMepClient. So that we can reuse the original message context for
     * resending through different endpoints.
     *
     * @param synCtx MessageContext sent by client to Synapse
     */
    public void send(MessageContext synCtx) {

        String endPointName = this.getName();
        if(endPointName ==null) {
            endPointName = Constants.ANONYMOUS_ENDPOINTS;
        }
        // Setting Required property to collect the End Point statistics
        boolean statisticsEnable =
                (org.apache.synapse.Constants.STATISTICS_ON == endpoint.getStatisticsEnable());
        if (statisticsEnable) {
            EndPointStatisticsStack endPointStatisticsStack = null;
            Object statisticsStackObj =
                    synCtx.getProperty(org.apache.synapse.Constants.ENDPOINT_STATISTICS_STACK);
            if (statisticsStackObj == null) {
                endPointStatisticsStack = new EndPointStatisticsStack();
                synCtx.setProperty(org.apache.synapse.Constants.ENDPOINT_STATISTICS_STACK,
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

        if (endpoint.getAddress() != null) {
            if (log.isDebugEnabled()) {
                log.debug("Sending message to endpoint :: name = " +
                        endPointName + " resolved address = " + endpoint.getAddress());
                log.debug("SOAPAction: " + (synCtx.getSoapAction() != null ?
                        synCtx.getSoapAction() : "null"));
                log.debug("WSA-Action: " + (synCtx.getWSAAction() != null ?
                        synCtx.getWSAAction() : "null"));
                log.debug("Body : \n" + synCtx.getEnvelope());
            }
        }

        // register this as the immediate fault handler for this message.
        synCtx.pushFaultHandler(this);

        // add this as the last endpoint to process this message. it is used by statistics code.
        synCtx.setProperty(Constants.PROCESSED_ENDPOINT, this);

        synCtx.getEnvironment().send(endpoint, synCtx);
    }

    public void onChildEndpointFail(Endpoint endpoint, MessageContext synMessageContext) {
        // nothing to do as this is a leaf level endpoint
    }

    public void setParentEndpoint(Endpoint parentEndpoint) {
        this.parentEndpoint = parentEndpoint;
    }

    public long getSuspendOnFailDuration() {
        return suspendOnFailDuration;
    }

    public void setSuspendOnFailDuration(long suspendOnFailDuration) {
        this.suspendOnFailDuration = suspendOnFailDuration;
    }

    public void onFault(MessageContext synCtx) {
        // perform retries here

        // if this endpoint has actually failed, inform the parent.
        setActive(false, synCtx);

        if (parentEndpoint != null) {
            parentEndpoint.onChildEndpointFail(this, synCtx);
        } else {
            Stack faultStack = synCtx.getFaultStack();
            if (!faultStack.isEmpty()) {
                ((FaultHandler) faultStack.pop()).handleFault(synCtx);
            }
        }
    }
}
