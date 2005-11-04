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
		try {
			if (r.getSpringBeanFactory() != null) {
				as = ac.getService("springmediator");
				messageContext.setProperty(
						"synapse.mediator.spring.beanFactory", r
								.getSpringBeanFactory());
				messageContext.setProperty("synapse.spring.mediatorName", r
						.getMediatorName());

			}
			if (as == null) {

				as = ac.getService(r.getMediatorName());
			}
			if (as == null) {
				Class c = null;
				try {
					c = messageContext.getSystemContext()
							.getAxisConfiguration().getService("classmediator")
							.getClassLoader().loadClass(r.getMediatorName());

				} catch (ClassNotFoundException ce) {

				}

				if (c != null) {
					messageContext.setProperty("synapse.mediator.class", c);
					as = ac.getService("classmediator");

				}
			}
			if (as == null) {
				throw new SynapseException(
						"Mediator "
								+ r.getMediatorName()
								+ " is not registered as a service in the current Axis Configuration");
			}

			ao = as.getOperation("mediate");
			OperationContext oc = OperationContextFactory
					.createOperationContext(ao.getAxisSpecifMEPConstant(), ao);
			ao.registerOperationContext(messageContext, oc);

			ServiceContext sc = Utils.fillContextInformation(ao, as, cc);
			oc.setParent(sc);
			messageContext.setOperationContext(oc);
			messageContext.setServiceContext(sc);

			System.out.println(r.getMediatorName() + ":" + as.getName());

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
