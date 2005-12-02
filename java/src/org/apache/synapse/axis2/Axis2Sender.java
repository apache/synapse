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

import org.apache.axis2.context.OperationContextFactory;

import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.om.OMAbstractFactory;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.synapse.Constants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseMessage;


public class Axis2Sender {

	public static void sendOn(SynapseMessage smc) {

		try {

			MessageContext messageContext = ((Axis2SynapseMessage) smc)
					.getMessageContext();
			AxisEngine ae = new AxisEngine(messageContext.getConfigurationContext());

			ConfigurationContext sc = messageContext.getConfigurationContext();

			MessageContext outMsgContext = Axis2FlexibleMEPClient
					.send(messageContext);

			// run all rules on response

			outMsgContext.setServerSide(true);

			// deal with the fact that AddressingOutHandler has a bug if
			// there
			// is no header at all.
			if (outMsgContext.getEnvelope().getHeader() == null)
				outMsgContext.getEnvelope().getBody().insertSiblingBefore(
						OMAbstractFactory.getSOAP11Factory()
								.getDefaultEnvelope().getHeader());
			Object os = messageContext
					.getProperty(MessageContext.TRANSPORT_OUT);
			outMsgContext.setProperty(MessageContext.TRANSPORT_OUT, os);
			Object ti = messageContext
					.getProperty(HTTPConstants.HTTPOutTransportInfo);
			outMsgContext.setProperty(HTTPConstants.HTTPOutTransportInfo, ti);

			SynapseDispatcher sd = new SynapseDispatcher();
			sd.initDispatcher();
			AxisService synapseService = sd.findService(messageContext);
			AxisOperation synapseOperation = sd.findOperation(synapseService,
					messageContext);

			outMsgContext.setConfigurationContext(sc);
			outMsgContext.setAxisService(synapseService);
			outMsgContext.setAxisOperation(synapseOperation);
			outMsgContext.setOperationContext(OperationContextFactory
					.createOperationContext(
							OperationContextFactory.MEP_CONSTANT_OUT_ONLY,
							synapseOperation));

			ae.receive(outMsgContext);

		} catch (Exception e) {
			e.printStackTrace();
			throw new SynapseException(e);
		}
	}

	public static void sendBack(SynapseMessage smc) {
		MessageContext messageContext = ((Axis2SynapseMessage) smc)
				.getMessageContext();
		AxisEngine ae = new AxisEngine(messageContext.getConfigurationContext());

		if (messageContext.getEnvelope().getHeader() == null)
			messageContext.getEnvelope().getBody().insertSiblingBefore(
					OMAbstractFactory.getSOAP11Factory().getDefaultEnvelope()
							.getHeader());

		messageContext.setProperty(Constants.ISRESPONSE_PROPERTY, new Boolean(
				true));

		try {
			ae.send(messageContext);
		} catch (AxisFault e) {
			throw new SynapseException(e);

		}

	}

}
