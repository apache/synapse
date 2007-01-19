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

package org.apache.sandesha2.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.RMMsgContext;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.beanmanagers.InvokerBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.RMDBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.SenderBeanMgr;
import org.apache.sandesha2.storage.beans.RMSBean;
import org.apache.sandesha2.storage.beans.InvokerBean;
import org.apache.sandesha2.storage.beans.RMDBean;
import org.apache.sandesha2.storage.beans.SenderBean;

/**
 * Contains logic to remove all the storad data of a sequence. Methods of this
 * are called by sending side and the receiving side when appropriate
 */

public class TerminateManager {

	private static Log log = LogFactory.getLog(TerminateManager.class);

	private static String CLEANED_ON_TERMINATE_MSG = "CleanedOnTerminateMsg";

	private static String CLEANED_AFTER_INVOCATION = "CleanedAfterInvocation";

	public static HashMap receivingSideCleanMap = new HashMap();

	/**
	 * Called by the receiving side to remove data related to a sequence. e.g.
	 * After sending the TerminateSequence message. Calling this methods will
	 * complete all the data if InOrder invocation is not sequired.
	 * 
	 * @param configContext
	 * @param sequenceID
	 * @throws SandeshaException
	 */
	public static void cleanReceivingSideOnTerminateMessage(ConfigurationContext configContext, String sequencePropertyKey ,String sequenceId,
			StorageManager storageManager) throws SandeshaException {

		// clean senderMap

		//removing any un-sent ack messages.
		SenderBean findAckBean = new SenderBean ();
		findAckBean.setSequenceID(sequenceId);
		findAckBean.setMessageType(Sandesha2Constants.MessageTypes.ACK);
		
		SenderBeanMgr senderBeanMgr = storageManager.getSenderBeanMgr();
		Iterator ackBeans = senderBeanMgr.find(findAckBean).iterator();
		while (ackBeans.hasNext()) {
			SenderBean ackBean = (SenderBean) ackBeans.next();
			senderBeanMgr.delete(ackBean.getMessageID());
		}
		
		// Currently in-order invocation is done for default values.
		boolean inOrderInvocation = SandeshaUtil.getDefaultPropertyBean(configContext.getAxisConfiguration())
				.isInOrder();

		if (!inOrderInvocation) {
			// there is no invoking by Sandesha2. So clean invocations storages.
			
			receivingSideCleanMap.put(sequenceId, CLEANED_ON_TERMINATE_MSG);
			cleanReceivingSideAfterInvocation(configContext, sequencePropertyKey, sequenceId, storageManager);
		} else {

			String cleanStatus = (String) receivingSideCleanMap.get(sequenceId);
			if (cleanStatus != null
					&& CLEANED_AFTER_INVOCATION.equals(cleanStatus))
				completeTerminationOfReceivingSide(configContext,
						sequencePropertyKey, sequenceId, storageManager);
			else
				receivingSideCleanMap.put(sequenceId, CLEANED_ON_TERMINATE_MSG);
		}
	}

	/**
	 * When InOrder invocation is anabled this had to be called to clean the
	 * data left by the above method. This had to be called after the Invocation
	 * of the Last Message.
	 * 
	 * @param configContext
	 * @param sequenceID
	 * @throws SandeshaException
	 */
	public static void cleanReceivingSideAfterInvocation(ConfigurationContext configContext, String sequencePropertyKey ,String sequenceId,
			StorageManager storageManager) throws SandeshaException {
		InvokerBeanMgr storageMapBeanMgr = storageManager.getInvokerBeanMgr();

		// removing storageMap entries
		InvokerBean findStorageMapBean = new InvokerBean();
		findStorageMapBean.setSequenceID(sequenceId);
		findStorageMapBean.setInvoked(true);
		Collection collection = storageMapBeanMgr.find(findStorageMapBean);
		Iterator iterator = collection.iterator();
		while (iterator.hasNext()) {
			InvokerBean storageMapBean = (InvokerBean) iterator.next();
			storageMapBeanMgr.delete(storageMapBean.getMessageContextRefKey());

			// removing the respective message context from the message store.
			// If this is an in-only message.
			// In-out message will be deleted when a ack is retrieved for the
			// out message.
			String messageStoreKey = storageMapBean.getMessageContextRefKey();
			storageManager.removeMessageContext(messageStoreKey);

		}

		String cleanStatus = (String) receivingSideCleanMap.get(sequenceId);
		if (cleanStatus != null && CLEANED_ON_TERMINATE_MSG.equals(cleanStatus))
			completeTerminationOfReceivingSide(configContext, sequencePropertyKey, sequenceId, storageManager);
		else {
			receivingSideCleanMap.put(sequenceId, CLEANED_AFTER_INVOCATION);
		}
	}

	/**
	 * This has to be called by the lastly invocated one of the above two
	 * methods.
	 * 
	 */
	private static void completeTerminationOfReceivingSide(ConfigurationContext configContext, String sequencePropertyKey,String sequenceId,
			StorageManager storageManager) throws SandeshaException {
		
		// removing nextMsgMgr entries
		RMDBeanMgr rMDBeanMgr = storageManager.getRMDBeanMgr();
		RMDBean findNextMsgBean = new RMDBean();
		findNextMsgBean.setSequenceID(sequenceId);
		Collection collection = rMDBeanMgr.find(findNextMsgBean);
		Iterator iterator = collection.iterator();
		while (iterator.hasNext()) {
			RMDBean rMDBean = (RMDBean) iterator.next();
			 rMDBeanMgr.delete(rMDBean.getSequenceID());
		}

	}

	/**
	 * This is called by the sending side to clean data related to a sequence.
	 * e.g. after sending the TerminateSequence message.
	 * 
	 * @param configContext
	 * @param sequenceID
	 * @throws SandeshaException
	 */
	public static void terminateSendingSide(RMSBean rmsBean,
			boolean serverSide, StorageManager storageManager) throws SandeshaException {

		// Indicate that the sequence is terminated
		rmsBean.setTerminated(true);		
		storageManager.getRMSBeanMgr().update(rmsBean);
		
		cleanSendingSideData (rmsBean.getSequenceID(), rmsBean.getInternalSequenceID(), serverSide, storageManager);
	}

	public static void timeOutSendingSideSequence(String sequencePropertyKey,String internalSequenceId,
			boolean serverside, StorageManager storageManager) throws SandeshaException {

		RMSBean rmsBean = SandeshaUtil.getRMSBeanFromInternalSequenceId(storageManager, internalSequenceId);
		rmsBean.setTimedOut(true);
		storageManager.getRMSBeanMgr().update(rmsBean);

		cleanSendingSideData(sequencePropertyKey,internalSequenceId, serverside, storageManager);
	}

	private static void cleanSendingSideData(String sequencePropertyKey,String internalSequenceId,
			boolean serverSide, StorageManager storageManager) throws SandeshaException {

		SenderBeanMgr retransmitterBeanMgr = storageManager.getSenderBeanMgr();

		// removing retransmitterMgr entries and corresponding message contexts.
		Collection collection = retransmitterBeanMgr.find(internalSequenceId);
		Iterator iterator = collection.iterator();
		while (iterator.hasNext()) {
			SenderBean retransmitterBean = (SenderBean) iterator.next();
			retransmitterBeanMgr.delete(retransmitterBean.getMessageID());

			String messageStoreKey = retransmitterBean.getMessageContextRefKey();
			storageManager.removeMessageContext(messageStoreKey);
		}
	}

	public static void addTerminateSequenceMessage(RMMsgContext referenceMessage, String internalSequenceID, String outSequenceId, StorageManager storageManager) throws AxisFault {
	
		if(log.isDebugEnabled())
			log.debug("Enter: TerminateManager::addTerminateSequenceMessage " + outSequenceId + ", " + internalSequenceID);

		RMSBean rmsBean = SandeshaUtil.getRMSBeanFromInternalSequenceId(storageManager, internalSequenceID);

		if (rmsBean.isTerminateAdded()) {
			if(log.isDebugEnabled())
				log.debug("Exit: TerminateManager::addTerminateSequenceMessage - terminate was added previously.");
			return;
		}

		RMMsgContext terminateRMMessage = RMMsgCreator.createTerminateSequenceMessage(referenceMessage, rmsBean, storageManager);
		terminateRMMessage.setFlow(MessageContext.OUT_FLOW);
		terminateRMMessage.setProperty(Sandesha2Constants.APPLICATION_PROCESSING_DONE, "true");

		
		//setting the To EPR.
		//First try to get it from an Endpoint property.
		//If not get it from the To property.
		
		EndpointReference toEPR = null;
		
		if (rmsBean.getOfferedEndPoint() != null)
			toEPR = new EndpointReference (rmsBean.getOfferedEndPoint());
		
		if (toEPR==null) {

			if (rmsBean.getToEPR()!=null) {
				toEPR = new EndpointReference(rmsBean.getToEPR());
				if (toEPR == null) {
					String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.toEPRNotValid, null);
					throw new SandeshaException(message);
				}
			}
		}

		if (toEPR!=null)
			terminateRMMessage.setTo(toEPR);
		
		if (rmsBean.getReplyToEPR()!=null) {
			terminateRMMessage.setReplyTo(new EndpointReference (rmsBean.getReplyToEPR()));
		}
		
		String rmVersion = rmsBean.getRMVersion();
		terminateRMMessage.setWSAAction(SpecSpecificConstants.getTerminateSequenceAction(rmVersion));
		terminateRMMessage.setSOAPAction(SpecSpecificConstants.getTerminateSequenceSOAPAction(rmVersion));

		if (rmsBean.getTransportTo() != null) {
			terminateRMMessage.setProperty(Constants.Configuration.TRANSPORT_URL, rmsBean.getTransportTo());
		}

		terminateRMMessage.addSOAPEnvelope();

		String key = SandeshaUtil.getUUID();

		SenderBean terminateBean = new SenderBean();
		terminateBean.setInternalSequenceID(internalSequenceID);
		terminateBean.setSequenceID(outSequenceId);
		terminateBean.setMessageContextRefKey(key);
		terminateBean.setMessageType(Sandesha2Constants.MessageTypes.TERMINATE_SEQ);

		// Set a retransmitter lastSentTime so that terminate will be send with
		// some delay.
		// Otherwise this get send before return of the current request (ack).
		// TODO: refine the terminate delay.
		terminateBean.setTimeToSend(System.currentTimeMillis() + Sandesha2Constants.TERMINATE_DELAY);

		terminateBean.setMessageID(terminateRMMessage.getMessageId());

		// this will be set to true at the sender.
		terminateBean.setSend(true);

		terminateRMMessage.getMessageContext().setProperty(Sandesha2Constants.QUALIFIED_FOR_SENDING,
				Sandesha2Constants.VALUE_FALSE);

		terminateBean.setReSend(false);
		
		terminateBean.setSequenceID(outSequenceId);
		
		terminateBean.setMessageType(Sandesha2Constants.MessageTypes.TERMINATE_SEQ);
		terminateBean.setInternalSequenceID(internalSequenceID);
		
		
		EndpointReference to = terminateRMMessage.getTo();
		if (to!=null)
			terminateBean.setToAddress(to.getAddress());

		rmsBean.setTerminateAdded(true);

		storageManager.getRMSBeanMgr().update(rmsBean);

		terminateRMMessage.setProperty(Sandesha2Constants.SET_SEND_TO_TRUE, Sandesha2Constants.VALUE_TRUE);
		
		//the propertyKey of the ackMessage will be the propertyKey for the terminate message as well.
//		terminateRMMessage.setProperty(Sandesha2Constants.MessageContextProperties.SEQUENCE_PROPERTY_KEY, sequencePropertyKey);
		
		// / addTerminateSeqTransaction.commit();
		SandeshaUtil.executeAndStore(terminateRMMessage, key);
		
		SenderBeanMgr retramsmitterMgr = storageManager.getSenderBeanMgr();
		
		
		retramsmitterMgr.insert(terminateBean);

		if(log.isDebugEnabled())
			log.debug("Exit: TerminateManager::addTerminateSequenceMessage");
	}

}
