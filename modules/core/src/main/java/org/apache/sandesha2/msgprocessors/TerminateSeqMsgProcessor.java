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

import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.context.OperationContextFactory;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.transport.TransportUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.RMMsgContext;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.Transaction;
import org.apache.sandesha2.storage.beanmanagers.RMDBeanMgr;
import org.apache.sandesha2.storage.beans.RMDBean;
import org.apache.sandesha2.storage.beans.RMSBean;
import org.apache.sandesha2.storage.beans.RMSequenceBean;
import org.apache.sandesha2.storage.beans.SenderBean;
import org.apache.sandesha2.util.AcknowledgementManager;
import org.apache.sandesha2.util.FaultManager;
import org.apache.sandesha2.util.MessageRetransmissionAdjuster;
import org.apache.sandesha2.util.MsgInitializer;
import org.apache.sandesha2.util.RMMsgCreator;
import org.apache.sandesha2.util.SandeshaUtil;
import org.apache.sandesha2.util.SpecSpecificConstants;
import org.apache.sandesha2.util.TerminateManager;
import org.apache.sandesha2.util.WSRMMessageSender;
import org.apache.sandesha2.wsrm.SequenceAcknowledgement;
import org.apache.sandesha2.wsrm.TerminateSequence;

/**
 * Responsible for processing an incoming Terminate Sequence message.
 */

public class TerminateSeqMsgProcessor extends WSRMMessageSender implements MsgProcessor {

	private static final Log log = LogFactory.getLog(TerminateSeqMsgProcessor.class);

	public boolean processInMessage(RMMsgContext terminateSeqRMMsg, Transaction transaction) throws AxisFault {

		if (log.isDebugEnabled())
			log.debug("Enter: TerminateSeqMsgProcessor::processInMessage");

		MessageContext terminateSeqMsg = terminateSeqRMMsg.getMessageContext();

		// Processing the terminate message
		// TODO Add terminate sequence message logic.
		TerminateSequence terminateSequence = terminateSeqRMMsg.getTerminateSequence();
		if (terminateSequence == null) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.noTerminateSeqPart);
			log.debug(message);
			throw new SandeshaException(message);
		}

		String sequenceId = terminateSequence.getIdentifier().getIdentifier();
		if (sequenceId == null || "".equals(sequenceId)) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.invalidSequenceID, null);
			log.debug(message);
			throw new SandeshaException(message);
		}
		
		ConfigurationContext context = terminateSeqMsg.getConfigurationContext();
		StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(context,context.getAxisConfiguration());
		
		// Check that the sender of this TerminateSequence holds the correct token
		RMSequenceBean rmBean = SandeshaUtil.getRMDBeanFromSequenceId(storageManager, sequenceId);
		if(rmBean==null){
			rmBean = SandeshaUtil.getRMSBeanFromSequenceId(storageManager, sequenceId);
		}
		
		//check security credentials
		SandeshaUtil.assertProofOfPossession(rmBean, terminateSeqMsg, 
				terminateSeqMsg.getEnvelope().getBody());

		if (FaultManager.checkForUnknownSequence(terminateSeqRMMsg, sequenceId, storageManager, false)) {
			if (log.isDebugEnabled())
				log.debug("Exit: TerminateSeqMsgProcessor::processInMessage, unknown sequence");
			return false;
		}

		// add the terminate sequence response if required.
		RMMsgContext terminateSequenceResponse = null;
		if (SpecSpecificConstants.isTerminateSequenceResponseRequired(terminateSeqRMMsg.getRMSpecVersion()))
			terminateSequenceResponse = getTerminateSequenceResponse(terminateSeqRMMsg, rmBean, sequenceId, storageManager);

		setUpHighestMsgNumbers(context, storageManager, sequenceId, terminateSeqRMMsg);
		
		boolean inOrderInvocation = SandeshaUtil.getDefaultPropertyBean(context.getAxisConfiguration()).isInOrder();
		
		//if the invocation is inOrder and if this is RM 1.1 there is a posibility of all the messages having aleady being invoked.
		//In this case we should do the full termination.
		
		boolean doFullTermination = false;
		
		if (inOrderInvocation && rmBean instanceof RMDBean) {

			long highestMsgNo = ((RMDBean)rmBean).getHighestInMessageNumber();
			long nextMsgToProcess = ((RMDBean)rmBean).getNextMsgNoToProcess();
			
			if (nextMsgToProcess>highestMsgNo) {
				//all the messages have been invoked, u can do the full termination
				doFullTermination = true;
			}
		} else {
			//for not-inorder case, always do the full termination.
			doFullTermination = true;
		}
		
		if (doFullTermination) {
			TerminateManager.cleanReceivingSideAfterInvocation(sequenceId, storageManager);
			TerminateManager.cleanReceivingSideOnTerminateMessage(context, sequenceId, storageManager);
		} else
			TerminateManager.cleanReceivingSideOnTerminateMessage(context, sequenceId, storageManager);

		rmBean.setTerminated(true);		
		rmBean.setLastActivatedTime(System.currentTimeMillis());
		if(rmBean instanceof RMDBean){
			storageManager.getRMDBeanMgr().update((RMDBean)rmBean);
		}
		else{
			storageManager.getRMSBeanMgr().update((RMSBean)rmBean);
		}

		//sending the terminate sequence response
		if (terminateSequenceResponse != null) {
			//
			// As we have processed the input and prepared the response we can commit the
			// transaction now.
			if(transaction != null && transaction.isActive()) transaction.commit();
			
			MessageContext outMessage = terminateSequenceResponse.getMessageContext();
			EndpointReference toEPR = outMessage.getTo();
			
			outMessage.setServerSide(true);
						
			try {							
				AxisEngine.send(outMessage);
			} catch (AxisFault e) {
				if (log.isDebugEnabled())
					log.debug("Unable to send terminate sequence response", e);
				
				throw new SandeshaException(
						SandeshaMessageHelper.getMessage(SandeshaMessageKeys.couldNotSendTerminateResponse), e);
			}
			
			if (toEPR.hasAnonymousAddress()) {
				TransportUtils.setResponseWritten(terminateSeqMsg, true);
			}

		} else {
			//if RM 1.0 Anonymous scenario we will be trying to attache the TerminateSequence of the response side 
			//as the response message.
			
			String outgoingSideInternalSeqId = SandeshaUtil.getOutgoingSideInternalSequenceID(sequenceId);
			SenderBean senderFindBean = new SenderBean ();
			senderFindBean.setInternalSequenceID(outgoingSideInternalSeqId);
			senderFindBean.setMessageType(Sandesha2Constants.MessageTypes.TERMINATE_SEQ);
			senderFindBean.setSend(true);
			senderFindBean.setReSend(false);
			
			SenderBean outgoingSideTerminateBean = storageManager.getSenderBeanMgr().findUnique(senderFindBean);
			if (outgoingSideTerminateBean!=null) {
			
				EndpointReference toEPR = new EndpointReference (outgoingSideTerminateBean.getToAddress());
				if (toEPR.hasAnonymousAddress()) {
					String messageKey = outgoingSideTerminateBean
							.getMessageContextRefKey();
					MessageContext message = storageManager
							.retrieveMessageContext(messageKey, context);

					RMMsgContext rmMessage = MsgInitializer.initializeMessage(message);
					
					// attaching the this outgoing terminate message as the
					// response to the incoming terminate message.
					message.setTransportOut(terminateSeqMsg.getTransportOut());
					message.setProperty(MessageContext.TRANSPORT_OUT,terminateSeqMsg.getProperty(MessageContext.TRANSPORT_OUT));
					message.setProperty(Constants.OUT_TRANSPORT_INFO, terminateSeqMsg.getProperty(Constants.OUT_TRANSPORT_INFO));
	                
					try {							
						AxisEngine.send(message);
						TransportUtils.setResponseWritten(terminateSeqMsg, true);
					} catch (AxisFault e) {
						if (log.isDebugEnabled())
							log.debug("Unable to send terminate sequence response", e);
						
						throw new SandeshaException(
								SandeshaMessageHelper.getMessage(SandeshaMessageKeys.couldNotSendTerminateResponse), e);
					}
					
					// TODO - should this be here?
					MessageRetransmissionAdjuster.adjustRetransmittion(rmMessage, outgoingSideTerminateBean, context, storageManager);
				}
				
			}

		}
		
		terminateSeqMsg.pause();

		if (log.isDebugEnabled())
			log.debug("Exit: TerminateSeqMsgProcessor::processInMessage " + Boolean.TRUE);
		return true;
	}

	private void setUpHighestMsgNumbers(ConfigurationContext configCtx, StorageManager storageManager,
			String sequenceId, RMMsgContext terminateRMMsg) throws SandeshaException {

		if (log.isDebugEnabled())
			log.debug("Enter: TerminateSeqMsgProcessor::setUpHighestMsgNumbers, " + sequenceId);

		RMDBeanMgr mgr = storageManager.getRMDBeanMgr();
		RMDBean bean = mgr.retrieve(sequenceId);

		long highestInMsgNo = bean.getHighestInMessageNumber();

		// following will be valid only for the server side, since the obtained
		// int. seq ID is only valid there.
		String responseSideInternalSequenceId = SandeshaUtil.getOutgoingSideInternalSequenceID(sequenceId);
		
		
		long highestOutMsgNo = 0;
		try {
			boolean addResponseSideTerminate = false;
			if (highestInMsgNo == 0) {
				addResponseSideTerminate = false;
			} else {
				// Mark up the highest inbound message as if it had the last message flag on it.
				// 
				String inMsgId = bean.getHighestInMessageId();
				bean.setLastInMessageId(inMsgId);
				
				// Update the RMDBean
				storageManager.getRMDBeanMgr().update(bean);
				
				// If an outbound message has already gone out with that relatesTo, then we can terminate
				// right away.
				RMSBean rmsBean = SandeshaUtil.getRMSBeanFromInternalSequenceId(storageManager, responseSideInternalSequenceId);

				if(rmsBean != null) {
					String highestOutRelatesTo = rmsBean.getHighestOutRelatesTo();
					if (highestOutRelatesTo != null && highestOutRelatesTo.equals(inMsgId)) {
						highestOutMsgNo = rmsBean.getHighestOutMessageNumber();
						addResponseSideTerminate = true;
						
						// It is possible that the message has gone out, but not been acked yet. In that case
						// we can store the HIGHEST_OUT_MSG_NUMBER as the LAST_OUT_MESSAGE_NO, so that when the
						// ack arrives we will terminate the sequence
						rmsBean.setLastOutMessage(highestOutMsgNo);
						storageManager.getRMSBeanMgr().update(rmsBean);
					}
				}
			}

			// If all the out message have been acked, add the outgoing
			// terminate seq msg.
			String outgoingSequnceID = SandeshaUtil.getSequenceIDFromInternalSequenceID(responseSideInternalSequenceId, storageManager); 

			if (addResponseSideTerminate && highestOutMsgNo > 0 && responseSideInternalSequenceId != null
					&& outgoingSequnceID != null) {
				boolean allAcked = SandeshaUtil.isAllMsgsAckedUpto(highestOutMsgNo, responseSideInternalSequenceId, storageManager);

				if (allAcked)
				{
					RMSBean rmsBean = SandeshaUtil.getRMSBeanFromSequenceId(storageManager, outgoingSequnceID);
					if (!rmsBean.isTerminateAdded()) {
						TerminateManager.addTerminateSequenceMessage(terminateRMMsg, rmsBean.getInternalSequenceID(), outgoingSequnceID , storageManager);
						String referenceMsgKey = rmsBean.getReferenceMessageStoreKey();
						if (referenceMsgKey==null) {
							String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.referenceMessageNotSetForSequence,rmsBean.getSequenceID());
							throw new SandeshaException (message);
						}
						
						MessageContext referenceMessage = storageManager.retrieveMessageContext(referenceMsgKey, configCtx);
						
						if (referenceMessage==null) {
							String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.referencedMessageNotFound, rmsBean.getSequenceID());
							throw new SandeshaException (message);
						}
						
						//RMMsgContext referenceRMMsg = MsgInitializer.initializeMessage(referenceMessage);
									
					}
				}
			}
		} catch (AxisFault e) {
			throw new SandeshaException(e);
		}
		if (log.isDebugEnabled())
			log.debug("Exit: TerminateSeqMsgProcessor::setUpHighestMsgNumbers");
	}

	private RMMsgContext getTerminateSequenceResponse(RMMsgContext terminateSeqRMMsg, RMSequenceBean rmBean, String sequenceId,
			StorageManager storageManager) throws AxisFault {

		if (log.isDebugEnabled())
			log.debug("Enter: TerminateSeqMsgProcessor::addTerminateSequenceResponse, " + sequenceId);

		RMMsgContext terminateSeqResponseRMMsg = RMMsgCreator.createTerminateSeqResponseMsg(terminateSeqRMMsg, rmBean);
		MessageContext outMessage = terminateSeqResponseRMMsg.getMessageContext();
		
		if(rmBean instanceof RMDBean){
			RMMsgContext ackRMMessage = AcknowledgementManager.generateAckMessage(terminateSeqRMMsg, (RMDBean)rmBean, 
					sequenceId,	storageManager, true);
			
			// copy over the ack parts
			Iterator iter = ackRMMessage.getSequenceAcknowledgements();
			while (iter.hasNext()) {
				SequenceAcknowledgement seqAck = (SequenceAcknowledgement) iter.next();
				terminateSeqResponseRMMsg.addSequenceAcknowledgement(seqAck);
			}
		}		
		terminateSeqResponseRMMsg.addSOAPEnvelope();

		terminateSeqResponseRMMsg.setFlow(MessageContext.OUT_FLOW);
		terminateSeqResponseRMMsg.setProperty(Sandesha2Constants.APPLICATION_PROCESSING_DONE, "true");

		outMessage.setResponseWritten(true);
		
		if (log.isDebugEnabled())
			log.debug("Exit: TerminateSeqMsgProcessor::addTerminateSequenceResponse");

		return terminateSeqResponseRMMsg;
	}

	public boolean processOutMessage(RMMsgContext rmMsgCtx, Transaction transaction) throws AxisFault {

		if (log.isDebugEnabled())
			log.debug("Enter: TerminateSeqMsgProcessor::processOutMessage");

		// Get the parent processor to setup the out message
		setupOutMessage(rmMsgCtx);
		
		RMSBean rmsBean = SandeshaUtil.getRMSBeanFromInternalSequenceId(getStorageManager(), getInternalSequenceID());
		
		// Check if the sequence is already terminated (stored on the internal sequenceid)
		if (rmsBean.isTerminateAdded()) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.terminateAddedPreviously);
			log.debug(message);
			if (log.isDebugEnabled())
				log.debug("Exit: TerminateSeqMsgProcessor::processOutMessage, sequence previously terminated");
			return true;
		}

		AxisOperation terminateOp = SpecSpecificConstants.getWSRMOperation(
				Sandesha2Constants.MessageTypes.TERMINATE_SEQ,
				rmMsgCtx.getRMSpecVersion(),
				getMsgContext().getAxisService());
	
		OperationContext opcontext = OperationContextFactory.createOperationContext(terminateOp.getAxisSpecificMEPConstant(), terminateOp, getMsgContext().getServiceContext());
		opcontext.setParent(getMsgContext().getServiceContext());

		getConfigurationContext().registerOperationContext(rmMsgCtx.getMessageId(),	opcontext);

		getMsgContext().setOperationContext(opcontext);
		getMsgContext().setAxisOperation(terminateOp);

		TerminateSequence terminateSequencePart = rmMsgCtx.getTerminateSequence();
		terminateSequencePart.getIdentifier().setIndentifer(getOutSequenceID());

		rmMsgCtx.setWSAAction(SpecSpecificConstants.getTerminateSequenceAction(getRMVersion()));
		rmMsgCtx.setSOAPAction(SpecSpecificConstants.getTerminateSequenceSOAPAction(getRMVersion()));
		
		rmsBean.setTerminateAdded(true);

		// Update the RMSBean with the terminate added flag
		getStorageManager().getRMSBeanMgr().update(rmsBean);

		// Send the outgoing message
		// Set a retransmitter lastSentTime so that terminate will be send with
		// some delay.
		// Otherwise this get send before return of the current request (ack).
		// TODO: refine the terminate delay.		
		sendOutgoingMessage(rmMsgCtx, Sandesha2Constants.MessageTypes.TERMINATE_SEQ, Sandesha2Constants.TERMINATE_DELAY, transaction);		

		// Pause the message context
		rmMsgCtx.pause();

		if (log.isDebugEnabled())
			log.debug("Exit: TerminateSeqMsgProcessor::processOutMessage " + Boolean.TRUE);
		return true;
	}
}
