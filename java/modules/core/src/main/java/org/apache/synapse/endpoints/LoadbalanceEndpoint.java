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
import org.apache.axis2.clustering.ClusterManager;
import org.apache.axis2.clustering.Member;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.FaultHandler;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.endpoints.algorithms.AlgorithmContext;
import org.apache.synapse.endpoints.algorithms.LoadbalanceAlgorithm;
import org.apache.synapse.endpoints.utils.EndpointDefinition;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.TimerTask;
import java.util.Timer;
import java.util.ArrayList;
import java.io.IOException;

/**
 * Load balance endpoint can have multiple endpoints. It will route messages according to the
 * specified load balancing algorithm. This will assume that all immediate child endpoints are
 * identical in state (state is replicated) or state is not maintained at those endpoints. If an
 * endpoint is failing, the failed endpoint is marked as inactive and the message to the next
 * endpoint obtained using the load balancing algorithm. If all the endpoints have failed and the
 * parent endpoint is available, onChildEndpointFail(...) method of parent endpoint is called. If
 * parent is not available, this will call next FaultHandler for the message context.
 */
public class LoadbalanceEndpoint implements Endpoint {

    private static final Log log = LogFactory.getLog(LoadbalanceEndpoint.class);
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
     * List of currently available application members amongst which the load is distributed
     */
    private List<Member> activeMembers = null;

    /**
     * List of currently unavailable members
     */
    private List<Member> inactiveMembers = null;

    /**
     * Algorithm used for selecting the next endpoint to direct the load. Default is RoundRobin.
     */
    private LoadbalanceAlgorithm algorithm = null;

    /**
     * If this supports load balancing with failover. If true, request will be directed to the next
     * endpoint if the current one is failing.
     */
    protected boolean failover = true;

    /**
     * Parent endpoint of this endpoint if this used inside another endpoint. Possible parents are
     * LoadbalanceEndpoint, SALoadbalanceEndpoint and FailoverEndpoint objects.
     */
    private Endpoint parentEndpoint = null;

    /**
     * The endpoint context , place holder for keep any runtime states related to the endpoint
     */
    private final EndpointContext endpointContext = new EndpointContext();

    /**
     * The algorithm context , place holder for keep any runtime states related to the load balance
     * algorithm
     */
    private final AlgorithmContext algorithmContext = new AlgorithmContext();

    public void startApplicationMembershipTimer(){
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new MemberActivatorTask(), 1000, 500);
    }

    public void send(MessageContext synMessageContext) {

        if (log.isDebugEnabled()) {
            log.debug("Start : Load-balance Endpoint");
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

        String endPointName = this.getName();
        if (endPointName == null) {

            if (isClusteringEnable) {
                log.warn("In a clustering environment , the endpoint  name should be specified" +
                         "even for anonymous endpoints. Otherwise , the clustering would not be " +
                         "functioned correctly if there are more than one anonymous endpoints. ");
            }
            endPointName = SynapseConstants.ANONYMOUS_ENDPOINT;
        }

        if (isClusteringEnable) {

            // if this is a cluster environment , then set configuration context to endpoint context
            if (endpointContext.getConfigurationContext() == null) {
                endpointContext.setConfigurationContext(cc);
                endpointContext.setContextID(endPointName);

            }
            // if this is a cluster environment , then set configuration context to load balance
            //  algorithm context
            if (algorithmContext.getConfigurationContext() == null) {
                algorithmContext.setConfigurationContext(cc);
                algorithmContext.setContextID(endPointName);
            }
        }

        if (endpoints != null) {
            sendToEndpoint(synMessageContext);
        } else if (activeMembers != null) {
            EndpointReference to = synMessageContext.getTo();
            LoadbalanceFaultHandler faultHandler = new LoadbalanceFaultHandler(to);
            if (failover) {
                synMessageContext.pushFaultHandler(faultHandler);
            }
            sendToApplicationMember(synMessageContext, to, faultHandler);
        }
    }

    private void sendToEndpoint(MessageContext synMessageContext) {
        Endpoint endpoint = algorithm.getNextEndpoint(synMessageContext, algorithmContext);
        if (endpoint != null) {

            // We have to build the envelop if we are supporting failover.
            // Failover should sent the original message multiple times if failures occur. So we
            // have to access the envelop multiple times.
            if (failover) {
                synMessageContext.getEnvelope().build();
            }

            endpoint.send(synMessageContext);

        } else {
            // there are no active child endpoints. so mark this endpoint as failed.
            setActive(false, synMessageContext);

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

    private void sendToApplicationMember(MessageContext synCtx,
                                         EndpointReference to,
                                         LoadbalanceFaultHandler faultHandler) {
        org.apache.axis2.context.MessageContext axis2MsgCtx =
                ((Axis2MessageContext) synCtx).getAxis2MessageContext();

        String transport = axis2MsgCtx.getTransportIn().getName();
        algorithm.setApplicationMembers(activeMembers);
        Member currentMember = algorithm.getNextApplicationMember(algorithmContext);
        faultHandler.setCurrentMember(currentMember);

        if (currentMember != null) {

            // URL rewrite
            if (transport.equals("http") || transport.equals("https")) {
                String address = to.getAddress();
                if (address.indexOf(":") != -1) {
                    try {
                        address = new URL(address).getPath();
                    } catch (MalformedURLException e) {
                        String msg = "URL " + address + " is malformed";
                        log.error(msg, e);
                        throw new SynapseException(msg, e);
                    }
                }
                EndpointReference epr =
                        new EndpointReference(transport + "://" + currentMember.getHostName() +
                                              ":" + currentMember.getHttpPort() + address);
                synCtx.setTo(epr);
                if (failover) {
                    synCtx.getEnvelope().build();
                }

                AddressEndpoint endpoint = new AddressEndpoint();
                EndpointDefinition definition = new EndpointDefinition();
                endpoint.setEndpoint(definition);
                endpoint.send(synCtx);
            } else {
                log.error("Cannot load balance for non-HTTP/S transport " + transport);
            }
        } else {
            synCtx.getFaultStack().pop(); // Remove the LoadbalanceFaultHandler
            String msg = "No application members available";
            log.error(msg);
            throw new SynapseException(msg);
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

    public void setMembers(List<Member> members) {
        this.activeMembers = members;
        this.inactiveMembers = new ArrayList<Member>();
    }

    public List<Member> getAllMembers() {
        List<Member> members = new ArrayList<Member>();
        if (activeMembers != null) {
            for (Member member:activeMembers) {
                if(!members.contains(member)){
                    members.add(member);
                }
            }
        }
        if (inactiveMembers != null) {
            for (Member member:inactiveMembers) {
                if(!members.contains(member)){
                    members.add(member);
                }
            }
        }
        return members;
    }
    
    /**
     * If this endpoint is in inactive state, checks if all immediate child endpoints are still
     * failed. If so returns false. If at least one child endpoint is in active state, sets this
     * endpoint's state to active and returns true. As this a sessionless load balancing endpoint
     * having one active child endpoint is enough to consider this as active.
     *
     * @param synMessageContext MessageContext of the current message. This is not used here.
     * @return true if active. false otherwise.
     */
    public boolean isActive(MessageContext synMessageContext) {
        boolean active = endpointContext.isActive();
        if (!active && endpoints != null) {
            for (Endpoint endpoint : endpoints) {
                if (endpoint.isActive(synMessageContext)) {
                    active = true;
                    endpointContext.setActive(true);

                    // don't break the loop though we found one active endpoint. calling isActive()
                    // on all child endpoints will update their active state. so this is a good
                    // time to do that.
                }
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Endpoint  '" + name + "' is in state ' " + active + " '");
        }

        return active;
    }

    public void setActive(boolean active, MessageContext synMessageContext) {
        // setting a volatile boolean variable is thread safe.
        endpointContext.setActive(active);
    }

    public boolean isFailover() {
        return failover;
    }

    public void setFailover(boolean failover) {
        this.failover = failover;
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

    public void onChildEndpointFail(Endpoint endpoint, MessageContext synMessageContext) {

        // resend (to a different endpoint) only if we support failover
        if (failover) {
            send(synMessageContext);
        } else {
            // we are not informing this to the parent endpoint as the failure of this loadbalance
            // endpoint. there can be more active endpoints under this, and current request has
            // failed only because the currently selected child endpoint has failed AND failover is
            // turned off in this load balance endpoint. so just call the next fault handler.
            Object o = synMessageContext.getFaultStack().pop();
            if (o != null) {
                ((FaultHandler) o).handleFault(synMessageContext);
            }
        }
    }

    /**
     * This FaultHandler will try to resend the message to another member if an error occurs
     * while sending to some member. This is a failover mechanism
     */
    private class LoadbalanceFaultHandler extends FaultHandler {

        private EndpointReference to;
        private Member currentMember;

        public void setCurrentMember(Member currentMember) {
            this.currentMember = currentMember;
        }

        private LoadbalanceFaultHandler(EndpointReference to) {
            this.to = to;
        }

        public void onFault(MessageContext synCtx) {
            if (currentMember == null) {
                return;
            }
            synCtx.pushFaultHandler(this);
            activeMembers.remove(currentMember); // This member has to be inactivated
            inactiveMembers.add(currentMember);
            sendToApplicationMember(synCtx, to, this);
        }
    }

    /**
     * The task which checks whther inactive members have become available again 
     */
    private class MemberActivatorTask extends TimerTask{

        public void run() {
            try {
                for(Member member: inactiveMembers){
                    if(canConnect(member)){
                        inactiveMembers.remove(member);
                        activeMembers.add(member);
                    }
                }
            } catch (Exception ignored) {
                // Ignore all exceptions. The timer should continue to run
            }
        }

        /**
         * Before activating a member, we will try to verify whether we can connect to it
         *
         * @param member The member whose connectvity needs to be verified
         * @return true, if the member can be contacted; false, otherwise.
         */
        private boolean canConnect(Member member) {
            if(log.isDebugEnabled()){
                log.debug("Trying to connect to member " + member.getHostName() + "...");
            }
            for (int retries = 30; retries > 0; retries--) {
                try {
                    InetAddress addr = InetAddress.getByName(member.getHostName());
                    int httpPort = member.getHttpPort();
                    if(log.isDebugEnabled()){
                        log.debug("HTTP Port=" + httpPort);
                    }
                    if (httpPort != -1) {
                        SocketAddress httpSockaddr = new InetSocketAddress(addr, httpPort);
                        new Socket().connect(httpSockaddr, 10000);
                    }
                    int httpsPort = member.getHttpsPort();
                    if(log.isDebugEnabled()){
                        log.debug("HTTPS Port=" + httpPort);
                    }
                    if (httpsPort != -1) {
                        SocketAddress httpsSockaddr = new InetSocketAddress(addr, httpsPort);
                        new Socket().connect(httpsSockaddr, 10000);
                    }
                    return true;
                } catch (IOException e) {
                    if(log.isDebugEnabled()){
                        log.debug("", e);
                    }
                    String msg = e.getMessage();
                    if (msg.indexOf("Connection refused") == -1 &&
                        msg.indexOf("connect timed out") == -1) {
                        log.error("Cannot connect to member " + member, e);
                    }
                }
            }
            return false;
        }
    }
}
