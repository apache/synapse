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

import org.apache.synapse.endpoints.algorithms.LoadbalanceAlgorithm;
import org.apache.synapse.endpoints.dispatch.Dispatcher;
import org.apache.synapse.MessageContext;
import org.apache.synapse.FaultHandler;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.axis2.context.OperationContext;

import java.util.ArrayList;
import java.util.List;

/**
 * SALoadbalanceEndpoint supports session affinity based load balancing. Each of this endpoint
 * maintains a list of dispatchers. These dispatchers will be updated for both request (for client
 * initiated sessions) and response (for server initiated sessions). Once updated, each dispatcher
 * will check if has already encountered that session. If not, it will update the
 * session -> endpoint map. To update sessions for response messages, all SALoadbalanceEndpoint
 * objects are kept in a global property. When a message passes through SALoadbalanceEndpoints, each
 * endpoint appends its "Synapse unique ID" to the operation context. Once the response for that
 * message arrives, response sender checks first endpoint of the endpoint sequence from the operation
 * context and get that endpoint from the above mentioned global property. Then it will invoke
 * updateSession(...) method of that endpoint. After that, each endpoint will call updateSession(...)
 * method of their appropriate child endpoint, so that all the sending endpoints for the session will
 * be updated.
 *
 * This endpoint gets the target endpoint first from the dispatch manager, which will ask all listed
 * dispatchers for a matching session. If a matching session is found it will just invoke the send(...)
 * method of that endpoint. If not it will find an endpoint using the loadbalance policy and send to
 * that endpoint.
 */
public class SALoadbalanceEndpoint implements Endpoint {

    private ArrayList endpoints = null;
    private LoadbalanceAlgorithm algorithm = null;
    private String name = null;
    private boolean active = true;
    private Endpoint parentEndpoint = null;
    private Dispatcher dispatcher = null;

    public void send(MessageContext synMessageContext) {

        Endpoint endpoint = null;

        // first check if this session is associated with a session. if so, get the endpoint
        // associated for that session.
        endpoint = dispatcher.getEndpoint(synMessageContext);
        if (endpoint == null) {
            // there is no endpoint associated with this session. get a new endpoint using the
            // load balance policy.
            endpoint = algorithm.getNextEndpoint(synMessageContext);

            // this is a start of a new session. so update session map.
            if (dispatcher.isServerInitiatedSession()) {
                // add this endpoint to the endpoint sequence of operation context.
                Axis2MessageContext axis2MsgCtx = (Axis2MessageContext) synMessageContext;
                OperationContext opCtx = axis2MsgCtx.getAxis2MessageContext().getOperationContext();
                Object o = opCtx.getProperty("endpointList");
                if (o != null) {
                    List endpointList = (List) o;
                    endpointList.add(this);

                    // if the next endpoint is not a session affinity one, endpoint sequence ends
                    // here. but we have to add the next endpoint to the list.
                    if (!(endpoint instanceof SALoadbalanceEndpoint)) {
                        endpointList.add(endpoint);
                    }
                } else {
                    // this is the first endpoint in the heirachy. so create the queue and insert
                    // this as the first element.
                    List endpointList = new ArrayList();
                    endpointList.add(this);

                    // if the next endpoint is not a session affinity one, endpoint sequence ends
                    // here. but we have to add the next endpoint to the list.
                    if (!(endpoint instanceof SALoadbalanceEndpoint)) {
                        endpointList.add(endpoint);
                    }

                    opCtx.setProperty("endpointList", endpointList);
                }

            } else {
                dispatcher.updateSession(synMessageContext, endpoint);
            }
        }

        if (endpoint != null) {
            endpoint.send(synMessageContext);
        } else {
            if (parentEndpoint != null) {
                parentEndpoint.onChildEndpointFail(this, synMessageContext);
            } else {
                Object o = synMessageContext.getFaultStack().pop();
                ((FaultHandler) o).handleFault(synMessageContext);
            }
        }
    }

    /**
     * This will be called for the response of the first message of each server initiated session.
     *
     * @param responseMsgCtx
     * @param endpointList
     */
    public void updateSession(MessageContext responseMsgCtx, List endpointList) {
        Endpoint endpoint = (Endpoint) endpointList.remove(0);
        dispatcher.updateSession(responseMsgCtx, endpoint);
        if (endpoint instanceof SALoadbalanceEndpoint) {
            ((SALoadbalanceEndpoint) endpoint).updateSession(responseMsgCtx, endpointList);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LoadbalanceAlgorithm getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(LoadbalanceAlgorithm algorithm) {
        this.algorithm = algorithm;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public ArrayList getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(ArrayList endpoints) {
        this.endpoints = endpoints;
    }

    public void setParentEndpoint(Endpoint parentEndpoint) {
        this.parentEndpoint = parentEndpoint;
    }

    public Dispatcher getDispatcher() {
        return dispatcher;
    }

    public void setDispatcher(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    /**
     * It is logically incorrect to failover a session affinity endpoint. If we redirect a message
     * belonging to a particular session, new endpoint is not aware of the session. So we can't handle
     * anything more at the endpoint level. Therefore, this method just deactivate the failed
     * endpoint and give the fault to the next fault handler.
     *
     * @param endpoint Failed endpoint.
     * @param synMessageContext MessageContext of the failed message.
     */
    public void onChildEndpointFail(Endpoint endpoint, MessageContext synMessageContext) {
        endpoint.setActive(false);
        Object o = synMessageContext.getFaultStack().pop();
        ((FaultHandler)o).handleFault(synMessageContext);
    }
}
