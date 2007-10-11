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
import org.wso2.throttle.*;
import org.wso2.throttle.factory.AccessControllerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 * The Mediator for the throttling - Throtting will occur according to the ws-policy which is specified as
 * the key for lookup from the registry or the inline policy
 * Only support IP based throttling- Throotling can manage per IP using the throttle policy
 */

public class ThrottleMediator extends AbstractMediator {

    /** static map to share all concurrent access controllers */
    public final static Map CONCURRENT_ACCESS_CONTROLLERS = Collections.synchronizedMap(new HashMap());

    /** The key for getting policy value - key refer to registry entry  */
    private String policyKey = null;

    /** InLine policy object - XML   */
    private OMElement inLinePolicy = null;

    /** The throttle - hold runtime + configuration data of throttle  */
    private Throttle throttle = null;

    /** The reference to the sequence which will execute when access deny*/
    private String onReject = null;

    /** The in-line sequence which will execute when access deny*/
    private Mediator onRejectMediator = null;

    /** The reference to the sequence which will execute when access accept */
    private String onAccept  = null;

    /** The in-line sequence which will execute when access accept */
    private Mediator onAcceptMediator = null;

    /** The concurrect access control group id */
    private String ID ;

    /** is this initiator of the concurrent access controller*/
    private boolean isInitiator = false;

    /** Lock used to ensure thread-safe creation and use of the above Transformer  */
    private final Object throttleLock = new Object();

    /** The ConcurrentAccessController cache */
    private ConcurrentAccessController concurrentAccessController;

    /* check to debug log level whether currently enable or not */
    private boolean debugOn;

    public ThrottleMediator() {
        this.debugOn = log.isDebugEnabled();
    }

    public boolean mediate(MessageContext synCtx) {
        boolean shouldTrace = shouldTrace(synCtx.getTracingState());
        try {
            if (shouldTrace) {
                trace.trace("Start : Throttle mediator");
            }
            synchronized (throttleLock) {
                //init method to init throttle
                initThrottle(synCtx, shouldTrace);
                if (concurrentAccessController == null && ID != null) {
                    lookupConcurrentAccessController();
                }
            }
            // check access allow or not
            return canAccess(synCtx, shouldTrace);
        } finally {
            if (shouldTrace) {
                trace.trace("End : Throttle mediator");
            }
        }
    }

    /**
     * To check whether allow access or not for caller
     * Current Implementaion only support IP Based Throttling
     *
     * @param synContext Current Message Context
     * @param shouldTrace indicate whether trace is eanabled or not
     * @return boolean which indicate whether this caller can or not access
     */
    protected boolean canAccess(MessageContext synContext, boolean shouldTrace) {

        boolean isResponse = synContext.isResponse();
        // do the concurrent throttling
        boolean canAccess = doConcurrentThrottling(isResponse, shouldTrace);
        if (canAccess) { // if the access is success then
            if (debugOn) {
                log.debug("Access success from concurrent throttlling");
            }
            if (!isResponse) {
                // do the normal throttling 
                canAccess = doThrottling(synContext, shouldTrace);
            }
        } else {
            if (debugOn) {
                log.debug("Access deny from concurrent throttlling");
            }
        }

        if (canAccess) {
            if (onAccept != null) {
                Mediator mediator = synContext.getSequence(onAccept);
                if (mediator != null) {
                    return mediator.mediate(synContext);
                } else {
                    return true;
                }
            } else if (onAcceptMediator != null) {
                return onAcceptMediator.mediate(synContext);
            } else {
                return true;
            }
        } else {
            if (onReject != null) {
                Mediator mediator = synContext.getSequence(onReject);
                if (mediator != null) {
                    return mediator.mediate(synContext);
                } else {
                    return false;
                }
            } else if (onRejectMediator != null) {
                return onRejectMediator.mediate(synContext);
            } else {
                return false;
            }
        }
    }

    /**
     * Doing concurrency throttlling
     * @param isResponse indicate whether message flow is OUT or IN
     * @param shouldTrace indicate whether trace is ON or OFF
     * @return True if message can continue ,otherwise false
     */
    private boolean doConcurrentThrottling(boolean isResponse, boolean shouldTrace) {

        boolean canAccess = true;
        if (concurrentAccessController != null) {
            if (!isResponse) {
                if (debugOn) {
                    log.debug("Incoming message process through the ConcurrentThrottlling");
                }
                canAccess = concurrentAccessController.beforeAccess();
                if (debugOn) {
                    if (!canAccess) {
                        log.debug("Access has currently been denied since allowed maximum concurrent access has exceeded");
                    }
                }
            } else {
                if (debugOn) {
                    log.debug("Outcoming message process through the ConcurrentThrottlling");
                }
                canAccess = concurrentAccessController.afterAccess();
            }
        }
        return canAccess;
    }

    /**
     * Processing throughh IP based throttle
     * @param synContext Current Message
     * @param shouldTrace Indicates whether trace is ON or OFF
     * @return  True if message can continue ,otherwise false
     */
    private boolean doThrottling(MessageContext synContext, boolean shouldTrace) {

        if (throttle == null) {
            if (debugOn) {
                log.debug("Can not find a throttle");
            }
            return true;
        }
        org.apache.axis2.context.MessageContext axis2MessageContext
            = ((Axis2MessageContext) synContext).getAxis2MessageContext();
        //IP based throttling
        Object remoteIP = axis2MessageContext.getProperty(
            org.apache.axis2.context.MessageContext.REMOTE_ADDR);
        if (remoteIP == null) {
            if (shouldTrace) {
                trace.trace("The IP Address of the caller is cannnot find- The Throttling will" +
                    "not occur");
            }
            if (debugOn) {
                log.debug("The IP address of the caller can not find - Currently only support caller-IP base"
                    + "access control - Thottling will not happen ");
            }
            return true;
        } else {
            if (shouldTrace) {
                trace.trace("The IP Address of the caller :" + remoteIP);
            }
            ThrottleContext throttleContext
                = throttle.getThrottleContext(ThrottleConstants.IP_BASED_THROTTLE_KEY);
            if (throttleContext == null) {
                if (log.isDebugEnabled()) {
                    log.debug("Can not find a configuartion context for the IP Based Throttle");
                }
                return true;
            }
            try {
                AccessController accessControler = AccessControllerFactory.createAccessControler(
                    ThrottleConstants.IP_BASE);
                boolean canAccess = accessControler.canAccess(throttleContext, remoteIP);
                if (!canAccess) {
                    String msg = "Access has currently been denied by" +
                        " the IP_BASE throttle for the IP :\t" + remoteIP;
                    if (shouldTrace) {
                        trace.trace(msg);
                    }
                    if (log.isDebugEnabled()) {
                        log.debug(msg);
                    }
                } else {
                    if(debugOn){
                       log.debug("Access was successful ");
                   }
                }
                return canAccess;
            }
            catch (ThrottleException e) {
                handleException("Error occur during throttling ", e);
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
     * @param synContext Current Message
     * @param shouldTrace Indicates trace is ON or OFF
     */
    protected void initThrottle(MessageContext synContext, boolean shouldTrace) {

        boolean reCreate = false; // It is not need to recreate ,if property is not dyanamic
        OMElement policyOmElement = null;

        if (policyKey != null) {

            Entry entry = synContext.getConfiguration().getEntryDefinition(policyKey);
            if (entry == null) {
                if (debugOn) {
                    log.debug("Cant not find a Entry from the Entry key " + policyKey);
                }
                return;
            }

            Object entryValue = entry.getValue();
            if (entryValue == null) {
                if (debugOn) {
                    log.debug("Cant not find a Policy(Enrty value) from the Entry key " + policyKey);
                }
                return;
            }

            if (!(entryValue instanceof OMElement)) {
                if (debugOn) {
                    log.debug("Entry value which is refered from the key " + policyKey + " is Incompatible " +
                        "for the policy element");
                }
                return;
            }

            // if entry is dynamic, need to check wheather updated or not
            if ((!entry.isCached() || entry.isExpired())) {
                reCreate = true;
            }
            policyOmElement = (OMElement) entryValue;

        } else if (inLinePolicy != null) {
            policyOmElement = inLinePolicy;
        }

        if (policyOmElement == null) {
            if (debugOn) {
                log.debug("Can not find a Policy - Throttling will not occur");
            }
            return;
        }

        if (shouldTrace) {
            trace.trace("The Throttle Policy :" + policyOmElement.toString());
        }
        if (!reCreate) {
            //The first time creation
            if (throttle == null) {
                createThrottleMetaData(policyOmElement, synContext.isResponse());
            }
        } else {
            createThrottleMetaData(policyOmElement, synContext.isResponse());
        }
    }

    /**
     * To create the Throttle from the policy element
     *
     * @param policyOmElement - valid throttle policy
     * @param isResponse - Indicates whether current message flow is IN or OUT
     */
    protected void createThrottleMetaData(OMElement policyOmElement, boolean isResponse) {
        try {
            if (debugOn) {
                log.debug("Creating a new throttle configuration by parsing the Policy");
            }
            throttle = ThrottlePolicyProcessor
                .processPolicy(PolicyEngine.getPolicy(policyOmElement));

            //set the concurrent access controller
            if (ID != null) {
                if (!CONCURRENT_ACCESS_CONTROLLERS.containsKey(ID)) {
                    reCreateConcurrentAccessController(isResponse);
                } else {
                    if (isInitiator) {
                        reCreateConcurrentAccessController(isResponse);
                    } else {
                        lookupConcurrentAccessController();
                    }
                }
            }
        }
        catch (ThrottleException e) {
            handleException("Error during processing the thorttle policy  " + e.getMessage());
        }
    }

    /**
     * create a ConcurrentAccessController if the current message is incoming message
     *
     * @param isResponse true if the current message flow is out
     */
    private void reCreateConcurrentAccessController(boolean isResponse) {

        if (!isResponse) {
            concurrentAccessController = throttle.getConcurrentAccessController();
            if (concurrentAccessController != null) {
                isInitiator = true;  // frist time creation of concurrent access controller
                if (CONCURRENT_ACCESS_CONTROLLERS.containsKey(ID)) {
                    if (debugOn) {
                        log.debug("Removing the ConcurrentAccessControler with Id " + ID);
                    }
                    CONCURRENT_ACCESS_CONTROLLERS.remove(ID);  // removing the old access controller
                }
                if (debugOn) {
                    log.debug("Initiating ConcurrentAccessControler for throttle group id " + ID);
                }
                CONCURRENT_ACCESS_CONTROLLERS.put(ID, concurrentAccessController);
            }
        }
    }

    /**
     * Looking up the ConcurrentAccessController which has initiated by a another throttle mediator
     */
    private void lookupConcurrentAccessController() {
        log.info("ConcurrentAccessController has already defined for id :" + ID);
        concurrentAccessController =
            (ConcurrentAccessController) CONCURRENT_ACCESS_CONTROLLERS.get(ID);

    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    private void handleException(String msg, Exception e) {
        log.error(e);
        throw new SynapseException(msg);
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

    public String getOnReject() {
        return onReject;
    }

    public void setOnReject(String onReject) {
        this.onReject = onReject;
    }

    public Mediator getOnRejectMediator() {
        return onRejectMediator;
    }

    public void setOnRejectMediator(Mediator onRejectMediator) {
        this.onRejectMediator = onRejectMediator;
    }

    public String getOnAccept() {
        return onAccept;
    }

    public void setOnAccept(String onAccept) {
        this.onAccept = onAccept;
    }

    public Mediator getOnAcceptMediator() {
        return onAcceptMediator;
    }

    public void setOnAcceptMediator(Mediator onAcceptMediator) {
        this.onAcceptMediator = onAcceptMediator;
    }

    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }
}
