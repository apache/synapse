/*
 * Copyright 2004,2005 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */


package org.apache.sandesha2.interop;

/**
 * RMInteropServiceMessageReceiverInOut message receiver
 */

public class RMInteropServiceMessageReceiverInOut extends
		org.apache.axis2.receivers.AbstractInOutSyncMessageReceiver {

	public void invokeBusinessLogic(
			org.apache.axis2.context.MessageContext msgContext,
			org.apache.axis2.context.MessageContext newMsgContext)
			throws org.apache.axis2.AxisFault {

		try {

			// get the implementation class for the Web Service
			Object obj = getTheImplementationObject(msgContext);

			// Inject the Message Context if it is asked for
			org.apache.axis2.engine.DependencyManager
					.configureBusinessLogicProvider(obj, msgContext
							.getOperationContext());

			RMInteropServiceSkeleton skel = (RMInteropServiceSkeleton) obj;
			// Out Envelop
			org.apache.axiom.soap.SOAPEnvelope envelope = null;
			// Find the axisOperation that has been set by the Dispatch phase.
			org.apache.axis2.description.AxisOperation op = msgContext
					.getOperationContext().getAxisOperation();
			if (op == null) {
				throw new org.apache.axis2.AxisFault(
						"Operation is not located, if this is doclit style the SOAP-ACTION should specified via the SOAP Action to use the RawXMLProvider");
			}

			java.lang.String methodName;
			if (op.getName() != null
					& (methodName = op.getName().getLocalPart()) != null) {

				if ("EchoString".equals(methodName)) {

					org.tempuri.EchoStringResponse param7 = null;

					// doc style
					org.tempuri.EchoStringRequest wrappedParam = (org.tempuri.EchoStringRequest) fromOM(
							msgContext.getEnvelope().getBody()
									.getFirstElement(),
							org.tempuri.EchoStringRequest.class,
							getEnvelopeNamespaces(msgContext.getEnvelope()));

					param7 = skel.EchoString(wrappedParam);

					envelope = toEnvelope(getSOAPFactory(msgContext), param7,
							false);

				}

				if ("echoString".equals(methodName)) {

					org.tempuri.EchoStringResponse param9 = null;

					// doc style
					org.tempuri.EchoStringRequest wrappedParam = (org.tempuri.EchoStringRequest) fromOM(
							msgContext.getEnvelope().getBody()
									.getFirstElement(),
							org.tempuri.EchoStringRequest.class,
							getEnvelopeNamespaces(msgContext.getEnvelope()));

					param9 = skel.echoString(wrappedParam);

					envelope = toEnvelope(getSOAPFactory(msgContext), param9,
							false);

				}

				newMsgContext.setEnvelope(envelope);
			}
		} catch (Exception e) {
			throw org.apache.axis2.AxisFault.makeFault(e);
		}
	}

	//
	private org.apache.axiom.om.OMElement toOM(
			org.tempuri.EchoStringRequest param, boolean optimizeContent) {

		return param.getOMElement(org.tempuri.EchoStringRequest.MY_QNAME,
				org.apache.axiom.om.OMAbstractFactory.getOMFactory());

	}

	private org.apache.axiom.om.OMElement toOM(
			org.tempuri.EchoStringResponse param, boolean optimizeContent) {

		return param.getOMElement(org.tempuri.EchoStringResponse.MY_QNAME,
				org.apache.axiom.om.OMAbstractFactory.getOMFactory());

	}

	private org.apache.axiom.om.OMElement toOM(org.tempuri.PingRequest param,
			boolean optimizeContent) {

		return param.getOMElement(org.tempuri.PingRequest.MY_QNAME,
				org.apache.axiom.om.OMAbstractFactory.getOMFactory());

	}

	private org.apache.axiom.soap.SOAPEnvelope toEnvelope(
			org.apache.axiom.soap.SOAPFactory factory,
			org.tempuri.EchoStringResponse param, boolean optimizeContent) {
		org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory
				.getDefaultEnvelope();

		emptyEnvelope.getBody().addChild(
				param.getOMElement(org.tempuri.EchoStringResponse.MY_QNAME,
						factory));

		return emptyEnvelope;
	}

	/**
	 * get the default envelope
	 */
	private org.apache.axiom.soap.SOAPEnvelope toEnvelope(
			org.apache.axiom.soap.SOAPFactory factory) {
		return factory.getDefaultEnvelope();
	}

	private java.lang.Object fromOM(org.apache.axiom.om.OMElement param,
			java.lang.Class type, java.util.Map extraNamespaces) {

		try {

			if (org.tempuri.EchoStringRequest.class.equals(type)) {

				return org.tempuri.EchoStringRequest.Factory.parse(param
						.getXMLStreamReaderWithoutCaching());

			}

			if (org.tempuri.EchoStringResponse.class.equals(type)) {

				return org.tempuri.EchoStringResponse.Factory.parse(param
						.getXMLStreamReaderWithoutCaching());

			}

			if (org.tempuri.PingRequest.class.equals(type)) {

				return org.tempuri.PingRequest.Factory.parse(param
						.getXMLStreamReaderWithoutCaching());

			}

			if (org.tempuri.EchoStringRequest.class.equals(type)) {

				return org.tempuri.EchoStringRequest.Factory.parse(param
						.getXMLStreamReaderWithoutCaching());

			}

			if (org.tempuri.EchoStringResponse.class.equals(type)) {

				return org.tempuri.EchoStringResponse.Factory.parse(param
						.getXMLStreamReaderWithoutCaching());

			}

			if (org.tempuri.PingRequest.class.equals(type)) {

				return org.tempuri.PingRequest.Factory.parse(param
						.getXMLStreamReaderWithoutCaching());

			}

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return null;
	}

	/**
	 * A utility method that copies the namepaces from the SOAPEnvelope
	 */
	private java.util.Map getEnvelopeNamespaces(
			org.apache.axiom.soap.SOAPEnvelope env) {
		java.util.Map returnMap = new java.util.HashMap();
		java.util.Iterator namespaceIterator = env.getAllDeclaredNamespaces();
		while (namespaceIterator.hasNext()) {
			org.apache.axiom.om.OMNamespace ns = (org.apache.axiom.om.OMNamespace) namespaceIterator
					.next();
			returnMap.put(ns.getPrefix(), ns.getNamespaceURI());
		}
		return returnMap;
	}

	private org.apache.axis2.AxisFault createAxisFault(java.lang.Exception e) {
		org.apache.axis2.AxisFault f;
		Throwable cause = e.getCause();
		if (cause != null) {
			f = new org.apache.axis2.AxisFault(e.getMessage(), cause);
		} else {
			f = new org.apache.axis2.AxisFault(e.getMessage());
		}

		return f;
	}

}// end of class
