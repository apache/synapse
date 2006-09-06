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

import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;

import javax.xml.namespace.QName;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.client.Options;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.MessageContextConstants;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.context.OperationContextFactory;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.context.ServiceGroupContext;
import org.apache.axis2.description.AxisDescription;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.AxisServiceGroup;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.Handler;
import org.apache.axis2.util.UUIDGenerator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.RMMsgContext;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.security.SecurityManager;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.beanmanagers.SequencePropertyBeanMgr;
import org.apache.sandesha2.storage.beans.SequencePropertyBean;
import org.apache.sandesha2.workers.Invoker;
import org.apache.sandesha2.workers.Sender;
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

	/**
	 * Create a new UUID.
	 * 
	 * @return
	 */
	public static String getUUID() {
		// String uuid = "uuid:" + UUIDGenerator.getUUID();
		String uuid = UUIDGenerator.getUUID();

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
	public static ArrayList getAckRangeArrayList(String msgNoStr, SOAPFactory factory, String rmNamespaceValue)
			throws SandeshaException {

		ArrayList ackRanges = new ArrayList();

		StringTokenizer tokenizer = new StringTokenizer(msgNoStr, ",");
		ArrayList sortedMsgNoArrayList = getSortedMsgNoArrayList(tokenizer);

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
				AcknowledgementRange ackRange = new AcknowledgementRange(factory, rmNamespaceValue);
				ackRange.setLowerValue(lower);
				ackRange.setUpperValue(upper);
				ackRanges.add(ackRange);

				lower = temp;
				upper = temp;
				completed = false;
			}
		}

		if (!completed) {
			AcknowledgementRange ackRange = new AcknowledgementRange(factory, rmNamespaceValue);
			ackRange.setLowerValue(lower);
			ackRange.setUpperValue(upper);
			ackRanges.add(ackRange);
			completed = true;
		}

		return ackRanges;
	}

	private static ArrayList getSortedMsgNoArrayList(StringTokenizer tokenizer) throws SandeshaException {
		ArrayList msgNubers = new ArrayList();

		while (tokenizer.hasMoreElements()) {
			String temp = tokenizer.nextToken();

			try {
				long msgNo = Long.parseLong(temp);
				msgNubers.add(new Long(msgNo));
			} catch (Exception ex) {
				String message = SandeshaMessageHelper.getMessage(
						SandeshaMessageKeys.invalidMsgNumberList);
				log.debug(message);
				throw new SandeshaException(message);
			}
		}

		ArrayList sortedMsgNumberList = sort(msgNubers);
		return sortedMsgNumberList;
	}

	public static ArrayList sort(ArrayList list) {

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

	public static void startSenderForTheSequence(ConfigurationContext context, String sequenceID) {
		
		Sender sender = (Sender) context.getProperty(Sandesha2Constants.SENDER);
		
		if (sender!=null)
			sender.runSenderForTheSequence(context, sequenceID);
		else {
			sender = new Sender ();
			context.setProperty(Sandesha2Constants.SENDER,sender);
			sender.runSenderForTheSequence(context, sequenceID);
		}
	}

	private static void stopSenderForTheSequence(String sequenceID, ConfigurationContext context) {
		Sender sender = (Sender) context.getProperty(Sandesha2Constants.SENDER);
		
		if (sender!=null) {
			sender.stopSenderForTheSequence(sequenceID);
		}
	}
	
	public static void stopSender(ConfigurationContext context) {
		Sender sender = (Sender) context.getProperty(Sandesha2Constants.SENDER);
		
		if (sender!=null) {
			sender.stopSending ();
		}
	}

	public static void startInvokerForTheSequence(ConfigurationContext context, String sequenceID) {
		
		Invoker invoker = (Invoker) context.getProperty(Sandesha2Constants.INVOKER);
		if (invoker!=null)
			invoker.runInvokerForTheSequence(context,sequenceID);
		else {
			invoker = new Invoker ();
			context.setProperty(Sandesha2Constants.INVOKER,invoker);
			invoker.runInvokerForTheSequence(context,sequenceID);
		}
	}

	private static void stopInvokerForTheSequence(String sequenceID, ConfigurationContext context) {
		Invoker invoker = (Invoker) context.getProperty(Sandesha2Constants.INVOKER);
		if (invoker!=null)
			invoker.stopInvokerForTheSequence(sequenceID);
	}
	
	public static void stopInvoker(ConfigurationContext context) {
		Invoker invoker = (Invoker) context.getProperty(Sandesha2Constants.INVOKER);
		if (invoker!=null)
			invoker.stopInvoking();
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
			log.error(message);
			throw new SandeshaException(message,e);
		}
	}

	public static int getSOAPVersion(SOAPEnvelope envelope) throws SandeshaException {
		String namespaceName = envelope.getNamespace().getName();
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
		if (header != null)
			sequenceElem = header.getFirstChildWithName(new QName(Sandesha2Constants.SPEC_2005_02.NS_URI,
					Sandesha2Constants.WSRM_COMMON.SEQUENCE));

		if (sequenceElem == null)
			sequenceElem = header.getFirstChildWithName(new QName(Sandesha2Constants.SPEC_2006_08.NS_URI,
					Sandesha2Constants.WSRM_COMMON.SEQUENCE));

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

//			Options referenceMsgOptions = referenceMessage.getOptions();
//			Options newOptions = null;
//			if (referenceMsgOptions!=null)
//				newOptions = new Options (referenceMsgOptions);
//			else
			
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
			} else {
				AxisService axisService = new AxisService("AnonymousRMService"); 
				
				AxisServiceGroup serviceGroup = newMessageContext.getAxisServiceGroup();
				axisService.setParent(serviceGroup);
				serviceGroup.addChild(axisService);
				
				ServiceContext serviceContext = new ServiceContext(axisService, newMessageContext.getServiceGroupContext());

				newMessageContext.setAxisService(axisService);
				newMessageContext.setServiceContext(serviceContext);
			}

			newMessageContext.setAxisOperation(operation);

			// setting parent child relationships
			AxisService service = newMessageContext.getAxisService();

			if (service != null && operation != null) {
				service.addChild(operation);
				operation.setParent(service);
			}
			

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
			newMessageContext.setExecutionChain(referenceMessage.getExecutionChain());

			return newMessageContext;

		} catch (AxisFault e) {
			log.debug(e.getMessage());
			throw new SandeshaException(e.getMessage());
		}

	}
	

	
	public static SandeshaPropertyBean getDefaultPropertyBean (AxisConfiguration axisConfiguration) throws SandeshaException {
		Parameter parameter = axisConfiguration.getParameter(Sandesha2Constants.SANDESHA_PROPERTY_BEAN);
		if (parameter==null)
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.defaultPropertyBeanNotSet));
		
		SandeshaPropertyBean sandeshaPropertyBean = (SandeshaPropertyBean) parameter.getValue();
		return sandeshaPropertyBean;
	}

	private static void copyNecessaryPropertiesFromRelatedContext(MessageContext fromMessage, MessageContext toMessage) throws SandeshaException {
		toMessage.setProperty(MessageContextConstants.TRANSPORT_URL, fromMessage
				.getProperty(MessageContextConstants.TRANSPORT_URL));
		
		String addressingVersion = (String) fromMessage.getProperty(AddressingConstants.WS_ADDRESSING_VERSION);
		if (addressingVersion==null) {
			OperationContext opCtx = fromMessage.getOperationContext();
			if (opCtx!=null) {
				try {
					MessageContext requestMsg = opCtx.getMessageContext(OperationContextFactory.MESSAGE_LABEL_IN_VALUE);
					if (requestMsg!=null)
						addressingVersion = (String) requestMsg.getProperty(AddressingConstants.WS_ADDRESSING_VERSION);
				} catch (AxisFault e) {
					throw new SandeshaException (e);
				}
			}
		}
		
		toMessage.setProperty(AddressingConstants.WS_ADDRESSING_VERSION,addressingVersion);
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

	public static ArrayList getArrayListFromMsgsString(String str) throws SandeshaException {

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

	public static String getSequenceIDFromInternalSequenceID(String internalSequenceID,
			StorageManager storageManager) throws SandeshaException {

		SequencePropertyBeanMgr sequencePropertyBeanMgr = storageManager.getSequencePropertyBeanMgr();

		SequencePropertyBean outSequenceBean = sequencePropertyBeanMgr.retrieve(internalSequenceID,
				Sandesha2Constants.SequenceProperties.OUT_SEQUENCE_ID);

		String sequeunceID = null;
		if (outSequenceBean != null)
			sequeunceID = outSequenceBean.getValue();

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

	// TODO complete below.
	public static ArrayList getExecutionChainFromString(String executionChainStr, ConfigurationContext configContext)
			throws SandeshaException {
		String[] nameStrs = executionChainStr.split(Sandesha2Constants.EXECUTIN_CHAIN_SEPERATOR);

		AxisConfiguration axisConfiguration = configContext.getAxisConfiguration();

		int length = nameStrs.length;
		for (int i = 0; i < length; i++) {
			String nameStr = nameStrs[i];
			// axisConfiguration.get

		}

		return null; // not complete yet.
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

	public static boolean isAllMsgsAckedUpto(long highestInMsgNo, String internalSequenceID,
			StorageManager storageManager) throws SandeshaException {

		String clientCompletedMessages = getSequenceProperty(internalSequenceID,
				Sandesha2Constants.SequenceProperties.CLIENT_COMPLETED_MESSAGES, storageManager);
		ArrayList ackedMsgsList = getArrayListFromString(clientCompletedMessages);

		long smallestMsgNo = 1;
		for (long tempMsgNo = smallestMsgNo; tempMsgNo <= highestInMsgNo; tempMsgNo++) {
			if (!ackedMsgsList.contains(new Long(tempMsgNo).toString()))
				return false;
		}

		return true; // all message upto the highest have been acked.
	}
	
	public static SandeshaPropertyBean getPropertyBean (AxisDescription axisDescription) throws SandeshaException {
		Parameter parameter = axisDescription.getParameter(Sandesha2Constants.SANDESHA_PROPERTY_BEAN);
		if (parameter==null)
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.defaultPropertyBeanNotSet));
		
		SandeshaPropertyBean propertyBean = (SandeshaPropertyBean) parameter.getValue();
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
			AckRequested ackRequested = (AckRequested) rmMessageContext
					.getMessagePart(Sandesha2Constants.MessageParts.ACK_REQUEST);
			sequenceID = ackRequested.getIdentifier().getIdentifier();
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
}
