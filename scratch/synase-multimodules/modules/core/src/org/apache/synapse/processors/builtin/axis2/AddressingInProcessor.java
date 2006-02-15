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

package org.apache.synapse.processors.builtin.axis2;


import org.apache.axis2.AxisFault;
import org.apache.axis2.context.*;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;


import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.AxisEngine;

import org.apache.axis2.util.Utils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Constants;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseMessage;


import org.apache.synapse.axis2.Axis2SynapseMessage;


import org.apache.synapse.processors.AbstractProcessor;


/**
 * <p/>
 * This class turns on the addressing module and then calls an empty
 * service There's probably a better way but this should work!
 */
public class AddressingInProcessor extends AbstractProcessor {
    private Log log = LogFactory.getLog(getClass());


    public boolean process(SynapseEnvironment se, SynapseMessage smc) {
        log.debug("Processing __AddressingInHandler__");
        try {
            MessageContext mc = ((Axis2SynapseMessage)smc).getMessageContext();
            se.setProperty(Constants.ADDRESSING_PROCESSED,Boolean.TRUE);
            // default configuration_contex and axis_configuration
            ConfigurationContext cc = mc.getConfigurationContext();
            AxisConfiguration ac = cc.getAxisConfiguration();
            AxisService as = ac.getService(Constants.EMPTYMEDIATOR);
            if (as == null)
                throw new SynapseException("cannot locate service "
                        + Constants.EMPTYMEDIATOR);

            AxisEngine ae = new AxisEngine(cc);
            AxisOperation ao = as
                    .getOperation(Constants.MEDIATE_OPERATION_NAME);
            OperationContext oc = OperationContextFactory
                    .createOperationContext(ao.getAxisSpecifMEPConstant(), ao);
            ao.registerOperationContext(mc,oc);
            ServiceContext sc = Utils.fillContextInformation(ao, as, cc);
            oc.setParent(sc);
            mc.setAxisOperation(ao);
            mc.setAxisService(as);
            ae.receive(mc);

        } catch (AxisFault axisFault) {
            throw new SynapseException(
                    "__AddresingInHandler__ caught an Exception" +
                            axisFault.getMessage());
        }
        return true;
    }


}
