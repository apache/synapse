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
package org.apache.synapse.mediators.builtin;

import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.Constants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.core.axis2.EmptyRMMessageReceiver;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.context.OperationContextFactory;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.InOutAxisOperation;
import org.apache.axis2.description.AxisModule;
import org.apache.axis2.AxisFault;
import org.apache.axis2.util.Utils;

import javax.xml.namespace.QName;
/*
 * 
 */

public class RMMediator extends AbstractMediator {

    private static Log log = LogFactory.getLog(RMMediator.class);

    private static final String EMPTY_RM_ENGAGED_SERVICE =
            "__EMPTY_RM_ENGAGED_SERVICE__";
    private static final QName EMPTY_OPERATION =
            new QName("__EMPTY_OPERTAION__");

    public boolean mediate(MessageContext synCtx) {
        log.debug("RM Mediator  ::  mediate() ");
        org.apache.axis2.context.MessageContext msgCtx =
                ((Axis2MessageContext) synCtx).getAxis2MessageContext();

        ConfigurationContext cc = msgCtx.getConfigurationContext();
        AxisConfiguration ac = cc.getAxisConfiguration();

        try {
            rmEnabledService(cc, ac, msgCtx);

            AxisEngine ae = new AxisEngine(cc);

            ae.receive(msgCtx);

            if (msgCtx.getProperty(
                    org.apache.synapse.Constants.MESSAGE_RECEIVED_RM_ENGAGED) !=
                                                                              null)
            {
                return ((Boolean) msgCtx.getProperty(
                        org.apache.synapse.Constants.MESSAGE_RECEIVED_RM_ENGAGED))
                        .booleanValue();
            }

        } catch (AxisFault axisFault) {
            throw new SynapseException(axisFault);
        }
        return false;
    }

    private void rmEnabledService(ConfigurationContext cc, AxisConfiguration ac,
                                  org.apache.axis2.context.MessageContext mc)
            throws AxisFault {
        AxisService as = ac.getService(EMPTY_RM_ENGAGED_SERVICE);

        if (as == null) {
            synchronized (RMMediator.class) {
                AxisService emptyRMEngagedService =
                        new AxisService(EMPTY_RM_ENGAGED_SERVICE);
                AxisOperation emptyOperation =
                        new InOutAxisOperation(EMPTY_OPERATION);
                emptyOperation.setMessageReceiver(new EmptyRMMessageReceiver());
                emptyRMEngagedService.addOperation(emptyOperation);
                ac.addService(emptyRMEngagedService);

                as = emptyRMEngagedService;
                AxisModule am = ac
                        .getModule(Constants.SANDESHA2_MODULE_NAME);

                if (am == null) {
                    throw new AxisFault("Sandesha 2 Module couldn't Find");
                }
                emptyRMEngagedService.engageModule(am, ac);
            }
        }


        AxisOperation ao = as.getOperation(EMPTY_OPERATION);
        OperationContext oc =
                OperationContextFactory.createOperationContext(
                        ao.getAxisSpecifMEPConstant(),
                        ao);
        ao.registerOperationContext(mc, oc);

        ServiceContext sc = Utils.fillContextInformation(as, cc);
        oc.setParent(sc);
        mc.setAxisOperation(ao);
        mc.setAxisService(as);


    }


}
