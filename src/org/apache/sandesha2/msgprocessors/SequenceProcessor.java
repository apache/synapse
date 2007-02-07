/*
 * Copyright 2006 The Apache Software Foundation.
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

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.addressing.RelatesTo;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.Handler.InvocationResponse;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.RMMsgContext;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.policy.SandeshaPolicyBean;
import org.apache.sandesha2.security.SecurityManager;
import org.apache.sandesha2.security.SecurityToken;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.beanmanagers.InvokerBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.RMDBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.SenderBeanMgr;
import org.apache.sandesha2.storage.beans.InvokerBean;
import org.apache.sandesha2.storage.beans.RMDBean;
import org.apache.sandesha2.storage.beans.RMSBean;
import org.apache.sandesha2.storage.beans.SenderBean;
import org.apache.sandesha2.util.AcknowledgementManager;
import org.apache.sandesha2.util.FaultManager;
import org.apache.sandesha2.util.Range;
import org.apache.sandesha2.util.RangeString;
import org.apache.sandesha2.util.SandeshaUtil;
import org.apache.sandesha2.util.TerminateManager;
import org.apache.sandesha2.workers.SandeshaThread;
import org.apache.sandesha2.wsrm.Sequence;

/**
 * Responsible for processing the Sequence header (if present) on an incoming
 * message.
 */

public class SequenceProcessor {

	private static final Log log = LogFactory.getLog(SequenceProcessor.class);

	public InvocationResponse processSequenceHeader(RMMsgContext rmMsgCtx) throws AxisFault {
		if (log.isDebugEnabled())
			log.debug("Enter: SequenceProcessor::processSequenceHeader");
		InvocationResponse result = InvocationResponse.CONTINUE;
		Sequence sequence = (Sequence) rmMsgCtx.getMessagePart(Sandesha2Constants.MessageParts.SEQUENCE);
		if(sequence != null) {
			// This is a reliable message, so hand it on to the main routine
			result = processReliableMessage(rmMsgCtx);
		} else {
			if (log.isDebugEnabled())
				log.debug("Message does not contain a sequence header");
		}
		if (log.isDebugEnabled())
			log.debug("Exit: SequenceProcessor::processSequenceHeader " + result);
		return result;
	}
	
	public InvocationResponse processReliableMessage(RMMsgContext rmMsgCtx) throws AxisFault {
		if (log.isDebugEnabled())
			log.debug("Enter: SequenceProcessor::processReliableMessage");

		InvocationResponse result = InvocationResponse.CONTINUE;
		
		if (rmMsgCtx.getProperty(Sandesha2Constants.APPLICATION_PROCESSING_DONE) != null
				&& rmMsgCtx.getProperty(Sandesha2Constants.APPLICATION_PROCESSING_DONE).equals("true")) {
			return result;
		}

		MessageContext msgCtx = rmMsgCtx.getMessageContext();
		StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(msgCtx.getConfigurationContext(),msgCtx.getConfigurationContext().getAxisConfiguration());
		Sequence sequence = (Sequence) rmMsgCtx.getMessagePart(Sandesha2Constants.MessageParts.SEQUENCE);
		String sequenceId = sequence.getIdentifier().getIdentifier();
		
		// Check that both the Sequence header and message body have been secured properly
		RMDBeanMgr mgr = storageManager.getRMDBeanMgr();
		RMDBean bean = mgr.retrieve(sequenceId);
		
		if(bean != null && bean.getSecurityTokenData() != null) {
			SecurityManager secManager = SandeshaUtil.getSecurityManager(msgCtx.getConfigurationContext());
			
			QName seqName = new QName(rmMsgCtx.getRMNamespaceValue(), Sandesha2Constants.WSRM_COMMON.SEQUENCE);
			
			SOAPEnvelope envelope = msgCtx.getEnvelope();
			OMElement body = envelope.getBody();
			OMElement seqHeader = envelope.getHeader().getFirstChildWithName(seqName);
			
			SecurityToken token = secManager.recoverSecurityToken(bean.getSecurityTokenData());
			
			secManager.checkProofOfPossession(token, seqHeader, msgCtx);
			secManager.checkProofOfPossession(token, body, msgCtx);
		}
		
		// setting acked msg no range
		ConfigurationContext configCtx = rmMsgCtx.getMessageContext().getConfigurationContext();
		if (configCtx == null) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.configContextNotSet);
			log.debug(message);
			throw new SandeshaException(message);
		}

		if (FaultManager.checkForUnknownSequence(rmMsgCtx, sequenceId, storageManager)) {
			if (log.isDebugEnabled())
				log.debug("Exit: SequenceProcessor::processReliableMessage, Unknown sequence");
			return InvocationResponse.ABORT;
		}
		

		// setting mustUnderstand to false.
		sequence.setMustUnderstand(false);
		rmMsgCtx.addSOAPEnvelope();

		// throwing a fault if the sequence is terminated
		if (FaultManager.checkForSequenceTerminated(rmMsgCtx, sequenceId, bean)) {
			if (log.isDebugEnabled())
				log.debug("Exit: SequenceProcessor::processReliableMessage, Sequence terminated");
			return InvocationResponse.ABORT;
		}
		
		// throwing a fault if the sequence is closed.
		if (FaultManager.checkForSequenceClosed(rmMsgCtx, sequenceId, bean)) {
			if (log.isDebugEnabled())
				log.debug("Exit: SequenceProcessor::processReliableMessage, Sequence closed");
			return InvocationResponse.ABORT;
		}
		FaultManager.checkForLastMsgNumberExceeded(rmMsgCtx, storageManager);
		
		long msgNo = sequence.getMessageNumber().getMessageNumber();
		
		if (FaultManager.checkForMessageRolledOver(rmMsgCtx, sequenceId, msgNo)) {
			
			if (log.isDebugEnabled())
				log.debug("Exit: SequenceProcessor::processReliableMessage, Message rolled over " + msgNo);
			
			return InvocationResponse.ABORT;
		}

		if (msgNo == 0) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.invalidMsgNumber, Long
					.toString(msgNo));
			log.debug(message);
			throw new SandeshaException(message);
		}

		// Pause the messages bean if not the right message to invoke.
		
		// updating the last activated time of the sequence.
		bean.setLastActivatedTime(System.currentTimeMillis());
		
		
		
		if (sequence.getLastMessage()!=null) {
			//setting this as the LastMessage number
			bean.setLastInMessageId(msgCtx.getMessageID());
		}
		
		EndpointReference replyTo = rmMsgCtx.getReplyTo();
		String key = SandeshaUtil.getUUID(); // key to store the message.
		// updating the Highest_In_Msg_No property which gives the highest
		// message number retrieved from this sequence.
		long highestInMsgNo = bean.getHighestInMessageNumber();

		if (msgNo > highestInMsgNo) {
			// If WS-Addressing is turned off there may not be a message id written into the SOAP
			// headers, but we can still fake one up to help us match up requests and replies within
			// this end of the connection.
			String messageId = msgCtx.getMessageID();
			if(messageId == null) {
				messageId = SandeshaUtil.getUUID();
				msgCtx.setMessageID(messageId);
			}
			
			bean.setHighestInMessageId(messageId);
			bean.setHighestInMessageNumber(msgNo);
		}

		// Get the server completed message ranges list
		RangeString serverCompletedMessageRanges = bean.getServerCompletedMessages();
		// See if the message is in the list of completed ranges
		boolean msgNoPresentInList = 
			serverCompletedMessageRanges.isMessageNumberInRanges(msgNo);
		
		String specVersion = rmMsgCtx.getRMSpecVersion();
		if (msgNoPresentInList
				&& (Sandesha2Constants.QOS.InvocationType.DEFAULT_INVOCATION_TYPE == Sandesha2Constants.QOS.InvocationType.EXACTLY_ONCE)) {
			// this is a duplicate message and the invocation type is EXACTLY_ONCE. We try to return
			// ack messages at this point, as if someone is sending duplicates then they may have
			// missed earlier acks. We also have special processing for sync 2-way with RM 1.0
			if((replyTo==null || replyTo.hasAnonymousAddress()) &&
			   (specVersion!=null && specVersion.equals(Sandesha2Constants.SPEC_VERSIONS.v1_0))) {

			    SenderBeanMgr senderBeanMgr = storageManager.getSenderBeanMgr();
			    SenderBean findSenderBean = new SenderBean ();
			    findSenderBean.setMessageType(Sandesha2Constants.MessageTypes.APPLICATION);
			    findSenderBean.setInboundSequenceId(sequence.getIdentifier().getIdentifier());
			    findSenderBean.setInboundMessageNumber(sequence.getMessageNumber().getMessageNumber());
			    findSenderBean.setSend(true);
		
			    SenderBean replyMessageBean = senderBeanMgr.findUnique(findSenderBean);
			    
			    // this is effectively a poll for the replyMessage, wo re-use the logic in the MakeConnection
			    // processor. This will use this thread to re-send the reply, writing it into the transport.
			    // As the reply is now written we do not want to continue processing, or suspend, so we abort.
			    if(replyMessageBean != null) {
			    	if(log.isDebugEnabled()) log.debug("Found matching reply for replayed message");
			    	MakeConnectionProcessor.replyToPoll(rmMsgCtx, replyMessageBean, storageManager, false, null);
					result = InvocationResponse.ABORT;
					if (log.isDebugEnabled())
						log.debug("Exit: SequenceProcessor::processReliableMessage, replayed message: " + result);
					return result;
			    }
		    }
			EndpointReference acksTo = new EndpointReference (bean.getAcksToEPR());
			if (acksTo.hasAnonymousAddress()) {
				RMMsgContext ackRMMsgContext = AcknowledgementManager.generateAckMessage(rmMsgCtx , sequenceId, storageManager,false,true);
				msgCtx.getOperationContext().setProperty(org.apache.axis2.Constants.RESPONSE_WRITTEN, Constants.VALUE_TRUE);
				AcknowledgementManager.sendAckNow(ackRMMsgContext);
				result = InvocationResponse.ABORT;
				if (log.isDebugEnabled())
					log.debug("Exit: SequenceProcessor::processReliableMessage, acking duplicate message: " + result);
				return result;
			}
			
			result = InvocationResponse.ABORT;
			if (log.isDebugEnabled())
				log.debug("Exit: SequenceProcessor::processReliableMessage, dropping duplicate: " + result);
			return result;
		}

		if (!msgNoPresentInList)
		{
			serverCompletedMessageRanges.addRange(new Range(msgNo));
		}
		
		// Update the RMD bean
		mgr.update(bean);
		
		// If we are doing sync 2-way over WSRM 1.0, then we may just have received one of
		// the reply messages that we were looking for. If so we can remove the matching sender bean.
		int mep = msgCtx.getAxisOperation().getAxisSpecifMEPConstant();
		if(specVersion!=null && specVersion.equals(Sandesha2Constants.SPEC_VERSIONS.v1_0) &&
				mep == WSDLConstants.MEP_CONSTANT_OUT_IN) {
			RelatesTo relatesTo = msgCtx.getRelatesTo();
			if(relatesTo != null) {
				String messageId = relatesTo.getValue();
				SenderBean matcher = new SenderBean();
				matcher.setMessageID(messageId);
				SenderBean sender = storageManager.getSenderBeanMgr().findUnique(matcher);
				if(sender != null) {
					if(log.isDebugEnabled()) log.debug("Deleting sender for sync-2-way message");
					storageManager.removeMessageContext(sender.getMessageContextRefKey());
					storageManager.getSenderBeanMgr().delete(messageId);
					
					// Try and terminate the corresponding outbound sequence
					RMSBean rmsBean = SandeshaUtil.getRMSBeanFromSequenceId(storageManager, sender.getSequenceID());
					TerminateManager.checkAndTerminate(rmMsgCtx, storageManager, rmsBean);
				}
			}
		}

		//setting properties for the messageContext
		rmMsgCtx.setProperty(Sandesha2Constants.MessageContextProperties.SEQUENCE_ID,sequenceId);
		rmMsgCtx.setProperty(Sandesha2Constants.MessageContextProperties.MESSAGE_NUMBER,new Long (msgNo));
		
//		adding of acks
		
//		if acksTo anonymous 
//			add an ack entry with an infinite ack interval so that it will be piggybacked by any possible response message
//		else 
//			add an ack entry here
		
		boolean backchannelFree = (replyTo != null && !replyTo.hasAnonymousAddress()) ||
									WSDLConstants.MEP_CONSTANT_IN_ONLY == mep;
		EndpointReference acksTo = new EndpointReference (bean.getAcksToEPR());
		if (acksTo.hasAnonymousAddress() && backchannelFree) {
			Object responseWritten = msgCtx.getOperationContext().getProperty(Constants.RESPONSE_WRITTEN);
			if (responseWritten==null || !Constants.VALUE_TRUE.equals(responseWritten)) {
				RMMsgContext ackRMMsgContext = AcknowledgementManager.generateAckMessage(rmMsgCtx , sequenceId, storageManager,false,true);
				msgCtx.getOperationContext().setProperty(org.apache.axis2.Constants.RESPONSE_WRITTEN, Constants.VALUE_TRUE);
				AcknowledgementManager.sendAckNow(ackRMMsgContext);
			}
		} else { //Scenario 2 and Scenario 3
			SandeshaPolicyBean policyBean = SandeshaUtil.getPropertyBean (msgCtx.getAxisOperation());
			if (policyBean==null) {
				String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.policyBeanNotFound);
				throw new SandeshaException (message);
			}
			//		having a negative value for timeToSend will make this behave as having an infinite ack interval.
			long timeToSend = -1;   
			if (!acksTo.hasAnonymousAddress()) {
				long ackInterval = policyBean.getAcknowledgementInterval();
				timeToSend = System.currentTimeMillis() + ackInterval;
			}
			
			RMMsgContext ackRMMsgContext = AcknowledgementManager.generateAckMessage(rmMsgCtx, sequenceId, storageManager,false,true);

			AcknowledgementManager.removeAckBeanEntries(sequenceId, storageManager);
			AcknowledgementManager.addAckBeanEntry(ackRMMsgContext, sequenceId, timeToSend, storageManager);
		}

		// If the storage manager has an invoker, then they may be implementing inOrder, or
		// transactional delivery. Either way, if they have one we should use it.
		SandeshaThread invoker = storageManager.getInvoker();
		if (invoker != null) {
			// Whatever the MEP, we stop processing here and the invoker will do the real work. We only
			// SUSPEND if we need to keep the backchannel open for the response... we may as well ABORT
			// to let other cases end more quickly.
			if(backchannelFree) {
				result = InvocationResponse.ABORT;
			} else {
				result = InvocationResponse.SUSPEND;
			}
			InvokerBeanMgr storageMapMgr = storageManager.getInvokerBeanMgr();

			storageManager.storeMessageContext(key, rmMsgCtx.getMessageContext());
			storageMapMgr.insert(new InvokerBean(key, msgNo, sequenceId));

			// This will avoid performing application processing more than once.
			rmMsgCtx.setProperty(Sandesha2Constants.APPLICATION_PROCESSING_DONE, "true");

		}
		
		if (log.isDebugEnabled())
			log.debug("Exit: SequenceProcessor::processReliableMessage " + result);
		
		return result;
	}


	public static void sendAckIfNeeded(RMMsgContext rmMsgCtx, StorageManager storageManager, boolean serverSide)
					throws AxisFault {

		if (log.isDebugEnabled())
			log.debug("Enter: SequenceProcessor::sendAckIfNeeded");
		
		Sequence sequence = (Sequence) rmMsgCtx
				.getMessagePart(Sandesha2Constants.MessageParts.SEQUENCE);
		
		if(sequence!=null){
			String sequenceId = sequence.getIdentifier().getIdentifier();
			ConfigurationContext configCtx = rmMsgCtx.getMessageContext()
					.getConfigurationContext();
			if (configCtx == null) {
				String message = SandeshaMessageHelper
						.getMessage(SandeshaMessageKeys.configContextNotSet);
				if (log.isDebugEnabled())
					log.debug(message);
				throw new SandeshaException(message);
			}

			RMMsgContext ackRMMsgCtx = AcknowledgementManager.generateAckMessage(
					rmMsgCtx , sequenceId, storageManager,
					false, serverSide);
			MessageContext ackMsgCtx = ackRMMsgCtx.getMessageContext();

			EndpointReference acksTo = ackRMMsgCtx.getTo();
			EndpointReference replyTo = rmMsgCtx.getReplyTo();
			boolean anonAck = (acksTo == null) || acksTo.hasAnonymousAddress();
			boolean anonReply = (replyTo == null) || replyTo.hasAnonymousAddress();

			// Only use the backchannel for ack messages if we are sure that the
			// application
			// doesn't need it. A 1-way MEP should be complete by now.
			boolean complete = ackMsgCtx.getOperationContext().isComplete();
			if (anonAck && anonReply && !complete) {
				if (log.isDebugEnabled())
					log
							.debug("Exit: SequenceProcessor::sendAckIfNeeded, avoiding using backchannel");
				return;
			}

			long ackInterval = SandeshaUtil.getPropertyBean(
					rmMsgCtx.getMessageContext().getAxisService())
					.getAcknowledgementInterval();

			long timeToSend = System.currentTimeMillis() + ackInterval;
			if (anonAck) {
				AcknowledgementManager.sendAckNow(ackRMMsgCtx);
			} else if (!anonAck) {
				AcknowledgementManager.addAckBeanEntry(ackRMMsgCtx, sequenceId, timeToSend, storageManager);
			}			
		}

		if (log.isDebugEnabled())
			log.debug("Exit: SequenceProcessor::sendAckIfNeeded");
	}

}
