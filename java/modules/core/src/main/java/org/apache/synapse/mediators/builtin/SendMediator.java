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
import org.apache.synapse.SynapseConstants;
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

    private Endpoint endpoint = null;

    /**
     * This is a leaf mediator. i.e. processing stops once send is invoked,
     * as it always returns false
     *
     * @param synCtx the current message to be sent
     * @return false always as this is a leaf mediator
     */
    public boolean mediate(MessageContext synCtx) {

        boolean traceOn = isTraceOn(synCtx);
        boolean traceOrDebugOn = isTraceOrDebugOn(traceOn);

        if (traceOrDebugOn) {
            traceOrDebug(traceOn, "Start : Send mediator");

            if (traceOn && trace.isTraceEnabled()) {
                trace.trace("Message : " + synCtx.getEnvelope());
            }
        }

        // if no endpoints are defined, send where implicitly stated
        if (endpoint == null) {

            if (traceOrDebugOn) {
                StringBuffer sb = new StringBuffer();
                sb.append("Sending " + (synCtx.isResponse() ? "response" : "request")
                    + " message using implicit message properties..");
                sb.append("\nSending To: " + (synCtx.getTo() != null ?
                        synCtx.getTo().getAddress() : "null"));
                sb.append("\nSOAPAction: " + (synCtx.getWSAAction() != null ?
                        synCtx.getWSAAction() : "null"));
                traceOrDebug(traceOn, sb.toString());
            }

            if (traceOn && trace.isTraceEnabled()) {
                trace.trace("Envelope : " + synCtx.getEnvelope());
            }

            if (synCtx.isResponse()) {
                Axis2MessageContext axis2MsgCtx = (Axis2MessageContext) synCtx;
                OperationContext opCtx = axis2MsgCtx.getAxis2MessageContext().getOperationContext();
                Object o = opCtx.getProperty("endpointList");
                if (o != null && o instanceof List) {
                    // we are in the response of the first message of a server initiated session
                    // so update all session maps
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

        if (traceOrDebugOn) {
            traceOrDebug(traceOn, "End : Send mediator");
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
