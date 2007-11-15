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
package org.apache.synapse.mediators.throttle;

import org.apache.axiom.om.OMElement;
import org.apache.neethi.PolicyEngine;
import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.transport.nhttp.NhttpConstants;
import org.apache.synapse.config.Entry;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.clustering.context.Replicator;
import org.apache.axis2.clustering.ClusteringFault;
import org.wso2.throttle.*;

import java.util.HashMap;
import java.util.Map;


/**
 * The Mediator for the throttling - Throtting will occur according to the ws-policy which is specified as
 * the key for lookup from the registry or the inline policy
 * Only support IP based throttling- Throotling can manage per IP using the throttle policy
 */

public class ThrottleMediator extends AbstractMediator {

    private static final String KEY = "keyOfthrottles";
    /**
     * The key for getting the throttling policy - key refers to a/an [registry] entry
     */
    private String policyKey = null;
    /**
     * InLine policy object - XML
     */
    private OMElement inLinePolicy = null;
    /**
     * The reference to the sequence which will execute when access is denied
     */
    private String onRejectSeqKey = null;
    /**
     * The in-line sequence which will execute when access is denied
     */
    private Mediator onRejectMediator = null;
    /**
     * The reference to the sequence which will execute when access is allowed
     */
    private String onAcceptSeqKey = null;
    /**
     * The in-line sequence which will execute when access is allowed
     */
    private Mediator onAcceptMediator = null;
    /**
     * The concurrect access control group id
     */
    private String id;
    /**
     * Access rate controller
     */
    private AccessController accessControler;

    private final Object throttleLock = new Object();

    public ThrottleMediator() {
        this.accessControler = new AccessController();
    }

    public boolean mediate(MessageContext synCtx) {

        boolean traceOn = isTraceOn(synCtx);
        boolean traceOrDebugOn = isTraceOrDebugOn(traceOn);
        boolean isResponse = synCtx.isResponse();
        Throttle throttle = null;
        ConcurrentAccessController concurrentAccessController = null;
        ConfigurationContext configctx;
        Object remoteIP;
        String domainName;

        if (traceOrDebugOn) {
            traceOrDebug(traceOn, "Start : Throttle mediator");

            if (traceOn && trace.isTraceEnabled()) {
                trace.trace("Message : " + synCtx.getEnvelope());
            }
        }
        synchronized (throttleLock) {
            
            org.apache.axis2.context.MessageContext axis2MessageContext
                = ((Axis2MessageContext) synCtx).getAxis2MessageContext();
            configctx = axis2MessageContext.getConfigurationContext();
            remoteIP = axis2MessageContext.getProperty(
                org.apache.axis2.context.MessageContext.REMOTE_ADDR);
            domainName = (String) axis2MessageContext.getProperty(NhttpConstants.REMOTE_HOST);
            //all the throttle states are in a map which itself in config context
            Map throttles = (Map) configctx.getProperty(KEY);
            if (throttles != null) {
                if (throttles.containsKey(id)) {
                    throttle = (Throttle) throttles.get(id);
                }
            } else {
                throttles = new HashMap();
                configctx.setProperty(KEY, throttles);
            }
            // Throttle only will be created ,if the massage flow is IN
            if (!isResponse) {
                // for request messages, read the policy for throttling and initialize
                if (inLinePolicy != null) {
                    // this uses a static policy
                    if (throttle == null) {
                        if (traceOn && trace.isTraceEnabled()) {
                            trace.trace("Initializing using static throttling policy : " + inLinePolicy);
                        }
                        try {
                            throttle = ThrottlePolicyProcessor.processPolicy(
                                PolicyEngine.getPolicy(inLinePolicy));
                            if (throttle != null) {
                                throttles.put(id, throttle);
                            }
                        } catch (ThrottleException e) {
                            handleException("Error processing the throttling policy", e, synCtx);
                        }
                    }

                } else if (policyKey != null) {

                    // load or re-load policy from registry or local entry if not already available
                    Entry entry = synCtx.getConfiguration().getEntryDefinition(policyKey);
                    if (entry == null) {
                        handleException("Cannot find throttling policy using key : " + policyKey, synCtx);

                    } else {
                        boolean reCreate = false;
                        // if the key refers to a dynamic resource
                        if (entry.isDynamic()) {
                            if (!entry.isCached() || entry.isExpired()) {
                                reCreate = true;
                            }
                        }
                        if (reCreate || throttle == null) {
                            Object entryValue = synCtx.getEntry(policyKey);
                            if (entryValue == null) {
                                handleException(
                                    "Null throttling policy returned by Entry : " + policyKey, synCtx);

                            } else {
                                if (!(entryValue instanceof OMElement)) {
                                    handleException("Policy returned from key : " + policyKey +
                                        " is not an OMElement", synCtx);

                                } else {
                                    try {
                                        throttle = ThrottlePolicyProcessor.processPolicy(
                                            PolicyEngine.getPolicy((OMElement) entryValue));
                                        if (throttle != null) {
                                            throttles.put(id, throttle);
                                        }
                                    } catch (ThrottleException e) {
                                        handleException("Error processing the throttling policy", e, synCtx);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // get the access controller
            if (throttle != null) {
                concurrentAccessController =
                    throttle.getConcurrentAccessController();
            }
        }

        boolean result = true;

        if (throttle != null) {

            if (concurrentAccessController != null) {
                // do the concurrecy throttling
                int concurrentLimit = concurrentAccessController.getLimit();
                if (traceOrDebugOn) {
                    traceOrDebug(traceOn, "Concurrent access controller for ID : " + id +
                        " allows : " + concurrentLimit + " concurrent accesses");
                }
                int available = 0;
                if (!isResponse) {
                    available = concurrentAccessController.getAndDecrement();
                    result = available > 0;
                    if (traceOrDebugOn) {
                        traceOrDebug(traceOn, "Access " + (result ? "allowed" : "denied") +
                            " :: " + available + " of available of " + concurrentLimit + " connections");
                    }
                } else {
                    available = concurrentAccessController.incrementAndGet();
                    if (traceOrDebugOn) {
                        traceOrDebug(traceOn, "Connection returned" +
                            " :: " + available + " of available of " + concurrentLimit + " connections");
                    }
                }
            }

            if (!isResponse && result) {

                ThrottleContext throttleContext = null;

                if (domainName != null) {
                    // do the domain based throttling
                    if (traceOrDebugOn) {
                        traceOrDebug(traceOn, "The Domain Name of the caller is :" + domainName);
                    }
                    throttleContext
                        = throttle.getThrottleContext(ThrottleConstants.DOMAIN_BASED_THROTTLE_KEY);

                    if (throttleContext != null) {
                        try {
                            result = accessControler.canAccess(throttleContext,
                                domainName, ThrottleConstants.DOMAIN_BASE);
                            if (traceOrDebugOn) {
                                traceOrDebug(traceOn, "Access " + (result ? "allowed" : "denied")
                                    + " for Domain Name : " + domainName);
                            }
                            if (!result && concurrentAccessController != null) {
                                concurrentAccessController.incrementAndGet();
                            }
                        } catch (ThrottleException e) {
                            handleException("Error occurd during throttling", e, synCtx);
                        }
                    }
                } else {
                    if (traceOrDebugOn) {
                        traceOrDebug(traceOn, "The Domain name of the caller cannot be found");
                    }
                }

                if (throttleContext == null) {
                    //do the IP-based throttling
                    if (remoteIP == null) {
                        if (traceOrDebugOn) {
                            traceOrDebug(traceOn, "The IP address of the caller cannot be found");
                        }
                        result = true;

                    } else {
                        if (traceOrDebugOn) {
                            traceOrDebug(traceOn, "The IP Address of the caller is :" + remoteIP);
                        }
                        try {
                            throttleContext =
                                throttle.getThrottleContext(ThrottleConstants.IP_BASED_THROTTLE_KEY);

                            if (throttleContext != null) {

                                result = accessControler.canAccess(throttleContext,
                                    remoteIP, ThrottleConstants.IP_BASE);

                                if (traceOrDebugOn) {
                                    traceOrDebug(traceOn, "Access " + (result ? "allowed" : "denied")
                                        + " for IP : " + remoteIP);
                                }

                                if (!result && concurrentAccessController != null) {
                                    concurrentAccessController.incrementAndGet();
                                }
                            }
                        } catch (ThrottleException e) {
                            handleException("Error occurd during throttling", e, synCtx);
                        }
                    }
                }
            }
            //replicate the current state
            if (configctx != null) {
                try {
                    Replicator.replicate(configctx);
                } catch (ClusteringFault clusteringFault) {
                    handleException("Error during replicate states ", clusteringFault, synCtx);
                }
            }
        }

        if (result) {
            if (onAcceptSeqKey != null) {
                Mediator mediator = synCtx.getSequence(onAcceptSeqKey);
                if (mediator != null) {
                    return mediator.mediate(synCtx);
                } else {
                    handleException("Unable to find onAccept sequence with key : "
                        + onAcceptSeqKey, synCtx);
                }
            } else if (onAcceptMediator != null) {
                return onAcceptMediator.mediate(synCtx);
            } else {
                return true;
            }

        } else {
            if (onRejectSeqKey != null) {
                Mediator mediator = synCtx.getSequence(onRejectSeqKey);
                if (mediator != null) {
                    return mediator.mediate(synCtx);
                } else {
                    handleException("Unable to find onReject sequence with key : "
                        + onRejectSeqKey, synCtx);
                }
            } else if (onRejectMediator != null) {
                return onRejectMediator.mediate(synCtx);
            } else {
                return false;
            }
        }

        if (traceOrDebugOn) {
            traceOrDebug(traceOn, "End : Throttle mediator");
        }
        return result;
    }

    public String getType() {
        return ThrottleMediator.class.getName();
    }

    /**
     * To get the policy key - The key for which will used to lookup policy from the registry
     *
     * @return String
     */

    public String getPolicyKey() {
        return policyKey;
    }

    /**
     * To set the policy key - The key for which lookup from the registry
     *
     * @param policyKey Key for picking policy from the registry
     */
    public void setPolicyKey(String policyKey) {
        this.policyKey = policyKey;
    }

    /**
     * getting throttle policy which has defined as InLineXML
     *
     * @return InLine Throttle Policy
     */
    public OMElement getInLinePolicy() {
        return inLinePolicy;
    }

    /**
     * setting throttle policy which has defined as InLineXML
     *
     * @param inLinePolicy Inline policy
     */
    public void setInLinePolicy(OMElement inLinePolicy) {
        this.inLinePolicy = inLinePolicy;
    }

    public String getOnRejectSeqKey() {
        return onRejectSeqKey;
    }

    public void setOnRejectSeqKey(String onRejectSeqKey) {
        this.onRejectSeqKey = onRejectSeqKey;
    }

    public Mediator getOnRejectMediator() {
        return onRejectMediator;
    }

    public void setOnRejectMediator(Mediator onRejectMediator) {
        this.onRejectMediator = onRejectMediator;
    }

    public String getOnAcceptSeqKey() {
        return onAcceptSeqKey;
    }

    public void setOnAcceptSeqKey(String onAcceptSeqKey) {
        this.onAcceptSeqKey = onAcceptSeqKey;
    }

    public Mediator getOnAcceptMediator() {
        return onAcceptMediator;
    }

    public void setOnAcceptMediator(Mediator onAcceptMediator) {
        this.onAcceptMediator = onAcceptMediator;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
