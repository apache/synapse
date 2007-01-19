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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPHeader;
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
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.beanmanagers.RMDBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.SenderBeanMgr;
import org.apache.sandesha2.storage.beans.RMDBean;
import org.apache.sandesha2.storage.beans.RMSBean;
import org.apache.sandesha2.storage.beans.SenderBean;
import org.apache.sandesha2.wsrm.AcknowledgementRange;
import org.apache.sandesha2.wsrm.Sequence;
import org.apache.sandesha2.wsrm.SequenceAcknowledgement;

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
		
		ConfigurationContext configurationContext = rmMessageContext.getConfigurationContext();
		SenderBeanMgr retransmitterBeanMgr = storageManager.getSenderBeanMgr();

		// If this message is going to an anonymous address then we add in an ack for the
		// sequence that was used on the inbound side.
		EndpointReference target = rmMessageContext.getTo();
		if(target.hasAnonymousAddress()) {
			Sequence sequence = (Sequence) rmMessageContext.getMessagePart(Sandesha2Constants.MessageParts.SEQUENCE);
			if(sequence != null) {
				String outboundSequenceId = sequence.getIdentifier().getIdentifier();
				RMSBean rmsBean = SandeshaUtil.getRMSBeanFromSequenceId(storageManager, outboundSequenceId);
				String outboundInternalSeq = rmsBean.getInternalSequenceID();
				String inboundSequenceId = SandeshaUtil.getServerSideIncomingSeqIdFromInternalSeqId(outboundInternalSeq);
				
				boolean validSequence = SequenceManager.isValidIncomingSequence (inboundSequenceId,storageManager);
				if (validSequence) {
					if(log.isDebugEnabled()) log.debug("Piggybacking ack for " + inboundSequenceId);
					RMMsgCreator.addAckMessage(rmMessageContext, rmsBean, inboundSequenceId, storageManager);
				}
			}
			if(log.isDebugEnabled()) log.debug("Exit: AcknowledgementManager::piggybackAcksIfPresent, anon");
			return;
		}
		
		SenderBean findBean = new SenderBean();
		findBean.setMessageType(Sandesha2Constants.MessageTypes.ACK);
		findBean.setSend(true);
		findBean.setToAddress(target.getAddress());

		Collection collection = retransmitterBeanMgr.find(findBean);
		Iterator it = collection.iterator();

		while (it.hasNext()) {
			SenderBean ackBean = (SenderBean) it.next();

			long timeNow = System.currentTimeMillis();
			if (ackBean.getTimeToSend() > timeNow) {
				// //Piggybacking will happen only if the end of ack interval
				// (timeToSend) is not reached.

				MessageContext ackMsgContext = storageManager.retrieveMessageContext(ackBean.getMessageContextRefKey(),
						configurationContext);

				if (log.isDebugEnabled()) log.debug("Adding ack headers");

				// deleting the ack entry.
				retransmitterBeanMgr.delete(ackBean.getMessageID());

				// Adding the ack(s) to the application message
				boolean acks = false;
				SOAPHeader appMsgHeaders = rmMessageContext.getMessageContext().getEnvelope().getHeader();
				
				SOAPHeader headers = ackMsgContext.getEnvelope().getHeader();
				if(headers != null) {
					for(int i = 0; i < Sandesha2Constants.SPEC_NS_URIS.length; i++) {

						QName name = new QName(Sandesha2Constants.SPEC_NS_URIS[i], Sandesha2Constants.WSRM_COMMON.SEQUENCE_ACK);
						Iterator iter = headers.getChildrenWithName(name);
						while(iter.hasNext()) {
							OMElement ackElement = (OMElement) iter.next();

							SequenceAcknowledgement sequenceAcknowledgement = new SequenceAcknowledgement (Sandesha2Constants.SPEC_NS_URIS[i]);
							sequenceAcknowledgement.fromOMElement(ackElement);
							
							sequenceAcknowledgement.toOMElement(appMsgHeaders);
							acks = true;
						}
					}
				}
				
				if (!acks) {
					String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.invalidAckMessageEntry,
							ackMsgContext.getEnvelope().toString());
					log.debug(message);
					throw new SandeshaException(message);
				}
			}
		}

		if (log.isDebugEnabled())
			log.debug("Exit: AcknowledgementManager::piggybackAcksIfPresent");
	}

	/**
	 * this is used to get the acked messages of a sequence. If this is an
	 * outgoing message the sequenceIdentifier should be the internal
	 * sequenceID.
	 * 
	 * @param sequenceIdentifier
	 * @param outGoingMessage
	 * @return
	 */
	public static RangeString getClientCompletedMessageRanges(String internalSequenceID, String sequenceID, StorageManager storageManager)
			throws SandeshaException {
		if (log.isDebugEnabled())
			log.debug("Enter: AcknowledgementManager::getClientCompletedMessageRanges " + internalSequenceID + ", " + sequenceID);
    
		RMSBean rmsBean = SandeshaUtil.getRMSBeanFromSequenceId(storageManager, sequenceID);

		if (rmsBean == null) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.completedMsgBeanIsNull, sequenceID);
			SandeshaException e = new SandeshaException(message);
			if(log.isDebugEnabled()) log.debug("Throwing exception", e);
			throw e;
		}
		
		RangeString completedMsgRanges = rmsBean.getClientCompletedMessages();

		if (log.isDebugEnabled())
			log.debug("Exit: AcknowledgementManager::getClientCompletedMessageRanges");
		return completedMsgRanges;
	}

	public static RangeString getServerCompletedMessageRanges(String sequenceID, StorageManager storageManager)
			throws SandeshaException {
		if (log.isDebugEnabled())
			log.debug("Enter: AcknowledgementManager::getServerCompletedMessageRanges " + sequenceID);

		RMDBean rmdBean = SandeshaUtil.getRMDBeanFromSequenceId(storageManager, sequenceID);
		
		if (rmdBean.getServerCompletedMessages() == null) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.completedMsgBeanIsNull, sequenceID);
			SandeshaException e = new SandeshaException(message);
			if(log.isDebugEnabled()) log.debug("Throwing exception", e);
			throw e;
		}

		if (log.isDebugEnabled())
			log.debug("Exit: AcknowledgementManager::getServerCompletedMessageRanges");
		return rmdBean.getServerCompletedMessages();
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
			String sequenceId,
			StorageManager storageManager, 
			boolean makeResponse,
			boolean serverSide
			
			) throws AxisFault {
		
		if (log.isDebugEnabled())
			log.debug("Enter: AcknowledgementManager::generateAckMessage");

		MessageContext referenceMsg = referenceRMMessage.getMessageContext();

		RMDBeanMgr rmdBeanMgr = storageManager.getRMDBeanMgr();
		RMDBean findBean = new RMDBean ();
		findBean.setSequenceID(sequenceId);
		RMDBean rmdBean = rmdBeanMgr.findUnique(findBean);

		EndpointReference acksTo = new EndpointReference(rmdBean.getAcksToEPR());
		String acksToStr = acksTo.getAddress();

		if (acksToStr == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(SandeshaMessageKeys.acksToStrNotSet));

		AxisOperation ackOperation = SpecSpecificConstants.getWSRMOperation(
				Sandesha2Constants.MessageTypes.ACK,
				referenceRMMessage.getRMSpecVersion(),
				referenceMsg.getAxisService());

		MessageContext ackMsgCtx = SandeshaUtil.createNewRelatedMessageContext(referenceRMMessage, ackOperation);
		if (makeResponse) {
			ackMsgCtx.setOperationContext(referenceMsg.getOperationContext());
		}
		
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
		RMMsgCreator.addAckMessage(ackRMMsgCtx, rmdBean ,sequenceId, storageManager);

		if (log.isDebugEnabled())
			log.debug("Exit: AcknowledgementManager::generateAckMessage");
		return ackRMMsgCtx;
	}

	
	

	public static boolean verifySequenceCompletion(Iterator ackRangesIterator, long lastMessageNo) {
		if (log.isDebugEnabled())
			log.debug("Enter: AcknowledgementManager::verifySequenceCompletion");

		HashMap startMap = new HashMap();

		while (ackRangesIterator.hasNext()) {
			AcknowledgementRange temp = (AcknowledgementRange) ackRangesIterator.next();
			startMap.put(new Long(temp.getLowerValue()), temp);
		}

		long start = 1;
		boolean result = false;
		while (!result) {
			AcknowledgementRange temp = (AcknowledgementRange) startMap.get(new Long(start));
			if (temp == null) {
				break;
			}

			if (temp.getUpperValue() >= lastMessageNo)
				result = true;

			start = temp.getUpperValue() + 1;
		}

		if (log.isDebugEnabled())
			log.debug("Enter: AcknowledgementManager::verifySequenceCompletion " + result);
		return result;
	}
	
	public static void addFinalAcknowledgement () {
		
	}
	
	public static void removeAckBeanEntries (String sequenceId, StorageManager storageManager) throws SandeshaException {
		SenderBean findBean = new SenderBean ();
		findBean.setSequenceID(sequenceId);
		findBean.setMessageType(Sandesha2Constants.MessageTypes.ACK);
		
		SenderBeanMgr senderBeanMgr = storageManager.getSenderBeanMgr();
		List senderBeans = senderBeanMgr.find(findBean);
		
		for (Iterator it = senderBeans.iterator();it.hasNext();) {
			SenderBean bean = (SenderBean) it.next();
			senderBeanMgr.delete(bean.getMessageID());
		}
		
	}
	
	public static void addAckBeanEntry (
			RMMsgContext ackRMMsgContext,
			String sequenceId, 
			long timeToSend,
			StorageManager storageManager) throws AxisFault {

		// / Transaction asyncAckTransaction =
		// storageManager.getTransaction();
		
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


		// Ack will be sent as stand alone, only after the retransmitter
		// interval.
//		long timeToSend = System.currentTimeMillis() + ackInterval;

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

		ackMsgContext.setProperty(Sandesha2Constants.QUALIFIED_FOR_SENDING, Sandesha2Constants.VALUE_FALSE);
		
		//asyncAckTransaction.commit();

		// passing the message through sandesha2sender
		ackMsgContext.setProperty(Sandesha2Constants.SET_SEND_TO_TRUE, Sandesha2Constants.VALUE_TRUE);
		
		SandeshaUtil.executeAndStore(ackRMMsgContext, key);

		// inserting the new ack.
		retransmitterBeanMgr.insert(ackBean);

		SandeshaUtil.startSenderForTheSequence(ackRMMsgContext.getConfigurationContext(), sequenceId);

	}
	
	public static void sendAckNow (RMMsgContext ackRMMsgContext) throws AxisFault {

		MessageContext ackMsgContext = ackRMMsgContext.getMessageContext();
		ConfigurationContext configContext = ackMsgContext.getConfigurationContext();
		
		// setting CONTEXT_WRITTEN since acksto is anonymous
		if (ackRMMsgContext.getMessageContext().getOperationContext() == null) {
			// operation context will be null when doing in a GLOBAL
			// handler.
			AxisOperation op = ackMsgContext.getAxisOperation();
			OperationContext opCtx = new OperationContext(op);
			ackRMMsgContext.getMessageContext().setOperationContext(opCtx);
		}

		ackRMMsgContext.getMessageContext().getOperationContext().setProperty(
				org.apache.axis2.Constants.RESPONSE_WRITTEN, Constants.VALUE_TRUE);

		ackRMMsgContext.getMessageContext().setProperty(Sandesha2Constants.ACK_WRITTEN, "true");

		ackRMMsgContext.getMessageContext().setServerSide(true);
		
		AxisEngine engine = new AxisEngine(configContext);
		engine.send(ackMsgContext);
		
	}

	
}
