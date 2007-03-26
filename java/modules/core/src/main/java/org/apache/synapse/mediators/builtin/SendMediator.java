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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Constants;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.endpoints.SALoadbalanceEndpoint;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.axis2.context.OperationContext;

import java.util.List;

/**
 * SendMediator sends a message using specified semantics. If it contains an endpoint it will send the
 * message to that endpoint. Once a message is sent to the endpoint further sending behaviors are completely
 * governed by that endpoint. If there is no endpoint available, SendMediator will send the message to
 * the implicitly stated destination.
 * */
public class SendMediator extends AbstractMediator {

    private static final Log log = LogFactory.getLog(SendMediator.class);
    private static final Log trace = LogFactory.getLog(Constants.TRACE_LOGGER);

    private Endpoint endpoint = null;

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
            if (endpoint == null) {
                if (log.isDebugEnabled()) {
                    log.debug("Sending message using implicit message properties..");
                    log.debug("Sending To: " + (synCtx.getTo() != null ?
                            synCtx.getTo().getAddress() : "null"));
                    log.debug("SOAPAction: " + (synCtx.getWSAAction() != null ?
                            synCtx.getWSAAction() : "null"));
                    log.debug("Body : \n" + synCtx.getEnvelope());
                }

                if (synCtx.isResponse()) {
                    Axis2MessageContext axis2MsgCtx = (Axis2MessageContext) synCtx;
                    OperationContext opCtx = axis2MsgCtx.getAxis2MessageContext().getOperationContext();
                    Object o = opCtx.getProperty("endpointList");
                    if (o != null) {
                        // we are in the response of the first message of a server initiated session.
                        // so update all session maps.
                        List endpointList = (List) o;
                        Object e = endpointList.remove(0);
                        if (e != null && e instanceof SALoadbalanceEndpoint) {
                            SALoadbalanceEndpoint saLoadbalanceEndpoint = (SALoadbalanceEndpoint) e;
                            saLoadbalanceEndpoint.updateSession(synCtx, endpointList);
                        }
                    }
                }
                synCtx.getEnvironment().send(null, synCtx);

            } else {
                endpoint.send(synCtx);
            }

        } finally {
            if (shouldTrace) {
                trace.trace("End : Send mediator");
            }
        }
        return true;
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(Endpoint endpoint) {
        this.endpoint = endpoint;
    }
}
