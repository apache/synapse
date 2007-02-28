/*
 * Copyright  1999-2004 The Apache Software Foundation.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.sandesha2.util;

import java.util.Collection;
import java.util.Iterator;

import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ContextFactory;
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
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.beanmanagers.SenderBeanMgr;
import org.apache.sandesha2.storage.beans.RMDBean;
import org.apache.sandesha2.storage.beans.SenderBean;

/**
 * Contains logic for managing acknowledgements.
 */

public class AcknowledgementManager {

	private static Log log = LogFactory.getLog(AcknowledgementManager.class);

	/**
	 * Piggybacks any available acks of the same sequence to the given
	 * application message.
	 * 
	 * @param applicationRMMsgContext
	 * @throws SandeshaException
	 */
	public static void piggybackAcksIfPresent(RMMsgContext rmMessageContext, StorageManager storageManager)
			throws SandeshaException {
		if (log.isDebugEnabled())
			log.debug("Enter: AcknowledgementManager::piggybackAcksIfPresent");
		
		SenderBeanMgr retransmitterBeanMgr = storageManager.getSenderBeanMgr();

		// If this message is going to an anonymous address, and the inbound sequence has
		// anonymous acksTo, then we add in an ack for the inbound sequence.
		EndpointReference target = rmMessageContext.getTo();
		if(target.hasAnonymousAddress()) {
			String inboundSequence = (String) rmMessageContext.getProperty(Sandesha2Constants.MessageContextProperties.INBOUND_SEQUENCE_ID);
			if(inboundSequence != null) {
				RMDBean inboundBean = SandeshaUtil.getRMDBeanFromSequenceId(storageManager, inboundSequence);
				if(inboundBean != null) {
					String acksTo = inboundBean.getAcksToEPR();
					EndpointReference acksToEPR = new EndpointReference(acksTo);
					
					if(acksTo == null || acksToEPR.hasAnonymousAddress()) {
						if(log.isDebugEnabled()) log.debug("Piggybacking ack for inbound sequence: " + inboundSequence);
						RMMsgCreator.addAckMessage(rmMessageContext, inboundSequence, inboundBean);
					}
				}
			}
			if(log.isDebugEnabled()) log.debug("Exit: AcknowledgementManager::piggybackAcksIfPresent, anon");
			return;
		}
		
		// From here on, we must be dealing with a real address. Piggyback all sequences that have an
		// acksTo that matches the To address, and that have an ackMessage queued up for sending.
		SenderBean findBean = new SenderBean();
		findBean.setMessageType(Sandesha2Constants.MessageTypes.ACK);
		findBean.setSend(true);
		findBean.setToAddress(target.getAddress());

		Collection collection = retransmitterBeanMgr.find(findBean);
		Iterator it = collection.iterator();
		while (it.hasNext()) {
			SenderBean ackBean = (SenderBean) it.next();

			// Piggybacking will happen only if the end of ack interval (timeToSend) is not reached.
			long timeNow = System.currentTimeMillis();
			if (ackBean.getTimeToSend() > timeNow) {
				// Delete the beans that would have sent the ack
				retransmitterBeanMgr.delete(ackBean.getMessageID());
				storageManager.removeMessageContext(ackBean.getMessageContextRefKey());

				String sequenceId = ackBean.getSequenceID();
				if (log.isDebugEnabled()) log.debug("Piggybacking ack for sequence: " + sequenceId);

				RMDBean rmdBean = SandeshaUtil.getRMDBeanFromSequenceId(storageManager, sequenceId);
				if(rmdBean != null) {
					RMMsgCreator.addAckMessage(rmMessageContext, sequenceId, rmdBean);
				}
			}
		}
		
		if (log.isDebugEnabled())
			log.debug("Exit: AcknowledgementManager::piggybackAcksIfPresent");
	}

	/**
	 * 
	 * @param referenceRMMessage
	 * @param sequencePropertyKey
	 * @param sequenceId
	 * @param storageManager
	 * @param makeResponse Some work will be done to make the new ack message the response of the reference message.
	 * @return
	 * @throws AxisFault
	 */
	public static RMMsgContext generateAckMessage(
			
			RMMsgContext referenceRMMessage,
			RMDBean rmdBean,
			String sequenceId,
			StorageManager storageManager, 
			boolean serverSide
			
			) throws AxisFault {
		
		if (log.isDebugEnabled())
			log.debug("Enter: AcknowledgementManager::generateAckMessage " + rmdBean);

		MessageContext referenceMsg = referenceRMMessage.getMessageContext();

		EndpointReference acksTo = new EndpointReference(rmdBean.getAcksToEPR());

		if (acksTo.getAddress() == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(SandeshaMessageKeys.acksToStrNotSet));

		AxisOperation ackOperation = SpecSpecificConstants.getWSRMOperation(
				Sandesha2Constants.MessageTypes.ACK,
				referenceRMMessage.getRMSpecVersion(),
				referenceMsg.getAxisService());

		MessageContext ackMsgCtx = SandeshaUtil.createNewRelatedMessageContext(referenceRMMessage, ackOperation);
		
		ackMsgCtx.setProperty(Sandesha2Constants.APPLICATION_PROCESSING_DONE, "true");

		RMMsgContext ackRMMsgCtx = MsgInitializer.initializeMessage(ackMsgCtx);
		ackRMMsgCtx.setFlow(MessageContext.OUT_FLOW);
		ackRMMsgCtx.setRMNamespaceValue(referenceRMMessage.getRMNamespaceValue());

		ackMsgCtx.setMessageID(SandeshaUtil.getUUID());

		SOAPFactory factory = SOAPAbstractFactory.getSOAPFactory(SandeshaUtil
				.getSOAPVersion(referenceMsg.getEnvelope()));

		// Setting new envelope
		SOAPEnvelope envelope = factory.getDefaultEnvelope();

		ackMsgCtx.setEnvelope(envelope);

		ackMsgCtx.setTo(acksTo);
		
		ackMsgCtx.setServerSide(serverSide);

		// adding the SequenceAcknowledgement part.
		RMMsgCreator.addAckMessage(ackRMMsgCtx, sequenceId, rmdBean);

		if (log.isDebugEnabled())
			log.debug("Exit: AcknowledgementManager::generateAckMessage");
		return ackRMMsgCtx;
	}

	
	

	public static boolean verifySequenceCompletion(RangeString ackRanges, long lastMessageNo) {
		if (log.isDebugEnabled())
			log.debug("Enter: AcknowledgementManager::verifySequenceCompletion");

		boolean result = false;
		Range complete = new Range(1, lastMessageNo);
		if(ackRanges.isRangeCompleted(complete)) {
			result = true;
		}

		if (log.isDebugEnabled())
			log.debug("Exit: AcknowledgementManager::verifySequenceCompletion " + result);
		return result;
	}
	
	public static void addAckBeanEntry (
			RMMsgContext ackRMMsgContext,
			String sequenceId, 
			long timeToSend,
			StorageManager storageManager) throws AxisFault {
		if(log.isDebugEnabled()) log.debug("Enter: AcknowledgementManager::addAckBeanEntry");

		// Write the acks into the envelope
		ackRMMsgContext.addSOAPEnvelope();
		
		MessageContext ackMsgContext = ackRMMsgContext.getMessageContext();

		SenderBeanMgr retransmitterBeanMgr = storageManager.getSenderBeanMgr();

		String key = SandeshaUtil.getUUID();

		SenderBean ackBean = new SenderBean();
		ackBean.setMessageContextRefKey(key);
		ackBean.setMessageID(ackMsgContext.getMessageID());
		ackBean.setReSend(false);
		ackBean.setSequenceID(sequenceId);
		EndpointReference to = ackMsgContext.getTo();
		if (to!=null)
			ackBean.setToAddress(to.getAddress());

		ackBean.setSend(true);
		ackMsgContext.setProperty(Sandesha2Constants.QUALIFIED_FOR_SENDING, Sandesha2Constants.VALUE_FALSE);

		ackBean.setMessageType(Sandesha2Constants.MessageTypes.ACK);

		// removing old acks.
		SenderBean findBean = new SenderBean();
		findBean.setMessageType(Sandesha2Constants.MessageTypes.ACK);
		findBean.setSend(true);
		findBean.setReSend(false);
		findBean.setSequenceID(sequenceId);
		Collection coll = retransmitterBeanMgr.find(findBean);
		Iterator it = coll.iterator();

		while(it.hasNext()) {
			SenderBean oldAckBean = (SenderBean) it.next();
			if(oldAckBean.getTimeToSend() < timeToSend)
				timeToSend = oldAckBean.getTimeToSend();

			// removing the retransmitted entry for the oldAck
			retransmitterBeanMgr.delete(oldAckBean.getMessageID());

			// removing the message store entry for the old ack
			storageManager.removeMessageContext(oldAckBean.getMessageContextRefKey());
		}

		ackBean.setTimeToSend(timeToSend);

		ackMsgContext.setProperty(Sandesha2Constants.QUALIFIED_FOR_SENDING, Sandesha2Constants.VALUE_FALSE);
		
		// passing the message through sandesha2sender
		ackMsgContext.setProperty(Sandesha2Constants.SET_SEND_TO_TRUE, Sandesha2Constants.VALUE_TRUE);
		
		SandeshaUtil.executeAndStore(ackRMMsgContext, key);

		// inserting the new ack.
		retransmitterBeanMgr.insert(ackBean);

		if(log.isDebugEnabled()) log.debug("Exit: AcknowledgementManager::addAckBeanEntry");
	}
	
	public static void sendAckNow (RMMsgContext ackRMMsgContext) throws AxisFault {
		if (log.isDebugEnabled())
			log.debug("Enter: AcknowledgementManager::sendAckNow");

		// Write the acks into the envelope
		ackRMMsgContext.addSOAPEnvelope();
		
		MessageContext ackMsgContext = ackRMMsgContext.getMessageContext();
		ConfigurationContext configContext = ackMsgContext.getConfigurationContext();
		
		// setting CONTEXT_WRITTEN since acksto is anonymous
		if (ackRMMsgContext.getMessageContext().getOperationContext() == null) {
			// operation context will be null when doing in a GLOBAL
			// handler.
			AxisOperation op = ackMsgContext.getAxisOperation();

			OperationContext opCtx = ContextFactory.createOperationContext(op, ackRMMsgContext.getMessageContext().getServiceContext());
			ackRMMsgContext.getMessageContext().setOperationContext(opCtx);
		}

		ackRMMsgContext.getMessageContext().getOperationContext().setProperty(
				org.apache.axis2.Constants.RESPONSE_WRITTEN, Constants.VALUE_TRUE);

		ackRMMsgContext.getMessageContext().setProperty(Sandesha2Constants.ACK_WRITTEN, "true");

		ackRMMsgContext.getMessageContext().setServerSide(true);
		
		AxisEngine engine = new AxisEngine(configContext);
		engine.send(ackMsgContext);
		
		if (log.isDebugEnabled())
			log.debug("Exit: AcknowledgementManager::sendAckNow");		
	}	
}
