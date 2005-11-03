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

import javax.xml.stream.FactoryConfigurationError;


import org.apache.axis2.AxisFault;
import org.apache.axis2.clientapi.Call;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;

import org.apache.axis2.engine.AxisEngine;

import org.apache.axis2.om.OMAbstractFactory;
import org.apache.axis2.transport.http.HTTPConstants;

import org.apache.synapse.SynapseException;

public class Sender {
	public static void send(MessageContext messageContext) {
		// TODO this only works for req-resp currently
		try {
			Call call = new Call();
			call.setTo(messageContext.getTo());

			SynapseDispatcher sd = new SynapseDispatcher();
			sd.initDispatcher();
			AxisService synapseService = sd.findService(messageContext);
			AxisOperation synapseOperation = sd.findOperation(synapseService,
					messageContext);

			AxisOperation ao = messageContext.getAxisOperation();

			MessageContext outMsgContext = call.invokeBlocking(ao,
					messageContext);

			AxisEngine ae = new AxisEngine(messageContext.getSystemContext());

			// deal with the fact that AddressingOutHandler has a bug if there
			// is no header at all.
			if (outMsgContext.getEnvelope().getHeader() == null)
				outMsgContext.getEnvelope().getBody().insertSiblingBefore(
						OMAbstractFactory.getSOAP11Factory()
								.getDefaultEnvelope().getHeader());

			outMsgContext.setAxisService(synapseService);
			outMsgContext.setAxisOperation(synapseOperation);

			// run all rules again
			ae.receive(outMsgContext);

			Object os = messageContext
					.getProperty(MessageContext.TRANSPORT_OUT);
			outMsgContext.setProperty(MessageContext.TRANSPORT_OUT, os);
			Object ti = messageContext
					.getProperty(HTTPConstants.HTTPOutTransportInfo);
			outMsgContext.setProperty(HTTPConstants.HTTPOutTransportInfo, ti);

			// respond to client
			ae.send(outMsgContext);

		} catch (AxisFault e) {
			throw new SynapseException(e);
		} catch (FactoryConfigurationError e) {
			throw new SynapseException(e);
			
		}
	}

}
