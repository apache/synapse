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
package org.apache.synapse.mediator;

import javax.xml.stream.FactoryConfigurationError;

import org.apache.axis2.AxisFault;

import org.apache.axis2.context.MessageContext;

import org.apache.axis2.engine.AxisEngine;

import org.apache.axis2.om.OMAbstractFactory;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.util.Utils;

import org.apache.synapse.SynapseException;

public class FaultMediator implements Mediator {
	public boolean mediate(MessageContext messageContext) {
		// TODO this only works for req-resp currently
		try {
	
			MessageContext outMC = Utils.createOutMessageContext(messageContext);
			outMC.setConfigurationContext(messageContext.getSystemContext());
			outMC.setServerSide(true);
			
			outMC.setEnvelope(	OMAbstractFactory.getSOAP11Factory()
									.getDefaultFaultEnvelope());
			
			AxisEngine ae = new AxisEngine(messageContext.getSystemContext());
			Object os = messageContext
						.getProperty(MessageContext.TRANSPORT_OUT);
				outMC.setProperty(MessageContext.TRANSPORT_OUT, os);
				Object ti = messageContext
						.getProperty(HTTPConstants.HTTPOutTransportInfo);
				outMC.setProperty(HTTPConstants.HTTPOutTransportInfo,
						ti);
				
				System.out.println("hit");
				
				//ae.receive(outMC);

				ae.send(outMC);
			
		} catch (AxisFault e) {
			throw new SynapseException(e);
		} catch (FactoryConfigurationError e) {
			throw new SynapseException(e);

		}
	
	return false;
	}
}
