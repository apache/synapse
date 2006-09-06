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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.AxisEngine;
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
import org.apache.sandesha2.storage.beanmanagers.SenderBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.SequencePropertyBeanMgr;
import org.apache.sandesha2.storage.beans.SenderBean;
import org.apache.sandesha2.storage.beans.SequencePropertyBean;
import org.apache.sandesha2.util.AcknowledgementManager;
import org.apache.sandesha2.util.FaultManager;
import org.apache.sandesha2.util.MsgInitializer;
import org.apache.sandesha2.util.SandeshaUtil;
import org.apache.sandesha2.util.SequenceManager;
import org.apache.sandesha2.util.SpecSpecificConstants;
import org.apache.sandesha2.util.TerminateManager;
import org.apache.sandesha2.wsrm.AcknowledgementRange;
import org.apache.sandesha2.wsrm.Nack;
import org.apache.sandesha2.wsrm.SequenceAcknowledgement;

/**
 * Responsible for processing acknowledgement headers on incoming messages.
 */

public class AcknowledgementProcessor {

	private static final Log log = LogFactory.getLog(AcknowledgementProcessor.class);

	public void processAckHeaders(MessageContext message) throws SandeshaException {
		if (log.isDebugEnabled())
			log.debug("Enter: AcknowledgementProcessor::processAckHeaders");

		SOAPEnvelope envelope = message.getEnvelope();
		SOAPHeader header = envelope.getHeader();
		
		for(int i = 0; i < Sandesha2Constants.SPEC_NS_URIS.length; i++) {
			QName headerName = new QName(Sandesha2Constants.SPEC_NS_URIS[i], Sandesha2Constants.WSRM_COMMON.SEQUENCE_ACK);
			
			Iterator acks = header.getChildrenWithName(headerName);
			while(acks.hasNext()) {
				OMElement ack = (OMElement) acks.next();
				SequenceAcknowledgement seqAck = new SequenceAcknowledgement(null, headerName.getNamespaceURI());
			  seqAck.fromOMElement(ack);
			  processAckHeader(message, seqAck);
			}
		}

		if (log.isDebugEnabled())
			log.debug("Exit: AcknowledgementProcessor::processAckHeaders");
	}
	
	private void processAckHeader(MessageContext msgCtx, SequenceAcknowledgement sequenceAck)
	throws SandeshaException
	{
		if (log.isDebugEnabled())
			log.debug("Enter: AcknowledgementProcessor::processAckHeader");
		
		// TODO: Note that this RMMessageContext is not really any use - but we need to create it
		// so that it can be passed to the fault handling chain. It's really no more than a
		// container for the correct addressing and RM spec levels, so we'd be better off passing
		// them in directly. Unfortunately that change ripples through the codebase...
		RMMsgContext rmMsgCtx = MsgInitializer.initializeMessage(msgCtx);

		ConfigurationContext configCtx = msgCtx.getConfigurationContext();

		StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(configCtx, configCtx
				.getAxisConfiguration());

		SenderBeanMgr retransmitterMgr = storageManager.getRetransmitterBeanMgr();
		SequencePropertyBeanMgr seqPropMgr = storageManager.getSequencePropertyBeanMgr();

		String outSequenceId = sequenceAck.getIdentifier().getIdentifier();
		if (outSequenceId == null || "".equals(outSequenceId)) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.outSeqIDIsNull);
			log.debug(message);
			throw new SandeshaException(message);
		}

		// Check that the sender of this Ack holds the correct token
		SequencePropertyBean tokenBean = seqPropMgr.retrieve(outSequenceId, Sandesha2Constants.SequenceProperties.SECURITY_TOKEN);
		if(tokenBean != null) {
			SecurityManager secManager = SandeshaUtil.getSecurityManager(configCtx);
			SecurityToken token = secManager.recoverSecurityToken(tokenBean.getValue());
			
			//TODO get the element from the SOAP Envelope
//			secManager.checkProofOfPossession(token, sequsenceAck.getOMElement(), msgCtx);
		}
		
		Iterator ackRangeIterator = sequenceAck.getAcknowledgementRanges().iterator();
		Iterator nackIterator = sequenceAck.getNackList().iterator();

		FaultManager faultManager = new FaultManager();
		RMMsgContext faultMessageContext = faultManager
				.checkForUnknownSequence(rmMsgCtx, outSequenceId, storageManager);
		if(faultMessageContext == null) {
			faultMessageContext = faultManager.checkForInvalidAcknowledgement(rmMsgCtx, storageManager);
		}
		if (faultMessageContext != null) {

			ConfigurationContext configurationContext = msgCtx.getConfigurationContext();
			AxisEngine engine = new AxisEngine(configurationContext);

			try {
				engine.sendFault(faultMessageContext.getMessageContext());
			} catch (AxisFault e) {
				throw new SandeshaException(
						SandeshaMessageHelper.getMessage(SandeshaMessageKeys.couldNotSendFault, e.toString()),
						e);
			}

			// TODO: Should a bad ack stop processing of the message?
			msgCtx.pause();
			return;
		}

		String internalSequenceID = SandeshaUtil.getSequenceProperty(outSequenceId,
				Sandesha2Constants.SequenceProperties.INTERNAL_SEQUENCE_ID, storageManager);

		// updating the last activated time of the sequence.
		SequenceManager.updateLastActivatedTime(internalSequenceID, storageManager);

		SequencePropertyBean internalSequenceBean = seqPropMgr.retrieve(outSequenceId,
				Sandesha2Constants.SequenceProperties.INTERNAL_SEQUENCE_ID);

		if (internalSequenceBean == null || internalSequenceBean.getValue() == null) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.tempSeqIdNotSet);
			log.debug(message);
			throw new SandeshaException(message);
		}

		String internalSequenceId = (String) internalSequenceBean.getValue();

		SenderBean input = new SenderBean();
		input.setSend(true);
		input.setReSend(true);
		Collection retransmitterEntriesOfSequence = retransmitterMgr.find(input);

		ArrayList ackedMessagesList = new ArrayList();
		while (ackRangeIterator.hasNext()) {
			AcknowledgementRange ackRange = (AcknowledgementRange) ackRangeIterator.next();
			long lower = ackRange.getLowerValue();
			long upper = ackRange.getUpperValue();

			for (long messageNo = lower; messageNo <= upper; messageNo++) {
				SenderBean retransmitterBean = getRetransmitterEntry(retransmitterEntriesOfSequence, messageNo);
				if (retransmitterBean != null) {
					retransmitterMgr.delete(retransmitterBean.getMessageID());

					// removing the application message from the storage.
					String storageKey = retransmitterBean.getMessageContextRefKey();
					storageManager.removeMessageContext(storageKey);
				}

				ackedMessagesList.add(new Long(messageNo));
			}
		}

		while (nackIterator.hasNext()) {
			Nack nack = (Nack) nackIterator.next();
			long msgNo = nack.getNackNumber();

			// TODO - Process Nack
		}

		// setting acked message date.
		// TODO add details specific to each message.
		long noOfMsgsAcked = getNoOfMessagesAcked(sequenceAck.getAcknowledgementRanges().iterator());
		SequencePropertyBean noOfMsgsAckedBean = seqPropMgr.retrieve(outSequenceId,
				Sandesha2Constants.SequenceProperties.NO_OF_OUTGOING_MSGS_ACKED);
		boolean added = false;

		if (noOfMsgsAckedBean == null) {
			added = true;
			noOfMsgsAckedBean = new SequencePropertyBean();
			noOfMsgsAckedBean.setSequenceID(outSequenceId);
			noOfMsgsAckedBean.setName(Sandesha2Constants.SequenceProperties.NO_OF_OUTGOING_MSGS_ACKED);
		}

		noOfMsgsAckedBean.setValue(Long.toString(noOfMsgsAcked));

		if (added)
			seqPropMgr.insert(noOfMsgsAckedBean);
		else
			seqPropMgr.update(noOfMsgsAckedBean);

		// setting the completed_messages list. This gives all the messages of
		// the sequence that were acked.
		SequencePropertyBean allCompletedMsgsBean = seqPropMgr.retrieve(internalSequenceId,
				Sandesha2Constants.SequenceProperties.CLIENT_COMPLETED_MESSAGES);
		if (allCompletedMsgsBean == null) {
			allCompletedMsgsBean = new SequencePropertyBean();
			allCompletedMsgsBean.setSequenceID(internalSequenceId);
			allCompletedMsgsBean.setName(Sandesha2Constants.SequenceProperties.CLIENT_COMPLETED_MESSAGES);

			seqPropMgr.insert(allCompletedMsgsBean);
		}

		String str = ackedMessagesList.toString();
		allCompletedMsgsBean.setValue(str);

		seqPropMgr.update(allCompletedMsgsBean);

		String lastOutMsgNoStr = SandeshaUtil.getSequenceProperty(internalSequenceId,
				Sandesha2Constants.SequenceProperties.LAST_OUT_MESSAGE_NO, storageManager);
		if (lastOutMsgNoStr != null) {
			long highestOutMsgNo = 0;
			if (lastOutMsgNoStr != null) {
				highestOutMsgNo = Long.parseLong(lastOutMsgNoStr);
			}

			if (highestOutMsgNo > 0) {
				boolean complete = AcknowledgementManager.verifySequenceCompletion(sequenceAck
						.getAcknowledgementRanges().iterator(), highestOutMsgNo);

				if (complete)
					TerminateManager.addTerminateSequenceMessage(rmMsgCtx, outSequenceId, internalSequenceId,
							storageManager);
			}
		}


		String action = msgCtx.getOptions().getAction();
		if (action!=null && action.equals(SpecSpecificConstants.getAckRequestAction(rmMsgCtx.getRMSpecVersion()))) {
			rmMsgCtx.pause();
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

	private static long getNoOfMessagesAcked(Iterator ackRangeIterator) {
		long noOfMsgs = 0;
		while (ackRangeIterator.hasNext()) {
			AcknowledgementRange acknowledgementRange = (AcknowledgementRange) ackRangeIterator.next();
			long lower = acknowledgementRange.getLowerValue();
			long upper = acknowledgementRange.getUpperValue();

			for (long i = lower; i <= upper; i++) {
				noOfMsgs++;
			}
		}

		return noOfMsgs;
	}

}
