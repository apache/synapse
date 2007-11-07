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
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.Entry;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.axis2.context.ConfigurationContext;
import org.wso2.throttle.*;
import org.wso2.throttle.factory.AccessControllerFactory;

import java.util.HashMap;
import java.util.Map;


/**
 * The Mediator for the throttling - Throtting will occur according to the ws-policy which is specified as
 * the key for lookup from the registry or the inline policy
 * Only support IP based throttling- Throotling can manage per IP using the throttle policy
 */

public class ThrottleMediator extends AbstractMediator {

    private static final String  KEY ="keyOfConcurrentAcessControllers";
    /** The key for getting the throttling policy - key refers to a/an [registry] entry  */
    private String policyKey = null;
    /** InLine policy object - XML */
    private OMElement inLinePolicy = null;
    /** The throttle - hold runtime + configuration data of throttle */
    private Throttle throttle = null;
    /** The reference to the sequence which will execute when access is denied */
    private String onRejectSeqKey = null;
    /** The in-line sequence which will execute when access is denied */
    private Mediator onRejectMediator = null;
    /** The reference to the sequence which will execute when access is allowed */
    private String onAcceptSeqKey = null;
    /** The in-line sequence which will execute when access is allowed */
    private Mediator onAcceptMediator = null;
    /** The concurrect access control group id */
    private String id;
    /** The ConcurrentAccessController for this mediator instance */
    private ConcurrentAccessController concurrentAccessController;
    /** Does my configuration state an IP based throttling policy segment */
    private boolean includesIPThrottling = false;
    /** The concurrent connection limit */
    private int concurrentLimit;
    /** Access rate controller*/
    private AccessController accessControler;

    public ThrottleMediator() {
        try {
            this.accessControler =
                AccessControllerFactory.createAccessControler(ThrottleConstants.IP_BASE);
        } catch (ThrottleException e) {
            String msg = "Error occurred when creating an accesscontroller";
            log.error(msg, e);
            throw new SynapseException(msg, e);
        }
    }

    public boolean mediate(MessageContext synCtx) {

        boolean traceOn = isTraceOn(synCtx);
        boolean traceOrDebugOn = isTraceOrDebugOn(traceOn);

        if (traceOrDebugOn) {
            traceOrDebug(traceOn, "Start : Throttle mediator");

            if (traceOn && trace.isTraceEnabled()) {
                trace.trace("Message : " + synCtx.getEnvelope());
            }
        }

        if (!synCtx.isResponse()) {
            // for request messages, read the policy for throttling and initialize
            initThrottle(synCtx, traceOrDebugOn, traceOn);

        } else {
            // for response messages, load the concurrent access controller object
            // do this ONLY ONCE - the first time when our iniial reference is null
            if (concurrentAccessController == null && id != null) {

                org.apache.axis2.context.MessageContext axis2MessageContext
                    = ((Axis2MessageContext) synCtx).getAxis2MessageContext();

                ConfigurationContext configctx = axis2MessageContext.getConfigurationContext();

                Map accessContollers = (Map) configctx.getProperty(KEY);

                if (accessContollers != null) {

                    concurrentAccessController =
                        (ConcurrentAccessController) accessContollers.get(id);

                    if (concurrentAccessController != null) {

                        concurrentLimit = concurrentAccessController.getLimit();
                        if (traceOrDebugOn) {
                            traceOrDebug(traceOn, "Concurrent access controller for ID : " + id +
                                " allows : " + concurrentLimit + " concurrent accesses");
                        }
                    }
                }
            }
        }

        // check access allow or not
        boolean result = canAccess(synCtx, traceOrDebugOn, traceOn);

        if (traceOrDebugOn) {
            traceOrDebug(traceOn, "End : Throttle mediator");
        }
        return result;
    }

    /**
     * To check whether allow access or not for caller
     * Current Implementaion only support IP Based Throttling
     *
     * @param synContext Current Message Context
     * @param traceOn    indicate whether trace is eanabled or not
     * @return boolean which indicate whether this caller can or not access
     */
    private boolean canAccess(MessageContext synContext, boolean traceOrDebugOn, boolean traceOn) {

        boolean isResponse = synContext.isResponse();
        // do the concurrent throttling
        boolean isAllowed = throttleByConcurrency(isResponse, traceOrDebugOn, traceOn);

        if (includesIPThrottling && isAllowed && !isResponse) {
            // do the normal throttling
            isAllowed = throttleByRate(synContext, traceOrDebugOn, traceOn);
        }

        if (isAllowed) {
            if (onAcceptSeqKey != null) {
                Mediator mediator = synContext.getSequence(onAcceptSeqKey);
                if (mediator != null) {
                    return mediator.mediate(synContext);
                } else {
                    handleException("Unable to find onAccept sequence with key : "
                        + onAcceptSeqKey, synContext);
                }
            } else if (onAcceptMediator != null) {
                return onAcceptMediator.mediate(synContext);
            } else {
                return true;
            }

        } else {
            if (onRejectSeqKey != null) {
                Mediator mediator = synContext.getSequence(onRejectSeqKey);
                if (mediator != null) {
                    return mediator.mediate(synContext);
                } else {
                    handleException("Unable to find onReject sequence with key : "
                        + onRejectSeqKey, synContext);
                }
            } else if (onRejectMediator != null) {
                return onRejectMediator.mediate(synContext);
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * Doing concurrency throttlling
     *
     * @param isResponse     indicate whether message flow is OUT or IN
     * @param traceOrDebugOn is tracing or debbug on
     * @param traceOn        indicate whether trace is ON or OFF
     * @return True if message can continue ,otherwise false
     */
    private boolean throttleByConcurrency(boolean isResponse, boolean traceOrDebugOn, boolean traceOn) {

        if (concurrentAccessController != null) {
            int available = 0;
            if (!isResponse) {
                available = concurrentAccessController.getAndDecrement();
                if (traceOrDebugOn) {
                    traceOrDebug(traceOn, "Access " + (available > 0 ? "allowed" : "denied") +
                        " :: " + available + " of available of " + concurrentLimit + " connections");
                }
                return available > 0;
            } else {
                available = concurrentAccessController.incrementAndGet();
                if (traceOrDebugOn) {
                    traceOrDebug(traceOn, "Connection returned" +
                        " :: " + available + " of available of " + concurrentLimit + " connections");
                }
                return true;
            }
        } else {
            return true;
        }
    }

    /**
     * Processing throughh IP based throttle
     *
     * @param synContext     Current Message
     * @param traceOrDebugOn
     * @param traceOn        Indicates whether trace is ON or OFF
     * @return True if message can continue ,otherwise false
     */
    private boolean throttleByRate(MessageContext synContext, boolean traceOrDebugOn, boolean traceOn) {

        if (throttle == null) {
            handleException("Cannot find throttle object instance", synContext);
            return true;
        }

        org.apache.axis2.context.MessageContext axis2MessageContext
            = ((Axis2MessageContext) synContext).getAxis2MessageContext();
        // IP based throttling
        Object remoteIP = axis2MessageContext.getProperty(
            org.apache.axis2.context.MessageContext.REMOTE_ADDR);

        if (remoteIP == null) {
            if (traceOrDebugOn) {
                traceOrDebug(traceOn, "The IP address of the caller cannot be found");
            }
            return true;

        } else {
            if (traceOrDebugOn) {
                traceOrDebug(traceOn, "The IP Address of the caller is :" + remoteIP);
            }

            ThrottleContext throttleContext
                = throttle.getThrottleContext(ThrottleConstants.IP_BASED_THROTTLE_KEY);
            if (throttleContext == null) {
                if (traceOrDebugOn) {
                    traceOrDebug(traceOn, "Cannot find the configuartion context for IP throttle");
                }
                return true;
            }

            try {
                boolean canAccess = accessControler.canAccess(throttleContext, remoteIP);
                if (traceOrDebugOn) {
                    traceOrDebug(traceOn, "Access " + (canAccess ? "allowed" : "denied")
                        + " for IP : " + remoteIP);
                }
                return canAccess;

            } catch (ThrottleException e) {
                handleException("Error occurd during throttling", e, synContext);
            }
        }
        return true;
    }

    /**
     * To init throttle with the policy
     * If the policy is defined as a Registry key ,then Policy will only process after it has expired
     * Any runtime changes to the policy will take effect
     * If the policy is defined as a Inline XML ,then only one time policy will process and any runtime
     * changes to the policy will not reflect
     *
     * @param synCtx         Current Message
     * @param traceOrDebugOn is tracing or debug on?
     * @param traceOn        is tracing on?
     */
    protected void initThrottle(MessageContext synCtx, boolean traceOrDebugOn, boolean traceOn) {

        if (inLinePolicy != null) {
            // this uses a static policy
            if (throttle == null) {
                if (traceOn && trace.isTraceEnabled()) {
                    trace.trace("Initializing using static throttling policy : " + inLinePolicy);
                }
                createThrottleMetaData(inLinePolicy, synCtx, traceOrDebugOn, traceOn, false);
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
                createThrottleMetaData(
                    null, synCtx, traceOrDebugOn, traceOn, reCreate);
            }
        }
    }

    /**
     * Create the throttling policy and the "Throttle" object applicable. If this is a
     * concurrent throttling instance, set the throttling access controller to the shared
     * map
     *
     * @param policy         throttling policy
     * @param synCtx         incoming message
     * @param traceOrDebugOn is tracing or debug on?
     * @param traceOn        is tracing on?
     * @param reCreate       is it need to reCreate the throttle
     */
    private synchronized void createThrottleMetaData(OMElement policy,
                                                     MessageContext synCtx, boolean traceOrDebugOn, boolean traceOn, boolean reCreate) {

        if (!reCreate && throttle != null) {
            // this uses a static policy, and one thread has already created the "Throttle"
            // object, just return...
            return;
        }
        try {
            if (traceOrDebugOn) {
                traceOrDebug(traceOn, "Creating a new throttle configuration by parsing the Policy");
            }
            if (policy != null) {
                throttle = ThrottlePolicyProcessor.processPolicy(
                    PolicyEngine.getPolicy(policy));
            } else {
                Object entryValue = synCtx.getEntry(policyKey);
                if (entryValue == null) {
                    handleException(
                        "Null throttling policy returned by Entry : " + policyKey, synCtx);

                } else {
                    if (!(entryValue instanceof OMElement)) {
                        handleException("Policy returned from key : " + policyKey +
                            " is not an OMElement", synCtx);

                    } else {
                        throttle = ThrottlePolicyProcessor.processPolicy(
                            PolicyEngine.getPolicy((OMElement) entryValue));
                    }
                }
            }

            if (throttle != null) {

                includesIPThrottling = (
                    throttle.getThrottleContext(ThrottleConstants.IP_BASED_THROTTLE_KEY) != null);

                if (id != null) {
                    concurrentAccessController = throttle.getConcurrentAccessController();

                    org.apache.axis2.context.MessageContext axis2MessageContext
                        = ((Axis2MessageContext) synCtx).getAxis2MessageContext();
                    ConfigurationContext configctx = axis2MessageContext.getConfigurationContext();
                    Map accessContollers = (Map) configctx.getProperty(KEY);

                    if (accessContollers == null) {
                        accessContollers = new HashMap();
                        configctx.setProperty(KEY, accessContollers);
                    }
                    if (concurrentAccessController == null) {
                        accessContollers.remove(id);
                    } else {
                        concurrentLimit = concurrentAccessController.getLimit();
                        if (traceOrDebugOn) {
                            traceOrDebug(traceOn,
                                "Initiating ConcurrentAccessControler for throttle group id : " + id
                                    + " limit : " + concurrentLimit);
                        }
                        accessContollers.put(id, concurrentAccessController);
                    }
                }
            }
        }
        catch (ThrottleException e) {
            handleException("Error processing the throttling policy", e, synCtx);
        }
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
