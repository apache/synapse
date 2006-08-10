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

import javax.xml.namespace.QName;

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
import org.apache.axis2.description.OutInAxisOperation;
import org.apache.axis2.description.TransportOutDescription;
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
import org.apache.sandesha2.transport.Sandesha2TransportOutDesc;
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

	public void processInMessage(RMMsgContext terminateSeqRMMsg) throws SandeshaException {

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

		ConfigurationContext context = terminateSeqMsg.getConfigurationContext();
		StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(context,context.getAxisConfiguration());
		SequencePropertyBeanMgr sequencePropertyBeanMgr = storageManager.getSequencePropertyBeanMgr();
		
		// Check that the sender of this TerminateSequence holds the correct token
		SequencePropertyBean tokenBean = sequencePropertyBeanMgr.retrieve(sequenceId, Sandesha2Constants.SequenceProperties.SECURITY_TOKEN);
		if(tokenBean != null) {
			SecurityManager secManager = SandeshaUtil.getSecurityManager(context);
			OMElement body = terminateSeqRMMsg.getSOAPEnvelope().getBody();
			SecurityToken token = secManager.recoverSecurityToken(tokenBean.getValue());
			secManager.checkProofOfPossession(token, body, terminateSeqRMMsg.getMessageContext());
		}

		FaultManager faultManager = new FaultManager();
		RMMsgContext faultMessageContext = faultManager.checkForUnknownSequence(terminateSeqRMMsg, sequenceId,
				storageManager);
		if (faultMessageContext != null) {
			ConfigurationContext configurationContext = terminateSeqMsg.getConfigurationContext();
			AxisEngine engine = new AxisEngine(configurationContext);

			try {
				engine.sendFault(faultMessageContext.getMessageContext());
			} catch (AxisFault e) {
				throw new SandeshaException(SandeshaMessageHelper.getMessage(SandeshaMessageKeys.couldNotSendFault, e
						.toString()), e);
			}

			terminateSeqMsg.pause();
			return;
		}


		SequencePropertyBean terminateReceivedBean = new SequencePropertyBean();
		terminateReceivedBean.setSequenceID(sequenceId);
		terminateReceivedBean.setName(Sandesha2Constants.SequenceProperties.TERMINATE_RECEIVED);
		terminateReceivedBean.setValue("true");

		sequencePropertyBeanMgr.insert(terminateReceivedBean);

		// add the terminate sequence response if required.
		if (SpecSpecificConstants.isTerminateSequenceResponseRequired(terminateSeqRMMsg.getRMSpecVersion()))
			addTerminateSequenceResponse(terminateSeqRMMsg, sequenceId, storageManager);

		setUpHighestMsgNumbers(context, storageManager, sequenceId, terminateSeqRMMsg);

		TerminateManager.cleanReceivingSideOnTerminateMessage(context, sequenceId, storageManager);

		SequencePropertyBean terminatedBean = new SequencePropertyBean(sequenceId,
				Sandesha2Constants.SequenceProperties.SEQUENCE_TERMINATED, Sandesha2Constants.VALUE_TRUE);

		sequencePropertyBeanMgr.insert(terminatedBean);

		// removing an entry from the listener
		String transport = terminateSeqMsg.getTransportIn().getName().getLocalPart();

		SequenceManager.updateLastActivatedTime(sequenceId, storageManager);

		terminateSeqMsg.pause();

		if (log.isDebugEnabled())
			log.debug("Exit: TerminateSeqMsgProcessor::processInMessage");
	}

	private void setUpHighestMsgNumbers(ConfigurationContext configCtx, StorageManager storageManager,
			String sequenceID, RMMsgContext terminateRMMsg) throws SandeshaException {

		if (log.isDebugEnabled())
			log.debug("Enter: TerminateSeqMsgProcessor::setUpHighestMsgNumbers, " + sequenceID);

		SequencePropertyBeanMgr seqPropMgr = storageManager.getSequencePropertyBeanMgr();

		String highestImMsgNumberStr = SandeshaUtil.getSequenceProperty(sequenceID,
				Sandesha2Constants.SequenceProperties.HIGHEST_IN_MSG_NUMBER, storageManager);
		String highestImMsgKey = SandeshaUtil.getSequenceProperty(sequenceID,
				Sandesha2Constants.SequenceProperties.HIGHEST_IN_MSG_KEY, storageManager);

		long highestInMsgNo = 0;
		if (highestImMsgNumberStr != null) {
			if (highestImMsgKey == null)
				throw new SandeshaException(SandeshaMessageHelper.getMessage(
						SandeshaMessageKeys.highestMsgKeyNotStored, sequenceID));

			highestInMsgNo = Long.parseLong(highestImMsgNumberStr);
		}

		// following will be valid only for the server side, since the obtained
		// int. seq ID is only valid there.
		String responseSideInternalSequenceID = SandeshaUtil.getOutgoingSideInternalSequenceID(sequenceID);

		long highestOutMsgNo = 0;
		try {
			boolean addResponseSideTerminate = false;
			if (highestInMsgNo == 0) {
				addResponseSideTerminate = false;
			} else {

				// setting the last in message property
				SequencePropertyBean lastInMsgBean = new SequencePropertyBean(sequenceID,
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
								responseSideInternalSequenceID,
								Sandesha2Constants.SequenceProperties.LAST_OUT_MESSAGE_NO, new Long(highestOutMsgNo)
										.toString());

						seqPropMgr.insert(highestOutMsgBean);
						addResponseSideTerminate = true;
					}
				}
			}

			// If all the out message have been acked, add the outgoing
			// terminate seq msg.
			String outgoingSqunceID = SandeshaUtil.getSequenceProperty(responseSideInternalSequenceID,
					Sandesha2Constants.SequenceProperties.OUT_SEQUENCE_ID, storageManager);
			if (addResponseSideTerminate && highestOutMsgNo > 0 && responseSideInternalSequenceID != null
					&& outgoingSqunceID != null) {
				boolean allAcked = SandeshaUtil.isAllMsgsAckedUpto(highestOutMsgNo, responseSideInternalSequenceID,
						storageManager);

				if (allAcked)
					TerminateManager.addTerminateSequenceMessage(terminateRMMsg, outgoingSqunceID,
							responseSideInternalSequenceID, storageManager);
			}
		} catch (AxisFault e) {
			throw new SandeshaException(e);
		}
		if (log.isDebugEnabled())
			log.debug("Exit: TerminateSeqMsgProcessor::setUpHighestMsgNumbers");
	}

	private void addTerminateSequenceResponse(RMMsgContext terminateSeqRMMsg, String sequenceID,
			StorageManager storageManager) throws SandeshaException {

		if (log.isDebugEnabled())
			log.debug("Enter: TerminateSeqMsgProcessor::addTerminateSequenceResponse, " + sequenceID);

		MessageContext terminateSeqMsg = terminateSeqRMMsg.getMessageContext();
		ConfigurationContext configCtx = terminateSeqMsg.getConfigurationContext();

		MessageContext outMessage = null;

		try {
			outMessage = Utils.createOutMessageContext(terminateSeqMsg);
		} catch (AxisFault e1) {
			throw new SandeshaException(e1);
		}

		RMMsgContext terminateSeqResponseRMMsg = RMMsgCreator.createTerminateSeqResponseMsg(terminateSeqRMMsg,
				outMessage, storageManager);

		RMMsgContext ackRMMessage = AcknowledgementManager.generateAckMessage(terminateSeqRMMsg, sequenceID,
				storageManager);
		SequenceAcknowledgement seqAck = (SequenceAcknowledgement) ackRMMessage
				.getMessagePart(Sandesha2Constants.MessageParts.SEQ_ACKNOWLEDGEMENT);
		terminateSeqResponseRMMsg.setMessagePart(Sandesha2Constants.MessageParts.SEQ_ACKNOWLEDGEMENT, seqAck);

		terminateSeqResponseRMMsg.addSOAPEnvelope();

		terminateSeqResponseRMMsg.setFlow(MessageContext.OUT_FLOW);
		terminateSeqResponseRMMsg.setProperty(Sandesha2Constants.APPLICATION_PROCESSING_DONE, "true");

		outMessage.setResponseWritten(true);

		AxisEngine engine = new AxisEngine(terminateSeqMsg.getConfigurationContext());

		EndpointReference toEPR = terminateSeqMsg.getTo();

		try {
			engine.send(outMessage);
		} catch (AxisFault e) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.couldNotSendTerminateResponse, e
					.toString());
			throw new SandeshaException(message, e);
		}

		String addressingNamespaceURI = SandeshaUtil.getSequenceProperty(sequenceID,
				Sandesha2Constants.SequenceProperties.ADDRESSING_NAMESPACE_VALUE, storageManager);
		String anonymousURI = SpecSpecificConstants.getAddressingAnonymousURI(addressingNamespaceURI);

		if (anonymousURI.equals(toEPR.getAddress())) {
			terminateSeqMsg.getOperationContext().setProperty(org.apache.axis2.Constants.RESPONSE_WRITTEN, "true");
		} else {
			terminateSeqMsg.getOperationContext().setProperty(org.apache.axis2.Constants.RESPONSE_WRITTEN, "false");
		}

		if (log.isDebugEnabled())
			log.debug("Exit: TerminateSeqMsgProcessor::addTerminateSequenceResponse");
	}

	public void processOutMessage(RMMsgContext rmMsgCtx) throws SandeshaException {

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

		// registring an InOutOperationContext for this.
		// since the serviceContext.fireAndForget only sets a inOnly One
		// this does not work when there is a terminateSequnceResponse
		// TODO do processing of terminateMessagesCorrectly., create a new
		// message instead of sendign the one given by the serviceClient
		// TODO important
		try {
			AxisOperation oldOPeration = msgContext.getAxisOperation();
			AxisOperation outInAxisOp = new OutInAxisOperation(new QName("temp"));
			// setting flows
			outInAxisOp.setRemainingPhasesInFlow(oldOPeration.getRemainingPhasesInFlow());

			OperationContext opcontext = OperationContextFactory.createOperationContext(
					WSDL20_2004Constants.MEP_CONSTANT_OUT_IN, outInAxisOp);
			opcontext.setParent(msgContext.getServiceContext());
			configurationContext.registerOperationContext(rmMsgCtx.getMessageId(), opcontext);
		} catch (AxisFault e1) {
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.axisOperationRegisterError, e1.toString()));
		}

		if (terminated != null && "true".equals(terminated)) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.terminateAddedPreviously);
			log.debug(message);
			return;
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

		try {
			rmMsgCtx.addSOAPEnvelope();
		} catch (AxisFault e) {
			throw new SandeshaException(e.getMessage());
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

		// this will be set to true at the sender.
		terminateBean.setSend(true);

		msgContext.setProperty(Sandesha2Constants.QUALIFIED_FOR_SENDING, Sandesha2Constants.VALUE_FALSE);

		terminateBean.setReSend(false);

		SenderBeanMgr retramsmitterMgr = storageManager.getRetransmitterBeanMgr();

		retramsmitterMgr.insert(terminateBean);

		SequencePropertyBean terminateAdded = new SequencePropertyBean();
		terminateAdded.setName(Sandesha2Constants.SequenceProperties.TERMINATE_ADDED);
		terminateAdded.setSequenceID(outSequenceID);
		terminateAdded.setValue("true");

		seqPropMgr.insert(terminateAdded);

		// This should be dumped to the storage by the sender
		TransportOutDescription transportOut = msgContext.getTransportOut();
		rmMsgCtx.setProperty(Sandesha2Constants.ORIGINAL_TRANSPORT_OUT_DESC, transportOut);
		rmMsgCtx.setProperty(Sandesha2Constants.MESSAGE_STORE_KEY, key);
		rmMsgCtx.setProperty(Sandesha2Constants.SET_SEND_TO_TRUE, Sandesha2Constants.VALUE_TRUE);
		rmMsgCtx.getMessageContext().setTransportOut(new Sandesha2TransportOutDesc());
		// / addTerminateSeqTransaction.commit();

		AxisEngine engine = new AxisEngine(configurationContext);
		try {
			engine.send(msgContext);
		} catch (AxisFault e) {
			throw new SandeshaException(e.getMessage());
		}

		if (log.isDebugEnabled())
			log.debug("Exit: TerminateSeqMsgProcessor::processOutMessage");
	}

}
