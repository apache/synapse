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
import org.apache.axis2.client.Options;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.MessageContextConstants;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.context.OperationContextFactory;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.util.Utils;
import org.apache.axis2.wsdl.WSDLConstants.WSDL20_2004Constants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.RMMsgContext;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.client.SandeshaClientConstants;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.security.SecurityManager;
import org.apache.sandesha2.security.SecurityToken;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.beanmanagers.SenderBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.SequencePropertyBeanMgr;
import org.apache.sandesha2.storage.beans.SenderBean;
import org.apache.sandesha2.storage.beans.SequencePropertyBean;
import org.apache.sandesha2.util.AcknowledgementManager;
import org.apache.sandesha2.util.FaultManager;
import org.apache.sandesha2.util.MsgInitializer;
import org.apache.sandesha2.util.RMMsgCreator;
import org.apache.sandesha2.util.SandeshaUtil;
import org.apache.sandesha2.util.SequenceManager;
import org.apache.sandesha2.util.SpecSpecificConstants;
import org.apache.sandesha2.util.TerminateManager;
import org.apache.sandesha2.wsrm.Sequence;
import org.apache.sandesha2.wsrm.SequenceAcknowledgement;
import org.apache.sandesha2.wsrm.TerminateSequence;

/**
 * Responsible for processing an incoming Terminate Sequence message.
 */

public class TerminateSeqMsgProcessor implements MsgProcessor {

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

		FaultManager faultManager = new FaultManager();
		SandeshaException fault = faultManager.checkForUnknownSequence(terminateSeqRMMsg, sequenceId,
				storageManager);
		if (fault != null) {
			throw fault;
		}


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

		TerminateManager.cleanReceivingSideOnTerminateMessage(context, sequencePropertyKey, sequenceId, storageManager);

		SequencePropertyBean terminatedBean = new SequencePropertyBean(sequencePropertyKey,
				Sandesha2Constants.SequenceProperties.SEQUENCE_TERMINATED, Sandesha2Constants.VALUE_TRUE);

		sequencePropertyBeanMgr.insert(terminatedBean);

		SequenceManager.updateLastActivatedTime(sequencePropertyKey, storageManager);

		//sending the terminate sequence response
		if (terminateSequenceResponse != null) {
			
			MessageContext outMessage = terminateSequenceResponse.getMessageContext();
			EndpointReference toEPR = outMessage.getTo();
			
			AxisEngine engine = new AxisEngine(terminateSeqMsg
					.getConfigurationContext());
			
			
			outMessage.setServerSide(true);
			
			
			engine.send(outMessage);

			String addressingNamespaceURI = SandeshaUtil
					.getSequenceProperty(
							sequencePropertyKey,
							Sandesha2Constants.SequenceProperties.ADDRESSING_NAMESPACE_VALUE,
							storageManager);

			String anonymousURI = SpecSpecificConstants
					.getAddressingAnonymousURI(addressingNamespaceURI);

			if (anonymousURI.equals(toEPR.getAddress())) {
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

		String highestImMsgNumberStr = SandeshaUtil.getSequenceProperty(requestSidesequencePropertyKey,
				Sandesha2Constants.SequenceProperties.HIGHEST_IN_MSG_NUMBER, storageManager);
		String highestImMsgKey = SandeshaUtil.getSequenceProperty(requestSidesequencePropertyKey,
				Sandesha2Constants.SequenceProperties.HIGHEST_IN_MSG_KEY, storageManager);

		long highestInMsgNo = 0;
		if (highestImMsgNumberStr != null) {
			if (highestImMsgKey == null)
				throw new SandeshaException(SandeshaMessageHelper.getMessage(
						SandeshaMessageKeys.highestMsgKeyNotStored, sequenceId));

			highestInMsgNo = Long.parseLong(highestImMsgNumberStr);
		}

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

				// setting the last in message property
				SequencePropertyBean lastInMsgBean = new SequencePropertyBean(requestSidesequencePropertyKey,
						Sandesha2Constants.SequenceProperties.LAST_IN_MESSAGE_NO, highestImMsgNumberStr);
				seqPropMgr.insert(lastInMsgBean);

				MessageContext highestInMsg = storageManager.retrieveMessageContext(highestImMsgKey, configCtx);

				// TODO get the out message in a storage friendly manner.
				MessageContext highestOutMessage = highestInMsg.getOperationContext().getMessageContext(
						OperationContextFactory.MESSAGE_LABEL_FAULT_VALUE);

				if (highestOutMessage == null || highestOutMessage.getEnvelope() == null)
					highestOutMessage = highestInMsg.getOperationContext().getMessageContext(
							OperationContextFactory.MESSAGE_LABEL_OUT_VALUE);

				if (highestOutMessage != null) {
					if (highestOutMessage.getEnvelope() == null)
						throw new SandeshaException(SandeshaMessageHelper
								.getMessage(SandeshaMessageKeys.outMsgHasNoEnvelope));

					RMMsgContext highestOutRMMsg = MsgInitializer.initializeMessage(highestOutMessage);
					Sequence seqPartOfOutMsg = (Sequence) highestOutRMMsg
							.getMessagePart(Sandesha2Constants.MessageParts.SEQUENCE);

					if (seqPartOfOutMsg != null) {

						// response message of the last in message can be
						// considered as the last out message.
						highestOutMsgNo = seqPartOfOutMsg.getMessageNumber().getMessageNumber();
						SequencePropertyBean highestOutMsgBean = new SequencePropertyBean(
								responseSideSequencePropertyKey ,
								Sandesha2Constants.SequenceProperties.LAST_OUT_MESSAGE_NO, new Long(highestOutMsgNo)
										.toString());

						seqPropMgr.insert(highestOutMsgBean);
						addResponseSideTerminate = true;
					}
				}
			}

			// If all the out message have been acked, add the outgoing
			// terminate seq msg.
			String outgoingSqunceID = SandeshaUtil.getSequenceProperty(responseSideSequencePropertyKey,
					Sandesha2Constants.SequenceProperties.OUT_SEQUENCE_ID, storageManager);
			if (addResponseSideTerminate && highestOutMsgNo > 0 && responseSideSequencePropertyKey != null
					&& outgoingSqunceID != null) {
				boolean allAcked = SandeshaUtil.isAllMsgsAckedUpto(highestOutMsgNo, responseSideSequencePropertyKey,
						storageManager);

				if (allAcked)
					TerminateManager.addTerminateSequenceMessage(terminateRMMsg, outgoingSqunceID,
							responseSideSequencePropertyKey, storageManager);
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

		MessageContext terminateSeqMsg = terminateSeqRMMsg.getMessageContext();

		MessageContext outMessage = null;

		try {
			outMessage = Utils.createOutMessageContext(terminateSeqMsg);
		} catch (AxisFault e1) {
			throw new SandeshaException(e1);
		}

		RMMsgContext terminateSeqResponseRMMsg = RMMsgCreator.createTerminateSeqResponseMsg(terminateSeqRMMsg,
				outMessage, storageManager);

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

		MessageContext msgContext = rmMsgCtx.getMessageContext();
		ConfigurationContext configurationContext = msgContext.getConfigurationContext();
		Options options = msgContext.getOptions();

		StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(configurationContext,
				configurationContext.getAxisConfiguration());

		SequencePropertyBeanMgr seqPropMgr = storageManager.getSequencePropertyBeanMgr();

		String toAddress = rmMsgCtx.getTo().getAddress();
		String sequenceKey = (String) options.getProperty(SandeshaClientConstants.SEQUENCE_KEY);
		String internalSeqenceID = SandeshaUtil.getInternalSequenceID(toAddress, sequenceKey);

		String outSequenceID = SandeshaUtil.getSequenceProperty(internalSeqenceID,
				Sandesha2Constants.SequenceProperties.OUT_SEQUENCE_ID, storageManager);
		if (outSequenceID == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.couldNotSendTerminateSeqNotFound, internalSeqenceID));

		// / Transaction addTerminateSeqTransaction =
		// storageManager.getTransaction();

		String terminated = SandeshaUtil.getSequenceProperty(outSequenceID,
				Sandesha2Constants.SequenceProperties.TERMINATE_ADDED, storageManager);

		AxisOperation terminateOp = SpecSpecificConstants.getWSRMOperation(
				Sandesha2Constants.MessageTypes.TERMINATE_SEQ,
				rmMsgCtx.getRMSpecVersion(),
				msgContext.getAxisService());
		OperationContext opcontext = OperationContextFactory
				.createOperationContext(
						WSDL20_2004Constants.MEP_CONSTANT_OUT_IN, terminateOp);
		opcontext.setParent(msgContext.getServiceContext());
		configurationContext.registerOperationContext(rmMsgCtx.getMessageId(),	opcontext);

		msgContext.setOperationContext(opcontext);
		msgContext.setAxisOperation(terminateOp);
		
		if (terminated != null && "true".equals(terminated)) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.terminateAddedPreviously);
			log.debug(message);
			return false;
		}

		TerminateSequence terminateSequencePart = (TerminateSequence) rmMsgCtx
				.getMessagePart(Sandesha2Constants.MessageParts.TERMINATE_SEQ);
		terminateSequencePart.getIdentifier().setIndentifer(outSequenceID);

		rmMsgCtx.setFlow(MessageContext.OUT_FLOW);
		msgContext.setProperty(Sandesha2Constants.APPLICATION_PROCESSING_DONE, "true");

		rmMsgCtx.setTo(new EndpointReference(toAddress));

		String rmVersion = SandeshaUtil.getRMVersion(internalSeqenceID, storageManager);
		if (rmVersion == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(SandeshaMessageKeys.cannotDecideRMVersion));

		rmMsgCtx.setWSAAction(SpecSpecificConstants.getTerminateSequenceAction(rmVersion));
		rmMsgCtx.setSOAPAction(SpecSpecificConstants.getTerminateSequenceSOAPAction(rmVersion));

		String transportTo = SandeshaUtil.getSequenceProperty(internalSeqenceID,
				Sandesha2Constants.SequenceProperties.TRANSPORT_TO, storageManager);
		if (transportTo != null) {
			rmMsgCtx.setProperty(MessageContextConstants.TRANSPORT_URL, transportTo);
		}
		
		
		//setting msg context properties
		rmMsgCtx.setProperty(Sandesha2Constants.MessageContextProperties.SEQUENCE_ID, outSequenceID);
		rmMsgCtx.setProperty(Sandesha2Constants.MessageContextProperties.INTERNAL_SEQUENCE_ID, internalSeqenceID);
		rmMsgCtx.setProperty(Sandesha2Constants.MessageContextProperties.SEQUENCE_PROPERTY_KEY , sequenceKey);

		try {
			rmMsgCtx.addSOAPEnvelope();
		} catch (AxisFault e) {
			throw new SandeshaException(e.getMessage(),e);
		}

		String key = SandeshaUtil.getUUID();

		SenderBean terminateBean = new SenderBean();
		terminateBean.setMessageContextRefKey(key);

		storageManager.storeMessageContext(key, msgContext);

		// Set a retransmitter lastSentTime so that terminate will be send with
		// some delay.
		// Otherwise this get send before return of the current request (ack).
		// TODO: refine the terminate delay.
		terminateBean.setTimeToSend(System.currentTimeMillis() + Sandesha2Constants.TERMINATE_DELAY);

		terminateBean.setMessageID(msgContext.getMessageID());
		
		EndpointReference to = msgContext.getTo();
		if (to!=null)
			terminateBean.setToAddress(to.getAddress());
		
		// this will be set to true at the sender.
		terminateBean.setSend(true);

		msgContext.setProperty(Sandesha2Constants.QUALIFIED_FOR_SENDING, Sandesha2Constants.VALUE_FALSE);

		terminateBean.setReSend(false);

		SenderBeanMgr retramsmitterMgr = storageManager.getRetransmitterBeanMgr();

		retramsmitterMgr.insert(terminateBean);

		SequencePropertyBean terminateAdded = new SequencePropertyBean();
		terminateAdded.setName(Sandesha2Constants.SequenceProperties.TERMINATE_ADDED);
		terminateAdded.setSequencePropertyKey(outSequenceID);
		terminateAdded.setValue("true");

		seqPropMgr.insert(terminateAdded);

		rmMsgCtx.setProperty(Sandesha2Constants.SET_SEND_TO_TRUE, Sandesha2Constants.VALUE_TRUE);

		SandeshaUtil.executeAndStore(rmMsgCtx, key);

		if (log.isDebugEnabled())
			log.debug("Exit: TerminateSeqMsgProcessor::processOutMessage " + Boolean.FALSE);
		return false;
	}

}
