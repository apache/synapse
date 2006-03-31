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

package org.apache.synapse.mediators.types.axis2;



import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.context.OperationContextFactory;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.util.Utils;
import org.apache.synapse.Constants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseMessage;

import org.apache.synapse.api.Mediator;
import org.apache.synapse.axis2.Axis2SynapseMessage;


/**
 *
 * <p>
 * This class executes a service in the "owning" axis2 engine.
 * The service operation will be "mediate" and the service name is set as the ServiceName property
 * 
 *
 */
public class ServiceMediator implements Mediator {
	

	private String serviceName = null;

	public boolean mediate(SynapseMessage smc) {
		MessageContext messageContext = null;
		try {
			messageContext = ((Axis2SynapseMessage) smc)
					.getMessageContext();
		} catch (ClassCastException cce) {
			throw new SynapseException(
					"A non-Axis2 MC SOAPMessageContext has been passed to the Axis2 MediationExecutor",
					cce);
		}

		

		try {
			ConfigurationContext cc = messageContext.getConfigurationContext();
			AxisConfiguration ac = cc.getAxisConfiguration();
			AxisEngine ae = new AxisEngine(cc);

			AxisService as = null;
			AxisOperation ao = null;
			
			as = ac.getService(getServiceName());
			if (as == null)
				throw new SynapseException("cannot locate service "
						+ getServiceName());
			
			ao = as.getOperation(Constants.MEDIATE_OPERATION_NAME);
			OperationContext oc = OperationContextFactory
					.createOperationContext(ao.getAxisSpecifMEPConstant(), ao);
			ao.registerOperationContext(messageContext, oc);

			ServiceContext sc = Utils.fillContextInformation(as, cc);
			oc.setParent(sc);

			messageContext.setOperationContext(oc);
			messageContext.setServiceContext(sc);

			messageContext.setAxisOperation(ao);
			messageContext.setAxisService(as);

			ae.receive(messageContext);

		} catch (AxisFault e) {
			throw new SynapseException(e);

		}

		return ((Boolean) messageContext
				.getProperty(Constants.MEDIATOR_RESPONSE_PROPERTY))
				.booleanValue();

	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getServiceName() {
		return serviceName;
	}

}
