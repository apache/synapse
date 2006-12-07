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

package org.apache.sandesha2.msgprocessors;

import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.addressing.RelatesTo;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.RMMsgContext;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.client.SandeshaClientConstants;
import org.apache.sandesha2.client.SandeshaListener;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.security.SecurityManager;
import org.apache.sandesha2.security.SecurityToken;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.beanmanagers.CreateSeqBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.SenderBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.SequencePropertyBeanMgr;
import org.apache.sandesha2.storage.beans.CreateSeqBean;
import org.apache.sandesha2.storage.beans.SenderBean;
import org.apache.sandesha2.storage.beans.SequencePropertyBean;
import org.apache.sandesha2.util.MsgInitializer;
import org.apache.sandesha2.util.RMMsgCreator;
import org.apache.sandesha2.util.SOAPAbstractFactory;
import org.apache.sandesha2.util.SandeshaUtil;
import org.apache.sandesha2.util.SequenceManager;
import org.apache.sandesha2.util.SpecSpecificConstants;
import org.apache.sandesha2.wsrm.AckRequested;
import org.apache.sandesha2.wsrm.CreateSequence;
import org.apache.sandesha2.wsrm.Identifier;
import org.apache.sandesha2.wsrm.LastMessage;
import org.apache.sandesha2.wsrm.MessageNumber;
import org.apache.sandesha2.wsrm.Sequence;
import org.apache.sandesha2.wsrm.SequenceOffer;

/**
 * Responsible for processing an incoming Application message.
 */

public class ApplicationMsgProcessor implements MsgProcessor {

	private static final Log log = LogFactory.getLog(ApplicationMsgProcessor.class);

	private String inboundSequence = null;
	
	public ApplicationMsgProcessor() {
		// Nothing to do
	}
	
	public ApplicationMsgProcessor(String inboundSequenceId) {
		this.inboundSequence = inboundSequenceId;
	}
	
	public boolean processInMessage(RMMsgContext rmMsgCtx) {
		if (log.isDebugEnabled()) {
			log.debug("Enter: ApplicationMsgProcessor::processInMessage");
			log.debug("Exit: ApplicationMsgProcessor::processInMessage");
		}
		return false;
	}
	
	public boolean processOutMessage(RMMsgContext rmMsgCtx) throws AxisFault {
		if (log.isDebugEnabled())
			log.debug("Enter: ApplicationMsgProcessor::processOutMessage");

		MessageContext msgContext = rmMsgCtx.getMessageContext();
		ConfigurationContext configContext = msgContext.getConfigurationContext();

		// setting the Fault callback
		SandeshaListener faultCallback = (SandeshaListener) msgContext.getOptions().getProperty(
				SandeshaClientConstants.SANDESHA_LISTENER);
		if (faultCallback != null) {
			OperationContext operationContext = msgContext.getOperationContext();
			if (operationContext != null) {
				operationContext.setProperty(SandeshaClientConstants.SANDESHA_LISTENER, faultCallback);
			}
		}

		StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(configContext, configContext
				.getAxisConfiguration());
		SequencePropertyBeanMgr seqPropMgr = storageManager.getSequencePropertyBeanMgr();

		boolean serverSide = msgContext.isServerSide();

		// setting message Id if null
		if (msgContext.getMessageID() == null)
			msgContext.setMessageID(SandeshaUtil.getUUID());

		// find internal sequence id
		String internalSequenceId = null;

		String storageKey = SandeshaUtil.getUUID(); // the key which will be
													// used to store this
													// message.

		/*
		 * Internal sequence id is the one used to refer to the sequence (since
		 * actual sequence id is not available when first msg arrives) server
		 * side - a derivation of the sequenceId of the incoming sequence client
		 * side - a derivation of wsaTo & SeequenceKey
		 */

		boolean lastMessage = false;
		if (serverSide) {
			if (inboundSequence == null || "".equals(inboundSequence)) {
				String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.incomingSequenceNotValidID, inboundSequence);
				log.debug(message);
				throw new SandeshaException(message);
			}

			internalSequenceId = SandeshaUtil.getOutgoingSideInternalSequenceID(inboundSequence);

			// Deciding weather this is the last message. We assume it is if it relates to
			// a message which arrived with the LastMessage flag on it.
			String lastRequestId = SandeshaUtil.getSequenceProperty(inboundSequence,
					Sandesha2Constants.SequenceProperties.LAST_IN_MSG_ID, storageManager);
			RelatesTo relatesTo = msgContext.getRelatesTo();
			if(relatesTo != null && lastRequestId != null &&
					lastRequestId.equals(relatesTo.getValue())) {
				lastMessage = true;
			}

		} else {
			// set the internal sequence id for the client side.
			EndpointReference toEPR = msgContext.getTo();
			if (toEPR == null || toEPR.getAddress() == null || "".equals(toEPR.getAddress())) {
				String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.toEPRNotValid, null);
				log.debug(message);
				throw new SandeshaException(message);
			}

			String to = toEPR.getAddress();
			String sequenceKey = (String) msgContext.getProperty(SandeshaClientConstants.SEQUENCE_KEY);
			internalSequenceId = SandeshaUtil.getInternalSequenceID(to, sequenceKey);

			String lastAppMessage = (String) msgContext.getProperty(SandeshaClientConstants.LAST_MESSAGE);
			if (lastAppMessage != null && "true".equals(lastAppMessage))
				lastMessage = true;
		}
		
		if (internalSequenceId!=null)
			rmMsgCtx.setProperty(Sandesha2Constants.MessageContextProperties.INTERNAL_SEQUENCE_ID,internalSequenceId);

		String sequencePropertyKey = SandeshaUtil.getSequencePropertyKey(rmMsgCtx);

		/*
		 * checking weather the user has given the messageNumber (most of the
		 * cases this will not be the case where the system will generate the
		 * message numbers
		 */

		// User should set it as a long object.
		Long messageNumberLng = (Long) msgContext.getProperty(SandeshaClientConstants.MESSAGE_NUMBER);

		long givenMessageNumber = -1;
		if (messageNumberLng != null) {
			givenMessageNumber = messageNumberLng.longValue();
			if (givenMessageNumber <= 0) {
				throw new SandeshaException(SandeshaMessageHelper.getMessage(
						SandeshaMessageKeys.msgNumberMustBeLargerThanZero, Long.toString(givenMessageNumber)));
			}
		}

		// the message number that was last used.
		long systemMessageNumber = getPreviousMsgNo(configContext, sequencePropertyKey, storageManager);

		// The number given by the user has to be larger than the last stored
		// number.
		if (givenMessageNumber > 0 && givenMessageNumber <= systemMessageNumber) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.msgNumberNotLargerThanLastMsg, Long
					.toString(givenMessageNumber));
			throw new SandeshaException(message);
		}

		// Finding the correct message number.
		long messageNumber = -1;
		if (givenMessageNumber > 0) // if given message number is valid use it.
									// (this is larger than the last stored due
									// to the last check)
			messageNumber = givenMessageNumber;
		else if (systemMessageNumber > 0) { // if system message number is valid
											// use it.
			messageNumber = systemMessageNumber + 1;
		} else { // This is the first message (systemMessageNumber = -1)
			messageNumber = 1;
		}

		// A dummy message is a one which will not be processed as a actual
		// application message.
		// The RM handlers will simply let these go.
		String dummyMessageString = (String) msgContext.getOptions().getProperty(SandeshaClientConstants.DUMMY_MESSAGE);
		boolean dummyMessage = false;
		if (dummyMessageString != null && Sandesha2Constants.VALUE_TRUE.equals(dummyMessageString))
			dummyMessage = true;

		//see if the sequence is closed
		SequencePropertyBean sequenceClosed = seqPropMgr.retrieve(sequencePropertyKey, Sandesha2Constants.SequenceProperties.SEQUENCE_CLOSED_CLIENT);
		if(sequenceClosed!=null){
			throw new SandeshaException(SandeshaMessageHelper.getMessage(SandeshaMessageKeys.cannotSendMsgAsSequenceClosed, internalSequenceId));
		}
		
		//see if the sequence is terminated
		SequencePropertyBean sequenceTerminated = seqPropMgr.retrieve(internalSequenceId, Sandesha2Constants.SequenceProperties.TERMINATE_ADDED);
		if(sequenceTerminated!=null){
			throw new SandeshaException(SandeshaMessageHelper.getMessage(SandeshaMessageKeys.cannotSendMsgAsSequenceTerminated, internalSequenceId));
		}

		// saving the used message number
		if (!dummyMessage)
			setNextMsgNo(configContext, sequencePropertyKey, messageNumber, storageManager);

		// set this as the response highest message.
		SequencePropertyBean responseHighestMsgBean = new SequencePropertyBean(sequencePropertyKey,
				Sandesha2Constants.SequenceProperties.HIGHEST_OUT_MSG_NUMBER, new Long(messageNumber).toString());
		seqPropMgr.insert(responseHighestMsgBean);
		RelatesTo relatesTo = msgContext.getRelatesTo();
		if(relatesTo != null) {
			SequencePropertyBean responseRelatesToBean = new SequencePropertyBean(sequencePropertyKey,
					Sandesha2Constants.SequenceProperties.HIGHEST_OUT_RELATES_TO, relatesTo.getValue());
			seqPropMgr.insert(responseRelatesToBean);
		}

		if (lastMessage) {
			SequencePropertyBean responseLastMsgKeyBean = new SequencePropertyBean(sequencePropertyKey,
					Sandesha2Constants.SequenceProperties.LAST_OUT_MESSAGE_NO, new Long(messageNumber).toString());

			seqPropMgr.insert(responseLastMsgKeyBean);
		}

		boolean sendCreateSequence = false;

		String outSequenceID = SandeshaUtil.getSequenceIDFromInternalSequenceID(internalSequenceId, storageManager);

		// setting async ack endpoint for the server side. (if present)
		if (serverSide) {
//			String incomingSequenceID = SandeshaUtil.getServerSideIncomingSeqIdFromInternalSeqId(internalSequenceId);
			SequencePropertyBean incomingToBean = seqPropMgr.retrieve(sequencePropertyKey,
					Sandesha2Constants.SequenceProperties.TO_EPR);
			if (incomingToBean != null) {
				String incomingTo = incomingToBean.getValue();
				msgContext.setProperty(SandeshaClientConstants.AcksTo, incomingTo);
			}
		}

		// FINDING THE SPEC VERSION
		String specVersion = null;
		if (msgContext.isServerSide()) {
			// in the server side, get the RM version from the request sequence.
			MessageContext requestMessageContext;
			try {
				requestMessageContext = msgContext.getOperationContext().getMessageContext(
						WSDLConstants.MESSAGE_LABEL_IN_VALUE);
			} catch (AxisFault e) {
				throw new SandeshaException(e);
			}

			if (requestMessageContext == null) {
				throw new SandeshaException(SandeshaMessageHelper.getMessage(SandeshaMessageKeys.requestMsgContextNull));
			}

			RMMsgContext requestRMMsgCtx = MsgInitializer.initializeMessage(requestMessageContext);
			
			Sequence sequence = (Sequence) requestRMMsgCtx.getMessagePart(Sandesha2Constants.MessageParts.SEQUENCE);
			String requestSequenceID = sequence.getIdentifier().getIdentifier();
			
			String requestSideSequencePropertyKey = SandeshaUtil.getSequencePropertyKey(requestRMMsgCtx);
			
			SequencePropertyBean specVersionBean = seqPropMgr.retrieve(requestSideSequencePropertyKey,
					Sandesha2Constants.SequenceProperties.RM_SPEC_VERSION);
			if (specVersionBean == null) {
				throw new SandeshaException(SandeshaMessageHelper.getMessage(
						SandeshaMessageKeys.specVersionPropertyNotAvailable, requestSequenceID));
			}

			specVersion = specVersionBean.getValue();
		} else {
			// in the client side, user will set the RM version.
			specVersion = (String) msgContext.getProperty(SandeshaClientConstants.RM_SPEC_VERSION);
		}

		if (specVersion == null)
			specVersion = SpecSpecificConstants.getDefaultSpecVersion(); // TODO
																			// change
																			// the
																			// default
																			// to
																			// v1_1.

		if (messageNumber == 1) {
			if (outSequenceID == null) { // out sequence will be set for the
										// server side, in the case of an offer.
				sendCreateSequence = true; // message number being one and not
											// having an out sequence, implies
											// that a create sequence has to be
											// send.
			}

			// if first message - setup the sending side sequence - both for the
			// server and the client sides
			SequenceManager.setupNewClientSequence(msgContext, sequencePropertyKey, specVersion, storageManager);
		}

		ServiceContext serviceContext = msgContext.getServiceContext();
		OperationContext operationContext = msgContext.getOperationContext();

		// SENDING THE CREATE SEQUENCE.
		EndpointReference acksToEPR = new EndpointReference(null); //use this to hold the acksTo EPR
		if (sendCreateSequence) {
			SequencePropertyBean responseCreateSeqAdded = seqPropMgr.retrieve(sequencePropertyKey,
					Sandesha2Constants.SequenceProperties.OUT_CREATE_SEQUENCE_SENT);

			String addressingNamespaceURI = SandeshaUtil.getSequenceProperty(sequencePropertyKey,
					Sandesha2Constants.SequenceProperties.ADDRESSING_NAMESPACE_VALUE, storageManager);
			String anonymousURI = SpecSpecificConstants.getAddressingAnonymousURI(addressingNamespaceURI);

			if (responseCreateSeqAdded == null) {
				responseCreateSeqAdded = new SequencePropertyBean(sequencePropertyKey,
						Sandesha2Constants.SequenceProperties.OUT_CREATE_SEQUENCE_SENT, "true");
				seqPropMgr.insert(responseCreateSeqAdded);

				if (serviceContext != null)
						acksToEPR.setAddress((String) msgContext.getProperty(SandeshaClientConstants.AcksTo));

				if (msgContext.isServerSide()) {
					// we do not set acksTo value to anonymous when the create
					// sequence is send from the server.
					MessageContext requestMessage;
					try {
						requestMessage = operationContext
								.getMessageContext(WSDLConstants.MESSAGE_LABEL_IN_VALUE);
					} catch (AxisFault e) {
						throw new SandeshaException(e);
					}

					if (requestMessage == null) {
						String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.requestMsgNotPresent);
						log.debug(message);
						throw new SandeshaException(message);
					}
					acksToEPR = requestMessage.getTo();

				} else {
					if (acksToEPR.getAddress() == null){
						EndpointReference replyToEPR = msgContext.getReplyTo();
						
						if(replyToEPR!=null && !replyToEPR.getAddress().equals("")){
							//use the replyTo address as acksTo
							if (log.isDebugEnabled())
								log.debug("Using replyTo " + replyToEPR + " EPR as AcksTo, addr=" + acksToEPR.getAddress());
							
							acksToEPR = replyToEPR;
						}
						else{
							acksToEPR.setAddress(anonymousURI);
						}
					}
				}

				if (acksToEPR.getAddress()!=null && !anonymousURI.equals(acksToEPR.getAddress()) && !serverSide) {
					String transportIn = (String) configContext // TODO verify
							.getProperty(MessageContext.TRANSPORT_IN);
					if (transportIn == null)
						transportIn = org.apache.axis2.Constants.TRANSPORT_HTTP;
				} else if (acksToEPR.getAddress() == null && serverSide) {
//					String incomingSequencId = SandeshaUtil
//							.getServerSideIncomingSeqIdFromInternalSeqId(internalSequenceId);
					
					try {
						MessageContext requestMsgContext = operationContext.getMessageContext(WSDLConstants.MESSAGE_LABEL_IN_VALUE);
						RMMsgContext requestRMMsgContext = MsgInitializer.initializeMessage(requestMsgContext);
						
						String requestSideSequencePropertyKey = SandeshaUtil.getSequencePropertyKey(requestRMMsgContext);
						
						SequencePropertyBean bean = seqPropMgr.retrieve(requestSideSequencePropertyKey,
								Sandesha2Constants.SequenceProperties.REPLY_TO_EPR);
						if (bean != null) {
							String beanAcksToValue = bean.getValue();
							if (beanAcksToValue != null)
								acksToEPR.setAddress(beanAcksToValue);
						}
					} catch (AxisFault e) {
						throw new SandeshaException (e);
					}
				} else if (anonymousURI.equals(acksToEPR.getAddress())) {
					// set transport in.
					Object trIn = msgContext.getProperty(MessageContext.TRANSPORT_IN);
					if (trIn == null) {
						// TODO
					}
				}
				addCreateSequenceMessage(rmMsgCtx, sequencePropertyKey ,internalSequenceId, acksToEPR, storageManager);
			}
		}

		SOAPEnvelope env = rmMsgCtx.getSOAPEnvelope();
		if (env == null) {
			SOAPEnvelope envelope = SOAPAbstractFactory.getSOAPFactory(SandeshaUtil.getSOAPVersion(env))
					.getDefaultEnvelope();
			rmMsgCtx.setSOAPEnvelop(envelope);
		}

		SOAPBody soapBody = rmMsgCtx.getSOAPEnvelope().getBody();
		if (soapBody == null) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.soapBodyNotPresent);
			log.debug(message);
			throw new SandeshaException(message);
		}

		String messageId1 = SandeshaUtil.getUUID();
		if (rmMsgCtx.getMessageId() == null) {
			rmMsgCtx.setMessageId(messageId1);
		}

		if (serverSide) {
			// let the request end with 202 if a ack has not been
			// written in the incoming thread.

			MessageContext reqMsgCtx = null;
			try {
				reqMsgCtx = msgContext.getOperationContext().getMessageContext(
						WSDLConstants.MESSAGE_LABEL_IN_VALUE);
			} catch (AxisFault e) {
				throw new SandeshaException(e);
			}

			if (reqMsgCtx.getProperty(Sandesha2Constants.ACK_WRITTEN) == null
					|| !"true".equals(reqMsgCtx.getProperty(Sandesha2Constants.ACK_WRITTEN)))
				reqMsgCtx.getOperationContext().setProperty(org.apache.axis2.Constants.RESPONSE_WRITTEN, "false");
		}

		EndpointReference toEPR = msgContext.getTo();
		if (toEPR == null) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.toEPRNotValid, null);
			log.debug(message);
			throw new SandeshaException(message);
		}

		// setting default actions.
		String to = toEPR.getAddress();
		String operationName = msgContext.getOperationContext().getAxisOperation().getName().getLocalPart();
		if (msgContext.getWSAAction() == null) {
			msgContext.setWSAAction(to + "/" + operationName);
		}
		if (msgContext.getSoapAction() == null) {
			msgContext.setSoapAction("\"" + to + "/" + operationName + "\"");
		}

		// processing the response if not an dummy.
		if (!dummyMessage)
			processResponseMessage(rmMsgCtx, internalSequenceId, outSequenceID, messageNumber, storageKey, storageManager);

		
		//Users wont be able to get reliable response msgs in the back channel in the back channel of a 
		//reliable message. If he doesn't have a endpoint he should use polling mechanisms.
		msgContext.pause();
		
		if (log.isDebugEnabled())
			log.debug("Exit: ApplicationMsgProcessor::processOutMessage " + Boolean.TRUE);
		return true;
	}

	private void addCreateSequenceMessage(RMMsgContext applicationRMMsg, String sequencePropertyKey, String internalSequenceId, EndpointReference acksTo,
			StorageManager storageManager) throws AxisFault {

		if (log.isDebugEnabled())
			log.debug("Enter: ApplicationMsgProcessor::addCreateSequenceMessage, " + internalSequenceId);

		MessageContext applicationMsg = applicationRMMsg.getMessageContext();
		ConfigurationContext configCtx = applicationMsg.getConfigurationContext();

		// generating a new create sequeuce message.
		RMMsgContext createSeqRMMessage = RMMsgCreator.createCreateSeqMsg(applicationRMMsg, sequencePropertyKey, acksTo,
				storageManager);

		createSeqRMMessage.setFlow(MessageContext.OUT_FLOW);
		CreateSequence createSequencePart = (CreateSequence) createSeqRMMessage
				.getMessagePart(Sandesha2Constants.MessageParts.CREATE_SEQ);

		SequencePropertyBeanMgr seqPropMgr = storageManager.getSequencePropertyBeanMgr();
		CreateSeqBeanMgr createSeqMgr = storageManager.getCreateSeqBeanMgr();
		SenderBeanMgr retransmitterMgr = storageManager.getRetransmitterBeanMgr();

		SequenceOffer offer = createSequencePart.getSequenceOffer();
		if (offer != null) {
			String offeredSequenceId = offer.getIdentifer().getIdentifier();

			SequencePropertyBean offeredSequenceBean = new SequencePropertyBean();
			offeredSequenceBean.setName(Sandesha2Constants.SequenceProperties.OFFERED_SEQUENCE);
			offeredSequenceBean.setSequencePropertyKey(sequencePropertyKey);
			offeredSequenceBean.setValue(offeredSequenceId);

			seqPropMgr.insert(offeredSequenceBean);
		}

		MessageContext createSeqMsg = createSeqRMMessage.getMessageContext();
		createSeqMsg.setRelationships(null); // create seq msg does not
												// relateTo anything
		
		// Set that the create sequence message is part of a transaction.
		createSeqMsg.setProperty(Sandesha2Constants.WITHIN_TRANSACTION, Sandesha2Constants.VALUE_TRUE);
		
		String createSequenceMessageStoreKey = SandeshaUtil.getUUID(); // the key that will be used to store 
																	   //the create sequence message.
		
		CreateSeqBean createSeqBean = new CreateSeqBean();
		createSeqBean.setInternalSequenceID(internalSequenceId);
		createSeqBean.setCreateSeqMsgID(createSeqMsg.getMessageID());
		createSeqBean.setCreateSequenceMsgStoreKey(createSequenceMessageStoreKey);
		
		//cloning the message and storing it as a reference.
		MessageContext clonedMessage = SandeshaUtil.cloneMessageContext(createSeqMsg);
		String clonedMsgStoreKey = SandeshaUtil.getUUID();
		storageManager.storeMessageContext(clonedMsgStoreKey, clonedMessage);
		createSeqBean.setReferenceMessageStoreKey(clonedMsgStoreKey);
		
		
		//TODO set the replyTo of CreateSeq (and others) to Anymomous if Application Msgs hv it as Anonymous.
		
//		//checking whether the sequence is in polling mode.
//		boolean pollingMode = false;
//		EndpointReference replyTo = applicationRMMsg.getReplyTo();
//		if (replyTo!=null && SandeshaUtil.isWSRMAnonymousReplyTo(replyTo.getAddress()))
//			pollingMode = true;
//		else if (replyTo!=null && offer!=null && 
//				(AddressingConstants.Final.WSA_ANONYMOUS_URL.equals(replyTo.getAddress()) || 
//						AddressingConstants.Submission.WSA_ANONYMOUS_URL.equals(replyTo.getAddress())))
//			pollingMode = true;
//		
//		createSeqBean.setPollingMode(pollingMode);
		
//		//if PollingMode is true, starting the pollingmanager.
//		if (pollingMode)
//			SandeshaUtil.startPollingManager(configCtx);
		
		SecurityToken token = (SecurityToken) createSeqRMMessage.getProperty(Sandesha2Constants.SequenceProperties.SECURITY_TOKEN);
		if(token != null) {
			SecurityManager secManager = SandeshaUtil.getSecurityManager(configCtx);
			createSeqBean.setSecurityTokenData(secManager.getTokenRecoveryData(token));
		}
		
		createSeqMgr.insert(createSeqBean);

		String addressingNamespaceURI = SandeshaUtil.getSequenceProperty(sequencePropertyKey,
				Sandesha2Constants.SequenceProperties.ADDRESSING_NAMESPACE_VALUE, storageManager);
		String anonymousURI = SpecSpecificConstants.getAddressingAnonymousURI(addressingNamespaceURI);

		if (createSeqMsg.getReplyTo() == null)
			createSeqMsg.setReplyTo(new EndpointReference(anonymousURI));

		SenderBean createSeqEntry = new SenderBean();
		createSeqEntry.setMessageContextRefKey(createSequenceMessageStoreKey);
		createSeqEntry.setTimeToSend(System.currentTimeMillis());
		createSeqEntry.setMessageID(createSeqRMMessage.getMessageId());
		createSeqEntry.setInternalSequenceID(sequencePropertyKey);
		// this will be set to true in the sender
		createSeqEntry.setSend(true);
		// Indicate that this message is a create sequence
		createSeqEntry.setMessageType(Sandesha2Constants.MessageTypes.CREATE_SEQ);
		EndpointReference to = createSeqRMMessage.getTo();
		if (to!=null)
			createSeqEntry.setToAddress(to.getAddress());

		createSeqMsg.setProperty(Sandesha2Constants.QUALIFIED_FOR_SENDING, Sandesha2Constants.VALUE_FALSE);
		
		SandeshaUtil.executeAndStore(createSeqRMMessage, createSequenceMessageStoreKey);

		retransmitterMgr.insert(createSeqEntry);

		if (log.isDebugEnabled())
			log.debug("Exit: ApplicationMsgProcessor::addCreateSequenceMessage");
	}

	private void processResponseMessage(RMMsgContext rmMsg, String internalSequenceId, String outSequenceID, long messageNumber,
			String storageKey, StorageManager storageManager) throws AxisFault {
		if (log.isDebugEnabled())
			log.debug("Enter: ApplicationMsgProcessor::processResponseMessage, " + internalSequenceId + ", " + outSequenceID);

		MessageContext msg = rmMsg.getMessageContext();

		SequencePropertyBeanMgr sequencePropertyMgr = storageManager.getSequencePropertyBeanMgr();
		SenderBeanMgr retransmitterMgr = storageManager.getRetransmitterBeanMgr();

		SequencePropertyBean toBean = sequencePropertyMgr.retrieve(internalSequenceId,
				Sandesha2Constants.SequenceProperties.TO_EPR);
		SequencePropertyBean replyToBean = sequencePropertyMgr.retrieve(internalSequenceId,
				Sandesha2Constants.SequenceProperties.REPLY_TO_EPR);

		if (toBean == null) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.toEPRNotValid, null);
			log.debug(message);
			throw new SandeshaException(message);
		}

		EndpointReference toEPR = new EndpointReference(toBean.getValue());

		EndpointReference replyToEPR = null;
		if (replyToBean != null) {
			replyToEPR = new EndpointReference(replyToBean.getValue());
		}

		if (toEPR == null || toEPR.getAddress() == null || "".equals(toEPR.getAddress())) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.toEPRNotValid, null);
			log.debug(message);
			throw new SandeshaException(message);
		}

		String newToStr = null;
		if (msg.isServerSide()) {

			MessageContext requestMsg = msg.getOperationContext().getMessageContext(
					WSDLConstants.MESSAGE_LABEL_IN_VALUE);
			if (requestMsg != null) {
				newToStr = requestMsg.getReplyTo().getAddress();
			}
		}

		if (newToStr != null)
			rmMsg.setTo(new EndpointReference(newToStr));
		else
			rmMsg.setTo(toEPR);

		if (replyToEPR != null)
			rmMsg.setReplyTo(replyToEPR);

		String rmVersion = SandeshaUtil.getRMVersion(internalSequenceId, storageManager);
		if (rmVersion == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.specVersionPropertyNotAvailable, internalSequenceId));

		String rmNamespaceValue = SpecSpecificConstants.getRMNamespaceValue(rmVersion);

		Sequence sequence = new Sequence(rmNamespaceValue);
		MessageNumber msgNumber = new MessageNumber(rmNamespaceValue);
		msgNumber.setMessageNumber(messageNumber);
		sequence.setMessageNumber(msgNumber);

		// setting last message
		if (msg.isServerSide()) {
			MessageContext requestMsg = null;

			requestMsg = msg.getOperationContext()
					.getMessageContext(WSDLConstants.MESSAGE_LABEL_IN_VALUE);

			RMMsgContext reqRMMsgCtx = MsgInitializer.initializeMessage(requestMsg);
			Sequence requestSequence = (Sequence) reqRMMsgCtx.getMessagePart(Sandesha2Constants.MessageParts.SEQUENCE);
			if (requestSequence == null) {
				String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.requestSeqIsNull);
				log.debug(message);
				throw new SandeshaException(message);
			}

			if (requestSequence.getLastMessage() != null) {
				sequence.setLastMessage(new LastMessage(rmNamespaceValue));
			}

		} else {
			// client side

			OperationContext operationContext = msg.getOperationContext();
			if (operationContext != null) {
				Object obj = msg.getProperty(SandeshaClientConstants.LAST_MESSAGE);
				if (obj != null && "true".equals(obj)) {

					SequencePropertyBean specVersionBean = sequencePropertyMgr.retrieve(internalSequenceId,
							Sandesha2Constants.SequenceProperties.RM_SPEC_VERSION);
					if (specVersionBean == null)
						throw new SandeshaException(SandeshaMessageHelper
								.getMessage(SandeshaMessageKeys.specVersionNotSet));

					String specVersion = specVersionBean.getValue();
					if (SpecSpecificConstants.isLastMessageIndicatorRequired(specVersion))
						sequence.setLastMessage(new LastMessage(rmNamespaceValue));
				}
			}
		}

		AckRequested ackRequested = null;

		boolean addAckRequested = false;
		// if (!lastMessage)
		// addAckRequested = true; //TODO decide the policy to add the
		// ackRequested tag

		// setting the Sequence id.
		// Set send = true/false depending on the availability of the out
		// sequence id.
		String identifierStr = null;
		if (outSequenceID == null) {
			identifierStr = Sandesha2Constants.TEMP_SEQUENCE_ID;

		} else {
			identifierStr = outSequenceID;
		}

		Identifier id1 = new Identifier(rmNamespaceValue);
		id1.setIndentifer(identifierStr);
		sequence.setIdentifier(id1);
		rmMsg.setMessagePart(Sandesha2Constants.MessageParts.SEQUENCE, sequence);

		if (addAckRequested) {
			ackRequested = new AckRequested(rmNamespaceValue);
			Identifier id2 = new Identifier(rmNamespaceValue);
			id2.setIndentifer(identifierStr);
			ackRequested.setIdentifier(id2);
			rmMsg.setMessagePart(Sandesha2Constants.MessageParts.ACK_REQUEST, ackRequested);
		}


		rmMsg.addSOAPEnvelope();


		// Retransmitter bean entry for the application message
		SenderBean appMsgEntry = new SenderBean();

		appMsgEntry.setMessageContextRefKey(storageKey);

		appMsgEntry.setTimeToSend(System.currentTimeMillis());
		appMsgEntry.setMessageID(rmMsg.getMessageId());
		appMsgEntry.setMessageNumber(messageNumber);
		appMsgEntry.setMessageType(Sandesha2Constants.MessageTypes.APPLICATION);
		if (outSequenceID == null) {
			appMsgEntry.setSend(false);
		} else {
			appMsgEntry.setSend(true);
			// Send will be set to true at the sender.
			msg.setProperty(Sandesha2Constants.SET_SEND_TO_TRUE, Sandesha2Constants.VALUE_TRUE);
		}
		
		EndpointReference to = rmMsg.getTo();
		if (to!=null)
			appMsgEntry.setToAddress(to.getAddress());
		
		appMsgEntry.setInternalSequenceID(internalSequenceId);

		msg.setProperty(Sandesha2Constants.QUALIFIED_FOR_SENDING, Sandesha2Constants.VALUE_FALSE);

		// increasing the current handler index, so that the message will not be
		// going throught the SandeshaOutHandler again.
		msg.setCurrentHandlerIndex(msg.getCurrentHandlerIndex() + 1);

		SandeshaUtil.executeAndStore(rmMsg, storageKey);

		retransmitterMgr.insert(appMsgEntry);

		if (log.isDebugEnabled())
			log.debug("Exit: ApplicationMsgProcessor::processResponseMessage");
	}

	private long getPreviousMsgNo(ConfigurationContext context, String sequencePropertyKey, StorageManager storageManager)
			throws SandeshaException {
		if (log.isDebugEnabled())
			log.debug("Enter: ApplicationMsgProcessor::getPreviousMsgNo, " + sequencePropertyKey);

		SequencePropertyBeanMgr seqPropMgr = storageManager.getSequencePropertyBeanMgr();

		SequencePropertyBean nextMsgNoBean = seqPropMgr.retrieve(sequencePropertyKey,
				Sandesha2Constants.SequenceProperties.NEXT_MESSAGE_NUMBER);

		long nextMsgNo = -1;
		if (nextMsgNoBean != null) {
			Long nextMsgNoLng = new Long(nextMsgNoBean.getValue());
			nextMsgNo = nextMsgNoLng.longValue();
		}

		if (log.isDebugEnabled())
			log.debug("Exit: ApplicationMsgProcessor::getPreviousMsgNo, " + nextMsgNo);

		return nextMsgNo;
	}

	private void setNextMsgNo(ConfigurationContext context, String sequencePropertyKey, long msgNo,
			StorageManager storageManager) throws SandeshaException {

		if (log.isDebugEnabled())
			log.debug("Enter: ApplicationMsgProcessor::setNextMsgNo, " + sequencePropertyKey + ", " + msgNo);

		if (msgNo <= 0) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.msgNumberMustBeLargerThanZero, Long
					.toString(msgNo));
			throw new SandeshaException(message);
		}

		SequencePropertyBeanMgr seqPropMgr = storageManager.getSequencePropertyBeanMgr();

		SequencePropertyBean nextMsgNoBean = seqPropMgr.retrieve(sequencePropertyKey,
				Sandesha2Constants.SequenceProperties.NEXT_MESSAGE_NUMBER);

		boolean update = true;
		if (nextMsgNoBean == null) {
			update = false;
			nextMsgNoBean = new SequencePropertyBean();
			nextMsgNoBean.setSequencePropertyKey(sequencePropertyKey);
			nextMsgNoBean.setName(Sandesha2Constants.SequenceProperties.NEXT_MESSAGE_NUMBER);
		}

		nextMsgNoBean.setValue(new Long(msgNo).toString());
		if (update)
			seqPropMgr.update(nextMsgNoBean);
		else
			seqPropMgr.insert(nextMsgNoBean);

		if (log.isDebugEnabled())
			log.debug("Exit: ApplicationMsgProcessor::setNextMsgNo");
	}
	
}
