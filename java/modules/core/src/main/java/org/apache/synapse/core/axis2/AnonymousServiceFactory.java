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

package org.apache.synapse.core.axis2;

import org.apache.axis2.AxisFault;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.AxisMessage;
import org.apache.axis2.description.OutOnlyAxisOperation;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.SynapseConfiguration;

import javax.xml.namespace.QName;

/**
 * Returns an anonymous service for the given QoS. If an instance does not already
 * exist, create one and set it to the Axis configuration
 */
public class AnonymousServiceFactory {

    private static final Log log = LogFactory.getLog(AnonymousServiceFactory.class);

    private static final String NONE            = "__NONE__";
    private static final String ADDR_ONLY       = "__ADDR_ONLY__";
    private static final String SEC_ONLY        = "__SEC_ONLY__";
    private static final String RM_AND_ADDR     = "__RM_AND_ADDR__";
    private static final String SEC_AND_ADDR    = "__SEC_AND_ADDR__";
    private static final String RM_SEC_AND_ADDR = "__RM_SEC_AND_ADDR__";

    public static final String OUT_IN_OPERATION   = "anonOutInOp";
    public static final String OUT_ONLY_OPERATION = "anonOutonlyOp";

    private static SynapseCallbackReceiver synapseCallbackReceiver = null;

    /**
     * Creates an AxisService for the requested QoS for sending out messages
     * Callers must guarantee that if wsRMon or wsSecOn is required, that wsAddrOn is also set
     * @param synCfg Synapse configuration
     * @param axisCfg Axis2 configuration
     * @param wsAddrOn whether addressing is on or not
     * @param wsRMOn whether RM is on ot not
     * @param wsSecOn whether security is on or not
     * @return An Axis service for the requested QoS
     */
    public static AxisService getAnonymousService(SynapseConfiguration synCfg,
                                                  AxisConfiguration axisCfg, boolean wsAddrOn,
                                                  boolean wsRMOn, boolean wsSecOn) {

        // if non of addressing, security and rm is engaged then checkbit is 0
        int checkbit = 0;
        // if addressing is on increase the checkbit by 1
        if (wsAddrOn) { checkbit += 1; }
        // if security is on increase the checkbit by 2
        if (wsSecOn) { checkbit += 2; }
        // if reliable messaging is on increase the checkbit by 4
        if (wsRMOn) { checkbit += 4; }

        String servicekey;
        switch (checkbit) {
            case 0 :
                servicekey = NONE;
                break;
            case 1 :
                servicekey = ADDR_ONLY;
                break;
            case 2 :
                servicekey = SEC_ONLY;
                break;
            case 3 :
                servicekey = SEC_AND_ADDR;
                break;
            case 4 & 5 :
                servicekey = RM_AND_ADDR;
                break;
            case 6 & 7 :
                servicekey = RM_SEC_AND_ADDR;
                break;
            default :
                servicekey = NONE;
                break;
        }

        try {
            AxisService service = axisCfg.getService(servicekey);
            if (service == null) {
                synchronized (AnonymousServiceFactory.class) {

                    // fix with double locking, issue found on performance test
                    service = axisCfg.getService(servicekey);
                    if (service != null) {
                        return service;
                    }

                    service = createAnonymousService(synCfg, axisCfg, servicekey);

                    if (wsAddrOn) {
                        service.engageModule(axisCfg.getModule(
                            SynapseConstants.ADDRESSING_MODULE_NAME), axisCfg);

                        if (wsRMOn) {
                            service.engageModule(axisCfg.getModule(
                                SynapseConstants.RM_MODULE_NAME), axisCfg);
                        }
                    }
                    // if WS-A is off, WS-RM should be too

                    if (wsSecOn) {
                        service.engageModule(axisCfg.getModule(
                                SynapseConstants.RAMPART_MODULE_NAME), axisCfg);
                    }
                }
            }
            return service;
        } catch (AxisFault e) {
            handleException("Error retrieving anonymous service for QoS : " + servicekey, e);
        }
        return null;
    }

    private static void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }

    /**
     * Create a new Anonymous Axis service for OUT-IN as default MEP
     * @param synCfg the Synapse Configuration
     * @param axisCfg the Axis2 configuration
     * @param serviceKey key for the service
     * @return an anonymous service named with the given QoS key
     */
    private static AxisService createAnonymousService(SynapseConfiguration synCfg,
        AxisConfiguration axisCfg, String serviceKey) {

        try {
            DynamicAxisOperation dynamicOperation =
                new DynamicAxisOperation(new QName(OUT_IN_OPERATION));
            dynamicOperation.setMessageReceiver(getCallbackReceiver(synCfg));
            AxisMessage inMsg = new AxisMessage();
            inMsg.setName("in-message");
            inMsg.setParent(dynamicOperation);
            AxisMessage outMsg = new AxisMessage();
            outMsg.setName("out-message");
            outMsg.setParent(dynamicOperation);
            dynamicOperation.addMessage(inMsg, WSDLConstants.MESSAGE_LABEL_OUT_VALUE);
            dynamicOperation.addMessage(outMsg, WSDLConstants.MESSAGE_LABEL_IN_VALUE);

            OutOnlyAxisOperation asyncOperation =
                new OutOnlyAxisOperation(new QName(OUT_ONLY_OPERATION));
            asyncOperation.setMessageReceiver(getCallbackReceiver(synCfg));
            AxisMessage outOnlyMsg = new AxisMessage();
            outOnlyMsg.setName("out-message");
            outOnlyMsg.setParent(asyncOperation);
            asyncOperation.addMessage(outMsg, WSDLConstants.MESSAGE_LABEL_OUT_VALUE);

            AxisService axisAnonymousService  = new AxisService(serviceKey);
            axisAnonymousService.addOperation(dynamicOperation);
            axisAnonymousService.addOperation(asyncOperation);
            axisCfg.addService(axisAnonymousService);
            axisCfg.getPhasesInfo().setOperationPhases(dynamicOperation);
            return axisAnonymousService;

        } catch (AxisFault e) {
            handleException(
                "Error occured while creating an anonymous service for QoS : " +
                 serviceKey, e);
        }
        return null;
    }

    /**
     * Create a single callback receiver if required, and return its reference
     * @param synCfg the Synapse configuration
     * @return the callback receiver thats created or now exists
     */
    private static synchronized SynapseCallbackReceiver getCallbackReceiver(
        SynapseConfiguration synCfg) {

        if (synapseCallbackReceiver == null) {
            synapseCallbackReceiver = new SynapseCallbackReceiver(synCfg);
        }
        return synapseCallbackReceiver;
    }
}
