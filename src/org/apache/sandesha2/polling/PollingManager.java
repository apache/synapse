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

package org.apache.sandesha2.polling;

import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.RMMsgContext;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.Transaction;
import org.apache.sandesha2.storage.beanmanagers.RMDBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.SenderBeanMgr;
import org.apache.sandesha2.storage.beans.RMDBean;
import org.apache.sandesha2.storage.beans.SenderBean;
import org.apache.sandesha2.util.MsgInitializer;
import org.apache.sandesha2.util.RMMsgCreator;
import org.apache.sandesha2.util.SandeshaUtil;

/**
 * This class is responsible for sending MakeConnection requests. This is a seperate thread that
 * keeps running. Will do MakeConnection based on the request queue or randomly.
 */
public class PollingManager extends Thread {
	private static final Log log = LogFactory.getLog(PollingManager.class);

	private ConfigurationContext configurationContext = null;
	private StorageManager storageManager = null;
	private boolean poll = false;
	/**
	 * By adding an entry to this, the PollingManager will be asked to do a polling request on this sequence.
	 */
	private HashMap sheduledPollingRequests = null;
	
	private final int POLLING_MANAGER_WAIT_TIME = 3000;
	
	public void run() {
		while (isPoll()) {
			Transaction t = null;
			try {
				t = storageManager.getTransaction();
				internalRun();
				t.commit();
				t = null;
			} catch (Exception e) {
				if(log.isDebugEnabled()) log.debug("Exception", e);
				if(t != null) {
					try {
						t.rollback();
					} catch(Exception e2) {
						if(log.isDebugEnabled()) log.debug("Exception during rollback", e);
					}
					t = null;
				}
			}
			try {
				Thread.sleep(POLLING_MANAGER_WAIT_TIME);
			} catch (InterruptedException e) {
				if(log.isDebugEnabled()) log.debug("Sleep was interrupted", e);
			}
		}
	}
	
	private void internalRun() throws AxisFault {
		RMDBeanMgr nextMsgMgr = storageManager.getRMDBeanMgr();
		
		//geting the sequences to be polled.
		//if shedule contains any requests, do the earliest one.
		//else pick one randomly.
		
		String sequenceId = getNextSheduleEntry ();
		RMDBean nextMsgBean = null;

		RMDBean findBean = new RMDBean();
		findBean.setPollingMode(true);
		findBean.setSequenceID(sequenceId); // Note that this may be null
		List results = nextMsgMgr.find(findBean);
		int size = results.size();
		if (size>0) {
			Random random = new Random ();
			int item = random.nextInt(size);
			nextMsgBean = (RMDBean) results.get(item);
		}
		
		//If not valid entry is found, try again later.
		if (nextMsgBean==null) {
			if(log.isDebugEnabled()) log.debug("No polling requests queued");
			return;
		}
		sequenceId = nextMsgBean.getSequenceID();
		
		if(log.isDebugEnabled()) log.debug("Polling for sequence " + sequenceId);

		//create a MakeConnection message  
		String referenceMsgKey = nextMsgBean.getReferenceMessageKey();
		
		String sequencePropertyKey = sequenceId;
		String replyTo = SandeshaUtil.getSequenceProperty(sequencePropertyKey,
				Sandesha2Constants.SequenceProperties.REPLY_TO_EPR,storageManager);
		String WSRMAnonReplyToURI = null;
		if (SandeshaUtil.isWSRMAnonymousReplyTo(replyTo))
			WSRMAnonReplyToURI = replyTo;
		
		MessageContext referenceMessage = storageManager.retrieveMessageContext(referenceMsgKey,configurationContext);
		RMMsgContext referenceRMMessage = MsgInitializer.initializeMessage(referenceMessage);
		RMMsgContext makeConnectionRMMessage = RMMsgCreator.createMakeConnectionMessage(referenceRMMessage,
				sequenceId , WSRMAnonReplyToURI,storageManager);
		
		// Put our transaction onto the message context
		makeConnectionRMMessage.setProperty(Sandesha2Constants.WITHIN_TRANSACTION, Sandesha2Constants.VALUE_TRUE);
		
		makeConnectionRMMessage.setProperty(MessageContext.TRANSPORT_IN,null);
		//storing the MakeConnection message.
		String makeConnectionMsgStoreKey = SandeshaUtil.getUUID();
		
		makeConnectionRMMessage.setProperty(Sandesha2Constants.MessageContextProperties.SEQUENCE_PROPERTY_KEY,
				sequencePropertyKey);
		
		//add an entry for the MakeConnection message to the sender (with ,send=true, resend=false)
		SenderBean makeConnectionSenderBean = new SenderBean ();
//		makeConnectionSenderBean.setInternalSequenceID(internalSequenceId);
		makeConnectionSenderBean.setMessageContextRefKey(makeConnectionMsgStoreKey);
		makeConnectionSenderBean.setMessageID(makeConnectionRMMessage.getMessageId());
		makeConnectionSenderBean.setMessageType(Sandesha2Constants.MessageTypes.MAKE_CONNECTION_MSG);
		makeConnectionSenderBean.setReSend(false);
		makeConnectionSenderBean.setSend(true);
		makeConnectionSenderBean.setSequenceID(sequenceId);
		EndpointReference to = makeConnectionRMMessage.getTo();
		if (to!=null)
			makeConnectionSenderBean.setToAddress(to.getAddress());

		SenderBeanMgr senderBeanMgr = storageManager.getSenderBeanMgr();
		
		//this message should not be sent until it is qualified. I.e. till it is sent through the Sandesha2TransportSender.
		makeConnectionRMMessage.setProperty(Sandesha2Constants.QUALIFIED_FOR_SENDING, Sandesha2Constants.VALUE_FALSE);
		
		SandeshaUtil.executeAndStore(makeConnectionRMMessage, makeConnectionMsgStoreKey);
		
		senderBeanMgr.insert(makeConnectionSenderBean);				
	}
	
	private synchronized String getNextSheduleEntry () {
		if(log.isDebugEnabled()) log.debug("Entry: PollingManager::getNextSheduleEntry");
		String sequenceId = null;
		
		if (sheduledPollingRequests.size()>0) {
			sequenceId = (String) sheduledPollingRequests.keySet().iterator().next();
			Integer sequencEntryCount = (Integer) sheduledPollingRequests.get(sequenceId);
			
			Integer leftCount = new Integer (sequencEntryCount.intValue() -1 );
			if (leftCount.intValue()==0) 
				sheduledPollingRequests.remove(sequenceId);
			
		}
		
		if(log.isDebugEnabled()) log.debug("Exit: PollingManager::getNextSheduleEntry, " + sequenceId);
		return sequenceId;
	}
	
	/**
	 * Starts the PollingManager.
	 * 
	 * @param configurationContext
	 * @throws SandeshaException
	 */
	public synchronized void start (ConfigurationContext configurationContext) throws SandeshaException {
		if(log.isDebugEnabled()) log.debug("Entry: PollingManager::start");

		this.configurationContext = configurationContext;
		this.sheduledPollingRequests = new HashMap ();
		this.storageManager = SandeshaUtil.getSandeshaStorageManager(configurationContext,configurationContext.getAxisConfiguration());
		setPoll(true);
		super.start();
		
		if(log.isDebugEnabled()) log.debug("Exit: PollingManager::start");
	}
	
	/**
	 * Asks the PollingManager to stop its work.
	 *
	 */
	public synchronized void stopPolling () {
		if(log.isDebugEnabled()) log.debug("Entry: PollingManager::stopPolling");
		setPoll(false);
		if(log.isDebugEnabled()) log.debug("Exit: PollingManager::stopPolling");
	}
	
	public synchronized void setPoll (boolean poll) {
		if(log.isDebugEnabled()) log.debug("Entry: PollingManager::setPoll");
		this.poll = poll;
		if(log.isDebugEnabled()) log.debug("Exit: PollingManager::setPoll");
	}
	
	public synchronized boolean isPoll () {
		if(log.isDebugEnabled()) log.debug("Entry: PollingManager::isPoll");
		if(log.isDebugEnabled()) log.debug("Exit: PollingManager::isPoll");
		return poll;
	}
	
	public void start () {
		throw new UnsupportedOperationException ("You must use the oveerloaded start method");
	}
	
	/**
	 * Asking the polling manager to do a polling request on the sequence identified by the
	 * given InternalSequenceId.
	 * 
	 * @param sequenceId
	 */
	public synchronized void shedulePollingRequest (String sequenceId) {
		if(log.isDebugEnabled()) log.debug("Entry: PollingManager::shedulePollingRequest, " + sequenceId);
		
		if (sheduledPollingRequests.containsKey (sequenceId)) {
			Integer sequenceEntryCount = (Integer) sheduledPollingRequests.get(sequenceId);
			Integer newCount = new Integer (sequenceEntryCount.intValue()+1);
			sheduledPollingRequests.put(sequenceId,newCount);
		} else {
			Integer sequenceEntryCount = new Integer (1);
			sheduledPollingRequests.put(sequenceId, sequenceEntryCount);
		}
		
		if(log.isDebugEnabled()) log.debug("Exit: PollingManager::shedulePollingRequest");
	}
}
