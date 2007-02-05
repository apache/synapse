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

import java.util.Iterator;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.context.OperationContextFactory;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.wsdl.WSDLConstants;
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
import org.apache.sandesha2.storage.beans.RMDBean;
import org.apache.sandesha2.storage.beans.RMSBean;
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

	public boolean processInMessage(RMMsgContext terminateSeqRMMsg) throws AxisFault {

		if (log.isDebugEnabled())
			log.debug("Enter: TerminateSeqMsgProcessor::processInMessage");

		MessageContext terminateSeqMsg = terminateSeqRMMsg.getMessageContext();

		// Processing the terminate message
		// TODO Add terminate sequence message logic.
		TerminateSequence terminateSequence = (TerminateSequence) terminateSeqRMMsg
				.getMessagePart(Sandesha2Constants.MessageParts.TERMINATE_SEQ);
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
		
		String sequencePropertyKey = SandeshaUtil.getSequencePropertyKey(terminateSeqRMMsg);

		ConfigurationContext context = terminateSeqMsg.getConfigurationContext();
		StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(context,context.getAxisConfiguration());
		
		// Check that the sender of this TerminateSequence holds the correct token
		RMDBean rmdBean = SandeshaUtil.getRMDBeanFromSequenceId(storageManager, sequenceId);
		if(rmdBean != null && rmdBean.getSecurityTokenData() != null) {
			SecurityManager secManager = SandeshaUtil.getSecurityManager(context);
			OMElement body = terminateSeqRMMsg.getSOAPEnvelope().getBody();
			SecurityToken token = secManager.recoverSecurityToken(rmdBean.getSecurityTokenData());
			secManager.checkProofOfPossession(token, body, terminateSeqRMMsg.getMessageContext());
		}

		if (FaultManager.checkForUnknownSequence(terminateSeqRMMsg, sequenceId, storageManager)) {
			if (log.isDebugEnabled())
				log.debug("Exit: TerminateSeqMsgProcessor::processInMessage, unknown sequence");
			return false;
		}

		// add the terminate sequence response if required.
		RMMsgContext terminateSequenceResponse = null;
		if (SpecSpecificConstants.isTerminateSequenceResponseRequired(terminateSeqRMMsg.getRMSpecVersion()))
			terminateSequenceResponse = getTerminateSequenceResponse(terminateSeqRMMsg, rmdBean, sequencePropertyKey, sequenceId, storageManager);

		setUpHighestMsgNumbers(context, storageManager,sequencePropertyKey, sequenceId, terminateSeqRMMsg);
		
		
		
		boolean inOrderInvocation = SandeshaUtil.getDefaultPropertyBean(context.getAxisConfiguration()).isInOrder();
		
		
		//if the invocation is inOrder and if this is RM 1.1 there is a posibility of all the messages having eleady being invoked.
		//In this case we should do the full termination.
		
		boolean doFullTermination = false;
		
		if (inOrderInvocation) {

			long highestMsgNo = rmdBean.getHighestInMessageNumber();
			long nextMsgToProcess = rmdBean.getNextMsgNoToProcess();
			
			if (nextMsgToProcess>highestMsgNo) {
				//all the messages have been invoked, u can do the full termination
				doFullTermination = true;
			}
		} else {
			//for not-inorder case, always do the full termination.
			doFullTermination = true;
		}
		
		if (doFullTermination) {
			TerminateManager.cleanReceivingSideAfterInvocation(context, sequencePropertyKey, sequenceId, storageManager);
			TerminateManager.cleanReceivingSideOnTerminateMessage(context, sequencePropertyKey, sequenceId, storageManager);
		} else
			TerminateManager.cleanReceivingSideOnTerminateMessage(context, sequencePropertyKey, sequenceId, storageManager);
		

		

		rmdBean.setTerminated(true);		
		rmdBean.setLastActivatedTime(System.currentTimeMillis());
		storageManager.getRMDBeanMgr().update(rmdBean);


		//sending the terminate sequence response
		if (terminateSequenceResponse != null) {
			
			MessageContext outMessage = terminateSequenceResponse.getMessageContext();
			EndpointReference toEPR = outMessage.getTo();
			
			AxisEngine engine = new AxisEngine(terminateSeqMsg
					.getConfigurationContext());
						
			outMessage.setServerSide(true);
						
			engine.send(outMessage);

			if (toEPR.hasAnonymousAddress()) {
				terminateSeqMsg.getOperationContext().setProperty(
						org.apache.axis2.Constants.RESPONSE_WRITTEN, "true");
			} else {
				terminateSeqMsg.getOperationContext().setProperty(
						org.apache.axis2.Constants.RESPONSE_WRITTEN, "false");
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
					message.setProperty(MessageContext.TRANSPORT_OUT,
							terminateSeqMsg
									.getProperty(MessageContext.TRANSPORT_OUT));
					
					terminateSeqMsg.getOperationContext().setProperty(
							org.apache.axis2.Constants.RESPONSE_WRITTEN, "true");
					AxisEngine engine = new AxisEngine(context);
					engine.send(message);
					
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
			String requestSidesequencePropertyKey, String sequenceId, RMMsgContext terminateRMMsg) throws SandeshaException {

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
					if (!rmsBean.isTerminateAdded())
						TerminateManager.addTerminateSequenceMessage(terminateRMMsg, rmsBean.getInternalSequenceID(), outgoingSequnceID , storageManager);
				}
			}
		} catch (AxisFault e) {
			throw new SandeshaException(e);
		}
		if (log.isDebugEnabled())
			log.debug("Exit: TerminateSeqMsgProcessor::setUpHighestMsgNumbers");
	}

	private RMMsgContext getTerminateSequenceResponse(RMMsgContext terminateSeqRMMsg, RMDBean rmdBean, String sequencePropertyKey,String sequenceId,
			StorageManager storageManager) throws AxisFault {

		if (log.isDebugEnabled())
			log.debug("Enter: TerminateSeqMsgProcessor::addTerminateSequenceResponse, " + sequenceId);

		RMMsgContext terminateSeqResponseRMMsg = RMMsgCreator.createTerminateSeqResponseMsg(terminateSeqRMMsg, rmdBean);
		MessageContext outMessage = terminateSeqResponseRMMsg.getMessageContext();

		RMMsgContext ackRMMessage = AcknowledgementManager.generateAckMessage(terminateSeqRMMsg, 
				sequenceId,	storageManager, false, true);
		
		Iterator iter = ackRMMessage.getMessageParts(Sandesha2Constants.MessageParts.SEQ_ACKNOWLEDGEMENT);
		
		if (iter.hasNext()) {
			SequenceAcknowledgement seqAck = (SequenceAcknowledgement) iter.next();
			if (seqAck==null) {
				String message = "No SequenceAcknowledgement part is present";
				throw new SandeshaException (message);
			}
		
			terminateSeqResponseRMMsg.setMessagePart(Sandesha2Constants.MessageParts.SEQ_ACKNOWLEDGEMENT, seqAck);
		} else {
			//TODO 
		}
		
		terminateSeqResponseRMMsg.addSOAPEnvelope();

		terminateSeqResponseRMMsg.setFlow(MessageContext.OUT_FLOW);
		terminateSeqResponseRMMsg.setProperty(Sandesha2Constants.APPLICATION_PROCESSING_DONE, "true");

		outMessage.setResponseWritten(true);
		
		if (log.isDebugEnabled())
			log.debug("Exit: TerminateSeqMsgProcessor::addTerminateSequenceResponse");

		return terminateSeqResponseRMMsg;
	}

	public boolean processOutMessage(RMMsgContext rmMsgCtx) throws AxisFault {

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
		OperationContext opcontext = OperationContextFactory
				.createOperationContext(
						WSDLConstants.MEP_CONSTANT_OUT_IN, terminateOp);
		opcontext.setParent(getMsgContext().getServiceContext());
		getConfigurationContext().registerOperationContext(rmMsgCtx.getMessageId(),	opcontext);

		getMsgContext().setOperationContext(opcontext);
		getMsgContext().setAxisOperation(terminateOp);

		TerminateSequence terminateSequencePart = (TerminateSequence) rmMsgCtx
				.getMessagePart(Sandesha2Constants.MessageParts.TERMINATE_SEQ);
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
		sendOutgoingMessage(rmMsgCtx, Sandesha2Constants.MessageTypes.TERMINATE_SEQ, Sandesha2Constants.TERMINATE_DELAY);		

		// Pause the message context
		rmMsgCtx.pause();

		if (log.isDebugEnabled())
			log.debug("Exit: TerminateSeqMsgProcessor::processOutMessage " + Boolean.TRUE);
		return true;
	}
}
