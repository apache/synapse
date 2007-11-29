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

import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPFault;
import org.apache.axiom.soap.SOAPFaultCode;
import org.apache.axiom.soap.SOAPFaultDetail;
import org.apache.axiom.soap.SOAPFaultReason;
import org.apache.axiom.soap.SOAPFaultSubCode;
import org.apache.axiom.soap.SOAPFaultText;
import org.apache.axiom.soap.SOAPFaultValue;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.addressing.RelatesTo;
import org.apache.axis2.client.async.AxisCallback;
import org.apache.axis2.client.async.Callback;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.engine.MessageReceiver;
import org.apache.axis2.engine.Handler.InvocationResponse;
import org.apache.axis2.transport.TransportUtils;
import org.apache.axis2.util.CallbackReceiver;
import org.apache.axis2.util.MessageContextBuilder;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.FaultData;
import org.apache.sandesha2.RMMsgContext;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.client.SandeshaClientConstants;
import org.apache.sandesha2.client.SandeshaListener;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.storage.SandeshaStorageException;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.Transaction;
import org.apache.sandesha2.storage.beanmanagers.RMSBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.SenderBeanMgr;
import org.apache.sandesha2.storage.beans.RMDBean;
import org.apache.sandesha2.storage.beans.RMSBean;
import org.apache.sandesha2.storage.beans.RMSequenceBean;
import org.apache.sandesha2.storage.beans.SenderBean;
import org.apache.sandesha2.wsrm.AcknowledgementRange;
import org.apache.sandesha2.wsrm.SequenceAcknowledgement;
import org.apache.sandesha2.wsrm.SequenceFault;

/**
 * Has logic to check for possible RM related faults and create it.
 */

public class FaultManager {

	private static final Log log = LogFactory.getLog(FaultManager.class);

	/**
	 * Check weather the LastMessage number has been exceeded and generate the
	 * fault if it is.
	 * 
	 * @param msgCtx
	 * @return
	 */
	public static void checkForLastMsgNumberExceeded(RMMsgContext applicationRMMessage, StorageManager storageManager)
			throws AxisFault {
		if (log.isDebugEnabled())
			log.debug("Enter: FaultManager::checkForLastMsgNumberExceeded");
/*	
 * TODO - This code currently doesn't actually work	
		Sequence sequence = (Sequence) applicationRMMessage.getMessagePart(Sandesha2Constants.MessageParts.SEQUENCE);
		long messageNumber = sequence.getMessageNumber().getMessageNumber();
		String sequenceID = sequence.getIdentifier().getIdentifier();

		boolean lastMessageNumberExceeded = false;
		String reason = null;
		
		RMSBean rmsBean = SandeshaUtil.getRMSBeanFromSequenceId(storageManager, sequenceID);
		if (rmsBean != null) {
			long lastMessageNo = rmsBean.getLastOutMessage();
			if (messageNumber > lastMessageNo) {
				lastMessageNumberExceeded = true;
				reason = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.msgNumberLargerThanLastMsg, Long
						.toString(messageNumber), Long.toString(lastMessageNo));
			}
		}

		if (lastMessageNumberExceeded) {
			FaultData faultData = new FaultData();
			faultData.setType(Sandesha2Constants.SOAPFaults.FaultType.LAST_MESSAGE_NO_EXCEEDED);
			int SOAPVersion = SandeshaUtil.getSOAPVersion(applicationRMMessage.getSOAPEnvelope());
			if (SOAPVersion == Sandesha2Constants.SOAPVersion.v1_1)
				faultData.setCode(SOAP11Constants.FAULT_CODE_SENDER);
			else
				faultData.setCode(SOAP12Constants.FAULT_CODE_SENDER);

			faultData.setSubcode(Sandesha2Constants.SOAPFaults.Subcodes.LAST_MESSAGE_NO_EXCEEDED);
			faultData.setReason(reason);
			
			SOAPFactory factory = SOAPAbstractFactory.getSOAPFactory(SOAPVersion);
			String rmNamespace = applicationRMMessage.getRMNamespaceValue();
			OMElement identifierElement = factory.createOMElement(Sandesha2Constants.WSRM_COMMON.IDENTIFIER,
					rmNamespace, Sandesha2Constants.WSRM_COMMON.NS_PREFIX_RM);
			identifierElement.setText(sequenceID);
			
			faultData.setDetail(identifierElement);
			
			if (log.isDebugEnabled())
				log.debug("Exit: FaultManager::checkForLastMsgNumberExceeded, lastMessageNumberExceeded");
			getFault(applicationRMMessage, faultData, storageManager);
		}
*/
		if (log.isDebugEnabled())
			log.debug("Exit: FaultManager::checkForLastMsgNumberExceeded");
	}

	public static RMMsgContext checkForMessageNumberRoleover(MessageContext messageContext) {
		return null;
	}

	/**
	 * Check whether a Sequence message (a) belongs to a unknown sequence
	 * (generates an UnknownSequence fault) (b) message number exceeds a
	 * predifined limit ( genenrates a Message Number Rollover fault)
	 * 
	 * @param msgCtx
	 * @return true if no exception has been thrown and the sequence doesn't exist 
	 * @throws SandeshaException
	 */
	public static boolean checkForUnknownSequence(RMMsgContext rmMessageContext, String sequenceID,
			StorageManager storageManager, boolean piggybackedMessage) throws AxisFault {
		if (log.isDebugEnabled())
			log.debug("Enter: FaultManager::checkForUnknownSequence, " + sequenceID);

		boolean validSequence = false;

		// Look for an outbound sequence
		if (SandeshaUtil.getRMSBeanFromSequenceId(storageManager, sequenceID) != null) {
			validSequence = true;
			// Look for an inbound sequence
		} else if(SandeshaUtil.getRMDBeanFromSequenceId(storageManager, sequenceID) != null) { 
				validSequence = true;
		}

		if (!validSequence) {

			if (log.isDebugEnabled())
				log.debug("Sequence not valid " + sequenceID);

			// Return an UnknownSequence error
			MessageContext messageContext = rmMessageContext.getMessageContext();

			int SOAPVersion = SandeshaUtil.getSOAPVersion(messageContext.getEnvelope());

			FaultData data = new FaultData();
			if (SOAPVersion == Sandesha2Constants.SOAPVersion.v1_1)
				data.setCode(SOAP11Constants.FAULT_CODE_SENDER);
			else
				data.setCode(SOAP12Constants.FAULT_CODE_SENDER);

			data.setSubcode(SpecSpecificConstants.getFaultSubcode(rmMessageContext.getRMNamespaceValue(), 
					Sandesha2Constants.SOAPFaults.FaultType.UNKNOWN_SEQUENCE ));

			SOAPFactory factory = SOAPAbstractFactory.getSOAPFactory(SOAPVersion);

			OMElement identifierElement = factory.createOMElement(Sandesha2Constants.WSRM_COMMON.IDENTIFIER,
					rmMessageContext.getRMNamespaceValue(), Sandesha2Constants.WSRM_COMMON.NS_PREFIX_RM);
			identifierElement.setText(sequenceID);
			
			data.setDetail(identifierElement);

			data.setReason(SandeshaMessageHelper.getMessage(SandeshaMessageKeys.unknownSequenceFault, sequenceID));
			
			data.setType(Sandesha2Constants.SOAPFaults.FaultType.UNKNOWN_SEQUENCE);

			if (log.isDebugEnabled())
				log.debug("Exit: FaultManager::checkForUnknownSequence, Sequence unknown");
			
			boolean faultThrowable = !piggybackedMessage;
			getOrSendFault(rmMessageContext, data, faultThrowable, null); //unknown sequence so cannot send to acksTo
			return true;
		}

		if (log.isDebugEnabled())
			log.debug("Exit: FaultManager::checkForUnknownSequence");
		return false;
	}

	/**
	 * Check weather the Acknowledgement is invalid and generate a fault if it
	 * is.
	 * 
	 * @param msgCtx
	 * @return
	 * @throws SandeshaException
	 */
	public static boolean checkForInvalidAcknowledgement(RMMsgContext ackRMMessageContext, SequenceAcknowledgement sequenceAcknowledgement,
			StorageManager storageManager, RMSBean rmsBean, boolean piggybackedMessage)
			throws AxisFault {
		if (log.isDebugEnabled())
			log.debug("Enter: FaultManager::checkForInvalidAcknowledgement");

		// check lower<=upper
		if (ackRMMessageContext.getMessageType() != Sandesha2Constants.MessageTypes.ACK) {
			if (log.isDebugEnabled())
				log.debug("Exit: FaultManager::checkForInvalidAcknowledgement, MessageType not an ACK");
		}

		boolean invalidAck = false;
		
		List sequenceAckList = sequenceAcknowledgement.getAcknowledgementRanges();
		Iterator it = sequenceAckList.iterator();

		while (it.hasNext()) {
			AcknowledgementRange acknowledgementRange = (AcknowledgementRange) it.next();
			long upper = acknowledgementRange.getUpperValue();
			long lower = acknowledgementRange.getLowerValue();

			if (lower > upper) {
				invalidAck = true;					
				// check upper isn't bigger than the highest out msg number
			} else if ( upper > rmsBean.getHighestOutMessageNumber() ) {
				invalidAck = true;
			}
				
			if (invalidAck) {
				makeInvalidAcknowledgementFault(ackRMMessageContext, sequenceAcknowledgement, 
						acknowledgementRange, storageManager, piggybackedMessage, rmsBean.getAcksToEndpointReference());
				return true;
			}
		}		

		if (log.isDebugEnabled())
			log.debug("Exit: FaultManager::checkForInvalidAcknowledgement");
		return false;
	}

	/**
	 * Makes an InvalidAcknowledgement fault.
	 * @param rmMsgCtx
	 * @param storageManager
	 * @param message
	 * @throws AxisFault 
	 */
	public static void makeInvalidAcknowledgementFault(RMMsgContext rmMsgCtx, 
			SequenceAcknowledgement sequenceAcknowledgement, AcknowledgementRange acknowledgementRange,
			StorageManager storageManager, boolean piggybackedMessage, EndpointReference acksToEPR) throws AxisFault {
		FaultData data = new FaultData();
		int SOAPVersion = SandeshaUtil.getSOAPVersion(rmMsgCtx.getMessageContext().getEnvelope());
		if (SOAPVersion == Sandesha2Constants.SOAPVersion.v1_1)
			data.setCode(SOAP11Constants.FAULT_CODE_SENDER);
		else
			data.setCode(SOAP12Constants.FAULT_CODE_SENDER);

		data.setType(Sandesha2Constants.SOAPFaults.FaultType.INVALID_ACKNOWLEDGEMENT);
		data.setSubcode(SpecSpecificConstants.getFaultSubcode(rmMsgCtx.getRMNamespaceValue(), 
				Sandesha2Constants.SOAPFaults.FaultType.INVALID_ACKNOWLEDGEMENT ));
		data.setReason(SandeshaMessageHelper.getMessage(SandeshaMessageKeys.invalidAckFault));

		SOAPFactory factory = SOAPAbstractFactory.getSOAPFactory(SOAPVersion);
		OMNamespace rmNamespace = factory.createOMNamespace(rmMsgCtx.getRMNamespaceValue(), Sandesha2Constants.WSRM_COMMON.NS_PREFIX_RM);
		OMElement seqAckElement = factory.createOMElement(Sandesha2Constants.WSRM_COMMON.SEQUENCE_ACK, rmNamespace);
	
		// Set the sequence Id
		sequenceAcknowledgement.getIdentifier().toOMElement(seqAckElement, rmNamespace);

		// Set the Ack Range
		acknowledgementRange.toOMElement(seqAckElement);
		
		data.setDetail(seqAckElement);
							
		if (log.isDebugEnabled())
			log.debug("Exit: FaultManager::checkForInvalidAcknowledgement, invalid ACK");
		
		boolean throwable = !piggybackedMessage;
		getOrSendFault(rmMsgCtx, data, throwable, acksToEPR);
  }

	/**
	 * Makes a Create sequence refused fault
	 */
	public static void makeCreateSequenceRefusedFault(RMMsgContext rmMessageContext, String detail, Exception e, EndpointReference acksToEPR) throws AxisFault {
		if (log.isDebugEnabled())
			log.debug("Enter: FaultManager::makeCreateSequenceRefusedFault, " + detail);
		
		// Return a CreateSequenceRefused error
		MessageContext messageContext = rmMessageContext.getMessageContext();

		int SOAPVersion = SandeshaUtil.getSOAPVersion(messageContext.getEnvelope());

		FaultData data = new FaultData();
		if (SOAPVersion == Sandesha2Constants.SOAPVersion.v1_1)
			data.setCode(SOAP11Constants.FAULT_CODE_SENDER);
		else
			data.setCode(SOAP12Constants.FAULT_CODE_SENDER);

		data.setSubcode(SpecSpecificConstants.getFaultSubcode(rmMessageContext.getRMNamespaceValue(), 
				Sandesha2Constants.SOAPFaults.FaultType.CREATE_SEQUENCE_REFUSED ));

		SOAPFactory factory = SOAPAbstractFactory.getSOAPFactory(SOAPVersion);
		OMElement identifierElement = factory.createOMElement(Sandesha2Constants.WSRM_COMMON.IDENTIFIER,
				rmMessageContext.getRMNamespaceValue(), Sandesha2Constants.WSRM_COMMON.NS_PREFIX_RM);
		identifierElement.setText(detail);
		data.setDetail(identifierElement);
		data.setDetailString(detail);

		data.setReason(SandeshaMessageHelper.getMessage(SandeshaMessageKeys.createSequenceRefused));
		
		data.setType(Sandesha2Constants.SOAPFaults.FaultType.CREATE_SEQUENCE_REFUSED);
		
		data.setExceptionString(SandeshaUtil.getStackTraceFromException(e));

		if (log.isDebugEnabled())
			log.debug("Exit: FaultManager::makeCreateSequenceRefusedFault");
		getOrSendFault(rmMessageContext, data, true, acksToEPR);
	}
	
	/**
	 * Makes the WSMC UnsupportedSelectionFault
	 */
	public static void makeUnsupportedSelectionFault(RMMsgContext rmMessageContext, QName unsupportedElement) throws AxisFault {
		if (log.isDebugEnabled())
			log.debug("Enter: FaultManager::makeUnsupportedSelectionFault, " + unsupportedElement);
		
		// Return a UnsupportedSelectionFault error

		int SOAPVersion = SandeshaUtil.getSOAPVersion(rmMessageContext.getMessageContext().getEnvelope());

		FaultData data = new FaultData();
		if (SOAPVersion == Sandesha2Constants.SOAPVersion.v1_1)
			data.setCode(SOAP11Constants.FAULT_CODE_RECEIVER);
		else
			data.setCode(SOAP12Constants.FAULT_CODE_RECEIVER);

		data.setSubcode(SpecSpecificConstants.getFaultSubcode(Sandesha2Constants.SPEC_2007_02.MC_NS_URI, 
				Sandesha2Constants.SOAPFaults.FaultType.UNSUPPORTED_SELECTION ));

		SOAPFactory factory = SOAPAbstractFactory.getSOAPFactory(SOAPVersion);
		OMElement element = factory.createOMElement(Sandesha2Constants.WSRM_COMMON.UNSUPPORTED_ELEMENT,
				Sandesha2Constants.SPEC_2007_02.MC_NS_URI, Sandesha2Constants.WSRM_COMMON.NS_PREFIX_MC);
		element.setText(unsupportedElement);
		data.setDetail(element);

		data.setReason(SandeshaMessageHelper.getMessage(SandeshaMessageKeys.unsuportedSelectionFault));
		
		data.setType(Sandesha2Constants.SOAPFaults.FaultType.UNSUPPORTED_SELECTION);

		makeMakeConnectionFault(rmMessageContext, data);
		if (log.isDebugEnabled())
			log.debug("Exit: FaultManager::makeUnsupportedSelectionFault");
	}

	/**
	 * Makes WSMC MissingSelectionFault
	 */
	public static void makeMissingSelectionFault(RMMsgContext rmMessageContext) throws AxisFault {
		if (log.isDebugEnabled())
			log.debug("Enter: FaultManager::makeMissingSelectionFault");
		
		int SOAPVersion = SandeshaUtil.getSOAPVersion(rmMessageContext.getMessageContext().getEnvelope());

		FaultData data = new FaultData();
		if (SOAPVersion == Sandesha2Constants.SOAPVersion.v1_1)
			data.setCode(SOAP11Constants.FAULT_CODE_RECEIVER);
		else
			data.setCode(SOAP12Constants.FAULT_CODE_RECEIVER);

		data.setSubcode(SpecSpecificConstants.getFaultSubcode(Sandesha2Constants.SPEC_2007_02.MC_NS_URI, 
				Sandesha2Constants.SOAPFaults.FaultType.MISSING_SELECTION ));

		data.setDetail(null);

		data.setReason(SandeshaMessageHelper.getMessage(SandeshaMessageKeys.missingSelectionFault));
		
		data.setType(Sandesha2Constants.SOAPFaults.FaultType.MISSING_SELECTION);

		makeMakeConnectionFault(rmMessageContext, data);
		
		if (log.isDebugEnabled())
			log.debug("Exit: FaultManager::makeMissingSelectionFault");
	}

	private static void makeMakeConnectionFault(RMMsgContext referenceRMMsgContext, FaultData data) throws AxisFault {
		SOAPFactory factory = (SOAPFactory) referenceRMMsgContext.getSOAPEnvelope().getOMFactory();
		
		SOAPFaultCode faultCode = factory.createSOAPFaultCode();
		SOAPFaultSubCode faultSubCode = factory.createSOAPFaultSubCode(faultCode);
		
		SOAPFaultValue faultColdValue = factory.createSOAPFaultValue(faultCode);
		SOAPFaultValue faultSubcodeValue = factory.createSOAPFaultValue(faultSubCode);
		
		faultSubcodeValue.setText(data.getSubcode());

		faultCode.setSubCode(faultSubCode);
		
		SOAPFaultReason reason = factory.createSOAPFaultReason();
		SOAPFaultText reasonText = factory.createSOAPFaultText();
		reasonText.setText(data.getReason());
		
		SOAPFaultDetail detail = factory.createSOAPFaultDetail();
		if (data.getDetail() != null)
			detail.addDetailEntry(data.getDetail());
		
		String SOAPNamespaceValue = factory.getSoapVersionURI();
		
		if (SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI.equals(SOAPNamespaceValue)) {
                        reasonText.setLang(Sandesha2Constants.LANG_EN);
			reason.addSOAPText(reasonText);
			referenceRMMsgContext.setProperty(SOAP12Constants.SOAP_FAULT_CODE_LOCAL_NAME, faultCode);
			referenceRMMsgContext.setProperty(SOAP12Constants.SOAP_FAULT_REASON_LOCAL_NAME, reason);
			referenceRMMsgContext.setProperty(SOAP12Constants.SOAP_FAULT_DETAIL_LOCAL_NAME, detail);
			faultColdValue.setText(data.getCode());
			
		} else if (SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI.equals (SOAPNamespaceValue)) {
			reason.setText(data.getReason());
			referenceRMMsgContext.setProperty(SOAP11Constants.SOAP_FAULT_CODE_LOCAL_NAME, faultCode);
			referenceRMMsgContext.setProperty(SOAP11Constants.SOAP_FAULT_DETAIL_LOCAL_NAME, detail);
			referenceRMMsgContext.setProperty(SOAP11Constants.SOAP_FAULT_STRING_LOCAL_NAME, reason);
			faultColdValue.setText(data.getSubcode());
			
		}
		
		AxisFault fault = new AxisFault(faultColdValue.getTextAsQName(), data.getReason(), "", "", data.getDetail());
		fault.setFaultAction(Sandesha2Constants.SPEC_2007_02.Actions.MC_FAULT);
		
		//if this is throwable throwing it out, else we will log here.
		throw fault; 
	}
	
	/**
	 * Checks if a sequence is terminated and returns a SequenceTerminated fault.
	 * @param referenceRMMessage
	 * @param sequenceID
	 * @param rmdBean
	 * @return
	 * @throws AxisFault 
	 */
	public static boolean checkForSequenceTerminated(RMMsgContext referenceRMMessage, String sequenceID, RMSequenceBean bean
			, boolean piggybackedMessage) 
	
	throws AxisFault {
		if (log.isDebugEnabled())
			log.debug("Enter: FaultManager::checkForSequenceTerminated, " + sequenceID);

		if (bean.isTerminated()) {
			MessageContext referenceMessage = referenceRMMessage.getMessageContext();
			FaultData data = new FaultData();
			int SOAPVersion = SandeshaUtil.getSOAPVersion(referenceMessage.getEnvelope());
			if (SOAPVersion == Sandesha2Constants.SOAPVersion.v1_1)
				data.setCode(SOAP11Constants.FAULT_CODE_SENDER);
			else
				data.setCode(SOAP12Constants.FAULT_CODE_SENDER);

			data.setSubcode(SpecSpecificConstants.getFaultSubcode(referenceRMMessage.getRMNamespaceValue(), 
					Sandesha2Constants.SOAPFaults.FaultType.SEQUENCE_TERMINATED ));
			data.setReason(SandeshaMessageHelper.getMessage(SandeshaMessageKeys.sequenceTerminatedFault, sequenceID));
			data.setType(Sandesha2Constants.SOAPFaults.FaultType.SEQUENCE_TERMINATED);
			
			SOAPFactory factory = SOAPAbstractFactory.getSOAPFactory(SOAPVersion);
			String rmNamespaceValue = referenceRMMessage.getRMNamespaceValue();
			OMElement identifierElement = factory.createOMElement(Sandesha2Constants.WSRM_COMMON.IDENTIFIER,
					rmNamespaceValue, Sandesha2Constants.WSRM_COMMON.NS_PREFIX_RM);
			identifierElement.setText(sequenceID);
			
			data.setDetail(identifierElement);

			if (log.isDebugEnabled())
				log.debug("Exit: FaultManager::checkForSequenceTerminated, sequence terminated");
			
			boolean throwable = !piggybackedMessage;
			getOrSendFault(referenceRMMessage, data, throwable, bean.getAcksToEndpointReference());
			return true;
		}

		if (log.isDebugEnabled())
			log.debug("Exit: FaultManager::checkForSequenceTerminated");
		return false;
  }

	public static boolean checkForSequenceClosed(RMMsgContext referenceRMMessage, String sequenceID,
			RMDBean rmdBean, boolean piggybackedMessage) throws AxisFault {
		if (log.isDebugEnabled())
			log.debug("Enter: FaultManager::checkForSequenceClosed, " + sequenceID);

		if (rmdBean.isClosed()) {
			MessageContext referenceMessage = referenceRMMessage.getMessageContext();
			FaultData data = new FaultData();
			int SOAPVersion = SandeshaUtil.getSOAPVersion(referenceMessage.getEnvelope());
			if (SOAPVersion == Sandesha2Constants.SOAPVersion.v1_1)
				data.setCode(SOAP11Constants.FAULT_CODE_SENDER);
			else
				data.setCode(SOAP12Constants.FAULT_CODE_SENDER);

			data.setSubcode(SpecSpecificConstants.getFaultSubcode(referenceRMMessage.getRMNamespaceValue(), 
					Sandesha2Constants.SOAPFaults.FaultType.SEQUENCE_CLOSED ));
			data.setReason(SandeshaMessageHelper.getMessage(SandeshaMessageKeys.cannotAcceptMsgAsSequenceClosedFault));
			data.setType(Sandesha2Constants.SOAPFaults.FaultType.SEQUENCE_CLOSED);
			
			SOAPFactory factory = SOAPAbstractFactory.getSOAPFactory(SOAPVersion);
			String rmNamespaceValue = referenceRMMessage.getRMNamespaceValue();
			OMElement identifierElement = factory.createOMElement(Sandesha2Constants.WSRM_COMMON.IDENTIFIER,
					rmNamespaceValue, Sandesha2Constants.WSRM_COMMON.NS_PREFIX_RM);
			identifierElement.setText(sequenceID);
			
			data.setDetail(identifierElement);

			if (log.isDebugEnabled())
				log.debug("Exit: FaultManager::checkForSequenceClosed, sequence closed");
			
			boolean throwable = !piggybackedMessage;
			getOrSendFault(referenceRMMessage, data,throwable, rmdBean.getAcksToEndpointReference());
			return true;
		}

		if (log.isDebugEnabled())
			log.debug("Exit: FaultManager::checkForSequenceClosed");
		return false;
	}
		
	/**
	 * Adds the necessary Fault elements as properties to the message context.
	 * Or if this is a SOAP11 Fault, generates the correct RM Fault and sends.
	 * 
	 * @param referenceRMMsgContext - Message in reference to which the fault will be generated.
	 * @param data - data for the fault
	 * @param throwable - This tells weather or not it is ok to throw the fault out. I.e. this should not be done when processing 
	 * 					  piggybacked acks since this will stop the carrier message from being processed.
	 * @return - The dummy fault to be thrown out.
	 * 
	 * @throws AxisFault
	 */
	public static void getOrSendFault(RMMsgContext referenceRMMsgContext, FaultData data, boolean throwable, EndpointReference acksToEPR) throws AxisFault {

		if (log.isDebugEnabled())
			log.debug("Enter: FaultManager::getOrSendFault: " + referenceRMMsgContext + "," + data + "," + throwable + "," + acksToEPR);
		SOAPFactory factory = (SOAPFactory) referenceRMMsgContext.getSOAPEnvelope().getOMFactory();
		
		SOAPFaultCode faultCode = factory.createSOAPFaultCode();
		SOAPFaultSubCode faultSubCode = factory.createSOAPFaultSubCode(faultCode);
		
		SOAPFaultValue faultColdValue = factory.createSOAPFaultValue(faultCode);
		SOAPFaultValue faultSubcodeValue = factory.createSOAPFaultValue(faultSubCode);
		
		faultColdValue.setText(data.getCode());
		
		faultSubcodeValue.setText(data.getSubcode());

		faultCode.setSubCode(faultSubCode);
		
		SOAPFaultReason reason = factory.createSOAPFaultReason();
		SOAPFaultText reasonText = factory.createSOAPFaultText();
		reasonText.setText(data.getReason());
		
		SOAPFaultDetail detail = factory.createSOAPFaultDetail();
		detail.addDetailEntry(data.getDetail());
		
		String SOAPNamespaceValue = factory.getSoapVersionURI();
		
		if (SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI.equals(SOAPNamespaceValue)) {
                        reasonText.setLang(Sandesha2Constants.LANG_EN);
			reason.addSOAPText(reasonText);
			referenceRMMsgContext.setProperty(SOAP12Constants.SOAP_FAULT_CODE_LOCAL_NAME, faultCode);
			referenceRMMsgContext.setProperty(SOAP12Constants.SOAP_FAULT_REASON_LOCAL_NAME, reason);
			referenceRMMsgContext.setProperty(SOAP12Constants.SOAP_FAULT_DETAIL_LOCAL_NAME, detail);
		} else if (SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI.equals (SOAPNamespaceValue)) {
			reason.setText(data.getReason());
			referenceRMMsgContext.setProperty(SOAP11Constants.SOAP_FAULT_CODE_LOCAL_NAME, faultCode);
			referenceRMMsgContext.setProperty(SOAP11Constants.SOAP_FAULT_DETAIL_LOCAL_NAME, detail);
			referenceRMMsgContext.setProperty(SOAP11Constants.SOAP_FAULT_STRING_LOCAL_NAME, reason);
			// Need to send this message as the Axis Layer doesn't set the "SequenceFault" header
			MessageContext faultMessageContext = 
				MessageContextBuilder.createFaultMessageContext(referenceRMMsgContext.getMessageContext(), null);
			if(acksToEPR!=null){
        		if (log.isDebugEnabled())
        			log.debug("Debug: FaultManager::getOrSendFault: rewrriting fault destination EPR to " + acksToEPR);
        		faultMessageContext.setTo(acksToEPR);
			}

			SOAPFaultEnvelopeCreator.addSOAPFaultEnvelope(faultMessageContext, Sandesha2Constants.SOAPVersion.v1_1, data, referenceRMMsgContext.getRMNamespaceValue());			
			
			// Set the action // TODO SET THE ACTION BASED ON THE SPEC
			faultMessageContext.setWSAAction(
					SpecSpecificConstants.getAddressingFaultAction(referenceRMMsgContext.getRMSpecVersion()));
			
			if (log.isDebugEnabled())
				log.debug("Sending fault message " + faultMessageContext.getEnvelope().getHeader());

				// Sending the message
				//having a surrounded try block will make sure that the error is logged here 
				//and that this does not disturb the processing of a carrier message.
				try {
					AxisEngine.sendFault(faultMessageContext);
					
					EndpointReference destination = faultMessageContext.getTo();
					if(destination == null || destination.hasAnonymousAddress()) {
						TransportUtils.setResponseWritten(referenceRMMsgContext.getMessageContext(), true);
					}
				} catch (Exception e) {
					AxisFault fault = new AxisFault(faultColdValue.getTextAsQName(), data.getReason(), "", "", data.getDetail());
					String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.couldNotSendFaultDueToException, fault.getMessage(), e.getMessage());
					log.error(message);
				}
			
			return;
			
		} else {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.unknownSoapVersion);
			throw new SandeshaException (message);
		}
		
		AxisFault fault = new AxisFault(faultColdValue.getTextAsQName(), data.getReason(), "", "", data.getDetail());
		fault.setFaultAction(SpecSpecificConstants.getAddressingFaultAction(referenceRMMsgContext.getRMSpecVersion()));
		
		//if this is throwable throwing it out, else we will log here.
		
		if (throwable)
            if (referenceRMMsgContext.getMessageContext().isServerSide()) {
        		if (log.isDebugEnabled())
        			log.debug("Exit: FaultManager::getOrSendFault: " + fault);
                throw fault; 
            }
        
		// TODO - Remove console written strings
		log.error("Sandesha2 got a fault when processing the message " + referenceRMMsgContext.getMessageId(), fault);

		if (log.isDebugEnabled())
			log.debug("Exit: FaultManager::getOrSendFault");
	}
	


	public static boolean isRMFault (String faultSubcodeValue) {
		if (faultSubcodeValue==null)
			return false;
		
		if (Sandesha2Constants.SOAPFaults.Subcodes.CREATE_SEQUENCE_REFUSED.equalsIgnoreCase (faultSubcodeValue) ||
			Sandesha2Constants.SOAPFaults.Subcodes.INVALID_ACKNOWLEDGEMENT.equalsIgnoreCase (faultSubcodeValue) ||	
			Sandesha2Constants.SOAPFaults.Subcodes.LAST_MESSAGE_NO_EXCEEDED.equalsIgnoreCase (faultSubcodeValue) ||
			Sandesha2Constants.SOAPFaults.Subcodes.MESSAGE_NUMBER_ROLEOVER.equalsIgnoreCase (faultSubcodeValue) ||
			Sandesha2Constants.SOAPFaults.Subcodes.SEQUENCE_CLOSED.equalsIgnoreCase (faultSubcodeValue) ||
			Sandesha2Constants.SOAPFaults.Subcodes.SEQUENCE_TERMINATED.equalsIgnoreCase (faultSubcodeValue) ||
			Sandesha2Constants.SOAPFaults.Subcodes.UNKNOWN_SEQUENCE.equalsIgnoreCase (faultSubcodeValue) ) {
		
			return true;
		}
		
		return false;
		
	}
	
	private static InvocationResponse manageIncomingFault (AxisFault fault, RMMsgContext rmMsgCtx, SOAPFault faultPart) throws AxisFault {
		if (log.isDebugEnabled())
			log.debug("Enter: FaultManager::manageIncomingFault");
		InvocationResponse response = InvocationResponse.CONTINUE;
		if (log.isErrorEnabled())
			log.error(fault);
		
		SandeshaListener listner = (SandeshaListener) rmMsgCtx.getProperty(SandeshaClientConstants.SANDESHA_LISTENER);
		if (listner!=null)
			listner.onError(fault);
		
		// Get the SOAPVersion
		SOAPFactory factory = (SOAPFactory) rmMsgCtx.getSOAPEnvelope().getOMFactory();		
		String SOAPNamespaceValue = factory.getSoapVersionURI();
		
		String soapFaultSubcode = null;
		String identifier = null;
		boolean isSOAP11SequenceUnknownFault = false;
		
		if (SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI.equals(SOAPNamespaceValue)) {
			// Need to get the sequence part from the Header.
			if (log.isDebugEnabled()) 
		    	log.debug("soap11");
//			try {
				SequenceFault sequenceFault = rmMsgCtx.getSequenceFault();
	      
				// If the sequence fault part is not null, then we have an RM specific fault.
				if (sequenceFault != null) {
					soapFaultSubcode = sequenceFault.getFaultCode().getFaultCode().getLocalPart();
					// Get the identifier - if there is one.
					identifier = sequenceFault.getFaultCode().getDetail();
					isSOAP11SequenceUnknownFault = true;
					if (log.isDebugEnabled()) 
						log.debug("isSOAP11SequenceUnknownFault " + identifier);
				} 
	    		    	
//			} catch (SandeshaException e) {
//				if (log.isDebugEnabled()) 
//					log.debug("Unable to process SequenceFault", e);
//			}
		}
	
		// If we haven't found a soapFaultSubcode at this point - look inside the AxisFault for the information.
		if (soapFaultSubcode == null && faultPart.getCode() != null && 
				faultPart.getCode().getSubCode() != null &&
				faultPart.getCode().getSubCode().getValue() != null)
		
			soapFaultSubcode = faultPart.getCode().getSubCode().getValue().getTextAsQName().getLocalPart();
		
		// Get the identifier, if there is one.
		SOAPFaultDetail detail = faultPart.getDetail();
		if (detail != null && !isSOAP11SequenceUnknownFault)
		{
			OMElement identifierOM = detail.getFirstChildWithName(new QName(rmMsgCtx.getRMNamespaceValue(), 
					Sandesha2Constants.WSRM_COMMON.IDENTIFIER));
			if (identifierOM != null)
				identifier = identifierOM.getText();
		}			

		if (Sandesha2Constants.SOAPFaults.Subcodes.CREATE_SEQUENCE_REFUSED.equals(soapFaultSubcode)) {
			processCreateSequenceRefusedFault(rmMsgCtx, fault);
		} else if (Sandesha2Constants.SOAPFaults.Subcodes.UNKNOWN_SEQUENCE.equals(soapFaultSubcode) ||
				Sandesha2Constants.SOAPFaults.Subcodes.SEQUENCE_TERMINATED.equals(soapFaultSubcode) ) {
			processSequenceUnknownFault(rmMsgCtx, fault, identifier);
		}
		
		// If the operation is an Sandesha In Only operation, or the fault is a recognised fault,
		// then stop the message from being processed further.
		// To configure the actions for Sandesha to drop, add them to the module.xml under
		// Sandesha2InOnly operation.
		if (isRMFault(soapFaultSubcode))
			response = InvocationResponse.ABORT;
		
		if (log.isDebugEnabled())
			log.debug("Exit: FaultManager::manageIncomingFault, " + response);
		return response;
	}
	
	public static InvocationResponse processMessagesForFaults (RMMsgContext rmMsgCtx, StorageManager storageManager) throws AxisFault {
		
		InvocationResponse response = InvocationResponse.CONTINUE;
		
		SOAPEnvelope envelope = rmMsgCtx.getSOAPEnvelope();
		if (envelope==null) 
			return response;
		
		SOAPFault faultPart = envelope.getBody().getFault();

		if (faultPart != null) {
	    Transaction transaction = null;

	    try {
	    	transaction = storageManager.getTransaction();

	    	// constructing the fault
	    	AxisFault axisFault = getAxisFaultFromFromSOAPFault(faultPart);
	    	response = manageIncomingFault (axisFault, rmMsgCtx, faultPart);
	    	
	    	if(transaction != null && transaction.isActive()) transaction.commit();
	    	transaction = null;
	    } finally {
	    	if (transaction != null && transaction.isActive())
	    		transaction.rollback();
			}
		}
		return response;
	}

	
	private static AxisFault getAxisFaultFromFromSOAPFault(SOAPFault faultPart) {
		AxisFault axisFault = new AxisFault(faultPart.getCode(), faultPart.getReason(), faultPart.getNode(), faultPart
				.getRole(), faultPart.getDetail());

		return axisFault;
	}

	/** 
	 * Checks to see if the message number received is == to the Long.MAX_VALUE
	 * 
	 * Throws and AxisFault, or sends a Fault message if the condition is met.
	 * @throws AxisFault 
	 */
	public static boolean checkForMessageRolledOver(RMMsgContext rmMessageContext, String sequenceId, long msgNo, RMDBean bean)throws AxisFault {
		if (msgNo == Long.MAX_VALUE) {
			if (log.isDebugEnabled()) 
				log.debug("Max message number reached " + msgNo);
			// Return a CreateSequenceRefused error
			MessageContext messageContext = rmMessageContext.getMessageContext();

			int SOAPVersion = SandeshaUtil.getSOAPVersion(messageContext.getEnvelope());

			FaultData data = new FaultData();
			data.setCode(SOAP11Constants.FAULT_CODE_SENDER);
			data.setSubcode(SpecSpecificConstants.getFaultSubcode(rmMessageContext.getRMNamespaceValue(), 
					Sandesha2Constants.SOAPFaults.FaultType.MESSAGE_NUMBER_ROLLOVER ));

			SOAPFactory factory = SOAPAbstractFactory.getSOAPFactory(SOAPVersion);
			OMElement identifierElement = factory.createOMElement(Sandesha2Constants.WSRM_COMMON.IDENTIFIER,
					rmMessageContext.getRMNamespaceValue(), Sandesha2Constants.WSRM_COMMON.NS_PREFIX_RM);
			identifierElement.setText(sequenceId);
			
			OMElement maxMsgNumber = factory.createOMElement(Sandesha2Constants.WSRM_COMMON.MAX_MSG_NUMBER,
					rmMessageContext.getRMNamespaceValue(), Sandesha2Constants.WSRM_COMMON.NS_PREFIX_RM);
			maxMsgNumber.setText(Long.toString(msgNo));
			
			data.setDetail(identifierElement);
			data.setDetail2(maxMsgNumber);

			data.setReason(SandeshaMessageHelper.getMessage(SandeshaMessageKeys.messageNumberRollover));
			
			data.setType(Sandesha2Constants.SOAPFaults.FaultType.MESSAGE_NUMBER_ROLLOVER);

			getOrSendFault(rmMessageContext, data, true, bean.getAcksToEndpointReference());
			
			return true;
		}
		return false;
	}
	
	/**
	 * On receipt of a CreateSequenceRefused fault, terminate the sequence and notify any waiting
	 * clients of the error.
	 * @param fault 
	 * @throws AxisFault 
	 */
	private static void processCreateSequenceRefusedFault(RMMsgContext rmMsgCtx, AxisFault fault) throws AxisFault {
		if (log.isDebugEnabled())
			log.debug("Enter: FaultManager::processCreateSequenceRefusedFault");

		ConfigurationContext configCtx = rmMsgCtx.getMessageContext().getConfigurationContext();

		StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(configCtx, configCtx
				.getAxisConfiguration());

		RelatesTo relatesTo = rmMsgCtx.getMessageContext().getRelatesTo();
		String createSeqMsgId = null;
		if (relatesTo != null) {
			createSeqMsgId = relatesTo.getValue();
		} else {
			// Work out the related message from the operation context
			OperationContext context = rmMsgCtx.getMessageContext().getOperationContext();
			MessageContext createSeq = context.getMessageContext(WSDLConstants.MESSAGE_LABEL_OUT_VALUE);
			if(createSeq != null) createSeqMsgId = createSeq.getMessageID();
		}
		if(createSeqMsgId == null) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.relatesToNotAvailable);
			log.error(message);
			throw new SandeshaException(message);
		}

		SenderBeanMgr retransmitterMgr = storageManager.getSenderBeanMgr();
		RMSBeanMgr rmsBeanMgr = storageManager.getRMSBeanMgr();

		RMSBean rmsBean = rmsBeanMgr.retrieve(createSeqMsgId);
		if (rmsBean == null) {
			if (log.isDebugEnabled())
				log.debug("Exit: FaultManager::processCreateSequenceRefusedFault Unable to find RMSBean");
			return;
		}
		if(rmsBean.getSequenceID()!=null){
			//we got a cseqRefused but the sequence is already setup - this implies a timing condition whereby several resends of the cSeqReq have been sent out.
			//The best thing to do here is to ignore it.
			if (log.isDebugEnabled())
				log.debug("Exit: FaultManager::processCreateSequenceRefusedFault Sequence already established - no requirement to cleanup");
			return;			
		}
		
		// Indicate that there was an error when sending the Create Sequence.
		rmsBean.setLastSendError(fault);
		// Mark the sequence as terminated
		rmsBean.setTerminated(true);
			
		// Update the RMSBean
		rmsBeanMgr.update(rmsBean);

		SenderBean createSequenceSenderBean = retransmitterMgr.retrieve(createSeqMsgId);
		if (createSequenceSenderBean == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(SandeshaMessageKeys.createSeqEntryNotFound));

		// deleting the create sequence entry.
		retransmitterMgr.delete(createSeqMsgId);
			
		// Notify the clients of a failure
		notifyClientsOfFault(rmsBean.getInternalSequenceID(), storageManager, configCtx, fault);
		
		rmMsgCtx.pause();
		
		// Cleanup sending side.
		if (log.isDebugEnabled())
			log.debug("Terminating sending sequence " + rmsBean);
		TerminateManager.terminateSendingSide(rmsBean, storageManager, false);

		if (log.isDebugEnabled())
			log.debug("Exit: FaultManager::processCreateSequenceRefusedFault");
	}

	/**
	 * If the RMD returns a SequenceTerminated, or an Unknown sequence fault, then we should 
	 * mark the RMS Sequence as terminated and notify clients of the error.
	 * 
	 * @param rmMsgCtx
	 * @param fault
	 * @param identifier 
	 */
	private static void processSequenceUnknownFault(RMMsgContext rmMsgCtx, AxisFault fault, String sequenceID) throws AxisFault {
		if (log.isDebugEnabled())
			log.debug("Enter: FaultManager::processSequenceUnknownFault " + sequenceID);

		ConfigurationContext configCtx = rmMsgCtx.getMessageContext().getConfigurationContext();

		StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(configCtx, configCtx
				.getAxisConfiguration());
		
		// Find the rmsBean
		RMSBean rmsBean = SandeshaUtil.getRMSBeanFromSequenceId(storageManager, sequenceID);
		if (rmsBean != null) {
					
			rmMsgCtx.pause();
			
			// Cleanup sending side.
			if (log.isDebugEnabled())
				log.debug("Terminating sending sequence " + rmsBean);
			if(!TerminateManager.terminateSendingSide(rmsBean, storageManager, true)){
				// We did not reallocate so we notify the clients of a failure
				notifyClientsOfFault(rmsBean.getInternalSequenceID(), storageManager, configCtx, fault);
			}
			
			// Update the last activated time.
			rmsBean.setLastActivatedTime(System.currentTimeMillis());
			
			// Update the bean in the map
			storageManager.getRMSBeanMgr().update(rmsBean);
		}
		else {
			RMDBean rmdBean = SandeshaUtil.getRMDBeanFromSequenceId(storageManager, sequenceID);
			if (rmdBean != null) {
				rmMsgCtx.pause();
				
				// Cleanup sending side.
				if (log.isDebugEnabled())
					log.debug("Terminating sending sequence " + rmdBean);
				TerminateManager.cleanReceivingSideOnTerminateMessage(configCtx, rmdBean.getSequenceID(), storageManager);
				
				// Update the last activated time.
				rmdBean.setLastActivatedTime(System.currentTimeMillis());
				
				// Update the bean in the map
				storageManager.getRMDBeanMgr().update(rmdBean);
			
			}
			else {
				if (log.isDebugEnabled())
					log.debug("Exit: FaultManager::processSequenceUnknownFault Unable to find sequence");
				return;
			}
		}

		if (log.isDebugEnabled())
			log.debug("Exit: FaultManager::processSequenceUnknownFault");	  
  }

	static void notifyClientsOfFault(String internalSequenceId, 
			StorageManager storageManager, ConfigurationContext configCtx, AxisFault fault) throws SandeshaStorageException {
		// Locate and update all of the messages for this sequence, now that we know
		// the sequence id.
		SenderBean target = new SenderBean();
		target.setInternalSequenceID(internalSequenceId);
		
		Iterator iterator = storageManager.getSenderBeanMgr().find(target).iterator();
		while (iterator.hasNext()) {
			SenderBean tempBean = (SenderBean) iterator.next();

			String messageStoreKey = tempBean.getMessageContextRefKey();
			
			// Retrieve the message context.
			MessageContext context = storageManager.retrieveMessageContext(messageStoreKey, configCtx);
			
      AxisOperation axisOperation = context.getAxisOperation();
      if (axisOperation != null)
      {
        MessageReceiver msgReceiver = axisOperation.getMessageReceiver();
        if ((msgReceiver != null) && (msgReceiver instanceof CallbackReceiver))
        {
            Object callback = ((CallbackReceiver)msgReceiver).lookupCallback(context.getMessageID());
            if (callback instanceof Callback)
            {
                ((CallbackReceiver)msgReceiver).addCallback(context.getMessageID(),(Callback)callback);
                ((Callback)callback).onError(fault);
            } else if(callback instanceof AxisCallback) {
                ((CallbackReceiver)msgReceiver).addCallback(context.getMessageID(),(AxisCallback)callback);
                ((AxisCallback)callback).onError(fault);
            }
        }
      }
		}

	}
}
