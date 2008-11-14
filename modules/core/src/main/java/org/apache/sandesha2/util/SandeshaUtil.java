/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.sandesha2.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.CopyUtils;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.context.OperationContextFactory;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.context.ServiceGroupContext;
import org.apache.axis2.description.AxisDescription;
import org.apache.axis2.description.AxisModule;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisServiceGroup;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.engine.Handler;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.RMMsgContext;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.addressing.EPRDecorator;
import org.apache.sandesha2.client.SandeshaClientConstants;
import org.apache.sandesha2.context.ContextManager;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.policy.SandeshaPolicyBean;
import org.apache.sandesha2.security.SecurityManager;
import org.apache.sandesha2.security.SecurityToken;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.beanmanagers.RMSBeanMgr;
import org.apache.sandesha2.storage.beans.RMDBean;
import org.apache.sandesha2.storage.beans.RMSBean;
import org.apache.sandesha2.storage.beans.RMSequenceBean;
import org.apache.sandesha2.transport.Sandesha2TransportOutDesc;
import org.apache.sandesha2.workers.SandeshaThread;
import org.apache.sandesha2.wsrm.AckRequested;
import org.apache.sandesha2.wsrm.CloseSequence;
import org.apache.sandesha2.wsrm.CloseSequenceResponse;
import org.apache.sandesha2.wsrm.Sequence;
import org.apache.sandesha2.wsrm.SequenceAcknowledgement;

/**
 * Contains utility methods that are used in many plases of Sandesha2.
 */

public class SandeshaUtil {

	// private static Hashtable storedMsgContexts = new Hashtable();

	private static Log log = LogFactory.getLog(SandeshaUtil.class);
	
	private static AxisModule axisModule = null;

        
	/**
	 * Private Constructor.
         * All utility methods are static.
	 */
	private SandeshaUtil() {}

        public static AxisModule getAxisModule() {
		return axisModule;
	}

	public static void setAxisModule(AxisModule module) {
		axisModule = module;
	}

	/**
	 * Create a new UUID.
	 * 
	 * @return
	 */
	public static String getUUID() {
		// String uuid = "uuid:" + UUIDGenerator.getUUID();
		String uuid = org.apache.axiom.om.util.UUIDGenerator.getUUID();

		return uuid;
	}

	/**
	 * Used to convert a RangeString into a set of AcknowledgementRanges.
	 * 
	 * @param msgNoStr
	 * @param factory
	 * @return
	 * @throws SandeshaException
	 */
	public static ArrayList<Range> getAckRangeArrayList(RangeString completedMessageRanges, String rmNamespaceValue) {

		ArrayList<Range> ackRanges = new ArrayList<Range>(); //the final ack ranges that we will build up

		Range[] ranges = completedMessageRanges.getRanges();
		for(int i=0; i<ranges.length; i++){
			Range ackRange = new Range(ranges[i].lowerValue, ranges[i].upperValue);
			ackRanges.add(ackRange);			
		}
		
		return ackRanges;
	}

	public static void startWorkersForSequence(ConfigurationContext context, RMSequenceBean sequence)
	throws SandeshaException {
		if (LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled())
			log.debug("Enter: SandeshaUtil::startWorkersForSequence, sequence " + sequence);
		
		StorageManager mgr = getSandeshaStorageManager(context, context.getAxisConfiguration());
		boolean polling = sequence.isPollingMode();
		
		SandeshaThread sender = mgr.getSender();
		SandeshaThread invoker = mgr.getInvoker();
		SandeshaThread pollMgr = mgr.getPollingManager();
		
		// Only start the polling manager if we are configured to use MakeConnection
		if(polling && pollMgr == null) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.makeConnectionDisabled);
			throw new SandeshaException(message);
		}

		if(sequence instanceof RMSBean) {
			// We pass in the internal sequence id for internal sequences.
			String sequenceId = ((RMSBean)sequence).getInternalSequenceID();
			sender.runThreadForSequence(context, sequenceId, true);
			if(polling) pollMgr.runThreadForSequence(context, sequenceId, true);
		} else {
			String sequenceId = sequence.getSequenceID();
			sender.runThreadForSequence(context, sequenceId, false);
			if(invoker != null) invoker.runThreadForSequence(context, sequenceId, false);
			if(polling) pollMgr.runThreadForSequence(context, sequenceId, false);
		}
		
		if (LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled()) log.debug("Exit: SandeshaUtil::startWorkersForSequence");
	}

	public static String getMessageTypeString(int messageType) {
		switch (messageType) {
		case Sandesha2Constants.MessageTypes.CREATE_SEQ:
			return "CreateSequence";
		case Sandesha2Constants.MessageTypes.CREATE_SEQ_RESPONSE:
			return "CreateSequenceResponse";
		case Sandesha2Constants.MessageTypes.ACK:
			return "Acknowledgement";
		case Sandesha2Constants.MessageTypes.APPLICATION:
			return "Application";
		case Sandesha2Constants.MessageTypes.TERMINATE_SEQ:
			return "TerminateSequence";
		case Sandesha2Constants.MessageTypes.ACK_REQUEST:
			return "AckRequest";
		case Sandesha2Constants.MessageTypes.CLOSE_SEQUENCE:
			return "CloseSequence";
		case Sandesha2Constants.MessageTypes.CLOSE_SEQUENCE_RESPONSE:
			return "CloseSequenceResponse";
		case Sandesha2Constants.MessageTypes.TERMINATE_SEQ_RESPONSE:
			return "TerminateSequenceResponse";
		case Sandesha2Constants.MessageTypes.FAULT_MSG:
			return "Fault";
		case Sandesha2Constants.MessageTypes.MAKE_CONNECTION_MSG:
			return "MakeConnection";
		case Sandesha2Constants.MessageTypes.LAST_MESSAGE:
			return "LastMessage";
		case Sandesha2Constants.MessageTypes.UNKNOWN:
			return "Unknown";
		default:
			return "Error";
		}
	}

	public static String getServerSideIncomingSeqIdFromInternalSeqId(String internalSequenceId)
			throws SandeshaException {

		String startStr = Sandesha2Constants.INTERNAL_SEQUENCE_PREFIX + ":";
		if (!internalSequenceId.startsWith(startStr)) {
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.invalidInternalSequenceID,
					internalSequenceId));
		}

		String incomingSequenceId = internalSequenceId.substring(startStr.length());
		return incomingSequenceId;
	}

	/**
	 * Used to obtain the storage Manager Implementation.
	 * 
	 * @param context
	 * @return
	 * @throws SandeshaException
	 */
	public static StorageManager getSandeshaStorageManager(ConfigurationContext context,AxisDescription description) throws SandeshaException {
		final String STORAGE_MANAGER_INSTANCE = "storageManagerInstance";
		StorageManager storageManagerInstance = (StorageManager)context.getProperty(STORAGE_MANAGER_INSTANCE);
		
		if(storageManagerInstance == null){
		
			Parameter parameter = description.getParameter(Sandesha2Constants.STORAGE_MANAGER_PARAMETER);
			if (parameter==null) {
				parameter = new Parameter (Sandesha2Constants.STORAGE_MANAGER_PARAMETER,Sandesha2Constants.DEFAULT_STORAGE_MANAGER);
			}
			
			String value = (String) parameter.getValue();
			
			if (Sandesha2Constants.INMEMORY_STORAGE_MANAGER.equals(value))
				storageManagerInstance = getInMemoryStorageManager(context);
			else if (Sandesha2Constants.PERMANENT_STORAGE_MANAGER.equals(value))
				storageManagerInstance = getPermanentStorageManager(context);
			else
				throw new SandeshaException (SandeshaMessageHelper.getMessage(
						SandeshaMessageKeys.cannotGetStorageManager));
			
			context.setProperty(STORAGE_MANAGER_INSTANCE, storageManagerInstance);
		}
		
		return storageManagerInstance;
	}	
	
	public static StorageManager getInMemoryStorageManager(ConfigurationContext context) throws SandeshaException {

		StorageManager inMemoryStorageManager = null;
		
		AxisConfiguration config = context.getAxisConfiguration();
		Parameter parameter = config.getParameter(Sandesha2Constants.INMEMORY_STORAGE_MANAGER);
		if(parameter != null) inMemoryStorageManager = (StorageManager) parameter.getValue();
		if (inMemoryStorageManager != null)	return inMemoryStorageManager;

		try {
			//Currently module policies (default) are used to find the storage manager. These cant be overriden
			//TODO change this so that different services can hv different storage managers.
			String storageManagerClassStr = getDefaultPropertyBean(context.getAxisConfiguration()).getInMemoryStorageManagerClass();
			inMemoryStorageManager = getStorageManagerInstance(storageManagerClassStr,context);
			parameter = new Parameter(Sandesha2Constants.INMEMORY_STORAGE_MANAGER, inMemoryStorageManager);
			config.addParameter(parameter);
		} catch(AxisFault e) {
			String message = SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.cannotInitInMemoryStorageManager,
					e.toString());
			throw new SandeshaException(message, e);
		}
		
		return inMemoryStorageManager;
	}
	
	public static StorageManager getPermanentStorageManager(ConfigurationContext context) throws SandeshaException {

		StorageManager permanentStorageManager = null;
		
		AxisConfiguration config = context.getAxisConfiguration();
		Parameter parameter = config.getParameter(Sandesha2Constants.PERMANENT_STORAGE_MANAGER);
		if(parameter != null) permanentStorageManager = (StorageManager) parameter.getValue();
		if (permanentStorageManager != null)	return permanentStorageManager;

		try {
			//Currently module policies (default) are used to find the storage manager. These cant be overriden
			//TODO change this so that different services can hv different storage managers.
			String storageManagerClassStr = getDefaultPropertyBean(context.getAxisConfiguration()).getPermanentStorageManagerClass();
			permanentStorageManager = getStorageManagerInstance(storageManagerClassStr,context);
			parameter = new Parameter(Sandesha2Constants.PERMANENT_STORAGE_MANAGER, permanentStorageManager);
			config.addParameter(parameter);
		} catch(AxisFault e) {
			String message = SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.cannotInitPersistentStorageManager,
					e.toString());
			throw new SandeshaException(message, e);
		}
		
		return permanentStorageManager;
	}
	
	private static StorageManager getStorageManagerInstance (String className,ConfigurationContext context) throws SandeshaException {
		
		StorageManager storageManager = null;
		try {
			ClassLoader classLoader = null;
			AxisConfiguration config = context.getAxisConfiguration();
			Parameter classLoaderParam = config.getParameter(Sandesha2Constants.MODULE_CLASS_LOADER);
			if(classLoaderParam != null) classLoader = (ClassLoader) classLoaderParam.getValue(); 

		    if (classLoader==null)
		    	throw new SandeshaException (SandeshaMessageHelper.getMessage(
							SandeshaMessageKeys.classLoaderNotFound));
		    
		    Class c = classLoader.loadClass(className);
			Class configContextClass = context.getClass();
			
			Constructor constructor = c.getConstructor(new Class[] { configContextClass });
			Object obj = constructor.newInstance(new Object[] {context});

			if (obj == null || !(obj instanceof StorageManager))
				throw new SandeshaException(SandeshaMessageHelper.getMessage(
						SandeshaMessageKeys.storageManagerMustImplement));

			StorageManager mgr = (StorageManager) obj;
			storageManager = mgr;
			return storageManager;
			
		} catch (Exception e) {
			String message = SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.cannotGetStorageManager);
			if (LoggingControl.isAnyTracingEnabled() && log.isErrorEnabled())
			  log.error(message, e);
			throw new SandeshaException(message,e);
		}
	}

	public static int getSOAPVersion(SOAPEnvelope envelope) throws SandeshaException {
		String namespaceName = envelope.getNamespace().getNamespaceURI();
		if (namespaceName.equals(SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI))
			return Sandesha2Constants.SOAPVersion.v1_1;
		else if (namespaceName.equals(SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI))
			return Sandesha2Constants.SOAPVersion.v1_2;
		else
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.unknownSoapVersion,
					namespaceName));
	}


	public static MessageContext createNewRelatedMessageContext(RMMsgContext referenceRMMessage, AxisOperation operation)
			throws SandeshaException {
		try {
			MessageContext referenceMessage = referenceRMMessage.getMessageContext();
			ConfigurationContext configContext = referenceMessage.getConfigurationContext();
			AxisConfiguration axisConfiguration = configContext.getAxisConfiguration();

			MessageContext newMessageContext = new MessageContext();
			newMessageContext.setConfigurationContext(configContext);
			
			Options oldOptions = referenceMessage.getOptions();
            Options newOptions = new Options ();
            newOptions.setProperties(oldOptions.getProperties());
			
			newMessageContext.setOptions(newOptions);
			
			if (referenceMessage.getAxisServiceGroup() != null) {
				newMessageContext.setAxisServiceGroup(referenceMessage.getAxisServiceGroup());
				
				if (referenceMessage.getServiceGroupContext()!=null) {
					newMessageContext.setServiceGroupContext(referenceMessage.getServiceGroupContext());
					newMessageContext.setServiceGroupContextId(referenceMessage.getServiceGroupContextId());
				} else {
					ServiceGroupContext serviceGroupContext = new ServiceGroupContext (
							configContext,referenceMessage.getAxisServiceGroup());
					newMessageContext.setServiceGroupContext(serviceGroupContext);
				}
			} else {
				AxisServiceGroup axisServiceGroup = new AxisServiceGroup(axisConfiguration);
				ServiceGroupContext serviceGroupContext = new ServiceGroupContext(configContext, axisServiceGroup);

				newMessageContext.setAxisServiceGroup(axisServiceGroup);
				newMessageContext.setServiceGroupContext(serviceGroupContext);
			}

			if (referenceMessage.getAxisService() != null) {
				newMessageContext.setAxisService(referenceMessage.getAxisService());
				
				if (referenceMessage.getServiceContext()!=null) {
					newMessageContext.setServiceContext(referenceMessage.getServiceContext());
					newMessageContext.setServiceContextID(referenceMessage.getServiceContextID());
				} else {
					ServiceGroupContext sgc = newMessageContext.getServiceGroupContext();
					ServiceContext serviceContext = sgc.getServiceContext(referenceMessage.getAxisService());
					newMessageContext.setServiceContext(serviceContext);
				}
			}

			newMessageContext.setAxisOperation(operation);

			//The message created will basically be used as a outMessage, so setting the AxisMessage accordingly
			newMessageContext.setAxisMessage(operation.getMessage(WSDLConstants.MESSAGE_LABEL_OUT_VALUE));
			
			OperationContext operationContext = OperationContextFactory.createOperationContext(operation.getAxisSpecificMEPConstant(), operation, newMessageContext.getServiceContext());
			newMessageContext.setOperationContext(operationContext);
			operationContext.addMessageContext(newMessageContext);

			// adding a blank envelope
			SOAPFactory factory = SOAPAbstractFactory.getSOAPFactory(SandeshaUtil.getSOAPVersion(referenceMessage
					.getEnvelope()));
			newMessageContext.setEnvelope(factory.getDefaultEnvelope());

			newMessageContext.setTransportIn(referenceMessage.getTransportIn());
			newMessageContext.setTransportOut(referenceMessage.getTransportOut());

			// copying transport info.
			newMessageContext.setProperty(MessageContext.TRANSPORT_OUT, referenceMessage
					.getProperty(MessageContext.TRANSPORT_OUT));

			newMessageContext.setProperty(Constants.OUT_TRANSPORT_INFO, referenceMessage
					.getProperty(Constants.OUT_TRANSPORT_INFO));
			newMessageContext.setProperty(MessageContext.TRANSPORT_HEADERS, referenceMessage
					.getProperty(MessageContext.TRANSPORT_HEADERS));
			newMessageContext.setProperty(MessageContext.TRANSPORT_IN, referenceMessage
					.getProperty(MessageContext.TRANSPORT_IN));
			newMessageContext.setProperty(MessageContext.TRANSPORT_OUT, referenceMessage
					.getProperty(MessageContext.TRANSPORT_OUT));
			
			newMessageContext.setProperty(AddressingConstants.WS_ADDRESSING_VERSION, 
					referenceMessage.getProperty(AddressingConstants.WS_ADDRESSING_VERSION));
			newMessageContext.setProperty(AddressingConstants.DISABLE_ADDRESSING_FOR_OUT_MESSAGES, 
					referenceMessage.getProperty(AddressingConstants.DISABLE_ADDRESSING_FOR_OUT_MESSAGES));
			
			copyConfiguredProperties (referenceMessage,newMessageContext);

			//copying the serverSide property
			newMessageContext.setServerSide(referenceMessage.isServerSide());
			
			//this had to be set here to avoid a double invocation.
			newOptions.setUseSeparateListener(oldOptions.isUseSeparateListener());
			
			return newMessageContext;

		} catch (AxisFault e) {
            if (LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled())
              log.debug(e.getMessage());
			throw new SandeshaException(e.getMessage());
		}

	}
	
	public static void assertProofOfPossession(RMSequenceBean bean, MessageContext context, OMElement elementToCheck)throws SandeshaException{
		if (LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled()) 
			log.debug("Enter: SandeshaUtil::assertProofOfPossession :" + bean + ", " + context + ", " + (elementToCheck!=null ? elementToCheck.getQName() : null));
		
		String tokenData = null;
		if(bean!=null){
			tokenData = bean.getSecurityTokenData();
		}
		if(tokenData != null) {
			if (LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled()) log.debug("debug:" + tokenData);
			SecurityManager secManager = SandeshaUtil.getSecurityManager(context.getConfigurationContext());
			SecurityToken token = secManager.recoverSecurityToken(tokenData);
			secManager.checkProofOfPossession(token, elementToCheck, context); //this will exception if there is no proof
		}
		
		if (LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled())
			log.debug("Exit: SandeshaUtil::assertProofOfPossession");
	}
	

	public static void copyConfiguredProperties (MessageContext fromMessage, MessageContext toMessage) throws AxisFault {

//		copying properties as configured in the module.xml properties. Module xml has several
		//properties which gives comma seperated lists of property names that have to be copited
		//from various places when creating related messages.
		
		if (axisModule==null) {
			String message = SandeshaMessageKeys.moduleNotSet;
			throw new SandeshaException (message);
		}
		
		Parameter propertiesFromRefMsg = axisModule.getParameter(Sandesha2Constants.propertiesToCopyFromReferenceMessageAsStringArray);
		if (propertiesFromRefMsg!=null) {
			String[] propertyNames = (String[]) propertiesFromRefMsg.getValue();
			if (propertyNames!=null) {
				for (int i=0;i<propertyNames.length;i++) {
					String tmp = propertyNames[i];
					String propertyName = null;
					String targetPropertyName = null;
					if (tmp.indexOf (":")>=0) {
						//if the property name is given as two values seperated by a colon, this gives the key of the from msg
						//and the key for the To Msg respsctively.
						String[] vals = tmp.split(":");
						propertyName = vals[0].trim();
						targetPropertyName = vals[1].trim();
					} else {
						propertyName = targetPropertyName = tmp;
					}
					
					Object val = fromMessage.getProperty(propertyName);
					if (val!=null) {
						toMessage.setProperty(targetPropertyName,val);
					}
				}
			}
		}
		
		Parameter propertiesFromRefReqMsg = axisModule.getParameter(Sandesha2Constants.propertiesToCopyFromReferenceRequestMessageAsStringArray);
		OperationContext referenceOpCtx = fromMessage.getOperationContext();
		MessageContext referenceRequestMessage = null;
		if (referenceOpCtx!=null) 
			referenceRequestMessage=referenceOpCtx.getMessageContext(WSDLConstants.MESSAGE_LABEL_IN_VALUE);
		
		if (propertiesFromRefReqMsg!=null && referenceRequestMessage!=null) {
			String[] propertyNames = (String[]) propertiesFromRefReqMsg.getValue();
			if (propertyNames!=null) {
				for (int i=0;i<propertyNames.length;i++) {
					String propertyName = propertyNames[i];
					Object val = referenceRequestMessage.getProperty(propertyName);
					if (val!=null) {
						toMessage.setProperty(propertyName,val);
					}
				}
			}
		}

		
	}
	
	public static SandeshaPolicyBean getDefaultPropertyBean (AxisConfiguration axisConfiguration) throws SandeshaException {
		Parameter parameter = axisConfiguration.getParameter(Sandesha2Constants.SANDESHA_PROPERTY_BEAN);
		if (parameter==null)
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.defaultPropertyBeanNotSet));
		
		SandeshaPolicyBean sandeshaPropertyBean = (SandeshaPolicyBean) parameter.getValue();
		return sandeshaPropertyBean;
	}

	//TODO change this method.
	public static ArrayList<String> getArrayListFromString(String str) {

		if (str == null || "".equals(str))
			return new ArrayList<String>();

		if (str.length() < 2) {
			String message = SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.invalidStringArray,
					str);
            if (LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled())
              log.debug(message);
			throw new IllegalArgumentException (message);
		}

		int length = str.length();

		if (str.charAt(0) != '[' || str.charAt(length - 1) != ']') {
			String message = SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.invalidStringArray, str);
			if (LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled())
              log.debug(message);
			throw new IllegalArgumentException(message);
		}

		ArrayList<String> retArr = new ArrayList<String>();

		String subStr = str.substring(1, length - 1);

		String[] parts = subStr.split(",");

		for (int i = 0; i < parts.length; i++) {
			if (!"".equals(parts[i]))
				retArr.add(parts[i].trim());
		}

		return retArr;
	}

	public static String getInternalSequenceID(String to, String sequenceKey) {
		if (to == null && sequenceKey == null)
			return null;
		else if (to == null)
			return sequenceKey;
		else if (sequenceKey == null)
			return to;
		else
			return Sandesha2Constants.INTERNAL_SEQUENCE_PREFIX + ":" + to + ":" + sequenceKey;
	}

	public static String getOutgoingSideInternalSequenceID(String sequenceID) {
		return Sandesha2Constants.INTERNAL_SEQUENCE_PREFIX + ":" + sequenceID;
	}

	public static final RMSBean getRMSBeanFromInternalSequenceId(StorageManager storageManager, String internalSequenceID) 
	
	throws SandeshaException {
		RMSBeanMgr rmsBeanMgr = storageManager.getRMSBeanMgr();
		RMSBean bean = rmsBeanMgr.retrieveByInternalSequenceID(internalSequenceID);
		return bean;
	}
	
	public static final RMSBean getRMSBeanFromSequenceId(StorageManager storageManager, String sequenceID)  throws SandeshaException {
		RMSBeanMgr rmsBeanMgr = storageManager.getRMSBeanMgr();
		RMSBean bean = rmsBeanMgr.retrieveBySequenceID(sequenceID);
		return bean;
	}

	public static RMDBean getRMDBeanFromSequenceId(StorageManager storageManager, String sequenceID) throws SandeshaException {
		RMDBean bean = storageManager.getRMDBeanMgr().retrieve(sequenceID);
		return bean;
	}
	
	public static long getLastMessageNumber(String internalSequenceID, StorageManager storageManager)throws SandeshaException {
		RMSBean rMSBean = getRMSBeanFromInternalSequenceId(storageManager, internalSequenceID);
		long lastMessageNumber = 0;
		if(rMSBean!=null){
			lastMessageNumber = rMSBean.getHighestOutMessageNumber();
		}
		return lastMessageNumber;
	}

	public static String getSequenceIDFromInternalSequenceID(String internalSequenceID,
			StorageManager storageManager) throws SandeshaException {

		RMSBean rMSBean = getRMSBeanFromInternalSequenceId(storageManager, internalSequenceID);

		String sequeunceID = null;
		if (rMSBean != null && 
				rMSBean.getSequenceID() != null &&
				!rMSBean.getSequenceID().equals(Sandesha2Constants.TEMP_SEQUENCE_ID))
			sequeunceID = rMSBean.getSequenceID();

		return sequeunceID;
	}

	public static String getExecutionChainString(ArrayList<Handler> executionChain) {
		Iterator<Handler> iter = executionChain.iterator();

		String executionChainStr = "";
		while (iter.hasNext()) {
			Handler handler = (Handler) iter.next();
			String name = handler.getName();
			executionChainStr = executionChainStr + Sandesha2Constants.EXECUTIN_CHAIN_SEPERATOR + name;
		}

		return executionChainStr;
	}
	
	public static boolean hasReferenceParameters(EndpointReference epr){
		Map refParams = epr.getAllReferenceParameters();
		if(refParams!=null){
			if(!refParams.isEmpty()){
				return true;
			}
		}
		return false;
	}

	public static boolean isAllMsgsAckedUpto(long highestInMsgNo, String internalSequenceId,
			StorageManager storageManager) throws SandeshaException {

		RMSBean rmsBean = SandeshaUtil.getRMSBeanFromInternalSequenceId(storageManager, internalSequenceId);
		
		RangeString ackedMsgRanges = rmsBean.getClientCompletedMessages();
		long smallestMsgNo = 1;
		Range interestedRange = new Range(smallestMsgNo, highestInMsgNo);
		boolean allComplete = false;
		if(ackedMsgRanges!=null && ackedMsgRanges.isRangeCompleted(interestedRange)){
			allComplete = true;
		}
		return allComplete;
	
	}
	
	public static SandeshaPolicyBean getPropertyBean (AxisDescription axisDescription) throws SandeshaException {
		Parameter parameter = axisDescription.getParameter(Sandesha2Constants.SANDESHA_PROPERTY_BEAN);
		if (parameter==null)
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.propertyBeanNotSet));
		
		SandeshaPolicyBean propertyBean = (SandeshaPolicyBean) parameter.getValue();
		if (propertyBean==null) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.policyBeanNotFound);
			throw new SandeshaException (message);
		}

		return propertyBean;
	}

	public static String getSequenceIDFromRMMessage(RMMsgContext rmMessageContext) {
		int messageType = rmMessageContext.getMessageType();

		String sequenceID = null;
		if (messageType == Sandesha2Constants.MessageTypes.APPLICATION) {
			Sequence sequence = rmMessageContext.getSequence();
			sequenceID = sequence.getIdentifier().getIdentifier();
		} else if (messageType == Sandesha2Constants.MessageTypes.ACK) {
			Iterator<SequenceAcknowledgement> sequenceAckIter = rmMessageContext
					.getSequenceAcknowledgements();
			
			//In case of ack messages sequenceId is decided based on the sequenceId of the first 
			//sequence Ack. In other words Sandesha2 does not expect to receive two SequenceAcknowledgements
			//of different RM specifications in the same incoming message.
			
			SequenceAcknowledgement sequenceAcknowledgement = (SequenceAcknowledgement) sequenceAckIter.next();
			sequenceID = sequenceAcknowledgement.getIdentifier().getIdentifier();
		} else if (messageType == Sandesha2Constants.MessageTypes.ACK_REQUEST) {
			Iterator<AckRequested> ackRequestIter = rmMessageContext
					.getAckRequests();
	
			//In case of ack request messages sequenceId is decided based on the sequenceId of the first 
			//AckRequested.
			
			AckRequested ackReq = (AckRequested) ackRequestIter.next();
			sequenceID = ackReq.getIdentifier().getIdentifier();
		} else if (messageType == Sandesha2Constants.MessageTypes.CLOSE_SEQUENCE) {
			CloseSequence closeSequence = rmMessageContext.getCloseSequence();
			sequenceID = closeSequence.getIdentifier().getIdentifier();
		} else if (messageType == Sandesha2Constants.MessageTypes.CLOSE_SEQUENCE_RESPONSE) {
			CloseSequenceResponse closeSequenceResponse = rmMessageContext.getCloseSequenceResponse();;
			sequenceID = closeSequenceResponse.getIdentifier().getIdentifier();
		}

		// TODO complete for other message types

		return sequenceID;
	}
	
	public static String getSequenceKeyFromInternalSequenceID(String internalSequenceID, String to){
		if(to==null){
			//sequenceKey is just the internalSequenceID
			return internalSequenceID;
		}
		else{
			//remove the prefix
			int postPrefixStringIndex = internalSequenceID.indexOf(Sandesha2Constants.INTERNAL_SEQUENCE_PREFIX);
			if(postPrefixStringIndex>=0){
				String postPrefixString = internalSequenceID.substring(postPrefixStringIndex + Sandesha2Constants.INTERNAL_SEQUENCE_PREFIX.length());
				//strip of the to epr and trailing and trailing ":"
				String toEPRString = ":" + to + ":";
				int indexOfToEPR = postPrefixString.indexOf(toEPRString);
				if(indexOfToEPR>=0){
					return postPrefixString.substring(indexOfToEPR + toEPRString.length());
				}
			}
		}
		return null; //could not find the sequenceKey
	}
	

	public static SecurityManager getSecurityManager(ConfigurationContext context) throws SandeshaException {
		SecurityManager util = null;
		AxisConfiguration config = context.getAxisConfiguration();
		Parameter p = config.getParameter(Sandesha2Constants.SECURITY_MANAGER);
		if(p != null) util = (SecurityManager) p.getValue();
		if (util != null) return util;

		try {
			//Currently module policies are used to find the security impl. These cant be overriden
			String securityManagerClassStr = getDefaultPropertyBean(context.getAxisConfiguration()).getSecurityManagerClass();
			util = getSecurityManagerInstance(securityManagerClassStr,context);
			p = new Parameter(Sandesha2Constants.SECURITY_MANAGER,util);
			config.addParameter(p);
		} catch(AxisFault e) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.cannotInitSecurityManager, e.toString());
			throw new SandeshaException(message,e);
		}
		return util;
	}

	public static EPRDecorator getEPRDecorator(ConfigurationContext context) throws SandeshaException {
		EPRDecorator decorator = null;
		AxisConfiguration config = context.getAxisConfiguration();
		Parameter p = config.getParameter(Sandesha2Constants.EPR_DECORATOR);
		if(p != null) decorator = (EPRDecorator) p.getValue();
		if (decorator != null) return decorator;

		try {
			//Currently module policies are used to find the decorator impl. These cant be overriden
			String decoratorClassStr = getDefaultPropertyBean(context.getAxisConfiguration()).getEPRDecoratorClass();
			decorator = getEPRDecoratorInstance(decoratorClassStr,context);
			p = new Parameter(Sandesha2Constants.EPR_DECORATOR,decorator);
			config.addParameter(p);
		} catch(AxisFault e) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.cannotInitEPRDecorator, e.toString());
			throw new SandeshaException(message,e);
		}
		return decorator;
}
	
	private static EPRDecorator getEPRDecoratorInstance (String className,ConfigurationContext context) throws SandeshaException {
		try {
			ClassLoader classLoader = null;
			AxisConfiguration config = context.getAxisConfiguration();
			Parameter classLoaderParam = config.getParameter(Sandesha2Constants.MODULE_CLASS_LOADER);
			if(classLoaderParam != null) classLoader = (ClassLoader) classLoaderParam.getValue(); 
				  if (classLoader==null)
	    	throw new SandeshaException (SandeshaMessageHelper.getMessage(SandeshaMessageKeys.classLoaderNotFound));
		    
		  Class c = classLoader.loadClass(className);
			Class configContextClass = context.getClass();
			
			Constructor constructor = c.getConstructor(new Class[] { configContextClass });
			Object obj = constructor.newInstance(new Object[] {context});
					if (!(obj instanceof EPRDecorator)) {
				String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.eprDecoratorMustImplement, className);
				throw new SandeshaException(message);
			}
			return (EPRDecorator) obj;
			
		} catch (Exception e) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.cannotInitEPRDecorator, e.toString());
			throw new SandeshaException(message,e);
		}
	}
	
	private static SecurityManager getSecurityManagerInstance (String className,ConfigurationContext context) throws SandeshaException {
		try {
			ClassLoader classLoader = null;
			AxisConfiguration config = context.getAxisConfiguration();
			Parameter classLoaderParam = config.getParameter(Sandesha2Constants.MODULE_CLASS_LOADER);
			if(classLoaderParam != null) classLoader = (ClassLoader) classLoaderParam.getValue(); 

			
		  if (classLoader==null)
	    	throw new SandeshaException (SandeshaMessageHelper.getMessage(SandeshaMessageKeys.classLoaderNotFound));
		    
		  	Class c = classLoader.loadClass(className);		  
			Class configContextClass = context.getClass();
			
			Constructor constructor = c.getConstructor(new Class[] { configContextClass });
			Object obj = constructor.newInstance(new Object[] {context});

			if (!(obj instanceof SecurityManager)) {
				String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.securityManagerMustImplement, className);
				throw new SandeshaException(message);
			}
			return (SecurityManager) obj;
			
			
		} catch (Exception e) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.cannotInitSecurityManager, e.toString());
			throw new SandeshaException(message,e);
		}
	}
	
	public static ContextManager getContextManager(ConfigurationContext context) throws SandeshaException {
		ContextManager mgr = null;
		AxisConfiguration config = context.getAxisConfiguration();
		Parameter p = config.getParameter(Sandesha2Constants.CONTEXT_MANAGER);
		if(p != null) mgr = (ContextManager) p.getValue();
		if (mgr != null) return mgr;

		try {
			//Currently module policies are used to find the context impl. These cant be overriden
			String securityManagerClassStr = getDefaultPropertyBean(context.getAxisConfiguration()).getContextManagerClass();
			mgr = getContextManagerInstance(securityManagerClassStr,context);
			p = new Parameter(Sandesha2Constants.CONTEXT_MANAGER,mgr);
			config.addParameter(p);
		} catch(AxisFault e) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.cannotInitContextManager, e.toString());
			throw new SandeshaException(message,e);
		}
		return mgr;
	}

	private static ContextManager getContextManagerInstance(String className,ConfigurationContext context) throws SandeshaException {
		try {
			ClassLoader classLoader = null;
			AxisConfiguration config = context.getAxisConfiguration();
			Parameter classLoaderParam = config.getParameter(Sandesha2Constants.MODULE_CLASS_LOADER);
			if(classLoaderParam != null) classLoader = (ClassLoader) classLoaderParam.getValue(); 

			if (classLoader==null)
				throw new SandeshaException (SandeshaMessageHelper.getMessage(SandeshaMessageKeys.classLoaderNotFound));
		    
			Class c = classLoader.loadClass(className);
			Class configContextClass = context.getClass();
			
			Constructor constructor = c.getConstructor(new Class[] { configContextClass });
			Object obj = constructor.newInstance(new Object[] {context});

			if (!(obj instanceof ContextManager)) {
				String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.contextManagerMustImplement, className);
				throw new SandeshaException(message);
			}
			return (ContextManager) obj;
			
		} catch (Exception e) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.cannotInitContextManager, e.toString());
			throw new SandeshaException(message,e);
		}
	}

	public static boolean isWSRMAnonymous(String address) {
		if (address!=null && address.startsWith(Sandesha2Constants.SPEC_2007_02.ANONYMOUS_URI_PREFIX))
			return true;
		 
		return false;
	}
	 public static void executeAndStore (RMMsgContext rmMsgContext, String storageKey, StorageManager manager) throws AxisFault {
		if (LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled())
			log.debug("Enter: SandeshaUtil::executeAndStore, " + storageKey);
		
		MessageContext msgContext = rmMsgContext.getMessageContext();
		ConfigurationContext configurationContext = msgContext.getConfigurationContext();

    if(manager.requiresMessageSerialization()) {
			msgContext.setProperty(Sandesha2Constants.QUALIFIED_FOR_SENDING, Sandesha2Constants.VALUE_TRUE);

			StorageManager store = getSandeshaStorageManager(configurationContext, configurationContext.getAxisConfiguration());
			store.storeMessageContext(storageKey, msgContext);
			
		} else {
			// message will be stored in the Sandesha2TransportSender
			msgContext.setProperty(Sandesha2Constants.MESSAGE_STORE_KEY, storageKey);
	
			TransportOutDescription transportOut = msgContext.getTransportOut();
	
			msgContext.setProperty(Sandesha2Constants.ORIGINAL_TRANSPORT_OUT_DESC, transportOut);
			msgContext.setProperty(Sandesha2Constants.SET_SEND_TO_TRUE, Sandesha2Constants.VALUE_TRUE);
	
			Sandesha2TransportOutDesc sandesha2TransportOutDesc = new Sandesha2TransportOutDesc();
			msgContext.setTransportOut(sandesha2TransportOutDesc);
			
			//this invocation has to be a blocking one.
			Boolean isTransportNonBlocking = (Boolean) msgContext.getProperty(MessageContext.TRANSPORT_NON_BLOCKING);
			if (isTransportNonBlocking!=null && isTransportNonBlocking.booleanValue())
				msgContext.setProperty(MessageContext.TRANSPORT_NON_BLOCKING, Boolean.FALSE);
	
	 		// sending the message once through Sandesha2TransportSender.
			if (msgContext.isPaused())
				AxisEngine.resumeSend(msgContext);
			else {
				AxisEngine.send(msgContext);	
			}
			//put the original value of isTransportNonBlocking back on
			msgContext.setProperty(MessageContext.TRANSPORT_NON_BLOCKING, isTransportNonBlocking);
		}
		if (LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled())
			log.debug("Exit: SandeshaUtil::executeAndStore");
	}
	
	public static void modifyExecutionChainForStoring (MessageContext message, StorageManager manager)
	{
		
		Object property = message.getProperty(Sandesha2Constants.RETRANSMITTABLE_PHASES);
		if (property!=null)
			return; //Phases are already set. Dont hv to redo.
		
	    if(manager.requiresMessageSerialization())
			return; // No need to mess with the transport when we use message serialization
		
		TransportOutDescription transportOutDescription = message.getTransportOut();
		if (!(transportOutDescription instanceof Sandesha2TransportOutDesc))
			return; //This message is aimed to be stored only if, Sandesha2TransportOutDescription is set.
		
		List<Handler> executionChain = message.getExecutionChain();
		ArrayList<Handler> retransmittablePhases = new ArrayList<Handler>();
		
		int executionChainLength = executionChain.size();
		for(int i=0;i<executionChainLength;i++){
			Handler handler = executionChain.get(i);
			if("Security".equals(handler.getName())){
				retransmittablePhases.add(handler);
			}
		}
		executionChain.removeAll(retransmittablePhases);
				
		message.setProperty(Sandesha2Constants.RETRANSMITTABLE_PHASES, retransmittablePhases);
	}
	
        /**
         * Clone the MessageContext
         * @param oldMsg
         * @return
         * @throws AxisFault
         */
        public static MessageContext cloneMessageContext (MessageContext oldMsg) throws AxisFault {
		MessageContext newMsg = new MessageContext ();
		newMsg.setOptions(new Options (oldMsg.getOptions()));
		
                // Create a copy of the envelope
                SOAPEnvelope oldEnvelope = oldMsg.getEnvelope();
                if (oldEnvelope != null) {
                    SOAPEnvelope newEnvelope = copySOAPEnvelope(oldMsg.getEnvelope());
                    newMsg.setEnvelope(newEnvelope);
                }
                
		newMsg.setConfigurationContext(oldMsg.getConfigurationContext());
		newMsg.setAxisService(oldMsg.getAxisService());
		newMsg.setTransportOut(oldMsg.getTransportOut());
		newMsg.setTransportIn(oldMsg.getTransportIn());

                //Copy property objects from oldMsg to newMsg
		copyConfiguredProperties(oldMsg,newMsg);
		return newMsg;
		
	}

	/** 
	 * Create a copy of the SOAPEnvelope
	 * @param sourceEnv
	 * @return targetEnv
	*/
	public static SOAPEnvelope copySOAPEnvelope(SOAPEnvelope sourceEnv) {
        if (LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled()) 
            log.debug("Enter: SandeshaUtil::copySOAPEnvelope");
        
        // Delegate to the CopuUtils provided by Axiom
        SOAPEnvelope targetEnv = CopyUtils.copy(sourceEnv);
        
        if (LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled()) 
            log.debug("Exit: SandeshaUtil::copySOAPEnvelope");            
		            
		return targetEnv;
	}
	
	public static void reallocateMessagesToNewSequence(StorageManager storageManager, RMSBean oldRMSBean, List<MessageContext> msgsToSend)throws AxisFault{
	    if (LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled())
	        log.debug("Enter: SandeshaUtil::reallocateMessagesToNewSequence");
	    
		ConfigurationContext ctx = storageManager.getContext();
		ServiceClient client = new ServiceClient(ctx,  null);
		
		//populate the client options
		Options options = client.getOptions();
		options.setTo(oldRMSBean.getToEndpointReference());
		options.setReplyTo(oldRMSBean.getReplyToEndpointReference());
		
        //internal sequence ID is different
        String internalSequenceID = oldRMSBean.getInternalSequenceID();
        //we also need to obtain the sequenceKey from the internalSequenceID.
        String oldSequenceKey = 
          SandeshaUtil.getSequenceKeyFromInternalSequenceID(internalSequenceID, oldRMSBean.getToEndpointReference().getAddress());
        //remove the old sequence key from the internal sequence ID
        internalSequenceID = internalSequenceID.substring(0, internalSequenceID.length()-oldSequenceKey.length());
        options.setProperty(SandeshaClientConstants.SEQUENCE_KEY, 
        		SandeshaUtil.getUUID()); //using a new sequence Key to differentiate from the old sequence 
        options.setProperty(Sandesha2Constants.MessageContextProperties.INTERNAL_SEQUENCE_ID, internalSequenceID);
        options.setProperty(SandeshaClientConstants.RM_SPEC_VERSION, oldRMSBean.getRMVersion());
      	options.setProperty(AddressingConstants.DISABLE_ADDRESSING_FOR_OUT_MESSAGES, Boolean.FALSE);
      	
        //send the msgs - this will setup a new sequence to the same endpoint
      	Iterator<MessageContext> it = msgsToSend.iterator();
      	while(it.hasNext()){
      		MessageContext msgCtx = (MessageContext)it.next();
      		client.getOptions().setAction(msgCtx.getWSAAction());
      		client.fireAndForget(msgCtx.getEnvelope().getBody().cloneOMElement().getFirstElement());
      	}
      	
	    if (LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled())
	        log.debug("Exit: SandeshaUtil::reallocateMessagesToNewSequence");
	}

  /**
   * Remove the MustUnderstand header blocks.
   * @param envelope
   */
  public static SOAPEnvelope removeMustUnderstand(SOAPEnvelope envelope) {
    if (LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled())
      log.debug("Enter: SandeshaUtil::removeMustUnderstand");
    // you have to explicitely set the 'processed' attribute for header
    // blocks, since it get lost in the above read from the stream.

    SOAPHeader header = envelope.getHeader();
    if (header != null) {
      Iterator childrenOfOldEnv = header.getChildElements();
      while (childrenOfOldEnv.hasNext()) {
        
        SOAPHeaderBlock oldEnvHeaderBlock = (SOAPHeaderBlock) childrenOfOldEnv.next();

        QName oldEnvHeaderBlockQName = oldEnvHeaderBlock.getQName();
        if (oldEnvHeaderBlockQName != null) {
          // If we've processed the part and it has a must understand, set it as processed
          if (oldEnvHeaderBlock.isProcessed() && oldEnvHeaderBlock.getMustUnderstand()) {
            // Remove the MustUnderstand part
            oldEnvHeaderBlock.setMustUnderstand(false);
          }
        }
      }
    }
    
    if (LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled())
      log.debug("Exit: SandeshaUtil::removeMustUnderstand");
    return envelope;
  }

	public static EndpointReference cloneEPR (EndpointReference epr) {
		EndpointReference newEPR = new EndpointReference (epr.getAddress());
		Map referenceParams = epr.getAllReferenceParameters();
		
		if (referenceParams != null) {
			for (Iterator entries = referenceParams.entrySet().iterator(); entries
					.hasNext();) {
				Entry entry = (Entry)entries.next();
				Object referenceParam = entry.getValue();

				if (referenceParam instanceof OMElement) {
					OMElement clonedElement = ((OMElement) referenceParam)
							.cloneOMElement();
					clonedElement.setText("false");
					newEPR.addReferenceParameter(clonedElement);
				}
			}
		}
		
		return newEPR;
	}	
	
	public static boolean isForbidMixedEPRsOnSequence(MessageContext mc)
	{
		if(LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled()) log.debug("Entry: SandeshaUtil::isForbidMixedEPRsOnSequence");
		boolean result = false;

		//look at the msg ctx first
		String auto = (String) mc.getProperty(SandeshaClientConstants.FORBID_MIXED_EPRS_ON_SEQUENCE);
		if ("true".equals(auto)) {
			if (log.isDebugEnabled()) log.debug("Mixed EPRs forbidden on message context");
			result = true;
		}			
		
		if(!result) {
			//look at the operation
			if (mc.getAxisOperation() != null) {
				Parameter mixedParam = mc.getAxisOperation().getParameter(SandeshaClientConstants.FORBID_MIXED_EPRS_ON_SEQUENCE);
				if (null != mixedParam && "true".equals(mixedParam.getValue())) {
					if (log.isDebugEnabled()) log.debug("mixed EPRs forbidden on operation");
					result = true;
				}
			}
		}
		
		if(LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled()) log.debug("Exit: SandeshaUtil::isForbidMixedEPRsOnSequence, " + result);
		return result;			
	}
	
	public static boolean isAutoStartNewSequence(MessageContext mc){
		if(LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled()) log.debug("Entry: SandeshaUtil::isAutoStartNewSequence");
		boolean result = false;

		//look at the msg ctx first
		String auto = (String) mc.getProperty(SandeshaClientConstants.AUTO_START_NEW_SEQUENCE);
		if ("true".equals(auto)) {
			if (LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled()) log.debug("Autostart message context");
			result = true;
		}			
		
		if(!result) {
			//look at the operation
			if (mc.getAxisOperation() != null) {
				Parameter autoParam = mc.getAxisOperation().getParameter(SandeshaClientConstants.AUTO_START_NEW_SEQUENCE);
				if (null != autoParam && "true".equals(autoParam.getValue())) {
					if (LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled()) log.debug("autostart operation");
					result = true;
				}
			}
		}
		
		if(LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled()) log.debug("Exit: SandeshaUtil::isAutoStartNewSequence, " + result);
		return result;		
	}
	
	public static boolean isMessageUnreliable(MessageContext mc) {
		if(LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled()) log.debug("Entry: SandeshaUtil::isMessageUnreliable");
		boolean result = false;

		//look at the msg ctx first. It is either forced on or off at the msg ctx level
		String unreliable = (String) mc.getProperty(SandeshaClientConstants.UNRELIABLE_MESSAGE);
		if ("true".equals(unreliable)) {
			if (LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled()) log.debug("Unreliable message context");
			result = true;
		}		
		else if("false".equals(unreliable)){
			//a forced reliable message
			if (LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled()) log.debug("Forced reliable message context");
			result = false;
		}	
		else if(!result) {
			//look at the operation
			if (mc.getAxisOperation() != null) {
				Parameter unreliableParam = mc.getAxisOperation().getParameter(SandeshaClientConstants.UNRELIABLE_MESSAGE);
				if (null != unreliableParam && "true".equals(unreliableParam.getValue())) {
					if (LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled()) log.debug("Unreliable operation");
					result = true;
				}
				else if(null != unreliableParam && "false".equals(unreliable)){
					//a forced reliable message
					if (LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled()) log.debug("Forced reliable message context");
					result = false;
				}	
			}
		}
		
		if(LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled()) log.debug("Exit: SandeshaUtil::isMessageUnreliable, " + result);
		return result;
	}
	
	
	public static boolean isDuplicateInOnlyMessage(MessageContext msgContext)
	{
		AxisOperation operation = msgContext.getAxisOperation();
		String localName = operation.getName().getLocalPart();
		if(localName.equals(Sandesha2Constants.RM_DUPLICATE_IN_ONLY_OPERATION.getLocalPart())){
			return true;
		}
		
		return false;
	}
	
	public static boolean isDuplicateInOutMessage(MessageContext msgContext)
	{
		AxisOperation operation = msgContext.getAxisOperation();
		String localName = operation.getName().getLocalPart();
		if(localName.equals(Sandesha2Constants.RM_DUPLICATE_IN_OUT_OPERATION.getLocalPart())){
			return true;
		}
		
		return false;
	}	
	 	  
	public static final String getStackTraceFromException(Exception e) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintWriter pw = new PrintWriter(baos);
    e.printStackTrace(pw);
    pw.flush();
    String stackTrace = baos.toString();
    return stackTrace;
	}

	public static EndpointReference rewriteEPR(RMSBean sourceBean, EndpointReference epr, ConfigurationContext configContext)
	throws SandeshaException
	{
		if (LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled())
			log.debug("Enter: SandeshaUtil::rewriteEPR " + epr);

		SandeshaPolicyBean policy = SandeshaUtil.getPropertyBean(configContext.getAxisConfiguration());
		if(!policy.isEnableRMAnonURI()) {
			if (LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled())
				log.debug("Exit: SandeshaUtil::rewriteEPR, anon uri is disabled");
			return epr;
		}

		// Handle EPRs that have not yet been set. These are effectively WS-A anon, and therefore
		// we can rewrite them.
		if(epr == null) epr = new EndpointReference(null);
		
		String address = epr.getAddress();
		if(address == null ||
		   AddressingConstants.Final.WSA_ANONYMOUS_URL.equals(address) ||
		   AddressingConstants.Submission.WSA_ANONYMOUS_URL.equals(address)) {
			// We use the sequence to hold the anonymous uuid, so that messages assigned to the
			// sequence will use the same UUID to identify themselves
			String uuid = sourceBean.getAnonymousUUID();
			if(uuid == null) {
				uuid = Sandesha2Constants.SPEC_2007_02.ANONYMOUS_URI_PREFIX + SandeshaUtil.getUUID();
				sourceBean.setAnonymousUUID(uuid);
			}
			
			if(LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled()) log.debug("Rewriting EPR with anon URI " + uuid);
			epr.setAddress(uuid);
		}
		
		if (LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled())
			log.debug("Exit: SandeshaUtil::rewriteEPR " + epr);
		return epr;
	}

	public static boolean isInOrder(MessageContext context) throws SandeshaException {
		if (LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled()) log.debug("Enter: SandeshaUtil::isInOrder");
		
		SandeshaPolicyBean policy = getPropertyBean(context.getConfigurationContext().getAxisConfiguration());
		boolean result = policy.isInOrder();
		
		if (LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled()) log.debug("Enter: SandeshaUtil::isInOrder, " + result);
		return result;
	}

}
