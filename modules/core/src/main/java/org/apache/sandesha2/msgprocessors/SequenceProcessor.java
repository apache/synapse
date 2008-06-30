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

import javax.xml.namespace.QName;

import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.addressing.RelatesTo;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.engine.Handler.InvocationResponse;
import org.apache.axis2.transport.RequestResponseTransport;
import org.apache.axis2.transport.TransportUtils;
import org.apache.axis2.transport.RequestResponseTransport.RequestResponseTransportStatus;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.RMMsgContext;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.context.ContextManager;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.msgreceivers.RMMessageReceiver;
import org.apache.sandesha2.policy.SandeshaPolicyBean;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.Transaction;
import org.apache.sandesha2.storage.beanmanagers.RMDBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.RMSBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.SenderBeanMgr;
import org.apache.sandesha2.storage.beans.InvokerBean;
import org.apache.sandesha2.storage.beans.RMDBean;
import org.apache.sandesha2.storage.beans.RMSBean;
import org.apache.sandesha2.storage.beans.SenderBean;
import org.apache.sandesha2.util.AcknowledgementManager;
import org.apache.sandesha2.util.FaultManager;
import org.apache.sandesha2.util.SandeshaUtil;
import org.apache.sandesha2.util.SpecSpecificConstants;
import org.apache.sandesha2.util.TerminateManager;
import org.apache.sandesha2.workers.InvokerWorker;
import org.apache.sandesha2.workers.Sender;
import org.apache.sandesha2.wsrm.Sequence;

/**
 * Responsible for processing the Sequence header (if present) on an incoming
 * message.
 */

public class SequenceProcessor {

	private static final Log log = LogFactory.getLog(SequenceProcessor.class);

	public InvocationResponse processSequenceHeader(RMMsgContext rmMsgCtx, Transaction transaction) throws AxisFault {
		if (log.isDebugEnabled())
			log.debug("Enter: SequenceProcessor::processSequenceHeader");
		
		InvocationResponse result = InvocationResponse.CONTINUE;
		Sequence sequence = rmMsgCtx.getSequence();
		if(sequence != null) {
			// This is a reliable message, so hand it on to the main routine
			result = processReliableMessage(rmMsgCtx, transaction);
		} else {
			if (log.isDebugEnabled())
				log.debug("Message does not contain a sequence header");
		}
		if (log.isDebugEnabled())
			log.debug("Exit: SequenceProcessor::processSequenceHeader " + result);
		return result;
	}
	
	public InvocationResponse processReliableMessage(RMMsgContext rmMsgCtx, Transaction transaction) throws AxisFault {
		if (log.isDebugEnabled())
			log.debug("Enter: SequenceProcessor::processReliableMessage");

		InvocationResponse result = InvocationResponse.CONTINUE;
		
		if (rmMsgCtx.getProperty(Sandesha2Constants.APPLICATION_PROCESSING_DONE) != null
				&& rmMsgCtx.getProperty(Sandesha2Constants.APPLICATION_PROCESSING_DONE).equals("true")) {
			return result;
		}

		MessageContext msgCtx = rmMsgCtx.getMessageContext();
		StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(msgCtx.getConfigurationContext(),msgCtx.getConfigurationContext().getAxisConfiguration());
		Sequence sequence = rmMsgCtx.getSequence();
		String sequenceId = sequence.getIdentifier().getIdentifier();
		long msgNo = sequence.getMessageNumber();
		boolean lastMessage = sequence.getLastMessage();
		
		// Check that both the Sequence header and message body have been secured properly
		RMDBeanMgr mgr = storageManager.getRMDBeanMgr();
		RMDBean bean = mgr.retrieve(sequenceId);
		
		//check the security credentials
		SandeshaUtil.assertProofOfPossession(bean, msgCtx, msgCtx.getEnvelope().getHeader().
				getFirstChildWithName(new QName(rmMsgCtx.getRMNamespaceValue(), Sandesha2Constants.WSRM_COMMON.SEQUENCE)));
		SandeshaUtil.assertProofOfPossession(bean, msgCtx, msgCtx.getEnvelope().getBody());
		
		
		// Store the inbound sequence id, number and lastMessage onto the operation context
		OperationContext opCtx = msgCtx.getOperationContext();
		if(opCtx != null) {
			opCtx.setProperty(Sandesha2Constants.MessageContextProperties.INBOUND_SEQUENCE_ID, sequenceId);
			opCtx.setProperty(Sandesha2Constants.MessageContextProperties.INBOUND_MESSAGE_NUMBER, new Long(msgNo));
			if(lastMessage) opCtx.setProperty(Sandesha2Constants.MessageContextProperties.INBOUND_LAST_MESSAGE, Boolean.TRUE);
		}
		
		// setting acked msg no range
		ConfigurationContext configCtx = msgCtx.getConfigurationContext();
		if (configCtx == null) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.configContextNotSet);
			log.debug(message);
			throw new SandeshaException(message);
		}

		if(bean == null){
			if (FaultManager.checkForUnknownSequence(rmMsgCtx, sequenceId, storageManager, false)) {
				if (log.isDebugEnabled())
					log.debug("Exit: SequenceProcessor::processReliableMessage, Unknown sequence");
				return InvocationResponse.ABORT;
			}
		}

		// throwing a fault if the sequence is terminated
		if (FaultManager.checkForSequenceTerminated(rmMsgCtx, sequenceId, bean, false)) {
			if (log.isDebugEnabled())
				log.debug("Exit: SequenceProcessor::processReliableMessage, Sequence terminated");
			return InvocationResponse.ABORT;
		}
		
		// throwing a fault if the sequence is closed.
		if (FaultManager.checkForSequenceClosed(rmMsgCtx, sequenceId, bean, false)) {
			if (log.isDebugEnabled())
				log.debug("Exit: SequenceProcessor::processReliableMessage, Sequence closed");
			return InvocationResponse.ABORT;
		}
		FaultManager.checkForLastMsgNumberExceeded(rmMsgCtx, storageManager);
		
		if (FaultManager.checkForMessageRolledOver(rmMsgCtx, sequenceId, msgNo, bean)) {
			
			if (log.isDebugEnabled())
				log.debug("Exit: SequenceProcessor::processReliableMessage, Message rolled over " + msgNo);
			
			return InvocationResponse.ABORT;
		}

		// updating the last activated time of the sequence.
		bean.setLastActivatedTime(System.currentTimeMillis());
		
		if (lastMessage) {
			//setting this as the LastMessage number
			bean.setLastInMessageId(msgCtx.getMessageID());
		}
		
		EndpointReference replyTo = rmMsgCtx.getReplyTo();
		if (log.isDebugEnabled())
			log.debug("SequenceProcessor::processReliableMessage replyTo = " + replyTo);
		
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
		
		String specVersion = rmMsgCtx.getRMSpecVersion();
		if ((SandeshaUtil.isDuplicateInOnlyMessage(msgCtx)
						||
					SandeshaUtil.isDuplicateInOutMessage(msgCtx))
				&& (Sandesha2Constants.QOS.InvocationType.DEFAULT_INVOCATION_TYPE == Sandesha2Constants.QOS.InvocationType.EXACTLY_ONCE)) {
			
			// this is a duplicate message and the invocation type is EXACTLY_ONCE. We try to return
			// ack messages at this point, as if someone is sending duplicates then they may have
			// missed earlier acks. We also have special processing for sync 2-way when no make connection is 
			// in use
			if(replyTo==null || replyTo.isWSAddressingAnonymous()) {

			  SenderBeanMgr senderBeanMgr = storageManager.getSenderBeanMgr();
			  SenderBean findSenderBean = new SenderBean ();
			  
			  if (rmMsgCtx.getMessageType()==Sandesha2Constants.MessageTypes.LAST_MESSAGE)
				  findSenderBean.setMessageType(Sandesha2Constants.MessageTypes.LAST_MESSAGE);
			  else
				  findSenderBean.setMessageType(Sandesha2Constants.MessageTypes.APPLICATION);
			  
			  findSenderBean.setInboundSequenceId(sequence.getIdentifier().getIdentifier());
			  findSenderBean.setInboundMessageNumber(sequence.getMessageNumber());
			  findSenderBean.setSend(true);
		
			  SenderBean replyMessageBean = senderBeanMgr.findUnique(findSenderBean);
			    
			  // this is effectively a poll for the replyMessage, so re-use the logic in the MakeConnection
			  // processor. This will use this thread to re-send the reply, writing it into the transport.
			  // As the reply is now written we do not want to continue processing, or suspend, so we abort.
			  if(replyMessageBean != null) {
			  	if(log.isDebugEnabled()) log.debug("Found matching reply for replayed message");
			   	MakeConnectionProcessor.replyToPoll(rmMsgCtx, replyMessageBean, storageManager, false, null, transaction);
					result = InvocationResponse.ABORT;
					if (log.isDebugEnabled())
						log.debug("Exit: SequenceProcessor::processReliableMessage, replayed message: " + result);
					return result;
			  }
		  }
			
			EndpointReference acksTo = bean.getAcksToEndpointReference();
			
			// Send an Ack if needed.
			//We are not sending acks for duplicate messages in the anon InOut case.
			//If a standalone ack get sent before the actualy message (I.e. before the original msg get
			//replied), the client may take this as a InOnly message and may avoid looking for the application
			//response if using replay.
			//Therefore we only send acks back in the anon InOnly case.
			int msgExchangePattern = rmMsgCtx.getMessageContext().getAxisOperation().getAxisSpecificMEPConstant();
			if (log.isDebugEnabled())
				log.debug("SequenceProcessor:: mep= " + msgExchangePattern);	
			if (WSDLConstants.MEP_CONSTANT_IN_ONLY ==  msgExchangePattern && 
					(replyTo==null || replyTo.getAddress()==null || replyTo.isWSAddressingAnonymous() || replyTo.hasNoneAddress())) {
				sendAckIfNeeded(bean, sequenceId, rmMsgCtx, storageManager, true, acksTo.hasAnonymousAddress());	
			}
			
			result = InvocationResponse.ABORT;
			if (log.isDebugEnabled())
				log.debug("Exit: SequenceProcessor::processReliableMessage, dropping duplicate: " + result);
			return result;
		}
		
		// If the message is a reply to an outbound message then we can update the RMSBean that
		// matches.
		EndpointReference toEPR = msgCtx.getTo();
		if(toEPR == null || toEPR.hasAnonymousAddress()) {
			RMSBean outBean = null;

			// Look for the correct outbound sequence by checking the anon uuid (if there is one)
			String toAddress = (toEPR == null) ? null : toEPR.getAddress();
			if(SandeshaUtil.isWSRMAnonymous(toAddress)) {
				RMSBean finderBean = new RMSBean();
				finderBean.setAnonymousUUID(toAddress);
				outBean = storageManager.getRMSBeanMgr().findUnique(finderBean);
			}
			
			// Fall back to the sequence that may have been offered at sequence creation time
			if(outBean == null) {
				String outboundSequence = bean.getOutboundInternalSequence();
				if(outboundSequence != null) {
					outBean = SandeshaUtil.getRMSBeanFromInternalSequenceId(storageManager, outboundSequence);
				}
			}
			
			// Update the reply count
			if(outBean != null && outBean.getExpectedReplies() > 0 ) {
				outBean.setExpectedReplies(outBean.getExpectedReplies() - 1);
				RMSBeanMgr outMgr = storageManager.getRMSBeanMgr();
				outMgr.update(outBean);
			}
		}
		
		// Set the last activated time
		bean.setLastActivatedTime(System.currentTimeMillis());
		
		// Update the RMD bean
		mgr.update(bean);
		
		// If we are doing sync 2-way over WSRM 1.0, then we may just have received one of
		// the reply messages that we were looking for. If so we can remove the matching sender bean.
		int mep = msgCtx.getAxisOperation().getAxisSpecificMEPConstant();
		if(specVersion!=null && specVersion.equals(Sandesha2Constants.SPEC_VERSIONS.v1_0) &&
				mep == WSDLConstants.MEP_CONSTANT_OUT_IN) {
			RelatesTo relatesTo = msgCtx.getRelatesTo();
			if(relatesTo != null) {
				String messageId = relatesTo.getValue();
				SenderBean sender = storageManager.getSenderBeanMgr().retrieve(messageId);
				if(sender != null) {
					if(log.isDebugEnabled()) log.debug("Deleting sender for sync-2-way message");
					
					storageManager.removeMessageContext(sender.getMessageContextRefKey());
					
					//this causes the request to be deleted even without an ack.
					storageManager.getSenderBeanMgr().delete(messageId);
					
					// Try and terminate the corresponding outbound sequence
					RMSBean rmsBean = SandeshaUtil.getRMSBeanFromSequenceId(storageManager, sender.getSequenceID());
					TerminateManager.checkAndTerminate(rmMsgCtx.getConfigurationContext(), storageManager, rmsBean);
				}
			}
		}

		//setting properties for the messageContext
		rmMsgCtx.setProperty(Sandesha2Constants.MessageContextProperties.SEQUENCE_ID,sequenceId);
		rmMsgCtx.setProperty(Sandesha2Constants.MessageContextProperties.MESSAGE_NUMBER,new Long (msgNo));
		
		// We only create an ack message if:
		// - We have anonymous acks, and the backchannel is free
		// - We have async acks
		boolean backchannelFree = (replyTo != null && !replyTo.hasAnonymousAddress()) ||
									WSDLConstants.MEP_CONSTANT_IN_ONLY == mep;
		
		boolean sendAck = false;
		
		// Need to special case 2005/02 LastMessage messages for replay model.
		boolean lastMessageMessage = lastMessage && (rmMsgCtx.getMessageType()==Sandesha2Constants.MessageTypes.LAST_MESSAGE) && bean.getOutboundInternalSequence()!=null;
		
		boolean ackBackChannel = SpecSpecificConstants.sendAckInBackChannel (rmMsgCtx.getMessageType());
		// If we are processing an inOnly message we must ack the back channel otherwise the connection stays open
		if (!ackBackChannel && mep == WSDLConstants.MEP_CONSTANT_IN_ONLY) ackBackChannel = true;
		EndpointReference acksTo = bean.getAcksToEndpointReference();
		if (acksTo.hasAnonymousAddress() && backchannelFree && ackBackChannel && !lastMessageMessage) {
			boolean responseWritten = TransportUtils.isResponseWritten(msgCtx);
			if (!responseWritten) {				
				sendAck = true;
			}
		} else if (!acksTo.hasAnonymousAddress()) {
			SandeshaPolicyBean policyBean = SandeshaUtil.getPropertyBean (msgCtx.getAxisOperation());
			long ackInterval = policyBean.getAcknowledgementInterval();
			
			((Sender)storageManager.getSender()).scheduleAddressableAcknowledgement(sequenceId, ackInterval, rmMsgCtx);
			
			// If the MEP doesn't need the backchannel, and nor do we, we should signal it so that it
			// can close off as soon as possible.
			if (backchannelFree) {
				TransportUtils.setResponseWritten(msgCtx, false);

				RequestResponseTransport t = null;
				t = (RequestResponseTransport) rmMsgCtx.getProperty(RequestResponseTransport.TRANSPORT_CONTROL);
				if(t != null && RequestResponseTransportStatus.WAITING.equals(t.getStatus())) {
					t.acknowledgeMessage(msgCtx);
				}
			}
		}
		
		// If this message matches the WSRM 1.0 pattern for an empty last message (e.g.
		// the sender wanted to signal the last message, but didn't have an application
		// message to send) then we direct it to the RMMessageReceiver.
		//This is not done when LastMsg is a response - it is sent throuth the normal response flow.
		if((Sandesha2Constants.SPEC_2005_02.Actions.ACTION_LAST_MESSAGE.equals(msgCtx.getWSAAction()) ||
		   Sandesha2Constants.SPEC_2005_02.Actions.SOAP_ACTION_LAST_MESSAGE.equals(msgCtx.getSoapAction()))) 
		{
			if (rmMsgCtx.getRelatesTo()==null) {
				if (log.isDebugEnabled())
					log.debug("SequenceProcessor::processReliableMessage, got WSRM 1.0 lastmessage");
				msgCtx.getAxisOperation().setMessageReceiver(new RMMessageReceiver ());
			}
		}
		
		// If the storage manager is implementing inOrder, or using transactional delivery
		// then we should hand the message over to the invoker thread. If not, we can invoke
		// it directly ourselves.
		InvokerWorker worker = null;
		if (SandeshaUtil.isInOrder(msgCtx)) {
			String key = SandeshaUtil.getUUID(); // key to store the message.
			InvokerBean invokerBean = new InvokerBean(key, msgNo, sequenceId);
			ContextManager contextMgr = SandeshaUtil.getContextManager(configCtx);

			if(contextMgr != null) invokerBean.setContext(contextMgr.storeContext());

			boolean wasAdded = storageManager.getInvokerBeanMgr().insert(invokerBean);

			// This will avoid performing application processing more than once.
			rmMsgCtx.setProperty(Sandesha2Constants.APPLICATION_PROCESSING_DONE, "true");
			
			// Whatever the MEP, we stop processing here and the invoker will do the real work. As we
			// are taking responsibility for the message we need to return SUSPEND
			result = InvocationResponse.SUSPEND;
            
			if (wasAdded) {
				storageManager.storeMessageContext(key, msgCtx);
				// We can invoke the message immediately, if this is the next message to invoke,
				// and we don't have a user transaction in play.
				if(bean.getNextMsgNoToProcess() == msgNo && !storageManager.hasUserTransaction(msgCtx)) {
					String workId = sequenceId;
					ConfigurationContext context = msgCtx.getConfigurationContext();
					
					worker = new InvokerWorker(context, invokerBean);
					worker.setWorkId(workId);
					
					// Actually take the lock
					worker.getLock().addWork(workId, worker);
				}
			} else {
				// Abort this message immediately as this message has already been added
				sendAck = false;
				result = InvocationResponse.ABORT;
				RequestResponseTransport t = null;
				t = (RequestResponseTransport) rmMsgCtx.getProperty(RequestResponseTransport.TRANSPORT_CONTROL);

				// Tell the transport that there will be no response message
				if(t != null && RequestResponseTransportStatus.WAITING.equals(t.getStatus())) {
					TransportUtils.setResponseWritten(msgCtx, false);
					t.acknowledgeMessage(msgCtx);
				}
			}
		}

		if (transaction != null && transaction.isActive()) 
			transaction.commit();
		
		if(worker != null) {
			try {
				worker.run();
			} catch(Exception e)  {
				log.error("Caught exception running InvokerWorker", e);
			}
		}

		if (sendAck) {
			try {
				transaction = storageManager.getTransaction();
				
				RMMsgContext ackRMMsgContext = AcknowledgementManager.generateAckMessage(rmMsgCtx, bean, sequenceId, storageManager,true);
				AcknowledgementManager.sendAckNow(ackRMMsgContext);
				TransportUtils.setResponseWritten(msgCtx, true);
				RequestResponseTransport t = 
					(RequestResponseTransport) rmMsgCtx.getProperty(RequestResponseTransport.TRANSPORT_CONTROL);
				
				// Tell the transport that we have finished with the message as the response should have been
				// written
				if(t != null && RequestResponseTransportStatus.WAITING.equals(t.getStatus())) {
					t.signalResponseReady();
				}

				if (transaction != null && transaction.isActive()) transaction.commit();
				transaction = null;
			
			} finally {
				if (transaction != null && transaction.isActive()) transaction.rollback();
			}
		}
		
		if (log.isDebugEnabled())
			log.debug("Exit: SequenceProcessor::processReliableMessage " + result);
		
		return result;
	}


	private void sendAckIfNeeded(RMDBean rmdBean, String sequenceId, RMMsgContext rmMsgCtx, 
			StorageManager storageManager, boolean serverSide, boolean anonymousAcksTo)
					throws AxisFault {

		if (log.isDebugEnabled())
			log.debug("Enter: SequenceProcessor::sendAckIfNeeded " + sequenceId);

			RMMsgContext ackRMMsgCtx = AcknowledgementManager.generateAckMessage(
					rmMsgCtx, rmdBean, sequenceId, storageManager, serverSide);

			if (anonymousAcksTo) {
				AcknowledgementManager.sendAckNow(ackRMMsgCtx);
				TransportUtils.setResponseWritten(rmMsgCtx.getMessageContext(), true);
			} else {				
				long ackInterval = SandeshaUtil.getPropertyBean(
						rmMsgCtx.getMessageContext().getAxisService())
						.getAcknowledgementInterval();
				long timeToSend = System.currentTimeMillis() + ackInterval;
				AcknowledgementManager.addAckBeanEntry(ackRMMsgCtx, sequenceId, timeToSend, storageManager);
			}			

		if (log.isDebugEnabled())
			log.debug("Exit: SequenceProcessor::sendAckIfNeeded");
	}

}
