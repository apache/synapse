package org.apache.synapse.processors;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.*;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.util.Utils;
import org.apache.synapse.Constants;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseMessage;
import org.apache.synapse.axis2.Axis2SynapseMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
*
*/

public class Axis2MediatorProcessor extends AbstractProcessor {
    private String mediatorName;
    private Log log = LogFactory.getLog(getClass());

    public boolean process(SynapseEnvironment se, SynapseMessage sm) {
        log.debug("process");
        MessageContext msgContext =
                ((Axis2SynapseMessage) sm).getMessageContext();
        AxisConfiguration ac =
                msgContext.getConfigurationContext().getAxisConfiguration();
        ConfigurationContext cc = msgContext.getConfigurationContext();


        AxisEngine ae = new AxisEngine(cc);
        AxisService as = null;
        try {
            as = ac.getService(mediatorName);

            if (as == null)
                throw new SynapseException("cannot locate service "
                        + mediatorName);

            AxisOperation ao = as
                    .getOperation(Constants.MEDIATE_OPERATION_NAME);
            OperationContext oc = OperationContextFactory
                    .createOperationContext(
                            ao.getAxisSpecifMEPConstant(), ao);
            ao.registerOperationContext(msgContext, oc);

            ServiceContext sc =
                    Utils.fillContextInformation(ao, as, cc);
            oc.setParent(sc);

            msgContext.setOperationContext(oc);
            msgContext.setServiceContext(sc);

            msgContext.setAxisOperation(ao);
            msgContext.setAxisService(as);

            msgContext.setProperty(Constants.MEDIATOR_SYNAPSE_ENV_PROPERTY, se);
            
            ae.receive(msgContext);
        } catch (AxisFault axisFault) {
            throw new SynapseException(axisFault);
        }

        return ((Boolean)msgContext.getProperty(Constants.MEDIATOR_STATUS)).booleanValue();
    }

    public void setServiceMediatorName(String mediatorName) {
        this.mediatorName = mediatorName;
    }
}
