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
import org.apache.axis2.wsdl.WSDLConstants.WSDL20_2004Constants;
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
import org.apache.sandesha2.storage.beanmanagers.SequencePropertyBeanMgr;
import org.apache.sandesha2.storage.beans.RMDBean;
import org.apache.sandesha2.storage.beans.RMSBean;
import org.apache.sandesha2.storage.beans.SequencePropertyBean;
import org.apache.sandesha2.util.AcknowledgementManager;
import org.apache.sandesha2.util.FaultManager;
import org.apache.sandesha2.util.RMMsgCreator;
import org.apache.sandesha2.util.SandeshaUtil;
import org.apache.sandesha2.util.SequenceManager;
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
		SequencePropertyBeanMgr sequencePropertyBeanMgr = storageManager.getSequencePropertyBeanMgr();
		
		// Check that the sender of this TerminateSequence holds the correct token
		SequencePropertyBean tokenBean = sequencePropertyBeanMgr.retrieve(sequencePropertyKey, Sandesha2Constants.SequenceProperties.SECURITY_TOKEN);
		if(tokenBean != null) {
			SecurityManager secManager = SandeshaUtil.getSecurityManager(context);
			OMElement body = terminateSeqRMMsg.getSOAPEnvelope().getBody();
			SecurityToken token = secManager.recoverSecurityToken(tokenBean.getValue());
			secManager.checkProofOfPossession(token, body, terminateSeqRMMsg.getMessageContext());
		}

		FaultManager.checkForUnknownSequence(terminateSeqRMMsg, sequenceId, storageManager);

		SequencePropertyBean terminateReceivedBean = new SequencePropertyBean();
		terminateReceivedBean.setSequencePropertyKey(sequencePropertyKey);
		terminateReceivedBean.setName(Sandesha2Constants.SequenceProperties.TERMINATE_RECEIVED);
		terminateReceivedBean.setValue("true");

		sequencePropertyBeanMgr.insert(terminateReceivedBean);

		// add the terminate sequence response if required.
		RMMsgContext terminateSequenceResponse = null;
		if (SpecSpecificConstants.isTerminateSequenceResponseRequired(terminateSeqRMMsg.getRMSpecVersion()))
			terminateSequenceResponse = getTerminateSequenceResponse(terminateSeqRMMsg, sequencePropertyKey, sequenceId, storageManager);

		setUpHighestMsgNumbers(context, storageManager,sequencePropertyKey, sequenceId, terminateSeqRMMsg);

		RMDBean rmdBean = SandeshaUtil.getRMDBeanFromSequenceId(storageManager, sequenceId);
		rmdBean.setTerminated(true);		
		storageManager.getRMDBeanMgr().update(rmdBean);

		TerminateManager.cleanReceivingSideOnTerminateMessage(context, sequencePropertyKey, sequenceId, storageManager);

		SequenceManager.updateLastActivatedTime(sequencePropertyKey, storageManager);

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

		SequencePropertyBeanMgr seqPropMgr = storageManager.getSequencePropertyBeanMgr();

		RMDBeanMgr mgr = storageManager.getRMDBeanMgr();
		RMDBean bean = mgr.retrieve(sequenceId);

		long highestInMsgNo = bean.getHighestInMessageNumber();

		// following will be valid only for the server side, since the obtained
		// int. seq ID is only valid there.
		String responseSideInternalSequenceId = SandeshaUtil.getOutgoingSideInternalSequenceID(sequenceId);
		
		//sequencePropertyKey is equal to the internalSequenceId for the outgoing sequence.
		String responseSideSequencePropertyKey = responseSideInternalSequenceId;
		
		long highestOutMsgNo = 0;
		try {
			boolean addResponseSideTerminate = false;
			if (highestInMsgNo == 0) {
				addResponseSideTerminate = false;
			} else {
				// Mark up the highest inbound message as if it had the last message flag on it.
				// 
				String inMsgId = bean.getHighestInMessageId();
				SequencePropertyBean lastInMsgBean = new SequencePropertyBean(requestSidesequencePropertyKey,
						Sandesha2Constants.SequenceProperties.LAST_IN_MSG_ID, bean.getHighestInMessageId());
				seqPropMgr.insert(lastInMsgBean);
				
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
			String outgoingSequnceID = SandeshaUtil.getSequenceIDFromInternalSequenceID(responseSideSequencePropertyKey, storageManager); 

			if (addResponseSideTerminate && highestOutMsgNo > 0 && responseSideSequencePropertyKey != null
					&& outgoingSequnceID != null) {
				boolean allAcked = SandeshaUtil.isAllMsgsAckedUpto(highestOutMsgNo, responseSideSequencePropertyKey,
						storageManager);

				if (allAcked)
				{
					String internalSequenceID = SandeshaUtil.getSequenceProperty(outgoingSequnceID,
							Sandesha2Constants.SequenceProperties.INTERNAL_SEQUENCE_ID, storageManager);

					TerminateManager.addTerminateSequenceMessage(terminateRMMsg, internalSequenceID, outgoingSequnceID,
							responseSideSequencePropertyKey, storageManager);
				}
			}
		} catch (AxisFault e) {
			throw new SandeshaException(e);
		}
		if (log.isDebugEnabled())
			log.debug("Exit: TerminateSeqMsgProcessor::setUpHighestMsgNumbers");
	}

	private RMMsgContext getTerminateSequenceResponse(RMMsgContext terminateSeqRMMsg, String sequencePropertyKey,String sequenceId,
			StorageManager storageManager) throws AxisFault {

		if (log.isDebugEnabled())
			log.debug("Enter: TerminateSeqMsgProcessor::addTerminateSequenceResponse, " + sequenceId);

		RMMsgContext terminateSeqResponseRMMsg = RMMsgCreator.createTerminateSeqResponseMsg(terminateSeqRMMsg);
		MessageContext outMessage = terminateSeqResponseRMMsg.getMessageContext();

		RMMsgContext ackRMMessage = AcknowledgementManager.generateAckMessage(terminateSeqRMMsg, sequencePropertyKey, 
				sequenceId,	storageManager);
		
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
						WSDL20_2004Constants.MEP_CONSTANT_OUT_IN, terminateOp);
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
