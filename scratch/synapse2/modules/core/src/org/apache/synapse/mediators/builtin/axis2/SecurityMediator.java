package org.apache.synapse.mediators.builtin.axis2;


import org.apache.synapse.SynapseMessage;
import org.apache.synapse.Constants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.api.Mediator;
import org.apache.synapse.axis2.Axis2SynapseMessage;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.context.OperationContextFactory;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.util.Utils;
import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.namespace.QName;
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

public class SecurityMediator implements Mediator {
    private Log log = LogFactory.getLog(getClass());

    public boolean mediate(SynapseMessage sm) {
        log.debug("process");
		try {
			MessageContext mc = ((Axis2SynapseMessage) sm)
					.getMessageContext();
			ConfigurationContext cc = mc.getConfigurationContext();
			AxisConfiguration ac = cc.getAxisConfiguration();
			AxisEngine ae = new AxisEngine(cc);
			AxisService as = ac.getService(Constants.SECURITY_QOS);
			if (as == null)
				throw new SynapseException("cannot locate service "
                        + Constants.SECURITY_QOS);
			ac.engageModule(new QName("security"));
			AxisOperation ao = as
					.getOperation(Constants.MEDIATE_OPERATION_NAME);
			OperationContext oc = OperationContextFactory
					.createOperationContext(ao.getAxisSpecifMEPConstant(), ao);
			ao.registerOperationContext(mc, oc);

			ServiceContext sc = Utils.fillContextInformation(as, cc);
			oc.setParent(sc);

			mc.setOperationContext(oc);
			mc.setServiceContext(sc);

			mc.setAxisOperation(ao);
			mc.setAxisService(as);

			ae.receive(mc);

		} catch (AxisFault e) {
			throw new SynapseException(e);
		}
		return true;
    }
}
