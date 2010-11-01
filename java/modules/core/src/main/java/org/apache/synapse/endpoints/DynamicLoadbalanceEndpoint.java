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

import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.clustering.Member;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.FaultHandler;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.transport.nhttp.NhttpConstants;
import org.apache.synapse.core.LoadBalanceMembershipHandler;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.endpoints.algorithms.AlgorithmContext;
import org.apache.synapse.endpoints.dispatch.Dispatcher;
import org.apache.synapse.endpoints.dispatch.SALSessions;
import org.apache.synapse.endpoints.dispatch.SessionInformation;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;

/**
 * Represents a dynamic load balance endpoint. The application membership is not static,
 * but discovered through some mechanism such as using a GCF
 */
public class DynamicLoadbalanceEndpoint extends LoadbalanceEndpoint {

    private static final Log log = LogFactory.getLog(DynamicLoadbalanceEndpoint.class);

    /**
     *  Flag to enable session affinity based load balancing.
     */
    private boolean sessionAffinity = false;

    /**
     * Dispatcher used for session affinity.
     */
    private Dispatcher dispatcher = null;

    /* Sessions time out interval*/
    private long sessionTimeout = -1;

    /**
     * The algorithm context , place holder for keep any runtime states related to the load balance
     * algorithm
     */
    private AlgorithmContext algorithmContext;

    @Override
    public void init(SynapseEnvironment synapseEnvironment) {
        ConfigurationContext cc =
                ((Axis2SynapseEnvironment) synapseEnvironment).getAxis2ConfigurationContext();
        if (!initialized) {
            super.init(synapseEnvironment);
            if (algorithmContext == null) {
                algorithmContext = new AlgorithmContext(isClusteringEnabled, cc, getName());
            }

            // Initialize the SAL Sessions if already has not been initialized.
            SALSessions salSessions = SALSessions.getInstance();
            if (!salSessions.isInitialized()) {
                salSessions.initialize(isClusteringEnabled, cc);
            }
        }
        log.info("Dynamic load balance endpoint initialized");
    }

    private LoadBalanceMembershipHandler lbMembershipHandler;

    public DynamicLoadbalanceEndpoint() {
    }

    public void setLoadBalanceMembershipHandler(LoadBalanceMembershipHandler lbMembershipHandler) {
        this.lbMembershipHandler = lbMembershipHandler;
    }

    public LoadBalanceMembershipHandler getLbMembershipHandler() {
        return lbMembershipHandler;
    }

    public void send(MessageContext synCtx) {
        SessionInformation sessionInformation = null;
        Member currentMember = null;
        ConfigurationContext configCtx =
                ((Axis2MessageContext) synCtx).getAxis2MessageContext().getConfigurationContext();
        if (lbMembershipHandler.getConfigurationContext() == null) {
            lbMembershipHandler.setConfigurationContext(configCtx);
        }

        if (isSessionAffinityBasedLB()) {
            // first check if this session is associated with a session. if so, get the endpoint
            // associated for that session.
            sessionInformation =
                    (SessionInformation) synCtx.getProperty(
                            SynapseConstants.PROP_SAL_CURRENT_SESSION_INFORMATION);

            currentMember = (Member) synCtx.getProperty(
                    SynapseConstants.PROP_SAL_ENDPOINT_CURRENT_MEMBER);

            if (sessionInformation == null && currentMember == null) {
                sessionInformation = dispatcher.getSession(synCtx);
                if (sessionInformation != null) {

                    if (log.isDebugEnabled()) {
                        log.debug("Current session id : " + sessionInformation.getId());
                    }

                    currentMember = sessionInformation.getMember();
                    synCtx.setProperty(
                            SynapseConstants.PROP_SAL_ENDPOINT_CURRENT_MEMBER, currentMember);
                    // This is for reliably recovery any session information if while response is getting ,
                    // session information has been removed by cleaner.
                    // This will not be a cost as  session information a not heavy data structure
                    synCtx.setProperty(
                            SynapseConstants.PROP_SAL_CURRENT_SESSION_INFORMATION, sessionInformation);
                }
            }

        }
        
        if (sessionInformation != null && currentMember != null) {
            //send message on current session
            sendToApplicationMember(synCtx, currentMember, false);
        } else {
            // prepare for a new session
            currentMember = lbMembershipHandler.getNextApplicationMember(algorithmContext);
            if (currentMember == null) {
                String msg = "No application members available";
                log.error(msg);
                throw new SynapseException(msg);
            }
            sendToApplicationMember(synCtx, currentMember, true);
        }
    }

    public void setName(String name) {
        super.setName(name);
//        algorithmContext.setContextID(name);
    }

  public Dispatcher getDispatcher() {
        return dispatcher;
    }

    public void setDispatcher(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public long getSessionTimeout() {
        return sessionTimeout;
    }

    public void setSessionTimeout(long sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    public void setSessionAffinity(boolean sessionAffinity){
        this.sessionAffinity = sessionAffinity;
    }

    public boolean isSessionAffinityBasedLB(){
        return sessionAffinity;
    }

    private void sendToApplicationMember(MessageContext synCtx,
                                         Member currentMember, boolean newSession) {
        //Rewriting the URL
        org.apache.axis2.context.MessageContext axis2MsgCtx =
                ((Axis2MessageContext) synCtx).getAxis2MessageContext();

        //Removing the REST_URL_POSTFIX - this is a hack.
        //In this loadbalance endpoint we create an endpoint per request by setting the complete url as the adress.
        //If a REST message comes Axis2FlexibleMEPClient append the REST_URL_POSTFIX to the adress. Hence endpoint fails
        //do send the request. e.g.  http://localhost:8080/example/index.html/example/index.html
        axis2MsgCtx.removeProperty(NhttpConstants.REST_URL_POSTFIX);

        String transport = axis2MsgCtx.getTransportIn().getName();
        String address = synCtx.getTo().getAddress();
        EndpointReference to = getEndpointReferenceAfterURLRewrite(currentMember,
                transport, address);
        synCtx.setTo(to);

        DynamicLoadbalanceFaultHandler faultHandler = new DynamicLoadbalanceFaultHandler(to);
        faultHandler.setCurrentMember(currentMember);
        if (isFailover()) {
            synCtx.pushFaultHandler(faultHandler);
            synCtx.getEnvelope().build();
        }

        Endpoint endpoint = getEndpoint(to, synCtx);
        if (isSessionAffinityBasedLB() && newSession) {
            prepareEndPointSequence(synCtx, endpoint);
            synCtx.setProperty(SynapseConstants.PROP_SAL_ENDPOINT_CURRENT_MEMBER, currentMember);
            synCtx.setProperty(SynapseConstants.PROP_SAL_ENDPOINT_CURRENT_DISPATCHER, dispatcher);
            // we should also indicate that this is the first message in the session. so that
            // onFault(...) method can resend only the failed attempts for the first message.
            synCtx.setProperty(SynapseConstants.PROP_SAL_ENDPOINT_FIRST_MESSAGE_IN_SESSION,
                    Boolean.TRUE);
        }
        endpoint.send(synCtx);
    }

    /*
    * Preparing the endpoint sequence for a new session establishment request
    */
    private void prepareEndPointSequence(MessageContext synCtx, Endpoint endpoint) {

        Object o = synCtx.getProperty(SynapseConstants.PROP_SAL_ENDPOINT_ENDPOINT_LIST);
        List<Endpoint> endpointList;
        if (o instanceof List) {
            endpointList = (List<Endpoint>) o;
            endpointList.add(this);

        } else {
            // this is the first endpoint in the hierarchy. so create the queue and
            // insert this as the first element.
            endpointList = new ArrayList<Endpoint>();
            endpointList.add(this);
            synCtx.setProperty(SynapseConstants.PROP_SAL_ENDPOINT_ENDPOINT_LIST, endpointList);
        }

        // if the next endpoint is not a session affinity one, endpoint sequence ends
        // here. but we have to add the next endpoint to the list.
        if (!(endpoint instanceof DynamicLoadbalanceEndpoint)) {
            endpointList.add(endpoint);
            // Clearing out if there any any session information with current message
            if (dispatcher.isServerInitiatedSession()) {
                dispatcher.removeSessionID(synCtx);
            }
        }
    }

    private EndpointReference getEndpointReferenceAfterURLRewrite(Member currentMember,
                                                                  String transport,
                                                                  String address) {
        // URL rewrite
        if (transport.equals("http") || transport.equals("https")) {
            if (address.indexOf(":") != -1) {
                try {
                    address = new URL(address).getPath();
                } catch (MalformedURLException e) {
                    String msg = "URL " + address + " is malformed";
                    log.error(msg, e);
                    throw new SynapseException(msg, e);
                }
            }

            return new EndpointReference(transport + "://" + currentMember.getHostName() +
                    ":" + ("http".equals(transport) ? currentMember.getHttpPort() :
                    currentMember.getHttpsPort()) + address);
        } else {
            String msg = "Cannot load balance for non-HTTP/S transport " + transport;
            log.error(msg);
            throw new SynapseException(msg);
        }
    }

    /**
     *
     * @param to get an endpoint to send the information
     * @param synCtx synapse context
     * @return the created endpoint
     */
    private Endpoint getEndpoint(EndpointReference to, MessageContext synCtx) {
        AddressEndpoint endpoint = new AddressEndpoint();
        endpoint.setName("DynamicLoadBalanceAddressEndpoint-" + Math.random());
        EndpointDefinition definition = new EndpointDefinition();
        definition.setReplicationDisabled(true);
        definition.setAddress(to.getAddress());
        endpoint.setDefinition(definition);
        endpoint.init((SynapseEnvironment)
                ((Axis2MessageContext) synCtx).getAxis2MessageContext().
                        getConfigurationContext().getAxisConfiguration().
                        getParameterValue(SynapseConstants.SYNAPSE_ENV));
        return endpoint;
    }

    /**
     * This FaultHandler will try to resend the message to another member if an error occurs
     * while sending to some member. This is a failover mechanism
     */
    private class DynamicLoadbalanceFaultHandler extends FaultHandler {

        private EndpointReference to;
        private Member currentMember;

        public void setCurrentMember(Member currentMember) {
            this.currentMember = currentMember;
        }

        private DynamicLoadbalanceFaultHandler(EndpointReference to) {
            this.to = to;
        }

        public void onFault(MessageContext synCtx) {
            if (currentMember == null) {
                return;
            }
            synCtx.getFaultStack().pop(); // Remove the LoadbalanceFaultHandler
            currentMember = lbMembershipHandler.getNextApplicationMember(algorithmContext);
            if(currentMember == null){
                String msg = "No application members available";
                log.error(msg);
                throw new SynapseException(msg);
            }
            synCtx.setTo(to);
            if(isSessionAffinityBasedLB()){
                //We are sending the this message on a new session,
                // hence we need to remove previous session information
                Set pros = synCtx.getPropertyKeySet();
                if (pros != null) {
                    pros.remove(SynapseConstants.PROP_SAL_CURRENT_SESSION_INFORMATION);
                }
            }
            sendToApplicationMember(synCtx, currentMember, true);
        }
    }
}
