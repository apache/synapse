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

package org.apache.sandesha2.msgprocessors;

import java.util.Iterator;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.RMMsgContext;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.polling.PollingManager;
import org.apache.sandesha2.security.SecurityManager;
import org.apache.sandesha2.security.SecurityToken;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.beanmanagers.RMDBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.SenderBeanMgr;
import org.apache.sandesha2.storage.beans.RMDBean;
import org.apache.sandesha2.storage.beans.RMSBean;
import org.apache.sandesha2.storage.beans.SenderBean;
import org.apache.sandesha2.util.FaultManager;
import org.apache.sandesha2.util.Range;
import org.apache.sandesha2.util.RangeString;
import org.apache.sandesha2.util.SandeshaUtil;
import org.apache.sandesha2.util.TerminateManager;
import org.apache.sandesha2.wsrm.AcknowledgementRange;
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

		Iterator iter = message.getSequenceAcknowledgements();
		while(iter.hasNext()){
			SequenceAcknowledgement sa = (SequenceAcknowledgement)iter.next();
			processAckHeader(message, sa.getOriginalSequenceAckElement(), sa);
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
		
		boolean piggybackedAck = !(rmMsgCtx.getMessageType()==Sandesha2Constants.MessageTypes.ACK);
		
		MessageContext msgCtx = rmMsgCtx.getMessageContext();
		ConfigurationContext configCtx = msgCtx.getConfigurationContext();

		StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(configCtx, configCtx
				.getAxisConfiguration());

		SenderBeanMgr retransmitterMgr = storageManager.getSenderBeanMgr();

		String outSequenceId = sequenceAck.getIdentifier().getIdentifier();
		RMSBean rmsBean = SandeshaUtil.getRMSBeanFromSequenceId(storageManager, outSequenceId);

		if(rmsBean==null){
		  if (log.isDebugEnabled())
			  log.debug("Exit: AcknowledgementProcessor::processAckHeader, Sequence bean not found");
		  return;
		}
		
		if (outSequenceId == null || outSequenceId.length()==0) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.outSeqIDIsNull);
			log.debug(message);
			throw new SandeshaException(message);
		}
		if (FaultManager.checkForSequenceTerminated(rmMsgCtx, outSequenceId, rmsBean, piggybackedAck)) {
			if (log.isDebugEnabled())
				log.debug("Exit: AcknowledgementProcessor::processAckHeader, Sequence terminated");
			return;
		}

		// Check that the sender of this Ack holds the correct token
		String internalSequenceId = rmsBean.getInternalSequenceID();
		if(rmsBean.getSecurityTokenData() != null) {
			SecurityManager secManager = SandeshaUtil.getSecurityManager(configCtx);
			SecurityToken token = secManager.recoverSecurityToken(rmsBean.getSecurityTokenData());
			
			secManager.checkProofOfPossession(token, soapHeader, msgCtx);
		}
		
		if(log.isDebugEnabled()) log.debug("Got Ack for RM Sequence: " + outSequenceId + ", internalSeqId: " + internalSequenceId);
		Iterator ackRangeIterator = sequenceAck.getAcknowledgementRanges().iterator();
		
		if (FaultManager.checkForInvalidAcknowledgement(rmMsgCtx, sequenceAck, storageManager, rmsBean, piggybackedAck)) {
			if (log.isDebugEnabled())
				log.debug("Exit: AcknowledgementProcessor::processAckHeader, Invalid Ack range ");
			return;
		}
		
		EndpointReference replyTo = rmsBean.getReplyToEndpointReference();
		boolean anonReplyTo = replyTo==null || replyTo.hasAnonymousAddress();
		
		String rmVersion = rmMsgCtx.getRMSpecVersion();
		
		// Compare the clientCompletedMessages with the range we just got, to work out if there
		// is any new information in this ack message
		RangeString completedMessages = rmsBean.getClientCompletedMessages();
		long numberOfNewMessagesAcked = 0;

		while(ackRangeIterator.hasNext()) {
			AcknowledgementRange ackRange = (AcknowledgementRange) ackRangeIterator.next();
			long lower = ackRange.getLowerValue();
			long upper = ackRange.getUpperValue();
			Range ackedRange = new Range(lower, upper);
			// Quick check to see if the whole range is already covered
			if(!completedMessages.isRangeCompleted(ackedRange)) {
				//we now know that this range is complete so we update it. This should aggregate the
				//ranges together and tell us which numbers are newly acked
				Range[] newRanges = completedMessages.addRange(ackedRange).getRanges();
				
				// We now take each newly acked message in turn and see if we need to update a sender bean
				for (int rangeIndex=0; rangeIndex < newRanges.length; rangeIndex++) {
					//now work on each newly acked message in this range
					for(long messageNo = newRanges[rangeIndex].lowerValue; messageNo<=newRanges[rangeIndex].upperValue; messageNo++){
						
						numberOfNewMessagesAcked++;
						SenderBean matcher = new SenderBean();
						matcher.setSequenceID(outSequenceId);
						
						matcher.setMessageNumber(messageNo);
						
						SenderBean retransmitterBean = retransmitterMgr.findUnique(matcher);
						if (retransmitterBean != null) {
							// Check we haven't got an Ack for a message that hasn't been sent yet !
							if (retransmitterBean.getSentCount() == 0) {
								FaultManager.makeInvalidAcknowledgementFault(rmMsgCtx, sequenceAck, ackRange,
										storageManager, piggybackedAck, null); //do not want to send the fault to acksTo in this case
								if (log.isDebugEnabled())
									log.debug("Exit: AcknowledgementProcessor::processAckHeader, Invalid Ack");
								return;
							}
							
							String storageKey = retransmitterBean.getMessageContextRefKey();
							
							boolean syncResponseNeeded = false;
							if (Sandesha2Constants.SPEC_VERSIONS.v1_0.equals(rmVersion) && anonReplyTo) {
								MessageContext applicationMessage = storageManager.retrieveMessageContext(storageKey, configCtx);
								AxisOperation operation = applicationMessage.getAxisOperation();
								if(operation!= null) {
									int mep = operation.getAxisSpecificMEPConstant();
									syncResponseNeeded = (mep == WSDLConstants.MEP_CONSTANT_OUT_IN);
								}
							}

							if (!syncResponseNeeded) {
								// removing the application message from the storage.
								retransmitterMgr.delete(retransmitterBean.getMessageID());
								storageManager.removeMessageContext(storageKey);
							}
						}
					}//end for
				}//end for
			} //end while
		}

		// updating the last activated time of the sequence.
		rmsBean.setLastActivatedTime(System.currentTimeMillis());

		//adding a MakeConnection for the response sequence if needed.
		if (rmsBean.getOfferedSequence() != null) {

			RMDBeanMgr rMDBeanMgr = storageManager.getRMDBeanMgr();
			RMDBean rMDBean = rMDBeanMgr.retrieve(outSequenceId);
			
			if (rMDBean!=null && rMDBean.isPollingMode()) {
				PollingManager manager = storageManager.getPollingManager();
				if(manager != null) manager.schedulePollingRequest(rMDBean.getSequenceID(), false);
			}
		}

		// We overwrite the previous client completed message ranges with the
		// latest view, but only if it is an update i.e. contained a new
		// ack range (which is because we do not previous acks arriving late 
		// to break us)
		if (numberOfNewMessagesAcked>0) {
			rmsBean.setClientCompletedMessages(completedMessages);
		}
		
		// Update the RMSBean
		storageManager.getRMSBeanMgr().update(rmsBean);

		// Try and terminate the sequence
		if (!rmsBean.isAvoidAutoTermination()) 
			TerminateManager.checkAndTerminate(rmMsgCtx.getConfigurationContext(), storageManager, rmsBean);

		if (log.isDebugEnabled())
			log.debug("Exit: AcknowledgementProcessor::processAckHeader");
	}


}
