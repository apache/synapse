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

package org.apache.sandesha2.handlers;

import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.RelatesTo;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.handlers.AbstractHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.RMMsgContext;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.msgprocessors.SequenceProcessor;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.Transaction;
import org.apache.sandesha2.storage.beans.RMDBean;
import org.apache.sandesha2.util.MsgInitializer;
import org.apache.sandesha2.util.Range;
import org.apache.sandesha2.util.RangeString;
import org.apache.sandesha2.util.SandeshaUtil;
import org.apache.sandesha2.wsrm.Sequence;

/**
 * The Global handler of Sandesha2. This is used to perform things that should
 * be done before diapatching such as duplicate detection.
 */

public class SandeshaGlobalInHandler extends AbstractHandler {

	private static final long serialVersionUID = -7187928423123306156L;

	private static final Log log = LogFactory.getLog(SandeshaGlobalInHandler.class);
	
	public InvocationResponse invoke(MessageContext msgContext) throws AxisFault {

		if (log.isDebugEnabled())
			log.debug("Enter: SandeshaGlobalInHandler::invoke, " + msgContext.getEnvelope().getHeader());

		InvocationResponse returnValue = InvocationResponse.CONTINUE;
		
		ConfigurationContext configContext = msgContext.getConfigurationContext();
		if (configContext == null)
			throw new AxisFault(SandeshaMessageHelper.getMessage(SandeshaMessageKeys.configContextNotSet));

		SOAPEnvelope envelope = msgContext.getEnvelope();
		if (envelope == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(SandeshaMessageKeys.soapEnvNotSet));

		String reinjectedMessage = (String) msgContext.getProperty(Sandesha2Constants.REINJECTED_MESSAGE);
		if (reinjectedMessage != null && Sandesha2Constants.VALUE_TRUE.equals(reinjectedMessage))
			return returnValue; // Reinjected messages are not processed by Sandesha2 inflow
													// handlers

		Transaction transaction = null;

		try {
			
			// Quitting the message with minimum processing if not intended for
			// RM.
			boolean isRMGlobalMessage = SandeshaUtil.isRMGlobalMessage(msgContext);
			if (!isRMGlobalMessage) {
				if (log.isDebugEnabled())
					log.debug("Exit: SandeshaGlobalInHandler::invoke, !isRMGlobalMessage " + returnValue);
				return returnValue;
			}

			StorageManager storageManager = null;
			try {
				storageManager = SandeshaUtil
						.getSandeshaStorageManager(configContext, configContext.getAxisConfiguration());
				if (storageManager == null) {
					log.debug("Sandesha2 cannot proceed. The StorageManager is not available " + returnValue);
					return returnValue;
				}
			} catch (SandeshaException e1) {
				// TODO make this a log
				log.debug("Sandesha2 cannot proceed. Exception thrown when looking for the StorageManager", e1);
				return returnValue;
			}

			transaction = storageManager.getTransaction();

			RMMsgContext rmMessageContext = MsgInitializer.initializeMessage(msgContext);

			// Dropping duplicates
			boolean dropped = dropIfDuplicate(rmMessageContext, storageManager);
			if (dropped) {
				returnValue = InvocationResponse.ABORT; //the msg has been dropped
				processDroppedMessage(rmMessageContext, storageManager);
				if (log.isDebugEnabled())
					log.debug("Exit: SandeshaGlobalInHandler::invoke, dropped " + returnValue);
				return returnValue;
			}

			// Persisting the application messages
			// if
			// (rmMessageContext.getMessageType()==Sandesha2Constants.MessageTypes.APPLICATION)
			// {
			// SandeshaUtil.PersistMessageContext ()
			// }

			// Process if global processing possible. - Currently none
			if (SandeshaUtil.isGloballyProcessableMessageType(rmMessageContext.getMessageType())) {
				doGlobalProcessing(rmMessageContext);
			}

		} catch (Exception e) {
			// message should not be sent in a exception situation.
			msgContext.pause();
			returnValue = InvocationResponse.SUSPEND;

			if (transaction != null) {
				try {
					transaction.rollback();
					transaction = null;
				} catch (Exception e1) {
					String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.rollbackError, e1.toString());
					log.debug(message, e);
				}
			}

			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.inMsgError, e.toString());
			if (log.isDebugEnabled())
				log.debug("Exit: SandeshaGlobalInHandler::invoke ", e);
			throw new AxisFault(message, e);
		} finally {
			if (transaction != null) {
				try {
					transaction.commit();
				} catch (Exception e) {
					String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.commitError, e.toString());
					if (log.isDebugEnabled())
						log.debug("Exit: SandeshaGlobalInHandler::invoke ", e);
					throw new AxisFault(message, e);
				}
			}
		}
		if (log.isDebugEnabled())
			log.debug("Exit: SandeshaGlobalInHandler::invoke " + returnValue);
		return returnValue;
	}

	private boolean dropIfDuplicate(RMMsgContext rmMsgContext, StorageManager storageManager) throws AxisFault {
		if (log.isDebugEnabled())
			log.debug("Enter: SandeshaGlobalInHandler::dropIfDuplicate");

		boolean drop = false;		
		
		if (rmMsgContext.getMessageType() == Sandesha2Constants.MessageTypes.APPLICATION) {

			Sequence sequence = (Sequence) rmMsgContext.getMessagePart(Sandesha2Constants.MessageParts.SEQUENCE);

			long msgNo = sequence.getMessageNumber().getMessageNumber();
			
			String propertyKey = SandeshaUtil.getSequencePropertyKey(rmMsgContext);

			if (propertyKey != null && msgNo > 0) {
				RMDBean rmdBean = SandeshaUtil.getRMDBeanFromSequenceId(storageManager, propertyKey);
				if (rmdBean != null) {
					if (rmdBean.getServerCompletedMessages() != null) {
						if (rmdBean.getServerCompletedMessages().isMessageNumberInRanges(msgNo))
							//this msg is in a completed range
							drop = true;
					}
	
					if (!drop) {
						// Checking for RM specific EMPTY_BODY LASTMESSAGE.
						SOAPBody body = rmMsgContext.getSOAPEnvelope().getBody();
						boolean emptyBody = false;
						if (body.getChildElements().hasNext() == false) {
							emptyBody = true;
						}
	
						if (emptyBody) {
							if (sequence.getLastMessage() != null) {
								log.debug(SandeshaMessageHelper.getMessage(SandeshaMessageKeys.emptyLastMsg));
								drop = true;
	
								RangeString serverCompletedMsgs = rmdBean.getServerCompletedMessages();
								
								// Add this message to the completed ranges
								serverCompletedMsgs.addRange(new Range(msgNo));
								// Update with the new ranges
								rmdBean.setServerCompletedMessages(serverCompletedMsgs);
	
								// TODO correct the syntac into '[received msgs]'
	
								// Update the rmdBean
								storageManager.getRMDBeanMgr().update(rmdBean);
							}
						}
					}
				}
			}
		} else if (rmMsgContext.getMessageType() != Sandesha2Constants.MessageTypes.UNKNOWN) {
			// droping other known message types if, an suitable operation
			// context is not available,
			// and if a relates to value is present.
			RelatesTo relatesTo = rmMsgContext.getRelatesTo();
			if (relatesTo != null) {
				String value = relatesTo.getValue();

				// TODO do not drop, relationshipTypes other than reply

				ConfigurationContext configurationContext = rmMsgContext.getMessageContext().getConfigurationContext();
				OperationContext operationContextFromMap = configurationContext.getOperationContext(value);
				OperationContext operationContext = rmMsgContext.getMessageContext().getOperationContext();

				// reply messages should be dropped if it cannot be instance
				// dispatched.
				// I.e. both not having a op. ctx not and not having a op. ctx
				// in the global list.
				if (operationContext == null && operationContextFromMap == null) {
					String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.droppingDuplicate);
					log.debug(message);
					drop = true;
				}
			}
		}

		if (drop) {
			rmMsgContext.getMessageContext().pause();
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.droppingDuplicate);
			log.debug(message);
			if (log.isDebugEnabled())
				log.debug("Exit: SandeshaGlobalInHandler::dropIfDuplicate, true");
			return true;
		}

		if (log.isDebugEnabled())
			log.debug("Exit: SandeshaGlobalInHandler::dropIfDuplicate, false");
		return false;
	}

	private void processDroppedMessage(RMMsgContext rmMsgContext, StorageManager storageManager)
			throws AxisFault {
		if (log.isDebugEnabled())
			log.debug("Enter: SandeshaGlobalInHandler::processDroppedMessage");

		if (rmMsgContext.getMessageType() == Sandesha2Constants.MessageTypes.APPLICATION) {

			// Even though the duplicate message is dropped, hv to send the ack
			// if needed.
			SequenceProcessor.sendAckIfNeeded(rmMsgContext, storageManager);

		}
		if (log.isDebugEnabled())
			log.debug("Exit: SandeshaGlobalInHandler::processDroppedMessage");
	}

	private void doGlobalProcessing(RMMsgContext rmMsgCtx) throws SandeshaException {
	}

	public String getName() {
		return Sandesha2Constants.GLOBAL_IN_HANDLER_NAME;
	}
}
