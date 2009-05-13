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

package org.apache.sandesha2.msgprocessors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.transport.RequestResponseTransport;
import org.apache.axis2.transport.TransportUtils;
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
import org.apache.sandesha2.storage.Transaction;
import org.apache.sandesha2.storage.beanmanagers.SenderBeanMgr;
import org.apache.sandesha2.storage.beans.RMDBean;
import org.apache.sandesha2.storage.beans.RMSBean;
import org.apache.sandesha2.storage.beans.RMSequenceBean;
import org.apache.sandesha2.storage.beans.SenderBean;
import org.apache.sandesha2.util.FaultManager;
import org.apache.sandesha2.util.LoggingControl;
import org.apache.sandesha2.util.MsgInitializer;
import org.apache.sandesha2.util.SOAPAbstractFactory;
import org.apache.sandesha2.util.SandeshaUtil;
import org.apache.sandesha2.util.SpecSpecificConstants;
import org.apache.sandesha2.workers.SandeshaThread;
import org.apache.sandesha2.workers.SenderWorker;
import org.apache.sandesha2.workers.WorkerLock;
import org.apache.sandesha2.wsrm.Identifier;
import org.apache.sandesha2.wsrm.MakeConnection;
import org.apache.sandesha2.wsrm.MessagePending;

/**
 * This class is responsible for processing MakeConnection request messages that come to the system.
 * MakeConnection is only supported by WSRM 1.1
 * Here a client can ask for reply messages using a polling mechanism, so even clients without real
 * endpoints can ask for reliable response messages.
 */
public class MakeConnectionProcessor implements MsgProcessor {

	private static final Log log = LogFactory.getLog(MakeConnectionProcessor.class);

	/**
	 * Prosesses incoming MakeConnection request messages.
	 * A message is selected by the set of SenderBeans that are waiting to be sent.
	 * This is processed using a SenderWorker.
	 */
	public boolean processInMessage(RMMsgContext rmMsgCtx, Transaction transaction) throws AxisFault {
		if (LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled())
			log.debug("Enter: MakeConnectionProcessor::processInMessage " + rmMsgCtx.getSOAPEnvelope().getBody());

		try {

			MakeConnection makeConnection = rmMsgCtx.getMakeConnection();

			String address = makeConnection.getAddress();
			Identifier identifier = makeConnection.getIdentifier();

			// If there is no address or identifier - make the MissingSelection Fault.
			if (address == null && identifier == null)
				FaultManager.makeMissingSelectionFault(rmMsgCtx);

			if (makeConnection.getUnexpectedElement() != null)
				FaultManager.makeUnsupportedSelectionFault(rmMsgCtx, makeConnection.getUnexpectedElement());

			//some initial setup
			ConfigurationContext configurationContext = rmMsgCtx.getConfigurationContext();
			StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(configurationContext, configurationContext.getAxisConfiguration());
			SecurityManager secManager = SandeshaUtil.getSecurityManager(configurationContext);
			SecurityToken token = secManager.getSecurityToken(rmMsgCtx.getMessageContext());

			//we want to find valid sender beans
			List<RMSequenceBean> possibleBeans = new ArrayList<RMSequenceBean>();
			int possibleBeanIndex = -10;
			SenderBean findSenderBean = new SenderBean();
			boolean secured = false;
			if (token != null && identifier == null) {
				secured = true;
				if (LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled()) log.debug("token found " + token);
				//this means we have to scope our search for sender beans that belong to sequences that own the same token
				String data = secManager.getTokenRecoveryData(token);
				//first look for RMS beans
				RMSBean finderRMS = new RMSBean();
				finderRMS.setSecurityTokenData(data);
				finderRMS.setToEPR(address);
				List<RMSBean> tempList2 = storageManager.getRMSBeanMgr().find(finderRMS);
				possibleBeans.addAll(tempList2);

				//try looking for RMD beans too
				RMDBean finderRMD = new RMDBean();
				finderRMD.setSecurityTokenData(data);
				finderRMD.setToAddress(address);
				List<RMDBean> tempList = storageManager.getRMDBeanMgr().find(finderRMD);

				//combine these two into one list
				possibleBeans.addAll(tempList);

				int size = possibleBeans.size();

				if (size > 0) {
					//select one at random: TODO better method?
					Random random = new Random();
					possibleBeanIndex = random.nextInt(size);
					RMSequenceBean selectedSequence = (RMSequenceBean) possibleBeans.get(possibleBeanIndex);
					findSenderBean.setSequenceID(selectedSequence.getSequenceID());
					if (LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled())
						log.debug("sequence selected " + findSenderBean.getSequenceID());
				} else {
					//we cannot match a RMD with the correct security credentials so we cannot process this msg under RSP
					if (LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled())
						log.debug("Exit: MakeConnectionProcessor::processInMessage : no RM sequence bean with security credentials");
					return false;
				}

				// Commit this transaction to clear up held RMS/RMDBeans
				if (transaction != null && transaction.isActive())
					transaction.commit();

				// Get a new transaction
				transaction = storageManager.getTransaction();
			}

			//lookup a sender bean
			SenderBeanMgr senderBeanMgr = storageManager.getSenderBeanMgr();

			//selecting the set of SenderBeans that suit the given criteria.
			findSenderBean.setSend(true);
			findSenderBean.setTransportAvailable(false);

			if (address != null)
				findSenderBean.setToAddress(address);

			if (identifier != null) {
				if (LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled())
					log.debug("identifier set, this violates RSP " + identifier);
				findSenderBean.setSequenceID(identifier.getIdentifier());
			}

			SenderBean senderBean = null;
			boolean pending = false;
			while (true) {
				// Set the time to send field to be now
				findSenderBean.setTimeToSend(System.currentTimeMillis());

				//finding the beans that go with the criteria of the passed SenderBean
				//The reSend flag is ignored for this selection, so there is no need to
				//set it.
				Collection<SenderBean> collection = senderBeanMgr.find(findSenderBean);

				//removing beans that does not pass the resend test
				for (Iterator<SenderBean> it = collection.iterator(); it.hasNext();) {
					SenderBean bean = (SenderBean) it.next();
					if (!bean.isReSend() && bean.getSentCount() > 0)
						it.remove();
				}

				//selecting a bean to send RANDOMLY. TODO- Should use a better mechanism.
				int size = collection.size();
				int itemToPick = -1;

				pending = false;
				if (size > 0) {
					Random random = new Random();
					itemToPick = random.nextInt(size);
				}

				if (size > 1)
					pending = true;  //there are more than one message to be delivered using the makeConnection.
				//So the MessagePending header should have value true;

				Iterator<SenderBean> it = collection.iterator();

				senderBean = null;
				for (int item = 0; item < size; item++) {
					senderBean = (SenderBean) it.next();
					if (item == itemToPick)
						break;
				}

				if (senderBean == null) {
					//If secured try another sequence
					//Remove old one from the list and pick another random one
					if (secured) {
						possibleBeans.remove(possibleBeanIndex);
						int possBeansSize = possibleBeans.size();

						if (possBeansSize > 0) {
							//select one at random: TODO better method?
							Random random = new Random();
							possibleBeanIndex = random.nextInt(possBeansSize);
							RMSequenceBean selectedSequence = (RMSequenceBean) possibleBeans.get(possibleBeanIndex);
							findSenderBean.setSequenceID(selectedSequence.getSequenceID());
							if (LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled())
								log.debug("sequence selected " + findSenderBean.getSequenceID());
						} else {
							if (LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled())
								log.debug("Exit: MakeConnectionProcessor::processInMessage, no matching message found");
							return false;
						}
					} else {
						if (LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled())
							log.debug("Exit: MakeConnectionProcessor::processInMessage, no matching message found");
						return false;
					}
				} else {
					break;
				}
			}

			if (transaction != null && transaction.isActive()) {
				transaction.commit();
				transaction = storageManager.getTransaction();
			}
			replyToPoll(rmMsgCtx, senderBean, storageManager, pending, makeConnection.getNamespaceValue(), transaction);

		} finally {
			if (transaction != null && transaction.isActive()) {
				transaction.rollback();
			}
		}

		if (LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled())
			log.debug("Exit: MakeConnectionProcessor::processInMessage");
		return false;
	}

	public static void replyToPoll(RMMsgContext pollMessage,
								   SenderBean matchingMessage,
								   StorageManager storageManager,
								   boolean pending,
								   String makeConnectionNamespace,
								   Transaction transaction)
			throws AxisFault {
		if (LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled())
			log.debug("Enter: MakeConnectionProcessor::replyToPoll");
		TransportOutDescription transportOut = pollMessage.getMessageContext().getTransportOut();
		if (transportOut == null) {
			String message = SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.cantSendMakeConnectionNoTransportOut);
			if (LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled()) log.debug(message);
			throw new SandeshaException(message);
		}

		String messageStorageKey = matchingMessage.getMessageContextRefKey();
		MessageContext returnMessage = storageManager.retrieveMessageContext(messageStorageKey, pollMessage.getConfigurationContext());
		if (returnMessage == null) {
			String message = "Cannot find the message stored with the key:" + messageStorageKey;
			if (LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled()) log.debug(message);
			// Someone else has either removed the sender & message, or another make connection got here first.
			return;
		}

		if (pending) addMessagePendingHeader(returnMessage, makeConnectionNamespace);
		boolean continueSending = true;
		RMMsgContext returnRMMsg = MsgInitializer.initializeMessage(returnMessage);
		if (returnRMMsg.getRMNamespaceValue() == null) {
			//this is the case when a stored application response msg was not sucecsfully returned
			//on the sending transport's backchannel. Since the msg was stored without a sequence header
			//we need to lookup the namespace using the RMS bean
			if (LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled())
				log.debug("Looking up rmNamespace from RMS bean");
			String sequenceID = matchingMessage.getSequenceID();
			if (sequenceID != null) {
				RMSBean rmsBean = new RMSBean();
				rmsBean.setSequenceID(sequenceID);
				rmsBean = storageManager.getRMSBeanMgr().findUnique(rmsBean);
				if (rmsBean != null) {
					returnRMMsg.setRMNamespaceValue(SpecSpecificConstants.getRMNamespaceValue(rmsBean.getRMVersion()));
				} else {
					//we will never be able to reply to this msg - at the moment the best bet is
					//to not process the reply anymore
					if (LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled())
						log.debug("Could not find RMS bean for polled msg");
					continueSending = false;
					//also remove the sender bean so that we do not select this again
					storageManager.getSenderBeanMgr().delete(matchingMessage.getMessageID());
				}
			}
		}

		if (continueSending) {

			// Commit the current transaction, so that the SenderWorker can do it's own locking
			// this transaction should be commited out before gettting the worker lock.
			// otherwise a dead lock can happen.
			if (transaction != null && transaction.isActive()) transaction.commit();

			SandeshaThread sender = storageManager.getSender();
			WorkerLock lock = sender.getWorkerLock();

			String workId = matchingMessage.getMessageID();
			SenderWorker worker = new SenderWorker(pollMessage.getConfigurationContext(), matchingMessage, pollMessage.getRMSpecVersion());
			worker.setLock(lock);
			worker.setWorkId(workId);
			while (!lock.addWork(workId, worker)) {
				try {
					// wait on the lock.
					lock.awaitRemoval(workId);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

			}

			setTransportProperties(returnMessage, pollMessage);

			// Link the response to the request

			AxisOperation operation = SpecSpecificConstants.getWSRMOperation(Sandesha2Constants.MessageTypes.POLL_RESPONSE_MESSAGE, pollMessage.getRMSpecVersion(), pollMessage.getMessageContext().getAxisService());
			OperationContext context = new OperationContext(operation, pollMessage.getMessageContext().getServiceContext());

			context.addMessageContext(returnMessage);
			returnMessage.setServiceContext(null);
			returnMessage.setOperationContext(context);

			returnMessage.setProperty(Sandesha2Constants.MAKE_CONNECTION_RESPONSE, Boolean.TRUE);
			returnMessage.setProperty(RequestResponseTransport.TRANSPORT_CONTROL, pollMessage.getProperty(RequestResponseTransport.TRANSPORT_CONTROL));

			//running the MakeConnection through a SenderWorker.
			//This will allow Sandesha2 to consider both of following senarios equally.
			//  1. A message being sent by the Sender thread.
			//  2. A message being sent as a reply to an MakeConnection.

			worker.setMessage(returnRMMsg);
			worker.run();

			TransportUtils.setResponseWritten(pollMessage.getMessageContext(), true);
		}

		if (LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled())
			log.debug("Exit: MakeConnectionProcessor::replyToPoll");
	}

	private static void addMessagePendingHeader(MessageContext returnMessage, String namespace) {
		MessagePending messagePending = new MessagePending();
		messagePending.setPending(true);
		if (returnMessage.getEnvelope().getHeader() == null) {
			int SOAPVersion = Sandesha2Constants.SOAPVersion.v1_1;
			if (!returnMessage.isSOAP11())
				SOAPVersion = Sandesha2Constants.SOAPVersion.v1_2;
			//The header might not be there because of the persistence code if it doesn't exist we need to add one
			SOAPAbstractFactory.getSOAPFactory(
					SOAPVersion).createSOAPHeader(returnMessage.getEnvelope());
		}
		messagePending.toHeader(returnMessage.getEnvelope().getHeader());
	}

	public boolean processOutMessage(RMMsgContext rmMsgCtx, Transaction transaction) {
		return false;
	}

	private static void setTransportProperties(MessageContext returnMessage, RMMsgContext makeConnectionMessage) {
		returnMessage.setProperty(MessageContext.TRANSPORT_OUT, makeConnectionMessage.getProperty(MessageContext.TRANSPORT_OUT));
		returnMessage.setProperty(Constants.OUT_TRANSPORT_INFO, makeConnectionMessage.getProperty(Constants.OUT_TRANSPORT_INFO));

		Object contentType = makeConnectionMessage.getProperty(Constants.Configuration.CONTENT_TYPE);
		returnMessage.setProperty(Constants.Configuration.CONTENT_TYPE, contentType);

		returnMessage.setTransportOut(makeConnectionMessage.getMessageContext().getTransportOut());
	}
}
