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

package org.apache.sandesha2.workers;

import java.util.ArrayList;

import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFault;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.transport.TransportSender;
import org.apache.axis2.transport.TransportUtils;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.RMMsgContext;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.Transaction;
import org.apache.sandesha2.storage.beanmanagers.SenderBeanMgr;
import org.apache.sandesha2.storage.beans.SenderBean;
import org.apache.sandesha2.util.AcknowledgementManager;
import org.apache.sandesha2.util.MessageRetransmissionAdjuster;
import org.apache.sandesha2.util.MsgInitializer;
import org.apache.sandesha2.util.SandeshaUtil;
import org.apache.sandesha2.util.TerminateManager;
import org.apache.sandesha2.wsrm.Sequence;
import org.apache.sandesha2.wsrm.TerminateSequence;

/**
 * This is responsible for sending and re-sending messages of Sandesha2. This
 * represent a thread that keep running all the time. This keep looking at the
 * Sender table to find out any entries that should be sent.
 */

public class Sender extends Thread {

	private boolean runSender = false;

	private ArrayList workingSequences = new ArrayList();

	private ConfigurationContext context = null;

	private static final Log log = LogFactory.getLog(Sender.class);

	private boolean hasStopped = false;

	public synchronized void stopSenderForTheSequence(String sequenceID) {
		if (log.isDebugEnabled())
			log.debug("Enter: Sender::stopSenderForTheSequence, " + sequenceID);
		workingSequences.remove(sequenceID);
		if (workingSequences.size() == 0) {
			runSender = false;
		}
		if (log.isDebugEnabled())
			log.debug("Exit: Sender::stopSenderForTheSequence");
	}

	public synchronized void stopSending() {
		if (log.isDebugEnabled())
			log.debug("Enter: Sender::stopSending");

		if (isSenderStarted()) {
			// the sender is started so stop it
			runSender = false;
			// wait for it to finish
			while (!hasStoppedSending()) {
				try {
					wait(Sandesha2Constants.SENDER_SLEEP_TIME);
				} catch (InterruptedException e1) {
					log.debug(e1.getMessage());
				}
			}
		}

		if (log.isDebugEnabled())
			log.debug("Exit: Sender::stopSending");
	}

	private synchronized boolean hasStoppedSending() {
		if (log.isDebugEnabled()) {
			log.debug("Enter: Sender::hasStoppedSending");
			log.debug("Exit: Sender::hasStoppedSending, " + hasStopped);
		}
		return hasStopped;
	}

	public synchronized boolean isSenderStarted() {
		if (log.isDebugEnabled()) {
			log.debug("Enter: Sender::isSenderStarted");
			log.debug("Exit: Sender::isSenderStarted, " + runSender);
		}
		return runSender;
	}

	public void run() {
		if (log.isDebugEnabled())
			log.debug("Enter: Sender::run");

		try {
			internalRun();
		} finally {
			// flag that we have exited the run loop and notify any waiting
			// threads
			synchronized (this) {
				hasStopped = true;
				notify();
			}
		}

		if (log.isDebugEnabled())
			log.debug("Exit: Sender::run");
	}

	private void internalRun() {
		if (log.isDebugEnabled())
			log.debug("Enter: Sender::internalRun");

		StorageManager storageManager = null;

		try {
			storageManager = SandeshaUtil.getSandeshaStorageManager(context, context.getAxisConfiguration());
		} catch (SandeshaException e2) {
			// TODO Auto-generated catch block
			log.debug(SandeshaMessageHelper.getMessage(SandeshaMessageKeys.cannotCointinueSender, e2.toString()), e2);
			e2.printStackTrace();
			return;
		}

		while (isSenderStarted()) {

			try {
				Thread.sleep(Sandesha2Constants.SENDER_SLEEP_TIME);
			} catch (InterruptedException e1) {
				// e1.printStackTrace();
				log.debug("Sender was interupted...");
				log.debug(e1.getMessage());
				log.debug("End printing Interrupt...");
			}

			Transaction transaction = null;
			boolean rolebacked = false;

			try {
				if (context == null) {
					String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.configContextNotSet);
					message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.cannotCointinueSender, message);
					log.debug(message);
					throw new SandeshaException(message);
				}

				transaction = storageManager.getTransaction();

				SenderBeanMgr mgr = storageManager.getRetransmitterBeanMgr();
				SenderBean senderBean = mgr.getNextMsgToSend();
				if (senderBean == null) {
					if (log.isDebugEnabled())
						log.debug("SenderBean not found");
					continue;
				}

				String key = senderBean.getMessageContextRefKey();
				MessageContext msgCtx = storageManager.retrieveMessageContext(key, context);
				msgCtx.setProperty(Sandesha2Constants.WITHIN_TRANSACTION, Sandesha2Constants.VALUE_TRUE);

				MessageRetransmissionAdjuster retransmitterAdjuster = new MessageRetransmissionAdjuster();
				boolean continueSending = retransmitterAdjuster.adjustRetransmittion(senderBean, context,
						storageManager);
				if (!continueSending) {
					continue;
				}

				if (msgCtx == null) {
					String message = "Message context is not present in the storage";
				}

				// sender will not send the message if following property is
				// set and not true.
				// But it will set if it is not set (null)

				// This is used to make sure that the mesage get passed the
				// Sandesha2TransportSender.

				String qualifiedForSending = (String) msgCtx.getProperty(Sandesha2Constants.QUALIFIED_FOR_SENDING);
				if (qualifiedForSending != null && !qualifiedForSending.equals(Sandesha2Constants.VALUE_TRUE)) {
					continue;
				}

				if (msgCtx == null) {
					log.debug(SandeshaMessageHelper.getMessage(SandeshaMessageKeys.sendHasUnavailableMsgEntry));
					break;
				}

				RMMsgContext rmMsgCtx = MsgInitializer.initializeMessage(msgCtx);

				// operation is the lowest level Sandesha2 should be attached
				ArrayList msgsNotToSend = SandeshaUtil.getPropertyBean(msgCtx.getAxisOperation()).getMsgTypesToDrop();

				if (msgsNotToSend != null && msgsNotToSend.contains(new Integer(rmMsgCtx.getMessageType()))) {
					continue;
				}

				updateMessage(msgCtx);

				int messageType = rmMsgCtx.getMessageType();
				if (messageType == Sandesha2Constants.MessageTypes.APPLICATION) {
					Sequence sequence = (Sequence) rmMsgCtx.getMessagePart(Sandesha2Constants.MessageParts.SEQUENCE);
					String sequenceID = sequence.getIdentifier().getIdentifier();
				}

				// checking weather this message can carry piggybacked acks
				if (isAckPiggybackableMsgType(messageType) && !isAckAlreadyPiggybacked(rmMsgCtx)) {
					// piggybacking if an ack if available for the same
					// sequence.
					// TODO do piggybacking based on wsa:To
					AcknowledgementManager.piggybackAcksIfPresent(rmMsgCtx, storageManager);
				}

				// sending the message
				TransportOutDescription transportOutDescription = msgCtx.getTransportOut();
				TransportSender transportSender = transportOutDescription.getSender();

				boolean successfullySent = false;
				if (transportSender != null) {

					// have to commit the transaction before sending. This may
					// get changed when WS-AT is available.
					transaction.commit();
					msgCtx.setProperty(Sandesha2Constants.WITHIN_TRANSACTION, Sandesha2Constants.VALUE_FALSE);
					try {

						// had to fully build the SOAP envelope to support
						// retransmissions.
						// Otherwise a 'parserAlreadyAccessed' exception could
						// get thrown in retransmissions.
						// But this has a performance reduction.
						msgCtx.getEnvelope().build();

						if (log.isDebugEnabled())
							log.debug("Invoking using transportSender " + transportSender + ", msgCtx="
									+ msgCtx.getEnvelope().getHeader());
						// TODO change this to cater for security.
						transportSender.invoke(msgCtx);
						successfullySent = true;
					} catch (Exception e) {
						// TODO Auto-generated catch block
						String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.sendMsgError, e
								.toString());
						log.debug(message, e);
					} finally {
						transaction = storageManager.getTransaction();
						msgCtx.setProperty(Sandesha2Constants.WITHIN_TRANSACTION, Sandesha2Constants.VALUE_TRUE);
					}
				}

				// update or delete only if the object is still present.
				SenderBean bean1 = mgr.retrieve(senderBean.getMessageID());
				if (bean1 != null) {
					if (senderBean.isReSend()) {
						bean1.setSentCount(senderBean.getSentCount());
						bean1.setTimeToSend(senderBean.getTimeToSend());
						mgr.update(bean1);
					} else {
						mgr.delete(bean1.getMessageID());

						// removing the message from the storage.
						String messageStoredKey = bean1.getMessageContextRefKey();
						storageManager.removeMessageContext(messageStoredKey);
					}
				}

				if (successfullySent) {
					if (!msgCtx.isServerSide())
						checkForSyncResponses(msgCtx);
				}

				if (rmMsgCtx.getMessageType() == Sandesha2Constants.MessageTypes.TERMINATE_SEQ) {
					// terminate sending side.
					TerminateSequence terminateSequence = (TerminateSequence) rmMsgCtx
							.getMessagePart(Sandesha2Constants.MessageParts.TERMINATE_SEQ);
					String sequenceID = terminateSequence.getIdentifier().getIdentifier();
					ConfigurationContext configContext = msgCtx.getConfigurationContext();

					String internalSequenceID = SandeshaUtil.getSequenceProperty(sequenceID,
							Sandesha2Constants.SequenceProperties.INTERNAL_SEQUENCE_ID, storageManager);
					TerminateManager.terminateSendingSide(configContext, internalSequenceID, msgCtx.isServerSide(),
							storageManager);
				}

				msgCtx.setProperty(Sandesha2Constants.WITHIN_TRANSACTION, Sandesha2Constants.VALUE_FALSE);

			} catch (Exception e) {

				// TODO : when this is the client side throw the exception to
				// the client when necessary.

				if (transaction != null) {
					try {
						transaction.rollback();
						rolebacked = true;
					} catch (Exception e1) {
						String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.rollbackError, e1
								.toString());
						log.debug(message, e1);
					}
				}

				String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.sendMsgError, e.toString());

				log.debug(message, e);
			} finally {
				if (transaction != null && !rolebacked) {
					try {
						transaction.commit();
					} catch (Exception e) {
						String message = SandeshaMessageHelper
								.getMessage(SandeshaMessageKeys.commitError, e.toString());
						log.debug(message, e);
					}
				}
			}
		}
		if (log.isDebugEnabled())
			log.debug("Exit: Sender::internalRun");
	}

	public synchronized void runSenderForTheSequence(ConfigurationContext context, String sequenceID) {
		if (log.isDebugEnabled())
			log.debug("Enter: Sender::runSenderForTheSequence, " + sequenceID);

		if (sequenceID != null && !workingSequences.contains(sequenceID))
			workingSequences.add(sequenceID);

		if (!isSenderStarted()) {
			this.context = context;
			runSender = true; // so that isSenderStarted()=true.
			super.start();
		}
		if (log.isDebugEnabled())
			log.debug("Exit: Sender::runSenderForTheSequence");
	}

	private void updateMessage(MessageContext msgCtx1) throws SandeshaException {
		// do updates if required.
	}

	private void checkForSyncResponses(MessageContext msgCtx) throws SandeshaException {
		if (log.isDebugEnabled())
			log.debug("Enter: Sender::checkForSyncResponses, " + msgCtx.getEnvelope().getHeader());

		try {

			boolean responsePresent = (msgCtx.getProperty(MessageContext.TRANSPORT_IN) != null);
			if (!responsePresent)
				return;

			// create the responseMessageContext

			MessageContext responseMessageContext = new MessageContext();
			responseMessageContext.setServerSide(false);
			responseMessageContext.setConfigurationContext(msgCtx.getConfigurationContext());
			responseMessageContext.setTransportIn(msgCtx.getTransportIn());
			responseMessageContext.setTransportOut(msgCtx.getTransportOut());

			responseMessageContext.setProperty(MessageContext.TRANSPORT_IN, msgCtx
					.getProperty(MessageContext.TRANSPORT_IN));
			responseMessageContext.setServiceContext(msgCtx.getServiceContext());
			responseMessageContext.setServiceGroupContext(msgCtx.getServiceGroupContext());

			// copying required properties from op. context to the response msg
			// ctx.
			OperationContext requestMsgOpCtx = msgCtx.getOperationContext();
			if (requestMsgOpCtx != null) {
				if (responseMessageContext.getProperty(HTTPConstants.MTOM_RECEIVED_CONTENT_TYPE) == null) {
					responseMessageContext.setProperty(HTTPConstants.MTOM_RECEIVED_CONTENT_TYPE, requestMsgOpCtx
							.getProperty(HTTPConstants.MTOM_RECEIVED_CONTENT_TYPE));
				}

				if (responseMessageContext.getProperty(HTTPConstants.CHAR_SET_ENCODING) == null) {
					responseMessageContext.setProperty(HTTPConstants.CHAR_SET_ENCODING, requestMsgOpCtx
							.getProperty(HTTPConstants.CHAR_SET_ENCODING));
				}
			}

			// If request is REST we assume the responseMessageContext is REST,
			// so set the variable

			responseMessageContext.setDoingREST(msgCtx.isDoingREST());

			SOAPEnvelope resenvelope = null;
			try {
				resenvelope = TransportUtils.createSOAPMessage(msgCtx, msgCtx.getEnvelope().getNamespace().getName());

			} catch (AxisFault e) {
				// TODO Auto-generated catch block
				log.debug(SandeshaMessageHelper.getMessage(SandeshaMessageKeys.soapEnvNotSet));
				log.debug(e.getStackTrace().toString());
			}

			// if the request msg ctx is withina a transaction, processing if
			// the response should also happen
			// withing the same transaction
			responseMessageContext.setProperty(Sandesha2Constants.WITHIN_TRANSACTION, msgCtx
					.getProperty(Sandesha2Constants.WITHIN_TRANSACTION));

			if (resenvelope != null) {
				responseMessageContext.setEnvelope(resenvelope);
				AxisEngine engine = new AxisEngine(msgCtx.getConfigurationContext());

				if (isFaultEnvelope(resenvelope)) {
					engine.receiveFault(responseMessageContext);
				} else {
					engine.receive(responseMessageContext);
				}
			}

		} catch (Exception e) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.noValidSyncResponse);
			log.debug(message, e);
			throw new SandeshaException(message, e);
		}
		if (log.isDebugEnabled())
			log.debug("Exit: Sender::checkForSyncResponses");
	}

	private boolean isAckPiggybackableMsgType(int messageType) {
		if (log.isDebugEnabled())
			log.debug("Enter: Sender::isAckPiggybackableMsgType, " + messageType);
		boolean piggybackable = true;

		if (messageType == Sandesha2Constants.MessageTypes.ACK)
			piggybackable = false;

		if (log.isDebugEnabled())
			log.debug("Exit: Sender::isAckPiggybackableMsgType, " + piggybackable);
		return piggybackable;
	}

	private boolean isAckAlreadyPiggybacked(RMMsgContext rmMessageContext) {
		if (rmMessageContext.getMessagePart(Sandesha2Constants.MessageParts.SEQ_ACKNOWLEDGEMENT) != null)
			return true;

		return false;
	}

	private boolean isFaultEnvelope(SOAPEnvelope envelope) throws SandeshaException {
		if (log.isDebugEnabled())
			log.debug("Enter: Sender::isFaultEnvelope, " + envelope.getBody().getFault());
		SOAPFault fault = envelope.getBody().getFault();
		if (fault != null) {
			if (log.isDebugEnabled())
				log.debug("Exit: Sender::isFaultEnvelope, TRUE");
			return true;
		}

		if (log.isDebugEnabled())
			log.debug("Exit: Sender::isFaultEnvelope, FALSE");
		return false;
	}

}
