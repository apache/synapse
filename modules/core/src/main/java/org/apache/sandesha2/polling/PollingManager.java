/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.sandesha2.polling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.RMMsgContext;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.storage.SandeshaStorageException;
import org.apache.sandesha2.storage.Transaction;
import org.apache.sandesha2.storage.beanmanagers.RMDBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.RMSBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.SenderBeanMgr;
import org.apache.sandesha2.storage.beans.RMDBean;
import org.apache.sandesha2.storage.beans.RMSBean;
import org.apache.sandesha2.storage.beans.RMSequenceBean;
import org.apache.sandesha2.storage.beans.SenderBean;
import org.apache.sandesha2.util.AcknowledgementManager;
import org.apache.sandesha2.util.MsgInitializer;
import org.apache.sandesha2.util.RMMsgCreator;
import org.apache.sandesha2.util.SandeshaUtil;
import org.apache.sandesha2.workers.SandeshaThread;
import org.apache.sandesha2.workers.SequenceEntry;

/**
 * This class is responsible for sending MakeConnection requests. This is a seperate thread that
 * keeps running. Will do MakeConnection based on the request queue or randomly.
 */
public class PollingManager extends SandeshaThread {
	private static final Log log = LogFactory.getLog(PollingManager.class);

	// Variables used to help round-robin across the sequences that we can poll for 
	private int nextIndex = 0;

	/**
	 * By adding an entry to this, the PollingManager will be asked to do a polling request on this sequence.
	 */
	private LinkedList scheduledPollingRequests = new LinkedList();
	
	private static final int POLLING_MANAGER_WAIT_TIME = 3000;
	
	private HashMap pollTimes = new HashMap();
	
	public PollingManager() {
		super(POLLING_MANAGER_WAIT_TIME);
	}
	
	protected boolean internalRun() {
		if(log.isDebugEnabled()) log.debug("Enter: PollingManager::internalRun");
		Transaction t = null;
		try {
			// If we have request scheduled, handle them first, and then pick
			// pick a sequence using a round-robin approach. Scheduled polls
			// bypass the normal polling checks, to make sure that they happen
			boolean forcePoll = false;
			SequenceEntry entry = null;
			synchronized (this) {
				if(!scheduledPollingRequests.isEmpty()) {
					entry = (SequenceEntry) scheduledPollingRequests.removeFirst();
					forcePoll = true;
				}
			}
			if(entry == null) {
				ArrayList allSequencesList = getSequences();
				int size = allSequencesList.size();
				if(log.isDebugEnabled()) log.debug("Choosing one from " + size + " sequences");
				if(nextIndex >= size) {
					nextIndex = 0;
					// We just looped over the set of sequences, so sleep before we try
					// polling them again.
					if (log.isDebugEnabled()) log.debug("Exit: PollingManager::internalRun, looped over all sequences, sleeping");
					return true;
				}
	
				entry = (SequenceEntry) allSequencesList.get(nextIndex++);
				
				long now = System.currentTimeMillis();
				Long time = (Long) pollTimes.get(entry.getSequenceId());
				if(time != null && (now - time.longValue()) < POLLING_MANAGER_WAIT_TIME) {
					// We have polled for this sequence too recently, so skip over this one
					if (log.isDebugEnabled()) log.debug("Exit: PollingManager::internalRun, skipping sequence, not sleeping");
					return false;
				}
				pollTimes.put(entry.getSequenceId(), new Long(now));
				
			}
			if(log.isDebugEnabled()) log.debug("Chose sequence " + entry.getSequenceId());

			t = storageManager.getTransaction();
			if(entry.isRmSource()) {
				pollRMSSide(entry, forcePoll);
			} else {
				pollRMDSide(entry, forcePoll);
			}
			if(t != null) t.commit();
			t = null;

		} catch (Exception e) {
			if(log.isDebugEnabled()) log.debug("Exception", e);
		} finally {
			if(t != null && t.isActive()) {
				try {
					t.rollback();
				} catch(Exception e2) {
					if(log.isDebugEnabled()) log.debug("Exception during rollback", e2);
				}
			}
		}
		
		if(log.isDebugEnabled()) log.debug("Exit: PollingManager::internalRun, not sleeping");
		return false;
	}
	
	private void pollRMSSide(SequenceEntry entry, boolean force) throws AxisFault {
		if(log.isDebugEnabled()) log.debug("Enter: PollingManager::pollRMSSide, force: " + force);
		
		RMSBeanMgr rmsBeanManager = storageManager.getRMSBeanMgr();
		RMSBean findRMS = new RMSBean();
		findRMS.setInternalSequenceID(entry.getSequenceId());
		findRMS.setPollingMode(true);
		findRMS.setTerminated(false);
		RMSBean beanToPoll = rmsBeanManager.findUnique(findRMS);
		
		if(beanToPoll == null) {
			// This sequence must have been terminated, or deleted
			stopThreadForSequence(entry.getSequenceId(), true);
		} else {
			if (log.isDebugEnabled())
				log.debug("Polling rms " + beanToPoll);
			// The sequence is there, but we still only poll if we are expecting reply messages,
			// or if we don't have clean ack state. (We assume acks are clean, and only unset
			// this if we find evidence to the contrary).
			boolean cleanAcks = true;
			if (beanToPoll.getNextMessageNumber() > -1)
				cleanAcks = AcknowledgementManager.verifySequenceCompletion(beanToPoll.getClientCompletedMessages(), beanToPoll.getNextMessageNumber());
			long  repliesExpected = beanToPoll.getExpectedReplies();
			if(beanToPoll.getSequenceID() != null && (force || !cleanAcks || repliesExpected > 0) && beanToPoll.getReferenceMessageStoreKey() != null)
		            pollForSequence(beanToPoll.getAnonymousUUID(), beanToPoll.getInternalSequenceID(), beanToPoll.getReferenceMessageStoreKey(), beanToPoll, entry);
		}
		

		if(log.isDebugEnabled()) log.debug("Exit: PollingManager::pollRMSSide");
	}

	private void pollRMDSide(SequenceEntry entry, boolean force) throws AxisFault {
		if(log.isDebugEnabled()) log.debug("Enter: PollingManager::pollRMDSide, force: " + force);
		RMDBeanMgr nextMsgMgr = storageManager.getRMDBeanMgr();
		RMDBean findBean = new RMDBean();
		findBean.setPollingMode(true);
		findBean.setTerminated(false);
		findBean.setSequenceID(entry.getSequenceId());
		RMDBean nextMsgBean = nextMsgMgr.findUnique(findBean);
		
		if(nextMsgBean == null) {
			// This sequence must have been terminated, or deleted
			stopThreadForSequence(entry.getSequenceId(), false);
		} else {
			// The sequence is still there, but if we have a running related sequence
			// that is not expecting replies then there is no need to poll.
			boolean doPoll = true;
			String outboundSequence = nextMsgBean.getOutboundInternalSequence();
			if(outboundSequence != null) {
				RMSBean findRMS = new RMSBean();
				findRMS.setInternalSequenceID(outboundSequence);
				findRMS.setTerminated(false);
				RMSBeanMgr mgr = storageManager.getRMSBeanMgr();
				RMSBean outbound = mgr.findUnique(findRMS);
				if(outbound != null && outbound.getExpectedReplies() == 0) {
					doPoll = false;
				}
			}
			if(force || doPoll)
				pollForSequence(null, null, nextMsgBean.getReferenceMessageKey(), nextMsgBean, entry);
		}

		if(log.isDebugEnabled()) log.debug("Exit: PollingManager::pollRMDSide");
	}

	private void pollForSequence(String anonUUID,       // Only set for RMS polling
								 String internalSeqId,  // Only set for RMS polling
								 String referenceMsgKey,
								 RMSequenceBean rmBean,
								 SequenceEntry entry)
	throws SandeshaException, SandeshaStorageException, AxisFault
	{
		if(log.isDebugEnabled()) log.debug("Enter: PollingManager::pollForSequence, rmBean: " + rmBean);
		
		//create a MakeConnection message  
		EndpointReference replyTo = rmBean.getReplyToEndpointReference();
		String wireSeqId = null;
		String wireAddress = null;
		if(anonUUID != null) {
			// If we are polling on a RM anon URI then we don't want to include the sequence id
			// in the MakeConnection message.
			wireAddress = anonUUID;
		} else if(replyTo!=null && SandeshaUtil.isWSRMAnonymous(replyTo.getAddress())) {
			// If we are polling on a RM anon URI then we don't want to include the sequence id
			// in the MakeConnection message.
			wireAddress = replyTo.getAddress();
		} else {
			wireSeqId = rmBean.getSequenceID(); //this case could make us non-RSP compliant
		}
		
		if(log.isDebugEnabled()) log.debug("Debug: PollingManager::pollForSequence, wireAddress=" + wireAddress + ", wireSeqId=" + wireSeqId);
		
		MessageContext referenceMessage = storageManager.retrieveMessageContext(referenceMsgKey,context);
		if(referenceMessage!=null){
			RMMsgContext referenceRMMessage = MsgInitializer.initializeMessage(referenceMessage);
			RMMsgContext makeConnectionRMMessage = RMMsgCreator.createMakeConnectionMessage(referenceRMMessage,
					rmBean, wireSeqId, wireAddress, storageManager);
			
			
			//we must set serverSide to false. Having serverSide as true (I.e. when polling for RMD) will cause the SenderWorker to ignore
			//the sync response message.
			makeConnectionRMMessage.getMessageContext().setServerSide(false);
			
			// Store properties so that we know which sequence we are polling for. This can be used
			// to match reply sequences up to requests, as well as to help process messagePending
			// headers.
			OperationContext ctx = makeConnectionRMMessage.getMessageContext().getOperationContext();
			ctx.setProperty(Sandesha2Constants.MessageContextProperties.MAKECONNECTION_ENTRY, entry);
			
			makeConnectionRMMessage.setProperty(MessageContext.TRANSPORT_IN,null);
			//storing the MakeConnection message.
			String makeConnectionMsgStoreKey = SandeshaUtil.getUUID();
			
			//add an entry for the MakeConnection message to the sender (with ,send=true, resend=false)
			SenderBean makeConnectionSenderBean = new SenderBean ();
			makeConnectionSenderBean.setInternalSequenceID(internalSeqId);
			makeConnectionSenderBean.setMessageContextRefKey(makeConnectionMsgStoreKey);
			makeConnectionSenderBean.setMessageID(makeConnectionRMMessage.getMessageId());
			makeConnectionSenderBean.setMessageType(Sandesha2Constants.MessageTypes.MAKE_CONNECTION_MSG);
			makeConnectionSenderBean.setReSend(false);
			makeConnectionSenderBean.setSend(true);
			makeConnectionSenderBean.setSequenceID(rmBean.getSequenceID());
			EndpointReference to = makeConnectionRMMessage.getTo();
			if (to!=null)
				makeConnectionSenderBean.setToAddress(to.getAddress());

			SenderBeanMgr senderBeanMgr = storageManager.getSenderBeanMgr();
			
			//this message should not be sent until it is qualified. I.e. till it is sent through the Sandesha2TransportSender.
			makeConnectionRMMessage.setProperty(Sandesha2Constants.QUALIFIED_FOR_SENDING, Sandesha2Constants.VALUE_FALSE);
			
	        SandeshaUtil.executeAndStore(makeConnectionRMMessage, makeConnectionMsgStoreKey, storageManager);
			
			senderBeanMgr.insert(makeConnectionSenderBean);			
		}
		
		if(log.isDebugEnabled()) log.debug("Exit: PollingManager::pollForSequence");
	}
	
	/**
	 * Asking the polling manager to do a polling request on the sequence identified by the
	 * given InternalSequenceId.
	 * 
	 * @param sequenceId
	 */
	public synchronized void schedulePollingRequest(String sequenceId, boolean rmSource) {
		if(log.isDebugEnabled()) log.debug("Enter: PollingManager::shedulePollingRequest, " + sequenceId);
		
		SequenceEntry entry = new SequenceEntry(sequenceId, rmSource);
		scheduledPollingRequests.add(entry);
		this.wakeThread();
		
		if(log.isDebugEnabled()) log.debug("Exit: PollingManager::shedulePollingRequest");
	}
	
}
