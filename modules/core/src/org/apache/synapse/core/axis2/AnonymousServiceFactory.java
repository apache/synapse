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
package org.apache.synapse.core.axis2;

import org.apache.axis2.AxisFault;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.OutInAxisOperation;
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

    private static final String NO_ADDRESSING   = "__NO_ADDRESSING__";
    private static final String ADDRESSING_ONLY = "__ADDRESSING_ONLY__";
    private static final String RM_ONLY         = "__RM_ONLY__";
    private static final String SEC_ONLY        = "__SEC_ONLY__";
    private static final String RM_AND_SEC      = "__RM_AND_SEC__";

    public static final String OPERATION_OUT_IN  = "__OPERATION_OUT_IN__";

    public static AxisService getAnonymousService(AxisConfiguration axisCfg,
        boolean wsAddrOn, boolean wsRMOn, boolean wsSecOn) {

        String servicekey = null;
        if (!wsAddrOn) {
            servicekey = NO_ADDRESSING;
        } else {
            if (!wsSecOn && !wsRMOn) {
                servicekey = ADDRESSING_ONLY;
            } else if (wsRMOn && !wsSecOn) {
                servicekey = RM_ONLY;
            } else if (wsSecOn && !wsRMOn) {
                servicekey = SEC_ONLY;
            } else {
                servicekey = RM_AND_SEC;
            }
        }

        try {
            AxisService service = axisCfg.getService(servicekey);
            if (service == null) {
                synchronized (AnonymousServiceFactory.class) {

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

    private static void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    /**
     * Create a new Anonymous Axis service for OUT-IN as default MEP
     * @param axisCfg the Axis2 configuration
     * @return an anonymous service named with the given QoS key
     */
    private static AxisService createAnonymousService(
        AxisConfiguration axisCfg, String serviceKey) {

        try {
            OutInAxisOperation outInOperation =
                new OutInAxisOperation(new QName(OPERATION_OUT_IN));
            AxisService axisAnonymousService  = new AxisService(serviceKey);
            axisAnonymousService.addOperation(outInOperation);
            axisCfg.addService(axisAnonymousService);
            axisCfg.getPhasesInfo().setOperationPhases(outInOperation);
            return axisAnonymousService;
        } catch (AxisFault e) {
            handleException(
                "Error occured while creating an anonymous service for QoS : " +
                 serviceKey, e);
        }
        return null;
    }

}
