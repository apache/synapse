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

        // TODO this may be really strange but true.. unless you call the below, sometimes it
        // results in an unbound URI exception for no credible reason - needs more investigation
        // seems like a woodstox issue. Use hack for now
        // synCtx.getEnvelope().build();
        boolean shouldTrace = shouldTrace(synCtx.getTracingState());
        if (shouldTrace) {
            trace.trace("Start : Send mediator");
            trace.trace("Sending Message :: " + synCtx.getEnvelope());
        }
        // if no endpoints are defined, send where implicitly stated
        if (endpoints.isEmpty()) {
            log.debug("Sending message using implicit message properties..");
            log.debug("Sending To: " + (synCtx.getTo() != null ?
                synCtx.getTo().getAddress() : "null"));
            log.debug("SOAPAction: " + (synCtx.getWSAAction() != null ?
                synCtx.getWSAAction() : "null"));
            log.debug("Body : \n" + synCtx.getEnvelope());
            synCtx.getEnvironment().send(synCtx);

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
            if (singleEndpoint.isForcePOX()) {
            	synCtx.setDoingPOX(true);
            } else if (singleEndpoint.isForceSOAP()) {
            	synCtx.setDoingPOX(false);
            }

            if (singleEndpoint.isUseMTOM()) {
                synCtx.setDoingMTOM(true);
            } else if (singleEndpoint.isUseSwa()) {
                synCtx.setDoingSWA(true);
            }

            if (singleEndpoint.isUseSeparateListener())
            {
            	synCtx.setProperty(Constants.OUTFLOW_USE_SEPARATE_LISTENER, Boolean.TRUE);
            }
            
            log.debug("Sending message to endpoint :: name = " +
                singleEndpoint.getName() + " resolved address = " + eprAddress);

            synCtx.setTo(new EndpointReference(eprAddress));
            log.debug("Sending To: " + (synCtx.getTo() != null ?
                synCtx.getTo().getAddress() : "null"));
            log.debug("SOAPAction: " + (synCtx.getWSAAction() != null ?
                synCtx.getWSAAction() : "null"));
            log.debug("Body : \n" + synCtx.getEnvelope());

            // if RM is turned on
            if (singleEndpoint.isReliableMessagingOn()) {
                synCtx.setProperty(Constants.OUTFLOW_ADDRESSING_ON, Boolean.TRUE);
                synCtx.setProperty(Constants.OUTFLOW_RM_ON, Boolean.TRUE);
                if (singleEndpoint.getWsRMPolicyKey() != null) {
                    synCtx.setProperty(Constants.OUTFLOW_RM_POLICY,
                        singleEndpoint.getWsRMPolicyKey());
                }
            }

            // if WS Security is specified
            if (singleEndpoint.isSecurityOn()) {
                synCtx.setProperty(Constants.OUTFLOW_ADDRESSING_ON, Boolean.TRUE);
                synCtx.setProperty(Constants.OUTFLOW_SECURITY_ON, Boolean.TRUE);
                if (singleEndpoint.getWsSecPolicyKey() != null) {
                    synCtx.setProperty(Constants.OUTFLOW_SEC_POLICY,
                        singleEndpoint.getWsSecPolicyKey());
                }
            }

            // if WS Addressing is specified
            if (singleEndpoint.isAddressingOn()) {
                synCtx.setProperty(Constants.OUTFLOW_ADDRESSING_ON, Boolean.TRUE);
            }

            synCtx.getEnvironment().send(synCtx);

        } else {
            String msg = "The send mediator currently supports only one endpoint";
            log.error(msg);
            throw new UnsupportedOperationException(msg);
        }
        if (shouldTrace) {
            trace.trace("End : Send mediator");
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
