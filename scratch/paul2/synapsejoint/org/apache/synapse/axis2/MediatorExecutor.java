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
package org.apache.synapse.axis2;

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
import org.apache.synapse.SynapseException;
import org.apache.synapse.mediator.MediatorMessageReceiver;

public class MediatorExecutor {
		
	// exexute mediators by calling them as services

	public static boolean execute(String mediatorName, MessageContext messageContext) {
		
		ConfigurationContext cc = messageContext.getSystemContext(); 
		AxisConfiguration ac = cc.getAxisConfiguration();
		AxisEngine ae = new AxisEngine(cc);
			
		AxisService as = null;
		AxisOperation ao = null;
		try {
			
			as = ac.getService(mediatorName);
			if (as!=null) {
			ao = as.getOperation("mediate");
			messageContext.setAxisService(as);
			messageContext.setAxisOperation(ao);
			
			 OperationContext oc = OperationContextFactory.createOperationContext(ao
		                .getAxisSpecifMEPConstant(), ao);
		        ao.registerOperationContext(messageContext, oc);

		        ServiceContext sc = Utils.fillContextInformation(ao, as, cc);//messageContext.getSystemContext());
		        oc.setParent(sc);
		        messageContext.setOperationContext(oc);
		        messageContext.setServiceContext(sc);

			
			ae.receive(messageContext);
			} else throw new SynapseException("Mediator "+mediatorName+" is not registered as a service in the current Axis Configuration");
		} catch (AxisFault e) {
			throw new SynapseException(e);
		}
		
		return ((Boolean)messageContext.getProperty(MediatorMessageReceiver.RESPONSE_PROPERTY)).booleanValue();

		
		
	}

}
