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

package org.apache.sandesha2.workers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.context.OperationContextFactory;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.OutOnlyAxisOperation;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.engine.Handler.InvocationResponse;
import org.apache.axis2.transport.RequestResponseTransport;
import org.apache.axis2.transport.TransportUtils;
import org.apache.axis2.transport.RequestResponseTransport.RequestResponseTransportStatus;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.RMMsgContext;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.storage.SandeshaStorageException;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.Transaction;
import org.apache.sandesha2.storage.beanmanagers.SenderBeanMgr;
import org.apache.sandesha2.storage.beans.RMDBean;
import org.apache.sandesha2.storage.beans.RMSBean;
import org.apache.sandesha2.storage.beans.SenderBean;
import org.apache.sandesha2.util.AcknowledgementManager;
import org.apache.sandesha2.util.MessageRetransmissionAdjuster;
import org.apache.sandesha2.util.MsgInitializer;
import org.apache.sandesha2.util.RMMsgCreator;
import org.apache.sandesha2.util.SandeshaUtil;
import org.apache.sandesha2.util.SpecSpecificConstants;
import org.apache.sandesha2.util.TerminateManager;
import org.apache.sandesha2.wsrm.AckRequested;
import org.apache.sandesha2.wsrm.CloseSequence;
import org.apache.sandesha2.wsrm.Identifier;
import org.apache.sandesha2.wsrm.Sequence;
import org.apache.sandesha2.wsrm.SequenceAcknowledgement;
import org.apache.sandesha2.wsrm.TerminateSequence;

public class SenderWorker extends SandeshaWorker implements Runnable {

  private static final Log log = LogFactory.getLog(SenderWorker.class);

	private ConfigurationContext configurationContext = null;
	private SenderBean senderBean = null;
	private RMMsgContext messageToSend = null;
	private String rmVersion = null;

	public SenderWorker (ConfigurationContext configurationContext, SenderBean senderBean, String rmVersion) {
		this.configurationContext = configurationContext;
		this.senderBean = senderBean;
		this.rmVersion = rmVersion; 
	}
	
	public void setMessage(RMMsgContext msg) {
		this.messageToSend = msg;
	}
	
	public void run () {
		
		if (log.isDebugEnabled())
			log.debug("Enter: SenderWorker::run");
		
	    // If we are not the holder of the correct lock, then we have to stop
	    if(lock != null && !lock.ownsLock(workId, this)) {
	      if (log.isDebugEnabled()) log.debug("Exit: SenderWorker::run, another worker holds the lock");
	      return;
	    }

		Transaction transaction = null;
		
		try {
			StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(configurationContext, configurationContext.getAxisConfiguration());
			SenderBeanMgr senderBeanMgr = storageManager.getSenderBeanMgr();
			
			transaction = storageManager.getTransaction();

			String key = senderBean.getMessageContextRefKey();
			MessageContext msgCtx = null;
			RMMsgContext   rmMsgCtx = null;
			if(messageToSend != null) {
				msgCtx = messageToSend.getMessageContext();
				rmMsgCtx = messageToSend;
			} else {
				msgCtx = storageManager.retrieveMessageContext(key, configurationContext);
      
				if (msgCtx == null) {
					// This sender bean has already been processed
					
					if(transaction != null && transaction.isActive()) transaction.commit();
					transaction = null;

					return;
				}
      
				rmMsgCtx = MsgInitializer.initializeMessage(msgCtx);
			}

			// sender will not send the message if following property is
			// set and not true.
			// But it will set if it is not set (null)

			// This is used to make sure that the mesage get passed the
			// Sandesha2TransportSender.

			String qualifiedForSending = (String) msgCtx.getProperty(Sandesha2Constants.QUALIFIED_FOR_SENDING);
			if (qualifiedForSending != null && !qualifiedForSending.equals(Sandesha2Constants.VALUE_TRUE)) {
				if (log.isDebugEnabled())
					log.debug("Exit: SenderWorker::run, !qualified for sending");
				
				if(transaction != null && transaction.isActive()) {
					transaction.commit();
					transaction = null;
				}
				
				return;
			}

			if (msgCtx == null) {
				if (log.isDebugEnabled())
					log.debug(SandeshaMessageHelper.getMessage(SandeshaMessageKeys.sendHasUnavailableMsgEntry));
				
				if(transaction != null && transaction.isActive()) {
					transaction.commit();
					transaction = null;
				}
				
				return;			
			}

			// operation is the lowest level Sandesha2 should be attached
			ArrayList<Integer> msgsNotToSend = SandeshaUtil.getPropertyBean(msgCtx.getAxisOperation()).getMsgTypesToDrop();

			if (msgsNotToSend != null && msgsNotToSend.contains(new Integer(rmMsgCtx.getMessageType()))) {
				if (log.isDebugEnabled())
					log.debug("Exit: SenderWorker::run, message type to be dropped " + rmMsgCtx.getMessageType());
				
				if(transaction != null && transaction.isActive()) {
					transaction.commit();
					transaction = null;
				}
				
				return;	
			}

			// If we are sending to the anonymous URI then we _must_ have a transport waiting,
			// or the message can't go anywhere. If there is nothing here then we leave the
			// message in the sender queue, and a MakeConnection (or a retransmitted request)
			// will hopefully pick it up soon.
			Boolean makeConnection = (Boolean) msgCtx.getProperty(Sandesha2Constants.MAKE_CONNECTION_RESPONSE);
			EndpointReference toEPR = msgCtx.getTo();

			MessageContext inMsg = null;
			OperationContext op = msgCtx.getOperationContext();
			
			RequestResponseTransport t = (RequestResponseTransport) msgCtx.getProperty(RequestResponseTransport.TRANSPORT_CONTROL);
			
			if (t==null) {
				if (op != null)
					inMsg = op.getMessageContext(WSDLConstants.MESSAGE_LABEL_IN_VALUE);
				if (inMsg != null)
					t = (RequestResponseTransport) inMsg.getProperty(RequestResponseTransport.TRANSPORT_CONTROL);
			}

			// If we are anonymous, and this is not a makeConnection, then we must have a transport waiting
			if((toEPR==null || toEPR.hasAnonymousAddress()) &&
			   (makeConnection == null || !makeConnection.booleanValue()) &&
			   (t == null || !t.getStatus().equals(RequestResponseTransportStatus.WAITING))) {
				
				// Mark this sender bean so that we know that the transport is unavailable, if the
				// bean is still stored.
				SenderBean bean = senderBeanMgr.retrieve(senderBean.getMessageID());
				if(bean != null && bean.isTransportAvailable()) {
					bean.setTransportAvailable(false);
					senderBeanMgr.update(bean);
				}
				
				// Commit the update
				if(transaction != null && transaction.isActive()) transaction.commit();
				transaction = null;
				
				if (log.isDebugEnabled())
					log.debug("Exit: SenderWorker::run, no response transport for anonymous message");
				return;
			}
			
			int messageType = senderBean.getMessageType();
			
			if (isAckPiggybackableMsgType(messageType)) {
				// Commit the update
				if(transaction != null && transaction.isActive()) transaction.commit();
				transaction = storageManager.getTransaction();

				// Piggyback ack messages based on the 'To' address of the message
				AcknowledgementManager.piggybackAcksIfPresent(rmMsgCtx, storageManager);
			}
			if (transaction != null && transaction.isActive()) 
				transaction.commit();			
			
			transaction = storageManager.getTransaction();
			
			senderBean = updateMessage(rmMsgCtx,senderBean,storageManager);

			if (senderBean == null) { 
				if (log.isDebugEnabled())
					log.debug("Exit: SenderWorker::run, !continueSending");
				
				if(transaction != null && transaction.isActive()) {
					transaction.rollback();
					transaction = null;
				}
				
				return;
			}

			// Although not actually sent yet, update the send count to indicate an attempt
			if (senderBean.isReSend()) {
				senderBeanMgr.update(senderBean);
			}

			// sending the message
			boolean successfullySent = false;
			
			// No need to redecorate application messages ... only for rm protocol messages
			if(Sandesha2Constants.MessageTypes.APPLICATION!=senderBean.getMessageType()){
				//try to redecorate the EPR if necessary
				if (log.isDebugEnabled())
					log.debug("Redecorate EPR : " + msgCtx.getEnvelope().getHeader());
				EndpointReference replyToEPR = msgCtx.getReplyTo();
				if(replyToEPR!=null){
					replyToEPR = SandeshaUtil.getEPRDecorator(msgCtx.getConfigurationContext()).decorateEndpointReference(replyToEPR);
					msgCtx.setReplyTo(replyToEPR); 
				}
			}
			// have to commit the transaction before sending. This may
			// get changed when WS-AT is available.
			if(transaction != null) {
				transaction.commit();
				transaction = null;
			}

			msgCtx.getOptions().setTimeOutInMilliSeconds(1000000);
			
			boolean processResponseForFaults = false ;
			try {
				InvocationResponse response = InvocationResponse.CONTINUE;
				
		        if(storageManager.requiresMessageSerialization()) {
					if(msgCtx.isPaused()) {
						if (log.isDebugEnabled())
							log.debug("Resuming a send for message : " + msgCtx.getEnvelope().getHeader());
						msgCtx.setPaused(false);
						msgCtx.setProperty(MessageContext.TRANSPORT_NON_BLOCKING, Boolean.FALSE);
						response = AxisEngine.resumeSend(msgCtx);
					} else {
						if (log.isDebugEnabled())
							log.debug("Sending a message : " + msgCtx.getEnvelope().getHeader());
						msgCtx.setProperty(MessageContext.TRANSPORT_NON_BLOCKING, Boolean.FALSE);
						AxisEngine.send(msgCtx);  // TODO check if this should return an invocation response
					}
				} else {
	
					ArrayList retransmittablePhases = (ArrayList) msgCtx.getProperty(Sandesha2Constants.RETRANSMITTABLE_PHASES);
					if (retransmittablePhases!=null) {
						msgCtx.setExecutionChain(retransmittablePhases);
					} else {
						ArrayList emptyExecutionChain = new ArrayList ();
						msgCtx.setExecutionChain(emptyExecutionChain);
					}
					
					msgCtx.setCurrentHandlerIndex(0);
					msgCtx.setCurrentPhaseIndex(0);
					msgCtx.setPaused(false);
				
					if (log.isDebugEnabled())
						log.debug("Resuming a send for message : " + msgCtx.getEnvelope().getHeader());
			        msgCtx.setProperty(MessageContext.TRANSPORT_NON_BLOCKING, Boolean.FALSE);
					response = AxisEngine.resumeSend(msgCtx);
				}
				if(log.isDebugEnabled()) log.debug("Engine resume returned " + response);
				if(response != InvocationResponse.SUSPEND) {
					if(t != null) {
						if(log.isDebugEnabled()) log.debug("Signalling transport in " + t);
						t.signalResponseReady();
					}
				}
				
				successfullySent = true;
				
			} catch (AxisFault e) {
				//this is a possible SOAP 1.2 Fault. So letting it proceed.
				
				processResponseForFaults = true;
				
				recordError(e, rmMsgCtx, storageManager);
				
			} catch (Exception e) {
				String message = SandeshaMessageHelper.getMessage(
						SandeshaMessageKeys.sendMsgError, e.toString());
				
				if (log.isDebugEnabled())
				  log.debug(message, e);
				
				recordError(e, rmMsgCtx, storageManager);
				
			}
			// Establish the transaction for post-send processing
			transaction = storageManager.getTransaction();

			// update or delete only if the object is still present.
			SenderBean bean1 = senderBeanMgr
					.retrieve(senderBean.getMessageID());
			if (bean1 != null) {
				if (senderBean.isReSend()) {
					bean1.setTimeToSend(senderBean.getTimeToSend());
					senderBeanMgr.update(bean1);
				} else {
					senderBeanMgr.delete(bean1.getMessageID());

					// removing the message from the storage.
					String messageStoredKey = bean1.getMessageContextRefKey();
					storageManager.removeMessageContext(messageStoredKey);
				}
			}

			// Commit the transaction to release the SenderBean

			if (transaction!=null)
				transaction.commit();
			
			transaction = null;

			if ((processResponseForFaults || successfullySent) && !msgCtx.isServerSide()) 
				checkForSyncResponses(msgCtx);
			
			if ((rmMsgCtx.getMessageType() == Sandesha2Constants.MessageTypes.TERMINATE_SEQ)
					&&
					 (Sandesha2Constants.SPEC_2005_02.NS_URI.equals(rmMsgCtx.getRMNamespaceValue()))) {
				try {
					transaction = storageManager.getTransaction();
					//terminate message sent using the SandeshaClient. Since the terminate message will simply get the
					//InFlow of the reference message get called which could be zero sized (OutOnly operations).
					
					// terminate sending side if this is the WSRM 1.0 spec. 
					// If the WSRM versoion is 1.1 termination will happen in the terminate sequence response message.
					
					TerminateSequence terminateSequence = rmMsgCtx.getTerminateSequence();
					String sequenceID = terminateSequence.getIdentifier().getIdentifier();
	
					RMSBean rmsBean = SandeshaUtil.getRMSBeanFromSequenceId(storageManager, sequenceID);
					TerminateManager.terminateSendingSide(rmsBean, storageManager, false);
					
					if(transaction != null && transaction.isActive()) transaction.commit();
					transaction = null;
				} finally {
					if(transaction != null && transaction.isActive()) {
						transaction.rollback();
						transaction = null;
					}
				}
			}

		} catch (Exception e) {
			if (log.isDebugEnabled()) log.debug("Caught exception", e);
		} finally {
			if (lock!=null && workId!=null) {
				lock.removeWork(workId);
			}

			if (transaction!=null && transaction.isActive()) {
				try {
					transaction.rollback();
				} catch (SandeshaStorageException e) {
					if (log.isWarnEnabled())
						log.warn("Caught exception rolling back transaction", e);
				}
			}
		}
		
		if (log.isDebugEnabled())
			log.debug("Exit: SenderWorker::run");
	}
	
	/**
	 * Update the message before sending it. We adjust the retransmission intervals and send counts
	 * for the message. If the message is an application message then we ensure that we have added
	 * the Sequence header.
	 */
	private SenderBean updateMessage(RMMsgContext rmMsgContext, SenderBean senderBean, StorageManager storageManager) throws AxisFault {
		
		// Lock the message to enable retransmission update
		senderBean = storageManager.getSenderBeanMgr().retrieve(senderBean.getMessageID());
		
		// Only continue if we find a SenderBean
		if (senderBean == null)
			return senderBean;

		int messageType = senderBean.getMessageType();

		boolean continueSending = MessageRetransmissionAdjuster.adjustRetransmittion(
				rmMsgContext, senderBean, rmMsgContext.getConfigurationContext(), storageManager);
		if(!continueSending) return null;
		
		Identifier id = null;

		if(messageType == Sandesha2Constants.MessageTypes.APPLICATION ||
		   messageType == Sandesha2Constants.MessageTypes.LAST_MESSAGE) {
			
			String namespace = SpecSpecificConstants.getRMNamespaceValue(rmVersion);
			Sequence sequence = rmMsgContext.getSequence();
			if(sequence == null) {
				sequence = new Sequence(namespace);
				sequence.setMessageNumber(senderBean.getMessageNumber());

				if(senderBean.isLastMessage() &&
				    SpecSpecificConstants.isLastMessageIndicatorRequired(rmVersion)) {
					sequence.setLastMessage(true);
				}
				
				// We just create the id here, we will add the value in later
				id = new Identifier(namespace);
				sequence.setIdentifier(id);
				rmMsgContext.setSequence(sequence);
				
			}
			
		} else if(messageType == Sandesha2Constants.MessageTypes.TERMINATE_SEQ) {
			TerminateSequence terminate = rmMsgContext.getTerminateSequence();
			id = terminate.getIdentifier();

		} else if(messageType == Sandesha2Constants.MessageTypes.CLOSE_SEQUENCE) {
			CloseSequence close = rmMsgContext.getCloseSequence();
			id = close.getIdentifier();
		
		} else if(messageType == Sandesha2Constants.MessageTypes.ACK_REQUEST) {
			// The only time that we can have a message of this type is when we are sending a
			// stand-alone ack request, and in that case we only expect to find a single ack
			// request header in the message.
			Iterator<AckRequested> ackRequests = rmMsgContext.getAckRequests();
			AckRequested ackRequest = (AckRequested) ackRequests.next(); 
			if (ackRequests.hasNext()) {
				throw new SandeshaException (SandeshaMessageHelper.getMessage(SandeshaMessageKeys.ackRequestMultipleParts));
			}
			id = ackRequest.getIdentifier();
		}
		
		// TODO consider adding an extra ack request, as we are about to send the message and we
		// know which sequence it is associated with.
		
		//if this is an sync WSRM 1.0 case we always have to add an ack
		boolean ackPresent = false;
		Iterator<SequenceAcknowledgement> it = rmMsgContext.getSequenceAcknowledgements();
		if (it.hasNext()) 
			ackPresent = true;
		
		if (!ackPresent && rmMsgContext.getMessageContext().isServerSide() 
				&&
			(messageType==Sandesha2Constants.MessageTypes.APPLICATION || 
		     messageType==Sandesha2Constants.MessageTypes.APPLICATION ||
		     messageType==Sandesha2Constants.MessageTypes.UNKNOWN ||
		     messageType==Sandesha2Constants.MessageTypes.LAST_MESSAGE)) {
			
			String inboundSequenceId = senderBean.getInboundSequenceId();
			if (inboundSequenceId==null)
				throw new SandeshaException ("InboundSequenceID is not set for the sequence:" + id);
			
			RMDBean incomingSequenceBean = SandeshaUtil.getRMDBeanFromSequenceId(storageManager, inboundSequenceId);

			if (incomingSequenceBean!=null)
				RMMsgCreator.addAckMessage(rmMsgContext, inboundSequenceId, incomingSequenceBean, false);
		}

		if(id != null && !senderBean.getSequenceID().equals(id.getIdentifier())) {
			id.setIndentifer(senderBean.getSequenceID());

			// Write the changes back into the message context
			rmMsgContext.addSOAPEnvelope();
		} else if(rmMsgContext.getProperty(RMMsgCreator.ACK_TO_BE_WRITTEN) != null){
			rmMsgContext.addSOAPEnvelope();
		}

		return senderBean;
	}
	
	private boolean isAckPiggybackableMsgType(int messageType) {
		if (log.isDebugEnabled())
			log.debug("Enter: SenderWorker::isAckPiggybackableMsgType, " + messageType);
		boolean piggybackable = true;

		if (messageType == Sandesha2Constants.MessageTypes.ACK)
			piggybackable = false;

		if (log.isDebugEnabled())
			log.debug("Exit: SenderWorker::isAckPiggybackableMsgType, " + piggybackable);
		return piggybackable;
	}
	
	private void checkForSyncResponses(MessageContext msgCtx) {
		if (log.isDebugEnabled())
			log.debug("Enter: SenderWorker::checkForSyncResponses, " + msgCtx.getEnvelope().getHeader());

		try {

			// create the responseMessageContext

			MessageContext responseMessageContext = msgCtx.getOperationContext().getMessageContext(WSDLConstants.MESSAGE_LABEL_IN_VALUE);
			SOAPEnvelope resenvelope = null;
			if (responseMessageContext!=null)
				resenvelope = responseMessageContext.getEnvelope();
			
			boolean transportInPresent = (msgCtx.getProperty(MessageContext.TRANSPORT_IN) != null);
			if (!transportInPresent && (responseMessageContext==null || responseMessageContext.getEnvelope()==null)) {
				if(log.isDebugEnabled()) log.debug("Exit: SenderWorker::checkForSyncResponses, no response present");
				return;
			}
			
			//to find out weather the response was built by me.
			boolean syncResponseBuilt = false;
			
			if (responseMessageContext==null || responseMessageContext.getEnvelope()==null) {
				if (responseMessageContext==null)
					responseMessageContext = new MessageContext();

				OperationContext requestMsgOpCtx = msgCtx.getOperationContext();
				//copy any necessary properties
				SandeshaUtil.copyConfiguredProperties(msgCtx, responseMessageContext);
				// If the request operation context is Out-In then it's reasonable to assume
				// that the response is related to the request.
				//int mep = requestMsgOpCtx.getAxisOperation().getAxisSpecificMEPConstant();
				//if(mep == WSDLConstants.MEP_CONSTANT_OUT_IN) {
					//this line causes issues in addressing
					//responseMessageContext.setOperationContext(requestMsgOpCtx);
				//}
				responseMessageContext.setProperty(Constants.Configuration.CHARACTER_SET_ENCODING, requestMsgOpCtx
								.getProperty(Constants.Configuration.CHARACTER_SET_ENCODING));
				responseMessageContext.setProperty(Constants.Configuration.CONTENT_TYPE, requestMsgOpCtx
								.getProperty(Constants.Configuration.CONTENT_TYPE));
				responseMessageContext.setProperty(HTTPConstants.MTOM_RECEIVED_CONTENT_TYPE, requestMsgOpCtx
								.getProperty(HTTPConstants.MTOM_RECEIVED_CONTENT_TYPE));

				//If the response MsgCtx was not available Axis2 would hv put the transport info into a 
				//HashMap, getting the data from it.
				HashMap transportInfoMap = (HashMap) msgCtx.getProperty(Constants.Configuration.TRANSPORT_INFO_MAP);
				if (transportInfoMap != null) {
					responseMessageContext.setProperty(Constants.Configuration.CONTENT_TYPE, 
									transportInfoMap.get(Constants.Configuration.CONTENT_TYPE));
					responseMessageContext.setProperty(Constants.Configuration.CHARACTER_SET_ENCODING,
									transportInfoMap.get(Constants.Configuration.CHARACTER_SET_ENCODING));
				}
			
				responseMessageContext.setConfigurationContext(msgCtx.getConfigurationContext());
				responseMessageContext.setTransportIn(msgCtx.getTransportIn());
				responseMessageContext.setTransportOut(msgCtx.getTransportOut());
				responseMessageContext.setProperty(MessageContext.TRANSPORT_IN, msgCtx
						.getProperty(MessageContext.TRANSPORT_IN));
				
				responseMessageContext.setServiceGroupContext(msgCtx.getServiceGroupContext());

				responseMessageContext.setProperty(Sandesha2Constants.MessageContextProperties.MAKECONNECTION_ENTRY,
				msgCtx.getProperty(Sandesha2Constants.MessageContextProperties.MAKECONNECTION_ENTRY));

				// If request is REST we assume the responseMessageContext is REST,
				// so set the variable

				responseMessageContext.setDoingREST(msgCtx.isDoingREST());

				resenvelope = responseMessageContext.getEnvelope();
				try {
					// MessageContext is modified in TransportUtils.createSOAPMessage(). It might be used by axis.engine or handler.
					// To catch the modification and pass it to engine or handler, resenvelope is created by responseMessageContext. 
					if (resenvelope==null) {
						//We try to build the response out of the transport stream.
						resenvelope = TransportUtils.createSOAPMessage(responseMessageContext, true);
						responseMessageContext.setEnvelope(resenvelope);
						syncResponseBuilt = true;
					}
				} catch (AxisFault e) {
					//Cannot find a valid SOAP envelope.
					if (log.isDebugEnabled() ) {
						log.debug (SandeshaMessageHelper
								.getMessage(SandeshaMessageKeys.soapEnvNotSet));
					log.debug ("Caught exception", e);
					}
				
					return;
				}
				
				//we will not be setting the operation context here since this msgs may not be an application reply.
				//we let other dispatchers find it.
				int messageType = MsgInitializer.initializeMessage(msgCtx).getMessageType();
				RMMsgContext responseRMMessage = MsgInitializer.initializeMessage(responseMessageContext);
				int responseMessageType = responseRMMessage.getMessageType();
				if(log.isDebugEnabled()) log.debug("inboundMsgType" + responseMessageType + "outgoing message type " + messageType);
				 				
				//if this is an application response msg in response to a make connection then we have to take care with the service context
				if ((messageType == Sandesha2Constants.MessageTypes.APPLICATION && responseMessageType == Sandesha2Constants.MessageTypes.APPLICATION)
					|| responseMessageType != Sandesha2Constants.MessageTypes.APPLICATION) {
					if(log.isDebugEnabled()) log.debug("setting service ctx on msg as this is NOT a makeConnection>appResponse exchange pattern");
					responseMessageContext.setServiceContext(msgCtx.getServiceContext());
				}
				else{
                                        //Setting the AxisService object
					responseMessageContext.setAxisService(msgCtx.getAxisService());

					//we cannot set service ctx for application response msgs since the srvc ctx will not match the op ctx, causing
					//problems with addressing
					if(log.isDebugEnabled()) log.debug("NOT setting service ctx for response type " + messageType + ", current srvc ctx =" + responseMessageContext.getServiceContext());
				}
				
				//If addressing is disabled we will be adding this message simply as the application response of the request message.
				Boolean addressingDisabled = (Boolean) msgCtx.getProperty(AddressingConstants.DISABLE_ADDRESSING_FOR_OUT_MESSAGES);
				if (addressingDisabled!=null && Boolean.TRUE.equals(addressingDisabled)) {
					// If the AxisOperation object doesn't have a message receiver, it means that this was
					// an out only op where we have added an ACK to the response.  Set the requestMsgOpCtx to
					// be the RMIn
					OperationContext responseMsgOpCtx = requestMsgOpCtx;
					if (requestMsgOpCtx.getAxisOperation().getMessageReceiver() == null) {
						// Generate a new RM In Only operation

						ServiceContext serviceCtx = responseMessageContext.getServiceContext();
						AxisOperation op = msgCtx.getAxisService().getOperation(Sandesha2Constants.RM_IN_ONLY_OPERATION);
						responseMsgOpCtx = OperationContextFactory.createOperationContext (op.getAxisSpecificMEPConstant(), op, serviceCtx);					
					}
					
					responseMessageContext.setOperationContext(responseMsgOpCtx);
				}
				
				AxisOperation operation = msgCtx.getAxisOperation();
				if (operation!=null && responseMessageContext.getAxisMessage()==null
						&& !(operation instanceof OutOnlyAxisOperation))
					responseMessageContext.setAxisMessage(operation.getMessage(WSDLConstants.MESSAGE_LABEL_IN_VALUE));

				if (responseRMMessage.getMessageType()==Sandesha2Constants.MessageTypes.ACK) {
					responseMessageContext.setAxisOperation(SpecSpecificConstants.getWSRMOperation
							(Sandesha2Constants.MessageTypes.ACK, responseRMMessage.getRMSpecVersion(), responseMessageContext.getAxisService()));
					responseMessageContext.setOperationContext(null);
				}
				
			}
			
			//if the syncResponseWas not built here and the client was not expecting a sync response. We will not try to execute 
			//here. Doing so will cause a double invocation for a async message. 
			if (msgCtx.getOptions().isUseSeparateListener()==true &&  !syncResponseBuilt) {
				return;
			}
			
			
			//setting the message as serverSide will let it go through the MessageReceiver (may be callback MR).
			responseMessageContext.setServerSide(true);
		
			if (responseMessageContext.getSoapAction()==null) {
				//if there is no SOAP action in the response message, Axis2 will wrongly identify it as a REST message
				//This happens because we set serverSide to True in a previous step.
				//So we have to add a empty SOAPAction here.
				responseMessageContext.setSoapAction("");
			}

			if (resenvelope!=null) {
				//Drive the response msg through the engine
				//disable addressing validation - this is an inbound response msg so we do not want addressing to validate replyTo
				//etc in the same way as it would for inbound request messages
				if (log.isDebugEnabled())
					log.debug("SenderWorker::disable addressing inbound checks, driving response through axis engine " + responseMessageContext);
				
				responseMessageContext.setProperty(AddressingConstants.ADDR_VALIDATE_INVOCATION_PATTERN, Boolean.FALSE);
				InvocationResponse response = AxisEngine.receive(responseMessageContext);
			}

		} catch (Exception e) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.noValidSyncResponse);
			if (log.isDebugEnabled())
				log.debug(message, e);
		}
		if (log.isDebugEnabled())
			log.debug("Exit: SenderWorker::checkForSyncResponses");
	}
	
	private void recordError (Exception e, RMMsgContext outRMMsg, StorageManager storageManager) throws SandeshaStorageException {
		// Store the Exception as a sequence property to enable the client to lookup the last 
		// exception time and timestamp.
		
		Transaction transaction = null;
		
		try
		{
			// Get the internal sequence id from the context
			String internalSequenceId = (String)outRMMsg.getProperty(Sandesha2Constants.MessageContextProperties.INTERNAL_SEQUENCE_ID);
			if(internalSequenceId == null) internalSequenceId = senderBean.getInternalSequenceID();
			
			if(internalSequenceId != null) {
				// Create a new Transaction
				transaction = storageManager.getTransaction();
				
				RMSBean bean = SandeshaUtil.getRMSBeanFromInternalSequenceId(storageManager, internalSequenceId);
			
				if (bean != null) {						
					bean.setLastSendError(e);
					bean.setLastSendErrorTimestamp(System.currentTimeMillis());

					// Update the RMSBean
					storageManager.getRMSBeanMgr().update(bean);
				}
				
				// Commit the properties
				if(transaction != null) {
					transaction.commit();
					transaction = null;
				}
			}
		}
		catch (Exception e1)
		{
			if (log.isDebugEnabled())
				log.debug(e1);
		} finally {
			if (transaction != null) {
				transaction.rollback();
				transaction = null;
			}
		}
	}
	
}
