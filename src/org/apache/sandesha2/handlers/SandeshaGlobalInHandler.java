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

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFault;
import org.apache.axiom.soap.SOAPFaultCode;
import org.apache.axiom.soap.SOAPFaultSubCode;
import org.apache.axiom.soap.SOAPFaultValue;
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
import org.apache.sandesha2.msgprocessors.ApplicationMsgProcessor;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.Transaction;
import org.apache.sandesha2.storage.beanmanagers.SequencePropertyBeanMgr;
import org.apache.sandesha2.storage.beans.SequencePropertyBean;
import org.apache.sandesha2.util.FaultManager;
import org.apache.sandesha2.util.MsgInitializer;
import org.apache.sandesha2.util.SandeshaUtil;
import org.apache.sandesha2.wsrm.Sequence;

/**
 * The Global handler of Sandesha2. This is used to perform things that should
 * be done before diapatching such as duplicate detection.
 */

public class SandeshaGlobalInHandler extends AbstractHandler {

	private static final long serialVersionUID = -7187928423123306156L;

	private static final Log log = LogFactory.getLog(SandeshaGlobalInHandler.class.getName());
	
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

		boolean withinTransaction = false;
		String withinTransactionStr = (String) msgContext.getProperty(Sandesha2Constants.WITHIN_TRANSACTION);
		if (withinTransactionStr != null && Sandesha2Constants.VALUE_TRUE.equals(withinTransactionStr)) {
			withinTransaction = true;
		}

		Transaction transaction = null;
		if (!withinTransaction) {
			transaction = storageManager.getTransaction();
			msgContext.setProperty(Sandesha2Constants.WITHIN_TRANSACTION, Sandesha2Constants.VALUE_TRUE);
		}
		boolean rolebacked = false;

		try {
			// processing faults.
			// Had to do this before dispatching. A fault message comes with the
			// relatesTo part. So this will
			// fill the opContext of te req/res message. But RM keeps
			// retransmitting. So RM has to report the
			// error and stop this fault being dispatched as the response
			// message.

			SOAPFault faultPart = envelope.getBody().getFault();

			if (faultPart != null) {

				// constructing the fault
				AxisFault axisFault = getAxisFaultFromFromSOAPFault(faultPart);

				//If this is a RM related fault. I.e. one that was defined in the WSRM spec. It will be 
				//handled at this point.
				SOAPFaultCode faultCode = axisFault.getFaultCodeElement();
				SOAPFaultSubCode faultSubCode = faultCode!=null?faultCode.getSubCode():null;
				SOAPFaultValue faultSubcodeValue = faultSubCode!=null?faultSubCode.getValue():null;
				String subCodeText = faultSubcodeValue!=null?faultSubcodeValue.getText():null;
				
				if (subCodeText!=null && FaultManager.isRMFault(subCodeText)) {
					//handling the fault here and pausing the message.
					
					FaultManager faultManager = new FaultManager ();
					faultManager.manageIncomingRMFault (axisFault, msgContext);
					
					msgContext.pause();
					returnValue = InvocationResponse.SUSPEND;
				}
				
			}

			// Quitting the message with minimum processing if not intended for
			// RM.
			boolean isRMGlobalMessage = SandeshaUtil.isRMGlobalMessage(msgContext);
			if (!isRMGlobalMessage) {
				if (log.isDebugEnabled())
					log.debug("Exit: SandeshaGlobalInHandler::invoke, !isRMGlobalMessage " + returnValue);
				return returnValue;
			}

			RMMsgContext rmMessageContext = MsgInitializer.initializeMessage(msgContext);

			// Dropping duplicates
			boolean dropped = dropIfDuplicate(rmMessageContext, storageManager);
			if (dropped) {
				returnValue = InvocationResponse.SUSPEND; //the msg has been paused
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

			if (!withinTransaction) {
				try {
					transaction.rollback();
					msgContext.setProperty(Sandesha2Constants.WITHIN_TRANSACTION, Sandesha2Constants.VALUE_FALSE);
					rolebacked = true;
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
			if (!withinTransaction && !rolebacked) {
				try {
					transaction.commit();
					msgContext.setProperty(Sandesha2Constants.WITHIN_TRANSACTION, Sandesha2Constants.VALUE_FALSE);
				} catch (Exception e) {
					String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.commitError, e.toString());
					log.debug(message, e);
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
			String sequenceId = null;

			long msgNo = sequence.getMessageNumber().getMessageNumber();
			
			String propertyKey = SandeshaUtil.getSequencePropertyKey(rmMsgContext);

			if (propertyKey != null && msgNo > 0) {
				SequencePropertyBeanMgr seqPropMgr = storageManager.getSequencePropertyBeanMgr();
				SequencePropertyBean receivedMsgsBean = seqPropMgr.retrieve(propertyKey,
						Sandesha2Constants.SequenceProperties.SERVER_COMPLETED_MESSAGES);
				if (receivedMsgsBean != null) {
					String receivedMsgStr = receivedMsgsBean.getValue();
					ArrayList msgNoArrList = SandeshaUtil.getSplittedMsgNoArraylist(receivedMsgStr);

					Iterator iterator = msgNoArrList.iterator();
					while (iterator.hasNext()) {
						String temp = (String) iterator.next();
						String msgNoStr = new Long(msgNo).toString();
						if (msgNoStr.equals(temp)) {
							drop = true;
						}
					}
				}

				if (drop == false) {
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

							if (receivedMsgsBean == null) {
								receivedMsgsBean = new SequencePropertyBean(sequenceId,
										Sandesha2Constants.SequenceProperties.SERVER_COMPLETED_MESSAGES, "");
								seqPropMgr.insert(receivedMsgsBean);
							}

							String receivedMsgStr = receivedMsgsBean.getValue();
							if (!receivedMsgStr.equals("") && receivedMsgStr != null)
								receivedMsgStr = receivedMsgStr + "," + Long.toString(msgNo);
							else
								receivedMsgStr = Long.toString(msgNo);

							receivedMsgsBean.setValue(receivedMsgStr);

							// TODO correct the syntac into '[received msgs]'

							seqPropMgr.update(receivedMsgsBean);

							ApplicationMsgProcessor ackProcessor = new ApplicationMsgProcessor();
							ackProcessor.sendAckIfNeeded(rmMsgContext, receivedMsgStr, storageManager);
							
							
							drop = true;

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
			Sequence sequence = (Sequence) rmMsgContext.getMessagePart(Sandesha2Constants.MessageParts.SEQUENCE);
			String sequenceId = null;

			if (sequence != null) {
				sequenceId = sequence.getIdentifier().getIdentifier();
			}

			SequencePropertyBeanMgr seqPropMgr = storageManager.getSequencePropertyBeanMgr();
			SequencePropertyBean receivedMsgsBean = seqPropMgr.retrieve(sequenceId,
					Sandesha2Constants.SequenceProperties.SERVER_COMPLETED_MESSAGES);
			String receivedMsgStr = receivedMsgsBean.getValue();

			ApplicationMsgProcessor ackProcessor = new ApplicationMsgProcessor();
			// Even though the duplicate message is dropped, hv to send the ack
			// if needed.
			ackProcessor.sendAckIfNeeded(rmMsgContext, receivedMsgStr, storageManager);

		}
		if (log.isDebugEnabled())
			log.debug("Exit: SandeshaGlobalInHandler::processDroppedMessage");
	}

	private void doGlobalProcessing(RMMsgContext rmMsgCtx) throws SandeshaException {
	}

	public String getName() {
		return Sandesha2Constants.GLOBAL_IN_HANDLER_NAME;
	}

	private AxisFault getAxisFaultFromFromSOAPFault(SOAPFault faultPart) {
		AxisFault axisFault = new AxisFault(faultPart.getCode(), faultPart.getReason(), faultPart.getNode(), faultPart
				.getRole(), faultPart.getDetail());

		return axisFault;
	}

}
