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
import org.apache.axis2.util.CallbackReceiver;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.AxisMessage;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Constants;
import org.apache.synapse.SynapseException;

import javax.xml.namespace.QName;

/**
 * Returns an anonymous service for the given QoS. If an instance does not already
 * exist, create one and set it to the Axis configuration
 */
public class AnonymousServiceFactory {

    private static final Log log = LogFactory.getLog(AnonymousServiceFactory.class);

    private static final String NONE            = "__NONE__";
    private static final String ADDR_ONLY       = "__ADDR_ONLY__";
    private static final String RM_AND_ADDR     = "__RM_AND_ADDR__";
    private static final String SEC_AND_ADDR    = "__SEC_AND_ADDR__";
    private static final String RM_SEC_AND_ADDR = "__RM_SEC_AND_ADDR__";

    public static final String DYNAMIC_OPERATION  = "__DYNAMIC_OPERATION__";

    private static final SynapseCallbackReceiver synapseCallback = new SynapseCallbackReceiver();

    /**
     * Creates an AxisService for the requested QoS for sending out messages
     * Callers must guarantee that if wsRMon or wsSecOn is required, that wsAddrOn is also set
     * @param axisCfg Axis2 configuration
     * @param wsAddrOn
     * @param wsRMOn
     * @param wsSecOn
     * @return An Axis service for the requested QoS
     */
    public static AxisService getAnonymousService(AxisConfiguration axisCfg,
        boolean wsAddrOn, boolean wsRMOn, boolean wsSecOn) {

        String servicekey = null;
        if (!wsAddrOn) {
            servicekey = NONE;
        } else {
            if (!wsSecOn && !wsRMOn) {
                servicekey = ADDR_ONLY;
            } else if (wsRMOn && !wsSecOn) {
                servicekey = RM_AND_ADDR;
            } else if (wsSecOn && !wsRMOn) {
                servicekey = SEC_AND_ADDR;
            } else {
                servicekey = RM_SEC_AND_ADDR;
            }
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

                    service = createAnonymousService(axisCfg, servicekey);

                    if (wsAddrOn) {
                        service.engageModule(axisCfg.getModule(
                            Constants.ADDRESSING_MODULE_NAME), axisCfg);

                        if (wsRMOn) {
                            service.engageModule(axisCfg.getModule(
                                Constants.SANDESHA2_MODULE_NAME), axisCfg);
                        }
                        if (wsSecOn) {
                            service.engageModule(axisCfg.getModule(
                                Constants.RAMPART_MODULE_NAME), axisCfg);
                        }
                    }
                    // if WS-A is off, WS-Sec and WS-RM should be too
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
     * @param axisCfg the Axis2 configuration
     * @return an anonymous service named with the given QoS key
     */
    private static AxisService createAnonymousService(
        AxisConfiguration axisCfg, String serviceKey) {

        try {
            DynamicAxisOperation dynamicOperation =
                new DynamicAxisOperation(new QName(DYNAMIC_OPERATION));
            dynamicOperation.setMessageReceiver(synapseCallback);
            AxisMessage inMsg = new AxisMessage();
            inMsg.setName("in-message");
            inMsg.setParent(dynamicOperation);
            AxisMessage outMsg = new AxisMessage();
            outMsg.setName("out-message");
            outMsg.setParent(dynamicOperation);
            dynamicOperation.addMessage(inMsg, WSDLConstants.MESSAGE_LABEL_OUT_VALUE);
            dynamicOperation.addMessage(outMsg, WSDLConstants.MESSAGE_LABEL_IN_VALUE);
            AxisService axisAnonymousService  = new AxisService(serviceKey);
            axisAnonymousService.addOperation(dynamicOperation);
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

}
