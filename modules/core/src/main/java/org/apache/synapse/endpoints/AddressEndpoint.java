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

import org.apache.axis2.clustering.ClusterManager;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.endpoints.utils.EndpointDefinition;
import org.apache.synapse.statistics.impl.EndPointStatisticsStack;

/**
 * This class represents an actual endpoint to send the message. It is responsible for sending the
 * message, performing retries if a failure occurred and informing the parent endpoint if a failure
 * couldn't be recovered.
 */
public class AddressEndpoint extends DefaultEndpoint {

    /**
     * The endpoint context , place holder for keep any runtime states related to the endpoint
     */
    private final EndpointContext endpointContext = new EndpointContext();

    /**
     * Checks if the endpoint is active (failed or not). If endpoint is in failed state and
     * suspendOnFailDuration has elapsed, it will be set to active.
     *
     * @param synMessageContext MessageContext of the current message. This is not used here.
     * @return true if endpoint is active. false otherwise.
     */
    public boolean isActive(MessageContext synMessageContext) {

        boolean active = endpointContext.isActive();
        if (!active) {

            long recoverOn = endpointContext.getRecoverOn();
            if (System.currentTimeMillis() > recoverOn) {
                active = true;
                endpointContext.setActive(true);
                endpointContext.setRecoverOn(0);                       

            }
        }

        if (log.isDebugEnabled()) {
            log.debug("AddressEndpoint with name '" + getName() + "' is in "
                    + (active ? "active" : "inactive") + " state");
        }

        return active;
    }

    /**
     * Sets if endpoint active or not. if endpoint is set as failed (active = false), the recover on
     * time is calculated so that it will be activated after the recover on time.
     *
     * @param active            true if active. false otherwise.
     * @param synMessageContext MessageContext of the current message. This is not used here.
     */
    public synchronized void setActive(boolean active, MessageContext synMessageContext) {

        // this is synchronized as recoverOn can be set to unpredictable values if two threads call
        // this method simultaneously.

        if (!active) {
            EndpointDefinition endpoint = getEndpoint();
            if (endpoint.getSuspendOnFailDuration() != -1) {
                // Calculating a new value by adding suspendOnFailDuration to current time.
                // as the endpoint is set as failed
                endpointContext.setRecoverOn(
                        System.currentTimeMillis() + endpoint.getSuspendOnFailDuration());
            } else {
                endpointContext.setRecoverOn(Long.MAX_VALUE);
            }
        }

        this.endpointContext.setActive(active);
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
            traceOrDebug(traceOn, "Start : Address Endpoint");

            if (traceOn && trace.isTraceEnabled()) {
                trace.trace("Message : " + synCtx.getEnvelope());
            }
        }

        boolean isClusteringEnable = false;
        // get Axis2 MessageContext and ConfigurationContext
        org.apache.axis2.context.MessageContext axisMC =
                ((Axis2MessageContext) synCtx).getAxis2MessageContext();
        ConfigurationContext cc = axisMC.getConfigurationContext();

        //The check for clustering environment

        ClusterManager clusterManager = cc.getAxisConfiguration().getClusterManager();
        if (clusterManager != null &&
                clusterManager.getContextManager() != null) {
            isClusteringEnable = true;
        }

        String endPointName = this.getName();
        if (endPointName == null) {

            if (traceOrDebugOn && isClusteringEnable) {
                log.warn(SALoadbalanceEndpoint.WARN_MESSAGE);
            }
            endPointName = SynapseConstants.ANONYMOUS_ENDPOINT;
        }

        if (isClusteringEnable) {

            // if this is a cluster environment , then set configuration context to endpoint context
            if (endpointContext.getConfigurationContext() == null) {
                endpointContext.setConfigurationContext(cc);
                endpointContext.setContextID(endPointName); // The context ID
            }
        }

        EndpointDefinition endpoint = getEndpoint();
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

        if (endpoint.getAddress() != null) {
            if (traceOrDebugOn) {
                traceOrDebug(traceOn, "Sending message to endpoint : " +
                        endPointName + " resolves to address = " + endpoint.getAddress());
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

    public void onFault(MessageContext synCtx) {
        // perform retries here

        // if this endpoint has actually failed, inform the parent.
        setActive(false, synCtx);
        super.onFault(synCtx);
    }
}
