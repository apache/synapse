/*
 * Copyright 1999-2004 The Apache Software Foundation.
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
 *  
 */

package org.apache.sandesha2.msgprocessors;

import java.util.Collection;
import java.util.Iterator;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.axis2.wsdl.WSDLConstants.WSDL20_2004Constants;
import org.apache.axis2.wsdl.WSDLConstants.WSDL20_2006Constants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.RMMsgContext;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.security.SecurityManager;
import org.apache.sandesha2.security.SecurityToken;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.beanmanagers.RMDBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.SenderBeanMgr;
import org.apache.sandesha2.storage.beans.RMDBean;
import org.apache.sandesha2.storage.beans.RMSBean;
import org.apache.sandesha2.storage.beans.SenderBean;
import org.apache.sandesha2.util.AcknowledgementManager;
import org.apache.sandesha2.util.FaultManager;
import org.apache.sandesha2.util.Range;
import org.apache.sandesha2.util.RangeString;
import org.apache.sandesha2.util.SandeshaUtil;
import org.apache.sandesha2.util.TerminateManager;
import org.apache.sandesha2.wsrm.AcknowledgementRange;
import org.apache.sandesha2.wsrm.Nack;
import org.apache.sandesha2.wsrm.SequenceAcknowledgement;

/**
 * Responsible for processing acknowledgement headers on incoming messages.
 */

public class AcknowledgementProcessor {

	private static final Log log = LogFactory.getLog(AcknowledgementProcessor.class);

	/**
	 * @param message
	 * @throws AxisFault
	 */
	public void processAckHeaders(RMMsgContext message) throws AxisFault {
		if (log.isDebugEnabled())
			log.debug("Enter: AcknowledgementProcessor::processAckHeaders");

		SOAPEnvelope envelope = message.getMessageContext().getEnvelope();
		SOAPHeader header = envelope.getHeader();
		if(header!=null)
		{
			for(int i = 0; i < Sandesha2Constants.SPEC_NS_URIS.length; i++) {
				QName headerName = new QName(Sandesha2Constants.SPEC_NS_URIS[i], Sandesha2Constants.WSRM_COMMON.SEQUENCE_ACK);
				
				Iterator acks = header.getChildrenWithName(headerName);
				while(acks.hasNext()) {
					OMElement ack = (OMElement) acks.next();
					SequenceAcknowledgement seqAck = new SequenceAcknowledgement(headerName.getNamespaceURI());
					seqAck.fromOMElement(ack);
					processAckHeader(message, ack, seqAck);
				}
			}			
		}


		if (log.isDebugEnabled())
			log.debug("Exit: AcknowledgementProcessor::processAckHeaders");
	}
	
	/**
	 * @param rmMsgCtx
	 * @param soapHeader
	 * @param sequenceAck
	 * @throws AxisFault
	 */
	private void processAckHeader(RMMsgContext rmMsgCtx, OMElement soapHeader, SequenceAcknowledgement sequenceAck)
		throws AxisFault {
		if (log.isDebugEnabled())
			log.debug("Enter: AcknowledgementProcessor::processAckHeader " + soapHeader);
		
		MessageContext msgCtx = rmMsgCtx.getMessageContext();
		ConfigurationContext configCtx = msgCtx.getConfigurationContext();

		StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(configCtx, configCtx
				.getAxisConfiguration());

		SenderBeanMgr retransmitterMgr = storageManager.getSenderBeanMgr();

		String outSequenceId = sequenceAck.getIdentifier().getIdentifier();
		if (outSequenceId == null || "".equals(outSequenceId)) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.outSeqIDIsNull);
			log.debug(message);
			throw new SandeshaException(message);
		}

		// Check that the sender of this Ack holds the correct token
		RMSBean rmsBean = SandeshaUtil.getRMSBeanFromSequenceId(storageManager, outSequenceId);
		String internalSequenceId = rmsBean.getInternalSequenceID();
		if(rmsBean.getSecurityTokenData() != null) {
			SecurityManager secManager = SandeshaUtil.getSecurityManager(configCtx);
			SecurityToken token = secManager.recoverSecurityToken(rmsBean.getSecurityTokenData());
			
			secManager.checkProofOfPossession(token, soapHeader, msgCtx);
		}
		
		if(log.isDebugEnabled()) log.debug("Got Ack for RM Sequence: " + outSequenceId + ", internalSeqId: " + internalSequenceId);
		Iterator ackRangeIterator = sequenceAck.getAcknowledgementRanges().iterator();
		Iterator nackIterator = sequenceAck.getNackList().iterator();

		if (FaultManager.checkForUnknownSequence(rmMsgCtx, outSequenceId, storageManager)) {
			if (log.isDebugEnabled())
				log.debug("Exit: AcknowledgementProcessor::processAckHeader, Unknown sequence ");
			return;
		}
		
		if (FaultManager.checkForInvalidAcknowledgement(rmMsgCtx, sequenceAck, storageManager, rmsBean)) {
			if (log.isDebugEnabled())
				log.debug("Exit: AcknowledgementProcessor::processAckHeader, Invalid Ack range ");
			return;
		}
		
		SenderBean input = new SenderBean();
		input.setSend(true);
		input.setReSend(true);
		input.setMessageType(Sandesha2Constants.MessageTypes.APPLICATION);
		input.setInternalSequenceID(internalSequenceId);
		Collection retransmitterEntriesOfSequence = retransmitterMgr.find(input);
		
		String replyToAddress = rmsBean.getReplyToEPR();
		EndpointReference replyTo = new EndpointReference (replyToAddress);
		boolean anonReplyTo = false;
		if (replyTo.hasAnonymousAddress())
			anonReplyTo = true;
		
		String rmVersion = rmMsgCtx.getRMSpecVersion();
		
		boolean syncResponseExpected = false;
		RangeString ackedMessagesRanges = new RangeString(); //keep track of the ranges in the ack msgs
		long numberOfNewMessagesAcked = 0;

		while (ackRangeIterator.hasNext()) {
			AcknowledgementRange ackRange = (AcknowledgementRange) ackRangeIterator.next();
			long lower = ackRange.getLowerValue();
			long upper = ackRange.getUpperValue();
			
			if(log.isDebugEnabled()) 
				log.debug("Ack Range: " + lower + " - " + upper);
			
			long rangeStart = lower;
			long messageNo;
			for (messageNo = lower; messageNo <= upper; messageNo++) {
				SenderBean retransmitterBean = getRetransmitterEntry(retransmitterEntriesOfSequence, messageNo);

				if (retransmitterBean != null) {
					
					// We've got an Ack for a message that hasn't been sent yet !
					if (retransmitterBean.getSentCount() == 0) {
						FaultManager.makeInvalidAcknowledgementFault(rmMsgCtx, sequenceAck, ackRange,
								storageManager, SandeshaMessageHelper.getMessage(SandeshaMessageKeys.ackInvalidNotSent));
						if (log.isDebugEnabled())
							log.debug("Exit: AcknowledgementProcessor::processAckHeader, Invalid Ack");
						return;
					}
					
					String storageKey = retransmitterBean.getMessageContextRefKey();
					
					boolean syncResponseNeeded = false;
					if (Sandesha2Constants.SPEC_VERSIONS.v1_0.equals(rmVersion) && anonReplyTo) {
						MessageContext applicationMessage = storageManager.retrieveMessageContext(storageKey, configCtx);
						AxisOperation operation = applicationMessage.getAxisOperation();
						boolean inOutMessage = false;
						if (operation!=null && 
								(WSDL20_2004Constants.MEP_URI_OUT_IN.equals(operation.getMessageExchangePattern()) ||
										WSDL20_2006Constants.MEP_URI_OUT_IN.equals(operation.getMessageExchangePattern()))) 
							inOutMessage = true;
							
						if (inOutMessage) {	
							OperationContext operationContext = applicationMessage.getOperationContext();
							if (operationContext!=null) {
								MessageContext responseMessage = operationContext.getMessageContext(WSDLConstants.MESSAGE_LABEL_IN_VALUE);
								if (responseMessage==null) {
									syncResponseNeeded = true;
									
									//adding upto the current messageNo
									//current one will not be considered as we need to get an sync response from an repeat of this.
									if (rangeStart<messageNo) {
										ackedMessagesRanges.addRange(new Range(rangeStart, messageNo-1)); 
										rangeStart = messageNo+1;
									}
								}
							}
						}
					}

 					if (!syncResponseNeeded) {
						// removing the application message from the storage.
						retransmitterMgr.delete(retransmitterBean.getMessageID());
						storageManager.removeMessageContext(storageKey);
						numberOfNewMessagesAcked++;
					} else {
						//sync response is needed at least for one message, this should stop the termination.
						syncResponseExpected = true;
					}
				}
			}
			
			if (rangeStart<=upper)
				ackedMessagesRanges.addRange(new Range (rangeStart, upper));
			
		}

		// updating the last activated time of the sequence.
		rmsBean.setLastActivatedTime(System.currentTimeMillis());

		while (nackIterator.hasNext()) {
			Nack nack = (Nack) nackIterator.next();
			long msgNo = nack.getNackNumber();

			// TODO - Process Nack
		}
		
		//adding a MakeConnection for the response sequence if needed.
		if (rmsBean.getOfferedSequence() != null) {

			RMDBeanMgr rMDBeanMgr = storageManager.getRMDBeanMgr();
			RMDBean rMDBean = rMDBeanMgr.retrieve(outSequenceId);
			
			if (rMDBean!=null && rMDBean.isPollingMode())
				SandeshaUtil.shedulePollingRequest(rmsBean.getOfferedSequence(), configCtx);
			
		}

		// setting acked message date.
		// TODO add details specific to each message.
		
		// We overwrite the previous client completed message ranges with the
		// latest view, but only if it is an update i.e. contained a new
		// ack range (which is because we do not previous acks arriving late 
		// to break us)
		if (numberOfNewMessagesAcked>0) {
		  rmsBean.setClientCompletedMessages(ackedMessagesRanges);
			long noOfMsgsAcked = 
				rmsBean.getNumberOfMessagesAcked() + numberOfNewMessagesAcked;
			rmsBean.setNumberOfMessagesAcked(noOfMsgsAcked);
		}
		
		long lastOutMessage = rmsBean.getLastOutMessage ();
		
		// Update the RMSBean
		storageManager.getRMSBeanMgr().update(rmsBean);

		if (lastOutMessage > 0) {
			boolean complete = AcknowledgementManager.verifySequenceCompletion(sequenceAck
					.getAcknowledgementRanges().iterator(), lastOutMessage);

			
			//If this is RM 1.1 and RMAnonURI scenario, dont do the termination unless the response side createSequence has been
			//received (RMDBean has been created) through polling, in this case termination will happen in the create sequence response processor.
			boolean pauseTerminationForCS = false;
			if (Sandesha2Constants.SPEC_VERSIONS.v1_1.equals( rmVersion) && SandeshaUtil.isWSRMAnonymous(replyToAddress)) {
				pauseTerminationForCS = true;
				
				RMDBean findBean = new RMDBean ();
				findBean.setPollingMode(true);
				
				RMDBeanMgr rmdBeanMgr = storageManager.getRMDBeanMgr();
				Iterator rmdBeanIteratort = rmdBeanMgr.find(findBean).iterator();
				while (rmdBeanIteratort.hasNext()) {
					RMDBean rmdBean = (RMDBean) rmdBeanIteratort.next();
					String toAddress = rmdBean.getToAddress();
					if (toAddress!=null && toAddress.equals(replyToAddress)) {
						pauseTerminationForCS = false;
						break;
					}
				}
				
				if (pauseTerminationForCS) {
					rmsBean.setTerminationPauserForCS(true);
					storageManager.getRMSBeanMgr().update(rmsBean);
				}
			}
			
			
			if (complete && !pauseTerminationForCS && !syncResponseExpected && !rmsBean.isTerminateAdded()) {
				TerminateManager.addTerminateSequenceMessage(rmMsgCtx, internalSequenceId, outSequenceId, storageManager);
			}
			
		}

		if (log.isDebugEnabled())
			log.debug("Exit: AcknowledgementProcessor::processAckHeader");
	}

	private SenderBean getRetransmitterEntry(Collection collection, long msgNo) {
		Iterator it = collection.iterator();
		while (it.hasNext()) {
			SenderBean bean = (SenderBean) it.next();
			if (bean.getMessageNumber() == msgNo)
				return bean;
		}

		return null;
	}
}
