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
import org.apache.axis2.context.OperationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.FaultHandler;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.endpoints.algorithms.AlgorithmContext;
import org.apache.synapse.endpoints.algorithms.LoadbalanceAlgorithm;
import org.apache.synapse.endpoints.dispatch.Dispatcher;
import org.apache.synapse.endpoints.dispatch.DispatcherContext;

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
 * message arrives, response sender checks first endpoint of the endpoint sequence from the
 * operation context and get that endpoint from the above mentioned global property. Then it will
 * invoke updateSession(...) method of that endpoint. After that, each endpoint will call
 * updateSession(...) method of their appropriate child endpoint, so that all the sending endpoints
 * for the session will be updated.
 * <p/>
 * This endpoint gets the target endpoint first from the dispatch manager, which will ask all listed
 * dispatchers for a matching session. If a matching session is found it will just invoke the
 * send(...) method of that endpoint. If not it will find an endpoint using the load balancing
 * policy and send to that endpoint.
 */
public class SALoadbalanceEndpoint implements Endpoint {

    private static final Log log = LogFactory.getLog(SALoadbalanceEndpoint.class);

    private static final String FIRST_MESSAGE_IN_SESSION = "first_message_in_session";
    public static final String ENDPOINT_LIST = "endpointList";
    public static final String ROOT_ENDPOINT = "rootendpoint";
    public static final String ENDPOINT_NAME_LIST = "endpointNameList";
    public static final String WARN_MESSAGE = "In a clustering environment, the endpoint " +
            "name should be specified even for anonymous endpoints. Otherwise the clustering " +
            "would not function properly, if there are more than one anonymous endpoints.";

    /**
     * Name of the endpoint. Used for named endpoints which can be referred using the key attribute
     * of indirect endpoints.
     */
    private String name = null;

    /**
     * List of endpoints among which the load is distributed. Any object implementing the Endpoint
     * interface could be used.
     */
    private List<Endpoint> endpoints = null;

    /**
     * Algorithm used for selecting the next endpoint to direct the first request of sessions.
     * Default is RoundRobin.
     */
    private LoadbalanceAlgorithm algorithm = null;

    /**
     * Parent endpoint of this endpoint if this used inside another endpoint. Although any endpoint
     * can be the parent, only SALoadbalanceEndpoint should be used here. Use of any other endpoint
     * would invalidate the session.
     */
    private Endpoint parentEndpoint = null;

    /**
     * Dispatcher used for session affinity.
     */
    private Dispatcher dispatcher = null;

    /**
     * The dispatcher context, place holder for keeping any runtime states that are used when
     * finding endpoint for the session
     */
    private final DispatcherContext dispatcherContext = new DispatcherContext();

    /**
     * The endpoint context, place holder for keeping any runtime states related to the endpoint
     */
    private final EndpointContext endpointContext = new EndpointContext();

    /**
     * The algorithm context, place holder for keeping any runtime states related to the load
     * balance algorithm
     */
    private final AlgorithmContext algorithmContext = new AlgorithmContext();


    public void send(MessageContext synMessageContext) {

        if (log.isDebugEnabled()) {
            log.debug("Start : Session Affinity Load-balance Endpoint " + name);
        }

        boolean isClusteringEnable = false;
        // get Axis2 MessageContext and ConfigurationContext
        org.apache.axis2.context.MessageContext axisMC =
                ((Axis2MessageContext) synMessageContext).getAxis2MessageContext();
        ConfigurationContext cc = axisMC.getConfigurationContext();

        //The check for clustering environment
        ClusterManager clusterManager = cc.getAxisConfiguration().getClusterManager();
        if (clusterManager != null &&
                clusterManager.getContextManager() != null) {
            isClusteringEnable = true;
        }

        String endpointName = this.getName();
        if (endpointName == null) {
            if (isClusteringEnable) {
                log.warn(WARN_MESSAGE);
            }
            if (log.isDebugEnabled()) {
                log.debug("Using the name for the anonymous endpoint as : '"
                        + SynapseConstants.ANONYMOUS_ENDPOINT + "'");
            }
            endpointName = SynapseConstants.ANONYMOUS_ENDPOINT;
        }

        if (isClusteringEnable) {

            // if this is a cluster environment, then set configuration context to endpoint context
            if (endpointContext.getConfigurationContext() == null) {

                if (log.isDebugEnabled()) {
                    log.debug("Setting the ConfigurationContext to " +
                            "the EndpointContext with the name " + endpointName +
                            " for replicating data on the cluster");
                }
                endpointContext.setConfigurationContext(cc);
                endpointContext.setContextID(endpointName);
            }

            // if this is a cluster environment, then set configuration context to load balance
            //  algorithm context
            if (algorithmContext.getConfigurationContext() == null) {

                if (log.isDebugEnabled()) {
                    log.debug("Setting the ConfigurationContext to " +
                            "the AlgorithmContext with the name " + endpointName +
                            " for replicating data on the cluster");
                }
                algorithmContext.setConfigurationContext(cc);
                algorithmContext.setContextID(endpointName);
            }

            // if this is a cluster environment, then set configuration context to session based
            // endpoint dispatcher
            if (dispatcherContext.getConfigurationContext() == null) {

                if (log.isDebugEnabled()) {
                    log.debug("Setting the ConfigurationContext to " +
                            "the DispatcherContext with the name " + endpointName +
                            " for replicating data on the cluster");
                }
                dispatcherContext.setConfigurationContext(cc);
                dispatcherContext.setContextID(endpointName);

                if (log.isDebugEnabled()) {
                    log.debug("Setting the endpoints to the DispatcherContext : " + endpoints);
                }
                dispatcherContext.setEndpoints(endpoints);
            }
        }

        // first check if this session is associated with a session. if so, get the endpoint
        // associated for that session.
        Endpoint endpoint = dispatcher.getEndpoint(synMessageContext, dispatcherContext);
        if (endpoint == null) {

            // there is no endpoint associated with this session. get a new endpoint using the
            // load balance policy.
            endpoint = algorithm.getNextEndpoint(synMessageContext, algorithmContext);

            // this is a start of a new session. so update session map.
            if (dispatcher.isServerInitiatedSession()) {

                if (log.isDebugEnabled()) {
                    log.debug("Adding a new server initiated session for the current message");
                }

                // add this endpoint to the endpoint sequence of operation context.
                Axis2MessageContext axis2MsgCtx = (Axis2MessageContext) synMessageContext;
                OperationContext opCtx = axis2MsgCtx.getAxis2MessageContext().getOperationContext();

                if (isClusteringEnable) {
                    // If running on a cluster keep endpoint names, because it is heavy to
                    // replicate endpoint itself

                    Object o = opCtx.getPropertyNonReplicable(ENDPOINT_NAME_LIST);
                    List<String> epNameList;
                    if (o instanceof List) {
                        epNameList = (List<String>) o;
                        epNameList.add(endpointName);
                    } else {
                        // this is the first endpoint in the heirachy. so create the queue and
                        // insert this as the first element.
                        epNameList = new ArrayList<String>();
                        epNameList.add(endpointName);
                        opCtx.setNonReplicableProperty(ROOT_ENDPOINT, this);
                    }
                    
                    // if the next endpoint is not a session affinity one, endpoint sequence ends
                    // here. but we have to add the next endpoint to the list.
                    if (!(endpoint instanceof SALoadbalanceEndpoint)) {

                        String name;
                        if (endpoint instanceof IndirectEndpoint) {
                            name = ((IndirectEndpoint) endpoint).getKey();
                        } else {
                            name = endpoint.getName();
                        }

                        if (name == null) {
                            log.warn(WARN_MESSAGE);
                            name = SynapseConstants.ANONYMOUS_ENDPOINT;
                        }
                        epNameList.add(name);
                    }

                    if (log.isDebugEnabled()) {
                        log.debug("Operating on a cluster. Setting the endpoint name list to " +
                                "the OperationContext : " + epNameList);
                    }
                    opCtx.setProperty(ENDPOINT_NAME_LIST, epNameList);

                } else {
                    
                    Object o = opCtx.getProperty(ENDPOINT_LIST);
                    List<Endpoint> endpointList;
                    if (o instanceof List) {
                        endpointList = (List<Endpoint>) o;
                        endpointList.add(this);
                    } else {
                        // this is the first endpoint in the heirachy. so create the queue and
                        // insert this as the first element.
                        endpointList = new ArrayList<Endpoint>();
                        endpointList.add(this);
                        opCtx.setProperty(ENDPOINT_LIST, endpointList);
                    }
                    
                    // if the next endpoint is not a session affinity one, endpoint sequence ends
                    // here. but we have to add the next endpoint to the list.
                    if (!(endpoint instanceof SALoadbalanceEndpoint)) {
                        endpointList.add(endpoint);
                    }
                }

            } else {
                dispatcher.updateSession(synMessageContext, dispatcherContext, endpoint);
            }

            // this is the first request. so an endpoint has not been bound to this session and we
            // are free to failover if the currently selected endpoint is not working. but for
            // failover to work, we have to build the soap envelope.
            synMessageContext.getEnvelope().build();

            // we should also indicate that this is the first message in the session. so that
            // onFault(...) method can resend only the failed attempts for the first message.
            synMessageContext.setProperty(FIRST_MESSAGE_IN_SESSION, Boolean.TRUE);
        }

        if (endpoint != null) {

            // endpoints given by session dispatchers may not be active. therefore, we have check
            // it here.
            if (endpoint.isActive(synMessageContext)) {
                if (log.isDebugEnabled()) {
                    log.debug("Using the endpoint on the session with "
                            + ((endpoint instanceof IndirectEndpoint) ? "key : "
                            + ((IndirectEndpoint) endpoint).getKey() : "name : "
                            + endpoint.getName()) + " for sending the message");
                }
                endpoint.send(synMessageContext);
            } else {
                informFailure(synMessageContext);
            }

        } else {

            // all child endpoints have failed. so mark this also as failed.
            if (log.isDebugEnabled()) {
                log.debug("Marking the Endpoint as failed, " +
                        "because all child endpoints has been failed");
            }
            setActive(false, synMessageContext);
            informFailure(synMessageContext);
        }
    }

    /**
     * This will be called for the response of the first message of each server initiated session.
     *
     * @param responseMsgCtx
     * @param endpointList
     * @param isClusteringEnable
     */
    public void updateSession(MessageContext responseMsgCtx, List endpointList,
                              boolean isClusteringEnable) {

        Endpoint endpoint = null;

        if (isClusteringEnable) {
            // if this is a clustering env. only keep endpoint names, because, it is heavy to
            // replicate endpoint itself
            String epNameObj = (String) endpointList.remove(0);
            for (Endpoint ep : endpoints) {
                if (ep != null) {

                    String name;
                    if (ep instanceof IndirectEndpoint) {
                        name = ((IndirectEndpoint) ep).getKey();
                    } else {
                        name = ep.getName();
                    }

                    if (name != null && name.equals(epNameObj)) {
                        endpoint = ep;
                        break;
                    }
                }
            }

        } else {
            endpoint = (Endpoint) endpointList.remove(0);
        }

        if (endpoint != null) {

            dispatcher.updateSession(responseMsgCtx, dispatcherContext, endpoint);
            if (endpoint instanceof SALoadbalanceEndpoint) {
                ((SALoadbalanceEndpoint) endpoint).updateSession(
                        responseMsgCtx, endpointList, isClusteringEnable);
            }
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name.trim();
    }

    public LoadbalanceAlgorithm getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(LoadbalanceAlgorithm algorithm) {
        this.algorithm = algorithm;
    }

    /**
     * This is active in below conditions:
     * If a session is not started AND at least one child endpoint is active.
     * If a session is started AND the binding endpoint is active.
     * <p/>
     * This is not active for all other conditions.
     *
     * @param synMessageContext MessageContext of the current message. This is used to determine the
     *                          session.
     * @return true is active. false otherwise.
     */
    public boolean isActive(MessageContext synMessageContext) {
        // todo: implement above
        boolean active;
        Endpoint endpoint = dispatcher.getEndpoint(synMessageContext, dispatcherContext);
        if (endpoint == null) { // If a session is not started
            active = endpointContext.isActive();
            if (!active && endpoints != null) {
                for (Endpoint ep : endpoints) {
                    if (ep != null) {
                        active = ep.isActive(synMessageContext);
                        if (active) {    //AND at least one child endpoint is active
                            endpointContext.setActive(active);
                            // don't break the loop though we found one active endpoint. calling isActive()
                            // on all child endpoints will update their active state. so this is a good
                            // time to do that.
                        }
                    }
                }
            }
        } else {
            //If a session is started AND the binding endpoint is active.
            active = endpoint.isActive(synMessageContext);
            if (active) {
                endpointContext.setActive(active);
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("SALoadbalanceEndpoint with name '" + getName() + "' is in "
                    + (active ? "active" : "inactive") + " state");
        }

        return active;
    }

    public void setActive(boolean active, MessageContext synMessageContext) {
        endpointContext.setActive(active);
    }

    public List<Endpoint> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(List<Endpoint> endpoints) {
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
     * It is logically incorrect to failover a session affinity endpoint after the session has started.
     * If we redirect a message belonging to a particular session, new endpoint is not aware of the
     * session. So we can't handle anything more at the endpoint level. Therefore, this method just
     * deactivate the failed endpoint and give the fault to the next fault handler.
     * <p/>
     * But if the session has not started (i.e. first message), the message will be resend by binding
     * it to a different endpoint.
     *
     * @param endpoint          Failed endpoint.
     * @param synMessageContext MessageContext of the failed message.
     */
    public void onChildEndpointFail(Endpoint endpoint, MessageContext synMessageContext) {

        Object o = synMessageContext.getProperty(FIRST_MESSAGE_IN_SESSION);

        if (o != null && Boolean.TRUE.equals(o)) {

            // this is the first message. so unbind the sesion with failed endpoint and start
            // new one by resending.
            dispatcher.unbind(synMessageContext, dispatcherContext);
            send(synMessageContext);

        } else {

            // session has already started. we can't failover.
            informFailure(synMessageContext);
        }
    }

    private void informFailure(MessageContext synMessageContext) {

        log.warn("Failed to send using the selected endpoint, becasue it is inactive");
        
        if (parentEndpoint != null) {
            parentEndpoint.onChildEndpointFail(this, synMessageContext);
        } else {
            Object o = synMessageContext.getFaultStack().pop();
            if (o != null) {
                ((FaultHandler) o).handleFault(synMessageContext);
            }
        }
    }

}
