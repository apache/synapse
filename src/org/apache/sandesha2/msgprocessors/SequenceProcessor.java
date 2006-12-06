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

import java.util.Collection;
import java.util.Iterator;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.description.AxisOperation;
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
import org.apache.sandesha2.storage.beanmanagers.InvokerBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.NextMsgBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.SenderBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.SequencePropertyBeanMgr;
import org.apache.sandesha2.storage.beans.InvokerBean;
import org.apache.sandesha2.storage.beans.NextMsgBean;
import org.apache.sandesha2.storage.beans.SenderBean;
import org.apache.sandesha2.storage.beans.SequencePropertyBean;
import org.apache.sandesha2.util.AcknowledgementManager;
import org.apache.sandesha2.util.FaultManager;
import org.apache.sandesha2.util.MsgInitializer;
import org.apache.sandesha2.util.SandeshaUtil;
import org.apache.sandesha2.util.SequenceManager;
import org.apache.sandesha2.wsrm.Sequence;

/**
 * Responsible for processing the Sequence header (if present) on an incoming
 * message.
 */

public class SequenceProcessor {

	private static final Log log = LogFactory.getLog(SequenceProcessor.class);

	public boolean processSequenceHeader(RMMsgContext rmMsgCtx) throws AxisFault {
		if (log.isDebugEnabled())
			log.debug("Enter: SequenceProcessor::processSequenceHeader");
		boolean result = false;
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
	
	public boolean processReliableMessage(RMMsgContext rmMsgCtx) throws AxisFault {
		if (log.isDebugEnabled())
			log.debug("Enter: SequenceProcessor::processReliableMessage");

		boolean msgCtxPaused = false;
		
		if (rmMsgCtx.getProperty(Sandesha2Constants.APPLICATION_PROCESSING_DONE) != null
				&& rmMsgCtx.getProperty(Sandesha2Constants.APPLICATION_PROCESSING_DONE).equals("true")) {
			return msgCtxPaused;
		}

		MessageContext msgCtx = rmMsgCtx.getMessageContext();
		StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(msgCtx.getConfigurationContext(),msgCtx.getConfigurationContext().getAxisConfiguration());
		SequencePropertyBeanMgr seqPropMgr = storageManager.getSequencePropertyBeanMgr();
		Sequence sequence = (Sequence) rmMsgCtx.getMessagePart(Sandesha2Constants.MessageParts.SEQUENCE);
		String sequenceId = sequence.getIdentifier().getIdentifier();
		
		String propertyKey = SandeshaUtil.getSequencePropertyKey(rmMsgCtx);
		
		// Check that both the Sequence header and message body have been secured properly
		SequencePropertyBean tokenBean = seqPropMgr.retrieve(propertyKey, Sandesha2Constants.SequenceProperties.SECURITY_TOKEN);
		if(tokenBean != null) {
			SecurityManager secManager = SandeshaUtil.getSecurityManager(msgCtx.getConfigurationContext());
			
			QName seqName = new QName(rmMsgCtx.getRMNamespaceValue(), Sandesha2Constants.WSRM_COMMON.SEQUENCE);
			
			SOAPEnvelope envelope = msgCtx.getEnvelope();
			OMElement body = envelope.getBody();
			OMElement seqHeader = envelope.getHeader().getFirstChildWithName(seqName);
			
			SecurityToken token = secManager.recoverSecurityToken(tokenBean.getValue());
			
			secManager.checkProofOfPossession(token, seqHeader, msgCtx);
			secManager.checkProofOfPossession(token, body, msgCtx);
		}
		
		//RM will not send sync responses. If sync acks are there this will be
		// made true again later.
		if (rmMsgCtx.getMessageContext().getOperationContext() != null) {
			rmMsgCtx.getMessageContext().getOperationContext().setProperty(Constants.RESPONSE_WRITTEN,
					Constants.VALUE_FALSE);
		}
		
		// setting acked msg no range
		ConfigurationContext configCtx = rmMsgCtx.getMessageContext().getConfigurationContext();
		if (configCtx == null) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.configContextNotSet);
			log.debug(message);
			throw new SandeshaException(message);
		}

		FaultManager faultManager = new FaultManager();
		SandeshaException fault = faultManager.checkForUnknownSequence(rmMsgCtx, sequenceId, storageManager);
		if (fault != null) {
			throw fault;
		}

		// setting mustUnderstand to false.
		sequence.setMustUnderstand(false);
		rmMsgCtx.addSOAPEnvelope();

		// throwing a fault if the sequence is closed.
		fault = faultManager.checkForSequenceClosed(rmMsgCtx, sequenceId, storageManager);
		if (fault != null) {
			throw fault;
		}

		fault = faultManager.checkForLastMsgNumberExceeded(rmMsgCtx, storageManager);
		if (fault != null) {
			throw fault;
		}

		// updating the last activated time of the sequence.
		SequenceManager.updateLastActivatedTime(propertyKey, storageManager);

		SequencePropertyBean msgsBean = seqPropMgr.retrieve(propertyKey,
				Sandesha2Constants.SequenceProperties.SERVER_COMPLETED_MESSAGES);

		long msgNo = sequence.getMessageNumber().getMessageNumber();
		if (msgNo == 0) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.invalidMsgNumber, Long
					.toString(msgNo));
			log.debug(message);
			throw new SandeshaException(message);
		}

		String key = SandeshaUtil.getUUID(); // key to store the message.

		// updating the Highest_In_Msg_No property which gives the highest
		// message number retrieved from this sequence.
		String highetsInMsgNoStr = SandeshaUtil.getSequenceProperty(propertyKey,
				Sandesha2Constants.SequenceProperties.HIGHEST_IN_MSG_NUMBER, storageManager);

		long highestInMsgNo = 0;
		if (highetsInMsgNoStr != null) {
			highestInMsgNo = Long.parseLong(highetsInMsgNoStr);
		}

		if (msgNo > highestInMsgNo) {
			SequencePropertyBean highestMsgNoBean = new SequencePropertyBean(propertyKey,
					Sandesha2Constants.SequenceProperties.HIGHEST_IN_MSG_NUMBER, Long.toString(msgNo));
			SequencePropertyBean highestMsgIdBean = new SequencePropertyBean(propertyKey,
					Sandesha2Constants.SequenceProperties.HIGHEST_IN_MSG_ID, msgCtx.getMessageID());

			if (highetsInMsgNoStr != null) {
				seqPropMgr.update(highestMsgNoBean);
				seqPropMgr.update(highestMsgIdBean);
			} else {
				seqPropMgr.insert(highestMsgNoBean);
				seqPropMgr.insert(highestMsgIdBean);
			}
		}

		String messagesStr = "";
		if (msgsBean != null)
			messagesStr = msgsBean.getValue();
		else {
			msgsBean = new SequencePropertyBean();
			msgsBean.setSequencePropertyKey(propertyKey);
			msgsBean.setName(Sandesha2Constants.SequenceProperties.SERVER_COMPLETED_MESSAGES);
			msgsBean.setValue(messagesStr);
		}

		boolean msgNoPresentInList = msgNoPresentInList(messagesStr, msgNo);
		
		if (msgNoPresentInList
				&& (Sandesha2Constants.QOS.InvocationType.DEFAULT_INVOCATION_TYPE == Sandesha2Constants.QOS.InvocationType.EXACTLY_ONCE)) {
			// this is a duplicate message and the invocation type is
			// EXACTLY_ONCE.
			rmMsgCtx.pause();
			msgCtxPaused = true;
		}

		if (!msgNoPresentInList)
		{
			if (messagesStr != null && !"".equals(messagesStr))
				messagesStr = messagesStr + "," + Long.toString(msgNo);
			else
				messagesStr = Long.toString(msgNo);
	
			msgsBean.setValue(messagesStr);
			seqPropMgr.update(msgsBean);
		}

		// Pause the messages bean if not the right message to invoke.
		NextMsgBeanMgr mgr = storageManager.getNextMsgBeanMgr();
		NextMsgBean bean = mgr.retrieve(sequenceId);

		if (bean == null) {
			throw new SandeshaException(SandeshaMessageHelper.getMessage(SandeshaMessageKeys.cannotFindSequence,
					sequenceId));
		}

		InvokerBeanMgr storageMapMgr = storageManager.getStorageMapBeanMgr();

		// inorder invocation is still a global property
		boolean inOrderInvocation = SandeshaUtil.getPropertyBean(
				msgCtx.getConfigurationContext().getAxisConfiguration()).isInOrder();


		//setting properties for the messageContext
		rmMsgCtx.setProperty(Sandesha2Constants.MessageContextProperties.SEQUENCE_ID,sequenceId);
		rmMsgCtx.setProperty(Sandesha2Constants.MessageContextProperties.MESSAGE_NUMBER,new Long (msgNo));
		
		if (inOrderInvocation && !msgNoPresentInList) {

			// saving the message.
			try {
				storageManager.storeMessageContext(key, rmMsgCtx.getMessageContext());
				storageMapMgr.insert(new InvokerBean(key, msgNo, sequenceId));

				// This will avoid performing application processing more
				// than
				// once.
				rmMsgCtx.setProperty(Sandesha2Constants.APPLICATION_PROCESSING_DONE, "true");

			} catch (Exception ex) {
				throw new SandeshaException(ex.getMessage(), ex);
			}

			// pause the message
			rmMsgCtx.pause();
			msgCtxPaused = true;

			// Starting the invoker if stopped.
			SandeshaUtil.startInvokerForTheSequence(msgCtx.getConfigurationContext(), sequenceId);

		}

		// Sending acknowledgements
		sendAckIfNeeded(rmMsgCtx, messagesStr, storageManager);

		if (log.isDebugEnabled())
			log.debug("Exit: SequenceProcessor::processReliableMessage " + msgCtxPaused);
		return msgCtxPaused;
	}

	// TODO convert following from INT to LONG
	private boolean msgNoPresentInList(String list, long no) {
		String[] msgStrs = list.split(",");

		int l = msgStrs.length;

		for (int i = 0; i < l; i++) {
			if (msgStrs[i].equals(Long.toString(no)))
				return true;
		}

		return false;
	}

	public static void sendAckIfNeeded(RMMsgContext rmMsgCtx, String messagesStr, StorageManager storageManager)
			throws AxisFault {

		if (log.isDebugEnabled())
			log.debug("Enter: SequenceProcessor::sendAckIfNeeded");

		String sequencePropertyKey = SandeshaUtil.getSequencePropertyKey(rmMsgCtx);
		
		Sequence sequence = (Sequence) rmMsgCtx.getMessagePart(Sandesha2Constants.MessageParts.SEQUENCE);
		String sequenceId = sequence.getIdentifier().getIdentifier();
		ConfigurationContext configCtx = rmMsgCtx.getMessageContext().getConfigurationContext();
		if (configCtx == null) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.configContextNotSet);
			if(log.isDebugEnabled()) log.debug(message);
			throw new SandeshaException(message);
		}
		
		RMMsgContext ackRMMsgCtx = AcknowledgementManager.generateAckMessage(rmMsgCtx, sequencePropertyKey, sequenceId, storageManager);
		MessageContext ackMsgCtx = ackRMMsgCtx.getMessageContext();
		
		EndpointReference acksTo = ackRMMsgCtx.getTo();
		
		if (SandeshaUtil.isAnonymousURI (acksTo.getAddress())) {

			// setting CONTEXT_WRITTEN since acksto is anonymous
			if (rmMsgCtx.getMessageContext().getOperationContext() == null) {
				// operation context will be null when doing in a GLOBAL
				// handler.
				AxisOperation op = ackMsgCtx.getAxisOperation();
				OperationContext opCtx = new OperationContext(op);
				rmMsgCtx.getMessageContext().setOperationContext(opCtx);
			}

			rmMsgCtx.getMessageContext().getOperationContext().setProperty(
					org.apache.axis2.Constants.RESPONSE_WRITTEN, Constants.VALUE_TRUE);

			rmMsgCtx.getMessageContext().setProperty(Sandesha2Constants.ACK_WRITTEN, "true");

			ackRMMsgCtx.getMessageContext().setServerSide(true);
			
			AxisEngine engine = new AxisEngine(configCtx);
			engine.send(ackRMMsgCtx.getMessageContext());

		} else {

			// / Transaction asyncAckTransaction =
			// storageManager.getTransaction();

			SenderBeanMgr retransmitterBeanMgr = storageManager.getRetransmitterBeanMgr();

			String key = SandeshaUtil.getUUID();

			SenderBean ackBean = new SenderBean();
			ackBean.setMessageContextRefKey(key);
			ackBean.setMessageID(ackMsgCtx.getMessageID());
			ackBean.setReSend(false);
			ackBean.setSequenceID(sequencePropertyKey);
			EndpointReference to = ackMsgCtx.getTo();
			if (to!=null)
				ackBean.setToAddress(to.getAddress());

			// this will be set to true in the sender.
			ackBean.setSend(true);

			ackMsgCtx.setProperty(Sandesha2Constants.QUALIFIED_FOR_SENDING, Sandesha2Constants.VALUE_FALSE);

			ackBean.setMessageType(Sandesha2Constants.MessageTypes.ACK);
			long ackInterval = SandeshaUtil.getPropertyBean(rmMsgCtx.getMessageContext().getAxisService())
					.getAcknowledgementInterval();

			// Ack will be sent as stand alone, only after the retransmitter
			// interval.
			long timeToSend = System.currentTimeMillis() + ackInterval;

			// removing old acks.
			SenderBean findBean = new SenderBean();
			findBean.setMessageType(Sandesha2Constants.MessageTypes.ACK);

			// this will be set to true in the sandesha2TransportSender.
			findBean.setSend(true);
			findBean.setReSend(false);
			Collection coll = retransmitterBeanMgr.find(findBean);
			Iterator it = coll.iterator();

			if (it.hasNext()) {
				SenderBean oldAckBean = (SenderBean) it.next();
				timeToSend = oldAckBean.getTimeToSend(); // If there is an
															// old ack. This ack
															// will be sent in
															// the old
															// timeToSend.

				// removing the retransmitted entry for the oldAck
				retransmitterBeanMgr.delete(oldAckBean.getMessageID());

				// removing the message store entry for the old ack
				storageManager.removeMessageContext(oldAckBean.getMessageContextRefKey());
			}

			ackBean.setTimeToSend(timeToSend);

			ackMsgCtx.setProperty(Sandesha2Constants.QUALIFIED_FOR_SENDING, Sandesha2Constants.VALUE_FALSE);
			
			// / asyncAckTransaction.commit();

			// passing the message through sandesha2sender
			ackMsgCtx.setProperty(Sandesha2Constants.SET_SEND_TO_TRUE, Sandesha2Constants.VALUE_TRUE);
			ackRMMsgCtx = MsgInitializer.initializeMessage(ackMsgCtx);
			
			SandeshaUtil.executeAndStore(ackRMMsgCtx, key);

			// inserting the new ack.
			retransmitterBeanMgr.insert(ackBean);

			SandeshaUtil.startSenderForTheSequence(ackRMMsgCtx.getConfigurationContext(), sequenceId);
		}
		
		
		if (log.isDebugEnabled())
			log.debug("Exit: SequenceProcessor::sendAckIfNeeded");
	}
}
