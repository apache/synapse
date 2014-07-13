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
import org.apache.synapse.Constants;
import org.apache.synapse.Rule;
import org.apache.synapse.SynapseException;
import org.apache.synapse.mediator.MediatorMessageReceiver;



public class MediatorExecutor {

	// exexute mediators by calling them as services

	


	

	

	

	

	public static boolean execute(Rule r, MessageContext messageContext) {

		ConfigurationContext cc = messageContext.getSystemContext();
		AxisConfiguration ac = cc.getAxisConfiguration();
		AxisEngine ae = new AxisEngine(cc);

		AxisService as = null;
		AxisOperation ao = null;
		System.out.println("invoking "+r.getMediatorName()+" of type "+(Integer.toString(r.getMediatorType())));
		try {
			switch (r.getMediatorType()) { 
				case  Constants.TYPE_SPRING: {

					as = ac.getService(Constants.SPRINGMEDIATOR);
					if (as == null) throw new SynapseException("cannot locate service " +Constants.SPRINGMEDIATOR);
					messageContext.setProperty(
							Constants.SYNAPSE_MEDIATOR_XML_BYTES, r.getXmlBytes());
									
					messageContext.setProperty(Constants.SYNAPSE_SPRING_MEDIATOR_NAME, r
							.getMediatorName());
					break;
					
				}
				case Constants.TYPE_AXIS2SERVICE: {
					as = ac.getService(r.getMediatorName());
					if (as == null) throw new SynapseException("cannot locate service " +r.getMediatorName());
					break;
				}
				case Constants.TYPE_CLASS: {
					Class c = null;
					try {
						c = messageContext.getSystemContext()
								.getAxisConfiguration().getService(Constants.CLASSMEDIATOR)
								.getClassLoader().loadClass(r.getMediatorName());

					} catch (ClassNotFoundException ce) {
						throw new SynapseException(ce);
					}

					messageContext.setProperty(Constants.SYNAPSE_MEDIATOR_CLASS, c);
					as = ac.getService(Constants.CLASSMEDIATOR);
					if (as==null) throw new SynapseException("cannot locate service "+Constants.CLASSMEDIATOR);
					break;
					
				}
				case Constants.TYPE_BPEL: {
					as = ac.getService(Constants.BPELMEDIATOR);
					if (as==null) throw new SynapseException("cannot locate service "+Constants.BPELMEDIATOR);
					messageContext.setProperty(
							Constants.SYNAPSE_MEDIATOR_XML_BYTES, r.getXmlBytes());
									
					messageContext.setProperty(Constants.SYNAPSE_SPRING_MEDIATOR_NAME, r
							.getMediatorName());
					break;
				}
					
			}
			
			

			ao = as.getOperation(Constants.MEDIATE_OPERATION_NAME);
			OperationContext oc = OperationContextFactory
					.createOperationContext(ao.getAxisSpecifMEPConstant(), ao);
			ao.registerOperationContext(messageContext, oc);

			ServiceContext sc = Utils.fillContextInformation(ao, as, cc);
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
				.getProperty(MediatorMessageReceiver.RESPONSE_PROPERTY))
				.booleanValue();

	}
}
