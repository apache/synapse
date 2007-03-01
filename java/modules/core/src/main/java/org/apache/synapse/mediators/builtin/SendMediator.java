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

package org.apache.synapse.mediators.builtin;

import org.apache.axis2.addressing.EndpointReference;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Constants;
import org.apache.synapse.MessageContext;
import org.apache.synapse.config.Endpoint;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.statistics.impl.EndPointStatisticsStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The Send mediator sends the message using the following semantics.
 * <p/>
 * This is a leaf mediator (i.e. further processing halts after this mediator completes)
 * <p/>
 * TODO support loadbalancing and failover
 */
public class SendMediator extends AbstractMediator {

    private static final Log log = LogFactory.getLog(SendMediator.class);
    private static final Log trace = LogFactory.getLog(Constants.TRACE_LOGGER);

    /** The list of endpoints to which the message should be sent to. If none
     * are specified, the message is sent where its implicitly stated in the
     * current message */
    private List endpoints = new ArrayList();

    /**
     * This is a leaf mediator. i.e. processing stops once send is invoked,
     * as it always returns false
     *
     * @param synCtx the current message to be sent
     * @return false always as this is a leaf mediator
     */
    public boolean mediate(MessageContext synCtx) {
        log.debug("Send mediator :: mediate()");

        boolean shouldTrace = shouldTrace(synCtx.getTracingState());
        try {
            if (shouldTrace) {
                trace.trace("Start : Send mediator");
                trace.trace("Sending Message :: " + synCtx.getEnvelope());
            }
            // if no endpoints are defined, send where implicitly stated
            if (endpoints.isEmpty()) {
                if (log.isDebugEnabled()) {
                    log.debug("Sending message using implicit message properties..");
                    log.debug("Sending To: " + (synCtx.getTo() != null ?
                            synCtx.getTo().getAddress() : "null"));
                    log.debug("SOAPAction: " + (synCtx.getWSAAction() != null ?
                            synCtx.getWSAAction() : "null"));
                    log.debug("Body : \n" + synCtx.getEnvelope());
                }
                synCtx.getEnvironment().send(null, synCtx);

            } else if (endpoints.size() == 1) {
                Endpoint singleEndpoint = (Endpoint) endpoints.get(0);
                String eprAddress = null;
                if (singleEndpoint.getAddress() != null) {
                    eprAddress = singleEndpoint.getAddress().toString();
                } else {
                    singleEndpoint = synCtx.getConfiguration().getNamedEndpoint(
                            singleEndpoint.getRef());
                    eprAddress = singleEndpoint.getAddress().toString();
                }

                if (shouldTrace) {
                    trace.trace("Sending to Endpoint : " + eprAddress);
                }
                String endPointName = singleEndpoint.getName();

                 // Setting Required property to collect the End Point statistics
                boolean statisticsEnable = (org.apache.synapse.Constants.STATISTICS_ON == singleEndpoint.getStatisticsEnable());
                if (endPointName != null && statisticsEnable) {
                    EndPointStatisticsStack endPointStatisticsStack = new EndPointStatisticsStack();
                    boolean isFault =synCtx.getEnvelope().getBody().hasFault();
                    endPointStatisticsStack.put(endPointName, System.currentTimeMillis(), !synCtx.isResponse(), statisticsEnable,isFault);
                    synCtx.setProperty(org.apache.synapse.Constants.ENDPOINT_STATISTICS_STACK, endPointStatisticsStack);
                }
                synCtx.setTo(new EndpointReference(eprAddress));

                if (log.isDebugEnabled()) {
                    log.debug("Sending message to endpoint :: name = " +
                            endPointName + " resolved address = " + eprAddress);
                    log.debug("Sending To: " + (synCtx.getTo() != null ?
                            synCtx.getTo().getAddress() : "null"));
                    log.debug("SOAPAction: " + (synCtx.getWSAAction() != null ?
                            synCtx.getWSAAction() : "null"));
                    log.debug("Body : \n" + synCtx.getEnvelope());
                }

                synCtx.getEnvironment().send(singleEndpoint, synCtx);

            } else {
                String msg = "The send mediator currently supports only one endpoint";
                synCtx.setProperty(Constants.SYNAPSE_ERROR, Boolean.TRUE);
                log.error(msg);
                throw new UnsupportedOperationException(msg);
            }
        } finally {
            if (shouldTrace) {
                trace.trace("End : Send mediator");
            }
        }
        return false;
    }

    /**
     * Add the given Endpoint as an endpoint for this Send mediator instance
     * @param e the Endpoint to be added
     * @return true if the endpoint list was updated
     */
    public boolean addEndpoint(Endpoint e) {
        return endpoints.add(e);
    }

    /**
     * Get list of Endpoints to which the message should be sent
     * @return the endpoints to which the message should be sent
     */
    public List getEndpoints() {
        return endpoints;
    }
}
