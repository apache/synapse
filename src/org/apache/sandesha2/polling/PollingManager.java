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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.engine.AxisEngine;
import org.apache.sandesha2.RMMsgContext;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.storage.SandeshaStorageException;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.beanmanagers.CreateSeqBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.NextMsgBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.SenderBeanMgr;
import org.apache.sandesha2.storage.beans.CreateSeqBean;
import org.apache.sandesha2.storage.beans.NextMsgBean;
import org.apache.sandesha2.storage.beans.SenderBean;
import org.apache.sandesha2.transport.Sandesha2TransportOutDesc;
import org.apache.sandesha2.util.MsgInitializer;
import org.apache.sandesha2.util.RMMsgCreator;
import org.apache.sandesha2.util.SandeshaUtil;

/**
 * This class is responsible for sending MakeConnection requests. This is a seperate thread that
 * keeps running. Will do MakeConnection based on the request queue or randomly.
 */
public class PollingManager extends Thread {

	private ConfigurationContext configurationContext = null;
	private StorageManager storageManager = null;
	private boolean poll = false;
	/**
	 * By adding an entry to this, the PollingManager will be asked to do a polling request on this sequence.
	 */
	private ArrayList sheduledPollingRequests = null;
	
	private final int POLLING_MANAGER_WAIT_TIME = 5000;
	
	public void run() {
		
		while (isPoll()) {
			
			try {
				
				try {
					Thread.sleep(POLLING_MANAGER_WAIT_TIME);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				NextMsgBeanMgr nextMsgMgr = storageManager.getNextMsgBeanMgr();
				
				//geting the sequences to be polled.
				//if shedule contains any requests, do the earliest one.
				//else pick one randomly.
				
				String sequenceId = null;
				if (sheduledPollingRequests.size()>0) {
					sequenceId = (String )sheduledPollingRequests.get(0);
					sheduledPollingRequests.remove(0);
				}

				NextMsgBean nextMsgBean = null;
				
				if (sequenceId==null) {
					NextMsgBean findBean = new NextMsgBean ();
					findBean.setPollingMode(true);
					
					List results = nextMsgMgr.find(findBean);
					int size = results.size();
					if (size>0) {
						Random random = new Random ();
						int item = random.nextInt(size);
						nextMsgBean = (NextMsgBean) results.get(item);
					}
					
					sequenceId = nextMsgBean.getSequenceID();
				} else {
					NextMsgBean findBean = new NextMsgBean ();
					findBean.setPollingMode(true);
					findBean.setSequenceID(sequenceId);
					
					nextMsgBean = nextMsgMgr.findUnique(findBean);
				}
				
				//If not valid entry is found, try again later.
				if (nextMsgBean==null)
					continue;
				
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
				
				makeConnectionRMMessage.setProperty(MessageContext.TRANSPORT_IN,null);
				//storing the MakeConnection message.
				String makeConnectionMsgStoreKey = SandeshaUtil.getUUID();
				storageManager.storeMessageContext(makeConnectionMsgStoreKey,makeConnectionRMMessage.getMessageContext());
				
				//add an entry for the MakeConnection message to the sender (with ,send=true, resend=false)
				SenderBean makeConnectionSenderBean = new SenderBean ();
//				makeConnectionSenderBean.setInternalSequenceID(internalSequenceId);
				makeConnectionSenderBean.setMessageContextRefKey(makeConnectionMsgStoreKey);
				makeConnectionSenderBean.setMessageID(makeConnectionRMMessage.getMessageId());
				makeConnectionSenderBean.setMessageType(Sandesha2Constants.MessageTypes.MAKE_CONNECTION_MSG);
				makeConnectionSenderBean.setReSend(false);
				makeConnectionSenderBean.setSend(true);
				makeConnectionSenderBean.setSequenceID(sequenceId);
				EndpointReference to = makeConnectionRMMessage.getTo();
				if (to!=null)
					makeConnectionSenderBean.setToAddress(to.getAddress());

				SenderBeanMgr senderBeanMgr = storageManager.getRetransmitterBeanMgr();
				
				//this message should not be sent until it is qualified. I.e. till it is sent through the Sandesha2TransportSender.
				makeConnectionRMMessage.setProperty(Sandesha2Constants.QUALIFIED_FOR_SENDING, Sandesha2Constants.VALUE_FALSE);
				
				makeConnectionRMMessage.setProperty(Sandesha2Constants.MESSAGE_STORE_KEY, makeConnectionMsgStoreKey);
				
				senderBeanMgr.insert(makeConnectionSenderBean);
				
				TransportOutDescription transportOut = makeConnectionRMMessage.getMessageContext().getTransportOut();
				makeConnectionRMMessage.setProperty(Sandesha2Constants.ORIGINAL_TRANSPORT_OUT_DESC, transportOut);

				Sandesha2TransportOutDesc sandesha2TransportOutDesc = new Sandesha2TransportOutDesc();
				makeConnectionRMMessage.getMessageContext().setTransportOut(sandesha2TransportOutDesc);

				// sending the message once through Sandesha2TransportSender.
				AxisEngine engine = new AxisEngine(configurationContext);
				engine.resumeSend(makeConnectionRMMessage.getMessageContext());
				
			} catch (SandeshaStorageException e) {
				e.printStackTrace();
			} catch (SandeshaException e) {
				e.printStackTrace();
			} catch (AxisFault e) {
				e.printStackTrace();
			} finally {
				try {
					Thread.sleep(POLLING_MANAGER_WAIT_TIME);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

		}
	}
	
	/**
	 * Starts the PollingManager.
	 * 
	 * @param configurationContext
	 * @throws SandeshaException
	 */
	public synchronized void start (ConfigurationContext configurationContext) throws SandeshaException {
		this.configurationContext = configurationContext;
		this.sheduledPollingRequests = new ArrayList ();
		this.storageManager = SandeshaUtil.getSandeshaStorageManager(configurationContext,configurationContext.getAxisConfiguration());
		setPoll(true);
		super.start();
	}
	
	/**
	 * Asks the PollingManager to stop its work.
	 *
	 */
	public synchronized void stopPolling () {
		setPoll(false);
	}
	
	public synchronized void setPoll (boolean poll) {
		this.poll = poll;
	}
	
	public synchronized boolean isPoll () {
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
	public synchronized void shedulePollingRequest (String internalSequenceId) {
		if (!sheduledPollingRequests.contains(internalSequenceId))
			sheduledPollingRequests.add(internalSequenceId);
	}

	
}
