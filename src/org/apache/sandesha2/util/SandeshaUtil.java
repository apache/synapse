/*
 * Copyright  1999-2004 The Apache Software Foundation.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.sandesha2.util;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.xml.namespace.QName;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axiom.soap.impl.builder.StAXSOAPModelBuilder;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
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
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.policy.SandeshaPolicyBean;
import org.apache.sandesha2.polling.PollingManager;
import org.apache.sandesha2.security.SecurityManager;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.beanmanagers.RMDBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.RMSBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.SequencePropertyBeanMgr;
import org.apache.sandesha2.storage.beans.RMDBean;
import org.apache.sandesha2.storage.beans.RMSBean;
import org.apache.sandesha2.storage.beans.SequencePropertyBean;
import org.apache.sandesha2.transport.Sandesha2TransportOutDesc;
import org.apache.sandesha2.workers.SandeshaThread;
import org.apache.sandesha2.wsrm.AckRequested;
import org.apache.sandesha2.wsrm.AcknowledgementRange;
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
	 * Used to convert a message number list (a comma seperated list of message
	 * numbers) into a set of AcknowledgementRanges. This breaks the list, sort
	 * the items and group them to create the AcknowledgementRange objects.
	 * 
	 * @param msgNoStr
	 * @param factory
	 * @return
	 * @throws SandeshaException
	 */
	public static ArrayList getAckRangeArrayList(List completedMessages, String rmNamespaceValue)
			throws SandeshaException {

		ArrayList ackRanges = new ArrayList();

		ArrayList sortedMsgNoArrayList = getSortedMsgNoArrayList(completedMessages);

		Iterator iterator = sortedMsgNoArrayList.iterator();
		long lower = 0;
		long upper = 0;
		boolean completed = true;

		while (iterator.hasNext()) {
			Long tempLng = (Long) iterator.next();
			long temp = tempLng.longValue();
			if (lower == 0) {
				lower = temp;
				upper = temp;
				completed = false;
			} else if (temp == (upper + 1)) {
				upper = temp;
				completed = false;
			} else {
				// add ackRange (lower,upper)
				AcknowledgementRange ackRange = new AcknowledgementRange(rmNamespaceValue);
				ackRange.setLowerValue(lower);
				ackRange.setUpperValue(upper);
				ackRanges.add(ackRange);

				lower = temp;
				upper = temp;
				completed = false;
			}
		}

		if (!completed) {
			AcknowledgementRange ackRange = new AcknowledgementRange(rmNamespaceValue);
			ackRange.setLowerValue(lower);
			ackRange.setUpperValue(upper);
			ackRanges.add(ackRange);
			completed = true;
		}

		return ackRanges;
	}

	public static ArrayList getSortedMsgNoArrayList(List list) {

		ArrayList sortedList = new ArrayList();

		long max = 0;
		Iterator it1 = list.iterator();
		while (it1.hasNext()) {
			Long tempLng = (Long) it1.next();
			long temp = tempLng.longValue();
			if (temp > max)
				max = temp;
		}

		int item = 0;
		for (long i = 1; i <= max; i++) {
			Long temp = new Long(i);
			if (list.contains(temp)) {
				sortedList.add(item, temp);
				item++;
			}
		}

		return sortedList;
	}

	public static void startSenderForTheSequence(ConfigurationContext context, String sequenceID) throws SandeshaException {
		if (log.isDebugEnabled())
			log.debug("Enter: SandeshaUtil::startSenderForTheSequence , context " + context + ", sequenceID " + sequenceID);
		
		SandeshaThread sender = getSandeshaStorageManager(context, context.getAxisConfiguration()).getSender();		
		sender.runThreadForSequence(context, sequenceID);
		
		if (log.isDebugEnabled())
			log.debug("Exit: SandeshaUtil::startSenderForTheSequence");
	}

	public static void startInvokerForTheSequence(ConfigurationContext context, String sequenceID) throws SandeshaException {
		if (log.isDebugEnabled())
			log.debug("Enter: SandeshaUtil::startInvokerForTheSequence , context " + context + ", sequenceID " + sequenceID);
		
		SandeshaThread invoker = getSandeshaStorageManager(context, context.getAxisConfiguration()).getInvoker();
		invoker.runThreadForSequence(context,sequenceID);

		if (log.isDebugEnabled())
			log.debug("Exit: SandeshaUtil::startInvokerForTheSequence");			
	}

	public static void startPollingManager (ConfigurationContext configurationContext) throws SandeshaException {
		PollingManager pollingManager = (PollingManager) configurationContext.getProperty(
				Sandesha2Constants.POLLING_MANAGER);
		
		//assums that if somebody hs set the PollingManager, he must hv already started it.
		if (pollingManager==null) {
			pollingManager = new PollingManager ();
			configurationContext.setProperty(Sandesha2Constants.POLLING_MANAGER,pollingManager);
			pollingManager.start(configurationContext);
		}
	}
	
	public static void stopPollingManager (ConfigurationContext configurationContext) {
		PollingManager pollingManager = (PollingManager) configurationContext.getProperty(
				Sandesha2Constants.POLLING_MANAGER);
		if (pollingManager!=null) 
			pollingManager.stopPolling ();
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
		case Sandesha2Constants.MessageTypes.UNKNOWN:
			return "Unknown";
		default:
			return "Error";
		}
	}

	public static boolean isGloballyProcessableMessageType(int type) {
		if (type == Sandesha2Constants.MessageTypes.ACK || type == Sandesha2Constants.MessageTypes.TERMINATE_SEQ) {
			return true;
		}

		return false;
	}

	public static boolean isDuplicateDropRequiredMsgType(int rmMessageType) {
		if (rmMessageType == Sandesha2Constants.MessageTypes.APPLICATION)
			return true;

		if (rmMessageType == Sandesha2Constants.MessageTypes.CREATE_SEQ_RESPONSE)
			return true;

		return false;
	}

	public static ArrayList getSplittedMsgNoArraylist(String str) {

		StringTokenizer tokenizer = new StringTokenizer(str, ",");

		ArrayList results = new ArrayList();

		while (tokenizer.hasMoreTokens()) {
			results.add(tokenizer.nextToken());
		}

		return results;
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

		Parameter parameter = description.getParameter(Sandesha2Constants.STORAGE_MANAGER_PARAMETER);
		if (parameter==null) {
			parameter = new Parameter (Sandesha2Constants.STORAGE_MANAGER_PARAMETER,Sandesha2Constants.DEFAULT_STORAGE_MANAGER);
		}
		
		String value = (String) parameter.getValue();
		
		if (Sandesha2Constants.INMEMORY_STORAGE_MANAGER.equals(value))
			return getInMemoryStorageManager(context);
		else if (Sandesha2Constants.PERMANENT_STORAGE_MANAGER.equals(value))
			return getPermanentStorageManager(context);
		else
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.cannotGetStorageManager));
	}
	
	public static StorageManager getInMemoryStorageManager(ConfigurationContext context) throws SandeshaException {

		StorageManager inMemoryStorageManager = (StorageManager) context.getProperty(Sandesha2Constants.INMEMORY_STORAGE_MANAGER);
		if (inMemoryStorageManager != null)
			return inMemoryStorageManager;

		//Currently module policies (default) are used to find the storage manager. These cant be overriden
		//TODO change this so that different services can hv different storage managers.
		String storageManagerClassStr = getDefaultPropertyBean(context.getAxisConfiguration()).getInMemoryStorageManagerClass();
		inMemoryStorageManager = getStorageManagerInstance(storageManagerClassStr,context);
		context.setProperty(Sandesha2Constants.INMEMORY_STORAGE_MANAGER,inMemoryStorageManager);
		
		return inMemoryStorageManager;
	}
	
	public static StorageManager getPermanentStorageManager(ConfigurationContext context) throws SandeshaException {

		StorageManager permanentStorageManager = (StorageManager) context.getProperty(Sandesha2Constants.PERMANENT_STORAGE_MANAGER);
		if (permanentStorageManager != null)
			return permanentStorageManager;

		//Currently module policies (default) are used to find the storage manager. These cant be overriden
		//TODO change this so that different services can hv different storage managers.
		String storageManagerClassStr = getDefaultPropertyBean(context.getAxisConfiguration()).getPermanentStorageManagerClass ();
		permanentStorageManager = getStorageManagerInstance(storageManagerClassStr,context);
		context.setProperty(Sandesha2Constants.PERMANENT_STORAGE_MANAGER,permanentStorageManager);
		
		return permanentStorageManager;
	}
	
	private static StorageManager getStorageManagerInstance (String className,ConfigurationContext context) throws SandeshaException {
		
		StorageManager storageManager = null;
		try {
		    ClassLoader classLoader = (ClassLoader)	context.getProperty(Sandesha2Constants.MODULE_CLASS_LOADER);

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
			if (log.isErrorEnabled())
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

	public static boolean isRMGlobalMessage(MessageContext msgCtx) {
		boolean rmGlobalMsg = false;

		String action = msgCtx.getWSAAction();
		SOAPEnvelope env = msgCtx.getEnvelope();
		SOAPHeader header = null;
		if (env != null)
			header = env.getHeader();
		else {
			log.error(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.soapEnvNotSet));
			return false;
		}

		// TODO make this spec indipendent

		OMElement sequenceElem = null;
		if (header != null) {
			sequenceElem = header.getFirstChildWithName(new QName(Sandesha2Constants.SPEC_2005_02.NS_URI,
						Sandesha2Constants.WSRM_COMMON.SEQUENCE));

			if (sequenceElem == null)
				sequenceElem = header.getFirstChildWithName(new QName(Sandesha2Constants.SPEC_2006_08.NS_URI,
						Sandesha2Constants.WSRM_COMMON.SEQUENCE));
		}

		if (sequenceElem != null)
			rmGlobalMsg = true;

		if (Sandesha2Constants.SPEC_2005_02.Actions.ACTION_SEQUENCE_ACKNOWLEDGEMENT.equals(action))
			rmGlobalMsg = true;

		if (Sandesha2Constants.SPEC_2005_02.Actions.ACTION_CREATE_SEQUENCE_RESPONSE.equals(action))
			rmGlobalMsg = true;

		if (Sandesha2Constants.SPEC_2005_02.Actions.ACTION_TERMINATE_SEQUENCE.equals(action))
			rmGlobalMsg = true;

		if (Sandesha2Constants.SPEC_2006_08.Actions.ACTION_SEQUENCE_ACKNOWLEDGEMENT.equals(action))
			rmGlobalMsg = true;

		if (Sandesha2Constants.SPEC_2006_08.Actions.ACTION_TERMINATE_SEQUENCE.equals(action))
			rmGlobalMsg = true;

		if (Sandesha2Constants.SPEC_2006_08.Actions.ACTION_CREATE_SEQUENCE_RESPONSE.equals(action))
			rmGlobalMsg = true;

		return rmGlobalMsg;
	}

	// RM will retry sending the message even if a fault arrive for following message types.
	public static boolean isRetriableOnFaults(MessageContext msgCtx) {
		boolean rmGlobalMsg = false;

		String action = msgCtx.getWSAAction();


		if (Sandesha2Constants.SPEC_2005_02.Actions.ACTION_CREATE_SEQUENCE.equals(action))
			rmGlobalMsg = true;

		if (Sandesha2Constants.SPEC_2006_08.Actions.ACTION_CREATE_SEQUENCE.equals(action))
			rmGlobalMsg = true;

		return rmGlobalMsg;
	}
	
	public static MessageContext createNewRelatedMessageContext(RMMsgContext referenceRMMessage, AxisOperation operation)
			throws SandeshaException {
		try {
			MessageContext referenceMessage = referenceRMMessage.getMessageContext();
			ConfigurationContext configContext = referenceMessage.getConfigurationContext();
			AxisConfiguration axisConfiguration = configContext.getAxisConfiguration();

			MessageContext newMessageContext = new MessageContext();
			newMessageContext.setConfigurationContext(configContext);
			
			Options newOptions = new Options ();
			
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
					ServiceContext serviceContext = new ServiceContext (referenceMessage.getAxisService(),newMessageContext.getServiceGroupContext());
					newMessageContext.setServiceContext(serviceContext);
				}
			}

			newMessageContext.setAxisOperation(operation);

			OperationContext operationContext = new OperationContext(operation);
			newMessageContext.setOperationContext(operationContext);
			operationContext.addMessageContext(newMessageContext);

			// adding a blank envelope
			SOAPFactory factory = SOAPAbstractFactory.getSOAPFactory(SandeshaUtil.getSOAPVersion(referenceMessage
					.getEnvelope()));
			newMessageContext.setEnvelope(factory.getDefaultEnvelope());

			newMessageContext.setTransportIn(referenceMessage.getTransportIn());
			newMessageContext.setTransportOut(referenceMessage.getTransportOut());

			copyNecessaryPropertiesFromRelatedContext(referenceMessage, newMessageContext);

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
			
			//copying properties as configured in the module.xml properties. Module xml has several
			//properties which gives comma seperated lists of property names that have to be copited
			//from various places when creating related messages.
			
			AxisModule axisModule = SandeshaUtil.getAxisModule();

			Parameter propertiesFromRefMsg = axisModule.getParameter(Sandesha2Constants.propertiesToCopyFromReferenceMessage);
			if (propertiesFromRefMsg!=null) {
				String value = (String) propertiesFromRefMsg.getValue();
				if (value!=null) {
					value = value.trim();
					String[] propertyNames = value.split(",");
					for (int i=0;i<propertyNames.length;i++) {
						String propertyName = propertyNames[i];
						Object val = referenceMessage.getProperty(propertyName);
						if (val!=null) {
							newMessageContext.setProperty(propertyName,val);
						}
					}
				}
			}
			
			Parameter propertiesFromRefReqMsg = axisModule.getParameter(Sandesha2Constants.propertiesToCopyFromReferenceRequestMessage);
			OperationContext referenceOpCtx = referenceMessage.getOperationContext();
			MessageContext referenceRequestMessage = null;
			if (referenceOpCtx!=null) 
				referenceRequestMessage=referenceOpCtx.getMessageContext(WSDLConstants.MESSAGE_LABEL_IN_VALUE);
			
			if (propertiesFromRefReqMsg!=null && referenceRequestMessage!=null) {
				String value = (String) propertiesFromRefReqMsg.getValue();
				if (value!=null) {
					value = value.trim();
					String[] propertyNames = value.split(",");
					for (int i=0;i<propertyNames.length;i++) {
						String propertyName = propertyNames[i];
						Object val = referenceRequestMessage.getProperty(propertyName);
						if (val!=null) {
							newMessageContext.setProperty(propertyName,val);
						}
					}
				}
			}

            
			newMessageContext.setExecutionChain(referenceMessage.getExecutionChain());

			return newMessageContext;

		} catch (AxisFault e) {
			log.debug(e.getMessage());
			throw new SandeshaException(e.getMessage());
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

	private static void copyNecessaryPropertiesFromRelatedContext(MessageContext fromMessage, MessageContext toMessage) throws SandeshaException {
		toMessage.setProperty(Constants.Configuration.TRANSPORT_URL, fromMessage
				.getProperty(Constants.Configuration.TRANSPORT_URL));
		
		String addressingVersion = (String) fromMessage.getProperty(AddressingConstants.WS_ADDRESSING_VERSION);
		if (addressingVersion==null) {
			OperationContext opCtx = fromMessage.getOperationContext();
			if (opCtx!=null) {
				try {
					MessageContext requestMsg = opCtx.getMessageContext(WSDLConstants.MESSAGE_LABEL_IN_VALUE);
					if (requestMsg!=null)
						addressingVersion = (String) requestMsg.getProperty(AddressingConstants.WS_ADDRESSING_VERSION);
				} catch (AxisFault e) {
					throw new SandeshaException (e);
				}
			}
		}
		toMessage.setProperty(AddressingConstants.WS_ADDRESSING_VERSION,addressingVersion);
		
		toMessage.setProperty(AddressingConstants.DISABLE_ADDRESSING_FOR_OUT_MESSAGES, fromMessage
				.getProperty(AddressingConstants.DISABLE_ADDRESSING_FOR_OUT_MESSAGES));
	}

	//TODO change this method.
	public static ArrayList getArrayListFromString(String str) throws SandeshaException {

		if (str == null || "".equals(str))
			return new ArrayList();

		if (str.length() < 2) {
			String message = SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.invalidStringArray,
					str);
			log.debug(message);
			throw new SandeshaException(message);
		}

		int length = str.length();

		if (str.charAt(0) != '[' || str.charAt(length - 1) != ']') {
			String message = SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.invalidStringArray, str);
			log.debug(message);
			throw new SandeshaException(message);
		}

		ArrayList retArr = new ArrayList();

		String subStr = str.substring(1, length - 1);

		String[] parts = subStr.split(",");

		for (int i = 0; i < parts.length; i++) {
			if (!"".equals(parts[i]))
				retArr.add(parts[i].trim());
		}

		return retArr;
	}

	public static ArrayList getArrayListFromMsgsString(String str) {

		if (str == null || "".equals(str))
			return new ArrayList();

		ArrayList retArr = new ArrayList();

		StringTokenizer tokenizer = new StringTokenizer(str, ",");

		while (tokenizer.hasMoreElements()) {
			String nextToken = tokenizer.nextToken();
			if (nextToken != null && !"".equals(nextToken)) {
				Long lng = new Long(nextToken);
				retArr.add(lng);
			}
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
		RMSBean bean = new RMSBean();
		bean.setInternalSequenceID(internalSequenceID);
		
		bean = rmsBeanMgr.findUnique(bean);

		return bean;
	}
	
	public static final RMSBean getRMSBeanFromSequenceId(StorageManager storageManager, String sequenceID) 
	
	throws SandeshaException {
		RMSBeanMgr rmsBeanMgr = storageManager.getRMSBeanMgr();
		RMSBean bean = new RMSBean();
		bean.setSequenceID(sequenceID);
		
		bean = rmsBeanMgr.findUnique(bean);

		return bean;
	}

	public static RMDBean getRMDBeanFromSequenceId(StorageManager storageManager, String sequenceID) 
	
	throws SandeshaException {
		RMDBeanMgr rmdBeanMgr = storageManager.getRMDBeanMgr();
		RMDBean bean = new RMDBean();
		bean.setSequenceID(sequenceID);
		
		bean = rmdBeanMgr.findUnique(bean);

		return bean;
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

	public static QName getQNameFromString(String qnameStr) throws SandeshaException {
		String[] parts = qnameStr.split(Sandesha2Constants.QNAME_SEPERATOR);
		if (!(parts.length == 3))
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.invalidQName));

		if (parts[0].equals(Sandesha2Constants.VALUE_NONE))
			parts[0] = null;

		if (parts[1].equals(Sandesha2Constants.VALUE_NONE))
			parts[1] = null;

		if (parts[2].equals(Sandesha2Constants.VALUE_NONE))
			parts[2] = null;

		if (parts[0].equals(Sandesha2Constants.VALUE_EMPTY))
			parts[0] = "";

		if (parts[1].equals(Sandesha2Constants.VALUE_EMPTY))
			parts[1] = "";

		if (parts[2].equals(Sandesha2Constants.VALUE_EMPTY))
			parts[2] = "";

		String namespace = parts[0];
		String localPart = parts[1];
		String prefix = parts[2];

		QName name = new QName(namespace, localPart, prefix);
		return name;
	}

	public static String getStringFromQName(QName name) {
		String localPart = name.getLocalPart();
		String namespace = name.getNamespaceURI();
		String prefix = name.getPrefix();

		if (localPart == null)
			localPart = Sandesha2Constants.VALUE_NONE;

		if (namespace == null)
			namespace = Sandesha2Constants.VALUE_NONE;

		if (prefix == null)
			prefix = Sandesha2Constants.VALUE_NONE;

		if ("".equals(localPart))
			localPart = Sandesha2Constants.VALUE_EMPTY;

		if ("".equals(namespace))
			namespace = Sandesha2Constants.VALUE_EMPTY;

		if ("".equals(prefix))
			prefix = Sandesha2Constants.VALUE_EMPTY;

		String QNameStr = namespace + Sandesha2Constants.QNAME_SEPERATOR + localPart
				+ Sandesha2Constants.QNAME_SEPERATOR + prefix;

		return QNameStr;
	}

	public static String getExecutionChainString(ArrayList executionChain) {
		Iterator iter = executionChain.iterator();

		String executionChainStr = "";
		while (iter.hasNext()) {
			Handler handler = (Handler) iter.next();
			String name = handler.getName();
			executionChainStr = executionChainStr + Sandesha2Constants.EXECUTIN_CHAIN_SEPERATOR + name;
		}

		return executionChainStr;
	}

	public static void printSOAPEnvelope(SOAPEnvelope envelope, OutputStream out) throws SandeshaException {
		try {
			XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(out);
			System.out.println("\n");
			envelope.serialize(writer);
		} catch (XMLStreamException e) {
			throw new SandeshaException(e.getMessage());
		} catch (FactoryConfigurationError e) {
			throw new SandeshaException(e.getMessage());
		}
	}

	/**
	 * 
	 * @param propertyKey
	 *            for the client side - internalSequenceID, for the server side -
	 *            sequenceID
	 * @param configurationContext
	 * @return
	 * @throws SandeshaException
	 */
	public static String getRMVersion(String propertyKey, StorageManager storageManager)
			throws SandeshaException {

		SequencePropertyBeanMgr sequencePropertyBeanMgr = storageManager.getSequencePropertyBeanMgr();
		SequencePropertyBean specVersionBean = sequencePropertyBeanMgr.retrieve(propertyKey,
				Sandesha2Constants.SequenceProperties.RM_SPEC_VERSION);

		if (specVersionBean == null)
			return null;

		return specVersionBean.getValue();
	}

	public static String getSequenceProperty(String id, String name, StorageManager storageManager)
			throws SandeshaException {
		SequencePropertyBeanMgr sequencePropertyBeanMgr = storageManager.getSequencePropertyBeanMgr();

		SequencePropertyBean sequencePropertyBean = sequencePropertyBeanMgr.retrieve(id, name);
		if (sequencePropertyBean == null)
			return null;
		else
			return sequencePropertyBean.getValue();
	}

	public static boolean isAllMsgsAckedUpto(long highestInMsgNo, String sequencePropertyKey,
			StorageManager storageManager) throws SandeshaException {

		String clientCompletedMessages = getSequenceProperty(sequencePropertyKey,
				Sandesha2Constants.SequenceProperties.CLIENT_COMPLETED_MESSAGES, storageManager);
		ArrayList ackedMsgsList = getArrayListFromString(clientCompletedMessages);

		long smallestMsgNo = 1;
		for (long tempMsgNo = smallestMsgNo; tempMsgNo <= highestInMsgNo; tempMsgNo++) {
			if (!ackedMsgsList.contains(new Long(tempMsgNo).toString()))
				return false;
		}

		return true; // all message upto the highest have been acked.
	}
	
	public static SandeshaPolicyBean getPropertyBean (AxisDescription axisDescription) throws SandeshaException {
		Parameter parameter = axisDescription.getParameter(Sandesha2Constants.SANDESHA_PROPERTY_BEAN);
		if (parameter==null)
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.propertyBeanNotSet));
		
		SandeshaPolicyBean propertyBean = (SandeshaPolicyBean) parameter.getValue();
		return propertyBean;
	}

	public static String getSequenceIDFromRMMessage(RMMsgContext rmMessageContext) throws SandeshaException {
		int messageType = rmMessageContext.getMessageType();

		String sequenceID = null;
		if (messageType == Sandesha2Constants.MessageTypes.APPLICATION) {
			Sequence sequence = (Sequence) rmMessageContext.getMessagePart(Sandesha2Constants.MessageParts.SEQUENCE);
			sequenceID = sequence.getIdentifier().getIdentifier();
		} else if (messageType == Sandesha2Constants.MessageTypes.ACK) {
			Iterator sequenceAckIter = rmMessageContext
					.getMessageParts(Sandesha2Constants.MessageParts.SEQ_ACKNOWLEDGEMENT);
			
			//In case of ack messages sequenceId is decided based on the sequenceId of the first 
			//sequence Ack. In other words Sandesha2 does not expect to receive two SequenceAcknowledgements
			//of different RM specifications in the same incoming message.
			
			SequenceAcknowledgement sequenceAcknowledgement = (SequenceAcknowledgement) sequenceAckIter.next();
			sequenceID = sequenceAcknowledgement.getIdentifier().getIdentifier();
		} else if (messageType == Sandesha2Constants.MessageTypes.ACK_REQUEST) {
			Iterator ackRequestIter = rmMessageContext
					.getMessageParts(Sandesha2Constants.MessageParts.ACK_REQUEST);
	
			//In case of ack request messages sequenceId is decided based on the sequenceId of the first 
			//AckRequested.
			
			AckRequested ackReq = (AckRequested) ackRequestIter.next();
			sequenceID = ackReq.getIdentifier().getIdentifier();
		} else if (messageType == Sandesha2Constants.MessageTypes.CLOSE_SEQUENCE) {
			CloseSequence closeSequence = (CloseSequence) rmMessageContext
					.getMessagePart(Sandesha2Constants.MessageParts.CLOSE_SEQUENCE);
			sequenceID = closeSequence.getIdentifier().getIdentifier();
		} else if (messageType == Sandesha2Constants.MessageTypes.CLOSE_SEQUENCE_RESPONSE) {
			CloseSequenceResponse closeSequenceResponse = (CloseSequenceResponse) rmMessageContext
					.getMessagePart(Sandesha2Constants.MessageParts.CLOSE_SEQUENCE_RESPONSE);
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
		SecurityManager util = (SecurityManager) context.getProperty(Sandesha2Constants.SECURITY_MANAGER);
		if (util != null) return util;

		//Currently module policies are used to find the security impl. These cant be overriden
		String securityManagerClassStr = getDefaultPropertyBean(context.getAxisConfiguration()).getSecurityManagerClass();
		util = getSecurityManagerInstance(securityManagerClassStr,context);
		context.setProperty(Sandesha2Constants.SECURITY_MANAGER,util);
		
		return util;
	}

	private static SecurityManager getSecurityManagerInstance (String className,ConfigurationContext context) throws SandeshaException {
		try {
		  ClassLoader classLoader = (ClassLoader)	context.getProperty(Sandesha2Constants.MODULE_CLASS_LOADER);

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
	
	/**This returns the Key used when store SequencePropertyBeans for the passed message.
	 * For the sending side this will be the internal sequence ID.
	 * For the receiving side this is the sequenceId.
	 * 
	 * @param rmMsgContext
	 * @return
	 */
	
	public static String getSequencePropertyKey (RMMsgContext rmMsgContext) throws AxisFault {
		String propertyKey = (String) rmMsgContext.getProperty(Sandesha2Constants.MessageContextProperties.SEQUENCE_PROPERTY_KEY);
		if (propertyKey!=null)
			return propertyKey;
		
		String sequenceId = (String) rmMsgContext.getProperty(Sandesha2Constants.MessageContextProperties.SEQUENCE_ID);
		String internalSequenceId = (String) rmMsgContext.getProperty(Sandesha2Constants.MessageContextProperties.INTERNAL_SEQUENCE_ID);

		int type = rmMsgContext.getMessageType();
		int flow = rmMsgContext.getMessageContext().getFLOW();
		
		if (flow==MessageContext.OUT_FLOW) {
			if (isSequenceResponseMessageType(type))
				propertyKey = sequenceId;
			else
				propertyKey = internalSequenceId;
		} else if (flow==MessageContext.IN_FLOW || 
							 flow==MessageContext.IN_FAULT_FLOW) {
			if (isSequenceResponseMessageType(type))
				propertyKey = internalSequenceId;
			else
				propertyKey = sequenceId;
		} else if (flow==MessageContext.OUT_FAULT_FLOW) {
			propertyKey = internalSequenceId;
		}
		
		//TODO handler cases not covered from above.
		
		if (propertyKey==null) {
			String typeStr = SandeshaUtil.getMessageTypeString(rmMsgContext.getMessageType());
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.couldNotFindPropertyKey,typeStr);
			throw new SandeshaException (message);
		}
		
		return propertyKey;
	}
	
	private static boolean isSequenceResponseMessageType (int messageType) {
		if (messageType==Sandesha2Constants.MessageTypes.CREATE_SEQ_RESPONSE ||
			messageType==Sandesha2Constants.MessageTypes.ACK ||
			messageType==Sandesha2Constants.MessageTypes.CLOSE_SEQUENCE_RESPONSE ||
			messageType==Sandesha2Constants.MessageTypes.TERMINATE_SEQ_RESPONSE) {
			
			return true;
		} else {
			return false;
		}
	}

	public static boolean isWSRMAnonymous(String address) {
		if (address!=null && address.startsWith(Sandesha2Constants.SPEC_2006_08.ANONYMOUS_URI_PREFIX))
			return true;
		else 
			return false;
	}
	public static void executeAndStore (RMMsgContext rmMsgContext, String storageKey) throws AxisFault {
		if (log.isDebugEnabled())
			log.debug("Enter: SandeshaUtil::executeAndStore, " + storageKey);
		
		MessageContext msgContext = rmMsgContext.getMessageContext();
		ConfigurationContext configurationContext = msgContext.getConfigurationContext();
		
		// message will be stored in the Sandesha2TransportSender
		msgContext.setProperty(Sandesha2Constants.MESSAGE_STORE_KEY, storageKey);

		TransportOutDescription transportOut = msgContext.getTransportOut();

		msgContext.setProperty(Sandesha2Constants.ORIGINAL_TRANSPORT_OUT_DESC, transportOut);
		msgContext.setProperty(Sandesha2Constants.SET_SEND_TO_TRUE, Sandesha2Constants.VALUE_TRUE);

		Sandesha2TransportOutDesc sandesha2TransportOutDesc = new Sandesha2TransportOutDesc();
		msgContext.setTransportOut(sandesha2TransportOutDesc);

 		// sending the message once through Sandesha2TransportSender.
 		AxisEngine engine = new AxisEngine(configurationContext);

		if (msgContext.isPaused())
			engine.resumeSend(msgContext);
		else {
			//this invocation has to be a blocking one.
			
			Boolean isTransportNonBlocking = (Boolean) msgContext.getProperty(MessageContext.TRANSPORT_NON_BLOCKING);
			if (isTransportNonBlocking!=null && isTransportNonBlocking.booleanValue())
				msgContext.setProperty(MessageContext.TRANSPORT_NON_BLOCKING, Boolean.FALSE);
			
			engine.send(msgContext);
			
			msgContext.setProperty(MessageContext.TRANSPORT_NON_BLOCKING, isTransportNonBlocking);
		}

		if (log.isDebugEnabled())
			log.debug("Exit: SandeshaUtil::executeAndStore");
	}
	
	public static void modifyExecutionChainForStoring (MessageContext message) {
		
		Object property = message.getProperty(Sandesha2Constants.RETRANSMITTABLE_PHASES);
		if (property!=null)
			return; //Phases are already set. Dont hv to redo.
		
		TransportOutDescription transportOutDescription = message.getTransportOut();
		if (!(transportOutDescription instanceof Sandesha2TransportOutDesc))
			return; //This message is aimed to be stored only if, Sandesha2TransportOutDescription is set.
		
		ArrayList executionChain = message.getExecutionChain();
		ArrayList retransmittablePhaseNames = getRetransmittablePhaseNameList();
		ArrayList retransmittablePhases = new ArrayList ();
		
		for (Iterator it=executionChain.iterator();it.hasNext();) {
			Handler handler = (Handler) it.next();
			
			if (retransmittablePhaseNames.contains(handler.getName())) {
				retransmittablePhases.add(handler);
				
				it.remove();
			}
		}
		
		message.setProperty(Sandesha2Constants.RETRANSMITTABLE_PHASES, retransmittablePhases);
	}
	
	private static ArrayList getRetransmittablePhaseNameList () {
		
		//TODO get this phase list from a property
		
		String security = "Security";
		
		ArrayList phases = new ArrayList ();
		phases.add(security);
		
		return phases;
	}
	
	public static MessageContext cloneMessageContext (MessageContext oldMsg) throws AxisFault {
		MessageContext newMsg = new MessageContext ();
		newMsg.setOptions(new Options (oldMsg.getOptions()));
		
		
		//TODO hd to use following hack since a 'clone' method was not available for SOAPEnvelopes.
		//Do it the correct way when that becomes available.
		OMElement newElement = oldMsg.getEnvelope().cloneOMElement();
		String elementString = newElement.toString();
		
		try {
			ByteArrayInputStream stream = new ByteArrayInputStream(
					elementString.getBytes());
			StAXSOAPModelBuilder builder = new StAXSOAPModelBuilder(
					XMLInputFactory.newInstance().createXMLStreamReader(stream),
					null);
			SOAPEnvelope envelope = builder.getSOAPEnvelope();

			newMsg.setEnvelope(envelope);
		} catch (XMLStreamException e) {
			throw new AxisFault (e);
		}
		
		newMsg.setConfigurationContext(oldMsg.getConfigurationContext());
		newMsg.setAxisService(oldMsg.getAxisService());
		newMsg.setTransportOut(oldMsg.getTransportOut());
		newMsg.setTransportIn(oldMsg.getTransportIn());
		
		return newMsg;
		
	}
  
	public static PollingManager getPollingManager (ConfigurationContext configurationContext) {
		PollingManager pollingManager = (PollingManager) configurationContext.getProperty(
				Sandesha2Constants.POLLING_MANAGER);
		
		return pollingManager;
	}
	
	public static void shedulePollingRequest (String sequenceId, ConfigurationContext configurationContext) { 
		PollingManager pollingManager = getPollingManager(configurationContext);
		pollingManager.shedulePollingRequest(sequenceId);
	}

	public static EndpointReference cloneEPR (EndpointReference epr) {
		EndpointReference newEPR = new EndpointReference (epr.getAddress());
		Map referenceParams = epr.getAllReferenceParameters();
		
		if (referenceParams != null) {
			for (Iterator keys = referenceParams.keySet().iterator(); keys
					.hasNext();) {
				Object key = keys.next();
				Object referenceParam = referenceParams.get(key);

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
}
