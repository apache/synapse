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

/*
 * RMInteropServiceStub java implementation
 */

public class RMInteropServiceStub extends org.apache.axis2.client.Stub {
	protected org.apache.axis2.description.AxisOperation[] _operations;

	// hashmaps to keep the fault mapping
	private java.util.HashMap faultExeptionNameMap = new java.util.HashMap();

	private java.util.HashMap faultExeptionClassNameMap = new java.util.HashMap();

	private java.util.HashMap faultMessageMap = new java.util.HashMap();

	private void populateAxisService() {

		// creating the Service with a unique name
		_service = new org.apache.axis2.description.AxisService(
				"RMInteropService" + this.hashCode());

		// creating the operations
		org.apache.axis2.description.AxisOperation __operation;

		_operations = new org.apache.axis2.description.AxisOperation[4];

		__operation = new org.apache.axis2.description.OutInAxisOperation();

		__operation.setName(new javax.xml.namespace.QName("", "EchoString"));

		_operations[0] = __operation;
		_service.addOperation(__operation);

		__operation = new org.apache.axis2.description.OutOnlyAxisOperation();

		__operation.setName(new javax.xml.namespace.QName("", "Ping"));

		_operations[1] = __operation;
		_service.addOperation(__operation);

		__operation = new org.apache.axis2.description.OutInAxisOperation();

		__operation.setName(new javax.xml.namespace.QName("", "echoString"));

		_operations[2] = __operation;
		_service.addOperation(__operation);

		__operation = new org.apache.axis2.description.OutOnlyAxisOperation();

		__operation.setName(new javax.xml.namespace.QName("", "ping"));

		_operations[3] = __operation;
		_service.addOperation(__operation);

	}

	// populates the faults
	private void populateFaults() {

	}

	/**
	 * Constructor that takes in a configContext
	 */
	public RMInteropServiceStub(
			org.apache.axis2.context.ConfigurationContext configurationContext,
			java.lang.String targetEndpoint) throws org.apache.axis2.AxisFault {
		// To populate AxisService
		populateAxisService();
		populateFaults();

		_serviceClient = new org.apache.axis2.client.ServiceClient(
				configurationContext, _service);
		configurationContext = _serviceClient.getServiceContext()
				.getConfigurationContext();

		_serviceClient.getOptions().setTo(
				new org.apache.axis2.addressing.EndpointReference(
						targetEndpoint));

	}

	/**
	 * Default Constructor
	 */
	public RMInteropServiceStub() throws org.apache.axis2.AxisFault {

		this("http://www.example.org/");

	}

	/**
	 * Constructor taking the target endpoint
	 */
	public RMInteropServiceStub(java.lang.String targetEndpoint)
			throws org.apache.axis2.AxisFault {
		this(null, targetEndpoint);
	}

	/**
	 * Auto generated method signature
	 * 
	 * @see org.apache.sandesha2.interop.RMInteropService#EchoString
	 * @param param48
	 * 
	 */
	public org.tempuri.EchoStringResponse EchoString(

	org.tempuri.EchoStringRequest param48) throws java.rmi.RemoteException

	{
		try {
			org.apache.axis2.client.OperationClient _operationClient = _serviceClient
					.createClient(_operations[0].getName());
			_operationClient.getOptions().setAction("urn:wsrm:EchoString");
			_operationClient.getOptions().setExceptionToBeThrownOnSOAPFault(
					true);

			// create SOAP envelope with that payload
			org.apache.axiom.soap.SOAPEnvelope env = null;

			// Style is Doc.

			env = toEnvelope(getFactory(_operationClient.getOptions()
					.getSoapVersionURI()), param48,
					optimizeContent(new javax.xml.namespace.QName("",
							"EchoString")));

			// adding SOAP headers
			_serviceClient.addHeadersToEnvelope(env);
			// create message context with that soap envelope
			org.apache.axis2.context.MessageContext _messageContext = new org.apache.axis2.context.MessageContext();
			_messageContext.setEnvelope(env);

			// add the message contxt to the operation client
			_operationClient.addMessageContext(_messageContext);

			// execute the operation client
			_operationClient.execute(true);

			org.apache.axis2.context.MessageContext _returnMessageContext = _operationClient
					.getMessageContext(org.apache.axis2.wsdl.WSDLConstants.MESSAGE_LABEL_IN_VALUE);
			org.apache.axiom.soap.SOAPEnvelope _returnEnv = _returnMessageContext
					.getEnvelope();

			java.lang.Object object = fromOM(_returnEnv.getBody()
					.getFirstElement(), org.tempuri.EchoStringResponse.class,
					getEnvelopeNamespaces(_returnEnv));
			_messageContext.getTransportOut().getSender().cleanup(
					_messageContext);
			return (org.tempuri.EchoStringResponse) object;

		} catch (org.apache.axis2.AxisFault f) {
			org.apache.axiom.om.OMElement faultElt = f.getDetail();
			if (faultElt != null) {
				if (faultExeptionNameMap.containsKey(faultElt.getQName())) {
					// make the fault by reflection
					try {
						java.lang.String exceptionClassName = (java.lang.String) faultExeptionClassNameMap
								.get(faultElt.getQName());
						java.lang.Class exceptionClass = java.lang.Class
								.forName(exceptionClassName);
						java.lang.Exception ex = (java.lang.Exception) exceptionClass
								.newInstance();
						// message class
						java.lang.String messageClassName = (java.lang.String) faultMessageMap
								.get(faultElt.getQName());
						java.lang.Class messageClass = java.lang.Class
								.forName(messageClassName);
						java.lang.Object messageObject = fromOM(faultElt,
								messageClass, null);
						java.lang.reflect.Method m = exceptionClass.getMethod(
								"setFaultMessage",
								new java.lang.Class[] { messageClass });
						m.invoke(ex, new java.lang.Object[] { messageObject });

						throw new java.rmi.RemoteException(ex.getMessage(), ex);
					} catch (java.lang.ClassCastException e) {
						// we cannot intantiate the class - throw the original
						// Axis fault
						throw f;
					} catch (java.lang.ClassNotFoundException e) {
						// we cannot intantiate the class - throw the original
						// Axis fault
						throw f;
					} catch (java.lang.NoSuchMethodException e) {
						// we cannot intantiate the class - throw the original
						// Axis fault
						throw f;
					} catch (java.lang.reflect.InvocationTargetException e) {
						// we cannot intantiate the class - throw the original
						// Axis fault
						throw f;
					} catch (java.lang.IllegalAccessException e) {
						// we cannot intantiate the class - throw the original
						// Axis fault
						throw f;
					} catch (java.lang.InstantiationException e) {
						// we cannot intantiate the class - throw the original
						// Axis fault
						throw f;
					}
				} else {
					throw f;
				}
			} else {
				throw f;
			}
		}
	}

	/**
	 * Auto generated method signature for Asynchronous Invocations
	 * 
	 * @see org.apache.sandesha2.interop.RMInteropService#startEchoString
	 * @param param48
	 * 
	 */
	public void startEchoString(

			org.tempuri.EchoStringRequest param48,
			final org.apache.sandesha2.interop.RMInteropServiceCallbackHandler callback)

	throws java.rmi.RemoteException {

		org.apache.axis2.client.OperationClient _operationClient = _serviceClient
				.createClient(_operations[0].getName());
		_operationClient.getOptions().setAction("urn:wsrm:EchoString");
		_operationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);

		// create SOAP envelope with that payload
		org.apache.axiom.soap.SOAPEnvelope env = null;

		// Style is Doc.

		env = toEnvelope(
				getFactory(_operationClient.getOptions().getSoapVersionURI()),
				param48,
				optimizeContent(new javax.xml.namespace.QName("", "EchoString")));

		// adding SOAP headers
		_serviceClient.addHeadersToEnvelope(env);
		// create message context with that soap envelope
		org.apache.axis2.context.MessageContext _messageContext = new org.apache.axis2.context.MessageContext();
		_messageContext.setEnvelope(env);

		// add the message contxt to the operation client
		_operationClient.addMessageContext(_messageContext);

		_operationClient
				.setCallback(new org.apache.axis2.client.async.Callback() {
					public void onComplete(
							org.apache.axis2.client.async.AsyncResult result) {
						java.lang.Object object = fromOM(result
								.getResponseEnvelope().getBody()
								.getFirstElement(),
								org.tempuri.EchoStringResponse.class,
								getEnvelopeNamespaces(result
										.getResponseEnvelope()));
						callback
								.receiveResultEchoString((org.tempuri.EchoStringResponse) object);
					}

					public void onError(java.lang.Exception e) {
						callback.receiveErrorEchoString(e);
					}
				});

		org.apache.axis2.util.CallbackReceiver _callbackReceiver = null;
		if (_operations[0].getMessageReceiver() == null
				&& _operationClient.getOptions().isUseSeparateListener()) {
			_callbackReceiver = new org.apache.axis2.util.CallbackReceiver();
			_operations[0].setMessageReceiver(_callbackReceiver);
		}

		// execute the operation client
		_operationClient.execute(false);

	}

	public void Ping(org.tempuri.PingRequest param50

	) throws java.rmi.RemoteException {

		org.apache.axis2.client.OperationClient _operationClient = _serviceClient
				.createClient(_operations[1].getName());
		_operationClient.getOptions().setAction("urn:wsrm:Ping");
		_operationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);

		org.apache.axiom.soap.SOAPEnvelope env = null;

		// Style is Doc.

		env = toEnvelope(getFactory(_operationClient.getOptions()
				.getSoapVersionURI()), param50,
				optimizeContent(new javax.xml.namespace.QName("", "Ping")));

		// adding SOAP headers
		_serviceClient.addHeadersToEnvelope(env);
		// create message context with that soap envelope
		org.apache.axis2.context.MessageContext _messageContext = new org.apache.axis2.context.MessageContext();
		_messageContext.setEnvelope(env);

		// add the message contxt to the operation client
		_operationClient.addMessageContext(_messageContext);

		_operationClient.execute(true);
		return;
	}

	/**
	 * Auto generated method signature
	 * 
	 * @see org.apache.sandesha2.interop.RMInteropService#echoString
	 * @param param51
	 * 
	 */
	public org.tempuri.EchoStringResponse echoString(

	org.tempuri.EchoStringRequest param51) throws java.rmi.RemoteException

	{
		try {
			org.apache.axis2.client.OperationClient _operationClient = _serviceClient
					.createClient(_operations[2].getName());
			_operationClient.getOptions().setAction("urn:wsrm:EchoString");
			_operationClient.getOptions().setExceptionToBeThrownOnSOAPFault(
					true);

			// create SOAP envelope with that payload
			org.apache.axiom.soap.SOAPEnvelope env = null;

			// Style is Doc.

			env = toEnvelope(getFactory(_operationClient.getOptions()
					.getSoapVersionURI()), param51,
					optimizeContent(new javax.xml.namespace.QName("",
							"echoString")));

			// adding SOAP headers
			_serviceClient.addHeadersToEnvelope(env);
			// create message context with that soap envelope
			org.apache.axis2.context.MessageContext _messageContext = new org.apache.axis2.context.MessageContext();
			_messageContext.setEnvelope(env);

			// add the message contxt to the operation client
			_operationClient.addMessageContext(_messageContext);

			// execute the operation client
			_operationClient.execute(true);

			org.apache.axis2.context.MessageContext _returnMessageContext = _operationClient
					.getMessageContext(org.apache.axis2.wsdl.WSDLConstants.MESSAGE_LABEL_IN_VALUE);
			org.apache.axiom.soap.SOAPEnvelope _returnEnv = _returnMessageContext
					.getEnvelope();

			java.lang.Object object = fromOM(_returnEnv.getBody()
					.getFirstElement(), org.tempuri.EchoStringResponse.class,
					getEnvelopeNamespaces(_returnEnv));
			_messageContext.getTransportOut().getSender().cleanup(
					_messageContext);
			return (org.tempuri.EchoStringResponse) object;

		} catch (org.apache.axis2.AxisFault f) {
			org.apache.axiom.om.OMElement faultElt = f.getDetail();
			if (faultElt != null) {
				if (faultExeptionNameMap.containsKey(faultElt.getQName())) {
					// make the fault by reflection
					try {
						java.lang.String exceptionClassName = (java.lang.String) faultExeptionClassNameMap
								.get(faultElt.getQName());
						java.lang.Class exceptionClass = java.lang.Class
								.forName(exceptionClassName);
						java.lang.Exception ex = (java.lang.Exception) exceptionClass
								.newInstance();
						// message class
						java.lang.String messageClassName = (java.lang.String) faultMessageMap
								.get(faultElt.getQName());
						java.lang.Class messageClass = java.lang.Class
								.forName(messageClassName);
						java.lang.Object messageObject = fromOM(faultElt,
								messageClass, null);
						java.lang.reflect.Method m = exceptionClass.getMethod(
								"setFaultMessage",
								new java.lang.Class[] { messageClass });
						m.invoke(ex, new java.lang.Object[] { messageObject });

						throw new java.rmi.RemoteException(ex.getMessage(), ex);
					} catch (java.lang.ClassCastException e) {
						// we cannot intantiate the class - throw the original
						// Axis fault
						throw f;
					} catch (java.lang.ClassNotFoundException e) {
						// we cannot intantiate the class - throw the original
						// Axis fault
						throw f;
					} catch (java.lang.NoSuchMethodException e) {
						// we cannot intantiate the class - throw the original
						// Axis fault
						throw f;
					} catch (java.lang.reflect.InvocationTargetException e) {
						// we cannot intantiate the class - throw the original
						// Axis fault
						throw f;
					} catch (java.lang.IllegalAccessException e) {
						// we cannot intantiate the class - throw the original
						// Axis fault
						throw f;
					} catch (java.lang.InstantiationException e) {
						// we cannot intantiate the class - throw the original
						// Axis fault
						throw f;
					}
				} else {
					throw f;
				}
			} else {
				throw f;
			}
		}
	}

	/**
	 * Auto generated method signature for Asynchronous Invocations
	 * 
	 * @see org.apache.sandesha2.interop.RMInteropService#startechoString
	 * @param param51
	 * 
	 */
	public void startechoString(

			org.tempuri.EchoStringRequest param51,
			final org.apache.sandesha2.interop.RMInteropServiceCallbackHandler callback)

	throws java.rmi.RemoteException {

		org.apache.axis2.client.OperationClient _operationClient = _serviceClient
				.createClient(_operations[2].getName());
		_operationClient.getOptions().setAction("urn:wsrm:EchoString");
		_operationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);

		// create SOAP envelope with that payload
		org.apache.axiom.soap.SOAPEnvelope env = null;

		// Style is Doc.

		env = toEnvelope(
				getFactory(_operationClient.getOptions().getSoapVersionURI()),
				param51,
				optimizeContent(new javax.xml.namespace.QName("", "echoString")));

		// adding SOAP headers
		_serviceClient.addHeadersToEnvelope(env);
		// create message context with that soap envelope
		org.apache.axis2.context.MessageContext _messageContext = new org.apache.axis2.context.MessageContext();
		_messageContext.setEnvelope(env);

		// add the message contxt to the operation client
		_operationClient.addMessageContext(_messageContext);

		_operationClient
				.setCallback(new org.apache.axis2.client.async.Callback() {
					public void onComplete(
							org.apache.axis2.client.async.AsyncResult result) {
						java.lang.Object object = fromOM(result
								.getResponseEnvelope().getBody()
								.getFirstElement(),
								org.tempuri.EchoStringResponse.class,
								getEnvelopeNamespaces(result
										.getResponseEnvelope()));
						callback
								.receiveResultechoString((org.tempuri.EchoStringResponse) object);
					}

					public void onError(java.lang.Exception e) {
						callback.receiveErrorechoString(e);
					}
				});

		org.apache.axis2.util.CallbackReceiver _callbackReceiver = null;
		if (_operations[2].getMessageReceiver() == null
				&& _operationClient.getOptions().isUseSeparateListener()) {
			_callbackReceiver = new org.apache.axis2.util.CallbackReceiver();
			_operations[2].setMessageReceiver(_callbackReceiver);
		}

		// execute the operation client
		_operationClient.execute(false);

	}

	public void ping(org.tempuri.PingRequest param53

	) throws java.rmi.RemoteException {

		org.apache.axis2.client.OperationClient _operationClient = _serviceClient
				.createClient(_operations[3].getName());
		_operationClient.getOptions().setAction("urn:wsrm:Ping");
		_operationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);

		org.apache.axiom.soap.SOAPEnvelope env = null;

		// Style is Doc.

		env = toEnvelope(getFactory(_operationClient.getOptions()
				.getSoapVersionURI()), param53,
				optimizeContent(new javax.xml.namespace.QName("", "ping")));

		// adding SOAP headers
		_serviceClient.addHeadersToEnvelope(env);
		// create message context with that soap envelope
		org.apache.axis2.context.MessageContext _messageContext = new org.apache.axis2.context.MessageContext();
		_messageContext.setEnvelope(env);

		// add the message contxt to the operation client
		_operationClient.addMessageContext(_messageContext);

		_operationClient.execute(true);
		return;
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

	private javax.xml.namespace.QName[] opNameArray = null;

	private boolean optimizeContent(javax.xml.namespace.QName opName) {

		if (opNameArray == null) {
			return false;
		}
		for (int i = 0; i < opNameArray.length; i++) {
			if (opName.equals(opNameArray[i])) {
				return true;
			}
		}
		return false;
	}

	// http://www.example.org/
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
			org.tempuri.EchoStringRequest param, boolean optimizeContent) {
		org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory
				.getDefaultEnvelope();

		emptyEnvelope.getBody().addChild(
				param.getOMElement(org.tempuri.EchoStringRequest.MY_QNAME,
						factory));

		return emptyEnvelope;
	}

	private org.apache.axiom.soap.SOAPEnvelope toEnvelope(
			org.apache.axiom.soap.SOAPFactory factory,
			org.tempuri.PingRequest param, boolean optimizeContent) {
		org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory
				.getDefaultEnvelope();

		emptyEnvelope.getBody().addChild(
				param.getOMElement(org.tempuri.PingRequest.MY_QNAME, factory));

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

	private void setOpNameArray() {
		opNameArray = null;
	}

}
