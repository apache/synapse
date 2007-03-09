/*
* Copyright 2004,2005 The Apache Software Foundation.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.synapse.mediators.throttle;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.neethi.PolicyEngine;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.Entry;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.wso2.throttle.factory.AccessControllerFactory;
import org.wso2.throttle.*;


/**
 * The Mediator for the throttling - Throtting will occur according to the ws-policy which is specified as
 * the key for lookup from the registry or the inline policy
 * Only support IP based throttling- Throotling can manage per IP using the throttle policy
 */

public class ThrottleMediator extends AbstractMediator {

    private static final Log log = LogFactory.getLog(ThrottleMediator.class);

    /** The key for getting policy value - key refer to registry entry  */
    private String policyKey = null;
    /** InLine policy object - XML   */
    private OMElement inLinePolicy = null;
    /** The throttle - hold runtime + configuration data of throttle  */
    Throttle throttle = null;

    public boolean mediate(MessageContext synCtx) {
        //init method to init throttle
        init(synCtx);
        // check access allow or not
        return canAccess(synCtx);

    }

    /**
     * To check whether allow access or not for caller
     * Current Implementaion only support IP Based Throttling
     * @param synContext
     * @return boolean which indicate whether this caller can or not access
     */
    protected boolean canAccess(MessageContext synContext) {

        if (throttle == null) {
            log.info("Can not find a throttle");
            return true;
        }
        org.apache.axis2.context.MessageContext axis2MessageContext
                = ((Axis2MessageContext) synContext).getAxis2MessageContext();
        //IP based throttling
        Object remoteIP = axis2MessageContext.getProperty(
                org.apache.axis2.context.MessageContext.REMOTE_ADDR);
        if (remoteIP == null) {
            log.info("IP address of the caller can not find - Currently only support caller-IP base access control" +
                    "- Thottling will not happen ");
            return true;
        } else {
            ThrottleContext throttleContext
                    = throttle.getThrottleContext(ThrottleConstants.IP_BASED_THROTTLE_KEY);
            if (throttleContext == null) {
                log.info("Can not find a configuartion for IP Based Throttle");
                return true;
            }
            try {
                AccessController accessControler = AccessControllerFactory.createAccessControler(ThrottleConstants.IP_BASE);
                boolean canAccess = accessControler.canAccess(throttleContext, remoteIP);
                if (!canAccess) {
                    log.info("Access has currently been denied by the IP_BASE throttle for IP :\t" + remoteIP);
                }
                return canAccess;
            }
            catch (ThrottleException e) {
                log.warn(e.getMessage());
                return true;
            }
        }
    }


    /**
     * To init throttle with the policy
     * If the policy is defined as a Registry key ,then Policy will only process after it has expired
     * Any runtime changes to the policy will take effect
     * If the policy is defined as a Inline XML ,then only one time policy will process and any runtime
     * changes to the policy will not reflect
     * @param synContext
     */
    protected void init(MessageContext synContext) {

        boolean reCreate = false; // It is not need to recreate ,if property is not dyanamic
        OMElement policyOmElement = null;
        if (policyKey != null) {
            Entry entry = synContext.getConfiguration().getEntryDefinition(policyKey);
            if (entry == null) {
                return;
            }
            Object entryValue = entry.getValue();

            if (!(entryValue instanceof OMElement)) {
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
            log.warn("Cant not find a Policy - Throttling will not occur");
            return;
        }
        if (!reCreate) {
            //The first time creation
            if (throttle == null) {
                createThrottleMetaData(policyOmElement);
            }
        } else {
            createThrottleMetaData(policyOmElement);
        }

    }

    /**
     * To create the Throttle from the policy element
     *
     * @param policyOmElement - valid throttle policy
     */
    protected void createThrottleMetaData(OMElement policyOmElement) {
        try {
            throttle = ThrottlePolicyProcessor
                    .processPoclicy(PolicyEngine.getPolicy(policyOmElement));
        }
        catch (ThrottleException e) {
            handleException("Error during processing thorttle policy  " + e.getMessage());
        }
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    public String getType() {
        return ThrottleMediator.class.getName();
    }

    /**
     * To get the policy key - The key for which lookup from the registry
     *
     * @return String
     */
    public String getPolicyKey() {
        return policyKey;
    }

    /**
     * To set the policy key - The key for which lookup from the registry
     *
     * @param policyKey
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
     * @param inLinePolicy
     */
    public void setInLinePolicy(OMElement inLinePolicy) {
        this.inLinePolicy = inLinePolicy;
    }
}
