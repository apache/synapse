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

import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.addressing.RelatesTo;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.description.AxisOperation;
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
import org.apache.sandesha2.policy.SandeshaPolicyBean;
import org.apache.sandesha2.security.SecurityManager;
import org.apache.sandesha2.security.SecurityToken;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.Transaction;
import org.apache.sandesha2.storage.beanmanagers.RMSBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.SenderBeanMgr;
import org.apache.sandesha2.storage.beans.RMDBean;
import org.apache.sandesha2.storage.beans.RMSBean;
import org.apache.sandesha2.storage.beans.SenderBean;
import org.apache.sandesha2.util.RMMsgCreator;
import org.apache.sandesha2.util.SOAPAbstractFactory;
import org.apache.sandesha2.util.SandeshaUtil;
import org.apache.sandesha2.util.SequenceManager;
import org.apache.sandesha2.workers.SandeshaThread;
import org.apache.sandesha2.workers.SenderWorker;
import org.apache.sandesha2.workers.WorkerLock;
import org.apache.sandesha2.wsrm.CreateSequence;
import org.apache.sandesha2.wsrm.SequenceOffer;

/**
 * Responsible for processing an incoming Application message.
 */

public class ApplicationMsgProcessor implements MsgProcessor {

	private static final Log log = LogFactory.getLog(ApplicationMsgProcessor.class);

	private String inboundSequence = null;
	private long   inboundMessageNumber;
	private Transaction appMsgProcTran = null;
	
	public ApplicationMsgProcessor() {
		// Nothing to do
	}
	
	public ApplicationMsgProcessor(String inboundSequenceId, long inboundMessageNumber) {
		this.inboundSequence = inboundSequenceId;
		this.inboundMessageNumber = inboundMessageNumber;
	}
	
	public boolean processInMessage(RMMsgContext rmMsgCtx, Transaction transaction) {
		if (log.isDebugEnabled()) {
			log.debug("Enter: ApplicationMsgProcessor::processInMessage");
			log.debug("Exit: ApplicationMsgProcessor::processInMessage");
		}
		return false;
	}
	
	private String getSequenceID(RMMsgContext rmMsgCtx, boolean serverSide, boolean forceNewSequence)throws SandeshaException{
		MessageContext msgContext = rmMsgCtx.getMessageContext();
		ConfigurationContext configContext = msgContext.getConfigurationContext();
		
		String internalSequenceId = null;
		if (serverSide) {
			if (inboundSequence == null || "".equals(inboundSequence)) {
				String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.incomingSequenceNotValidID, inboundSequence);
				log.debug(message);
				throw new SandeshaException(message);
			}

			internalSequenceId = SandeshaUtil.getOutgoingSideInternalSequenceID(inboundSequence);
		} else {
			// set the internal sequence id for the client side.
			EndpointReference toEPR = msgContext.getTo();
			if (toEPR == null || toEPR.getAddress() == null || "".equals(toEPR.getAddress())) {
				String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.toEPRNotValid, null);
				log.debug(message);
				throw new SandeshaException(message);
			}

			String to = toEPR.getAddress();
			String sequenceKey = null;
			if(forceNewSequence){
				sequenceKey = SandeshaUtil.getUUID();
				msgContext.setProperty(SandeshaClientConstants.SEQUENCE_KEY, sequenceKey);
			}
			else{
				sequenceKey = (String) msgContext.getProperty(SandeshaClientConstants.SEQUENCE_KEY);
				if (sequenceKey == null)
					sequenceKey = (String)configContext.getAxisConfiguration().getParameterValue(SandeshaClientConstants.SEQUENCE_KEY);
			}

			
			internalSequenceId = SandeshaUtil.getInternalSequenceID(to, sequenceKey);
		}
		return internalSequenceId;
	}
	
	public boolean processOutMessage(RMMsgContext rmMsgCtx, Transaction tran) throws AxisFault {
		if (log.isDebugEnabled())
			log.debug("Enter: ApplicationMsgProcessor::processOutMessage");

		appMsgProcTran= tran;
		MessageContext msgContext = rmMsgCtx.getMessageContext();
		ConfigurationContext configContext = msgContext.getConfigurationContext();
		
		//Please note: no need to check that RM1.0 annon out-in has a sequence offer, since we actually force an offer in this case
		
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

		//check the TO address is ok
		SandeshaUtil.getEPRDecorator(configContext).checkEndpointReference(msgContext.getTo());
		
		boolean serverSide = msgContext.isServerSide();

		// setting message Id if null
		if (msgContext.getMessageID() == null)
			msgContext.setMessageID(SandeshaUtil.getUUID());

		

		/*
		 * Internal sequence id is the one used to refer to the sequence (since
		 * actual sequence id is not available when first msg arrives) server
		 * side - a derivation of the sequenceId of the incoming sequence client
		 * side - a derivation of wsaTo & SeequenceKey
		 */
		String internalSequenceId = getSequenceID(rmMsgCtx, serverSide, false); //get a sequenceID, possibly pre-existing
		
		boolean lastMessage = false;
		if(!serverSide){
			String lastAppMessage = (String) msgContext.getProperty(SandeshaClientConstants.LAST_MESSAGE);
			if (lastAppMessage != null && "true".equals(lastAppMessage))
				lastMessage = true;
		}
		
		if (internalSequenceId!=null)
			rmMsgCtx.setProperty(Sandesha2Constants.MessageContextProperties.INTERNAL_SEQUENCE_ID,internalSequenceId);

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

		// A dummy message is a one which will not be processed as a actual
		// application message.
		// The RM handlers will simply let these go.
		String dummyMessageString = (String) msgContext.getOptions().getProperty(SandeshaClientConstants.DUMMY_MESSAGE);
		boolean dummyMessage = false;
		if (dummyMessageString != null && Sandesha2Constants.VALUE_TRUE.equals(dummyMessageString))
			dummyMessage = true;

		RMSBean rmsBean = SandeshaUtil.getRMSBeanFromInternalSequenceId(storageManager, internalSequenceId);

		//see if the sequence is closed
		if(rmsBean != null && 
			(rmsBean.isSequenceClosedClient() || rmsBean.isTerminateAdded() || rmsBean.isTimedOut())){
			if(SandeshaUtil.isAutoStartNewSequence(msgContext)){
				internalSequenceId = getSequenceID(rmMsgCtx, serverSide, true); //require a new sequence
				rmMsgCtx.setProperty(Sandesha2Constants.MessageContextProperties.INTERNAL_SEQUENCE_ID,internalSequenceId);
			}
			else if(rmsBean.isSequenceClosedClient()){
				throw new SandeshaException(SandeshaMessageHelper.getMessage(SandeshaMessageKeys.cannotSendMsgAsSequenceClosed, internalSequenceId));
			}
			else if(rmsBean.isTerminateAdded()){
				throw new SandeshaException(SandeshaMessageHelper.getMessage(SandeshaMessageKeys.cannotSendMsgAsSequenceTerminated, internalSequenceId));
			}
			else if(rmsBean.isTimedOut()){
				throw new SandeshaException(SandeshaMessageHelper.getMessage(SandeshaMessageKeys.cannotSendMsgAsSequenceTimedout, internalSequenceId));
			}
		}
		
		// If the call application is a 2-way MEP, and uses a anonymous replyTo, and the
		// RM 1.1 spec level, then we must have MakeConnection enabled. We check that here,
		// before we start creating a new Sequence.
		if(!serverSide) {
			AxisOperation op = msgContext.getAxisOperation();
			int mep = WSDLConstants.MEP_CONSTANT_INVALID;
			if(op != null) {
				mep = op.getAxisSpecificMEPConstant();
			}
			if(mep == WSDLConstants.MEP_CONSTANT_OUT_IN) {
				String specVersion = null;
				if(rmsBean == null) {
					specVersion = SequenceManager.getSpecVersion(msgContext, storageManager);
				} else {
					specVersion = rmsBean.getRMVersion();
				}
				if(specVersion.equals(Sandesha2Constants.SPEC_VERSIONS.v1_1)) {
					EndpointReference replyTo = msgContext.getReplyTo();
					if(replyTo == null || replyTo.hasAnonymousAddress()) {
						//we are sync
						SandeshaPolicyBean policy = SandeshaUtil.getPropertyBean(configContext.getAxisConfiguration());
						if(!policy.isEnableMakeConnection()) {
							String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.makeConnectionDisabled);
							throw new SandeshaException(message);
						}
					}
				}
			}
		}

		//setting the reference msg store key.
		if (rmsBean!=null && rmsBean.getReferenceMessageStoreKey()==null) {
			//setting this application message as the reference, if it hsnt already been set.
			
			String referenceMsgKey = SandeshaUtil.getUUID();
			storageManager.storeMessageContext(referenceMsgKey, msgContext);
			rmsBean.setReferenceMessageStoreKey(referenceMsgKey);
		}
		
		String outSequenceID = null;


		// Work out if there is a user transaction involved before updating any store state
		// to give any storage manager interface a chance to setup any transactional state
		boolean hasUserTransaction = storageManager.hasUserTransaction(msgContext);
		
		try {
			
			if (rmsBean == null) { 
				// SENDING THE CREATE SEQUENCE.
				while (rmsBean == null) {
					// There is a timing window where 2 sending threads can hit this point
					// at the same time and both will create an RMSBean to the same endpoint
					// with the same internal sequenceid
					// Check that someone hasn't created the bean
					rmsBean = SandeshaUtil.getRMSBeanFromInternalSequenceId(storageManager, internalSequenceId);
	
					// if first message - setup the sending side sequence - both for the
					// server and the client sides.
					if (rmsBean == null) {
						rmsBean = SequenceManager.setupNewClientSequence(msgContext, internalSequenceId, storageManager);
						rmsBean = addCreateSequenceMessage(rmMsgCtx, rmsBean, storageManager);
						if(rmsBean != null) outSequenceID = rmsBean.getSequenceID();
						
						if (rmsBean == null && appMsgProcTran != null && appMsgProcTran.isActive()) {
							// Rollback the current locks.
							appMsgProcTran.rollback();
	
							// Create a new tran.  This avoids a potential deadlock where the RMS/RMDBeans
							// are taken in reverse order.
							appMsgProcTran = storageManager.getTransaction();
						}
					}

				}
	
			} else {
				outSequenceID = rmsBean.getSequenceID();
			}
			
			// the message number that was last used.
			long systemMessageNumber = rmsBean.getNextMessageNumber();
	
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
	
			if (serverSide) {
				// Deciding whether this is the last message. We assume it is if it relates to
				// a message which arrived with the LastMessage flag on it. 
				RMDBean rmdBean = SandeshaUtil.getRMDBeanFromSequenceId(storageManager, inboundSequence);			
				// Get the last in message
				String lastRequestId = rmdBean.getLastInMessageId();
				RelatesTo relatesTo = msgContext.getRelatesTo();
				if(relatesTo != null && lastRequestId != null &&
						lastRequestId.equals(relatesTo.getValue())) {
					lastMessage = true;
				}
				
				//or a constant property may call it as the last msg
				Boolean inboundLast = (Boolean) msgContext.getProperty(Sandesha2Constants.MessageContextProperties.INBOUND_LAST_MESSAGE); 
				if (inboundLast!=null && inboundLast.booleanValue())
					lastMessage = true;
			}
			
			if (lastMessage) 
				rmsBean.setLastOutMessage(messageNumber);		
	
			// set this as the response highest message.
			rmsBean.setHighestOutMessageNumber(messageNumber);
			
			// saving the used message number, and the expected reply count
			boolean startPolling = false;
			if (!dummyMessage) {
				rmsBean.setNextMessageNumber(messageNumber);
	
				// Identify the MEP associated with the message.
				AxisOperation op = msgContext.getAxisOperation();
				int mep = WSDLConstants.MEP_CONSTANT_INVALID;
				if(op != null) {
					mep = op.getAxisSpecificMEPConstant();
				}
	
				if(mep == WSDLConstants.MEP_CONSTANT_OUT_IN) {
					// We only match up requests and replies when we are doing sync interactions
					if (log.isDebugEnabled()) log.debug("MEP OUT_IN");
					EndpointReference replyTo = msgContext.getReplyTo();
					if(replyTo == null || replyTo.hasAnonymousAddress()) {
						long expectedReplies = rmsBean.getExpectedReplies();
						rmsBean.setExpectedReplies(expectedReplies + 1);
					}
	
					// If we support the RM anonymous URI then rewrite the ws-a anon to use the RM equivalent.
					//(do should be done only for WSRM 1.1)
					
					String specVersion = SequenceManager.getSpecVersion(rmMsgCtx.getMessageContext(), storageManager);
					if (Sandesha2Constants.SPEC_VERSIONS.v1_1.equals(specVersion)) {
						if (log.isDebugEnabled()) log.debug("SPEC_1_1");
						String oldAddress = (replyTo == null) ? null : replyTo.getAddress();
						EndpointReference newReplyTo = SandeshaUtil.rewriteEPR(rmsBean, msgContext
								.getReplyTo(), configContext);
						String newAddress = (newReplyTo == null) ? null : newReplyTo.getAddress();
						if (newAddress != null && !newAddress.equals(oldAddress)) {
							// We have rewritten the replyTo. If this is the first message that we have needed to
							// rewrite then we should set the sequence up for polling, and once we have saved the
							// changes to the sequence then we can start the polling thread.
							
							//Firstly, we are going to use make connection in this configuration so we should now ensure that
							//WS-Adressing is enabled
							if (log.isDebugEnabled()) log.debug("Ensuring that WS-A is enabled for msg " + msgContext);
							msgContext.setProperty(AddressingConstants.DISABLE_ADDRESSING_FOR_OUT_MESSAGES,Boolean.FALSE);
							msgContext.setReplyTo(newReplyTo);
							
							//start the polling process to pull back response messages
							if (!rmsBean.isPollingMode()) {
								rmsBean.setPollingMode(true);
								startPolling = true;
							}
						}
					}
				}
			}
			if (log.isDebugEnabled()) log.debug("App msg using replyTo EPR as " + msgContext.getReplyTo());
			
			RelatesTo relatesTo = msgContext.getRelatesTo();
			if(relatesTo != null) {
				rmsBean.setHighestOutRelatesTo(relatesTo.getValue());
			}
	
			// setting async ack endpoint for the server side. (if present)
			if (serverSide) {
				if (rmsBean.getToEndpointReference() != null) {
					msgContext.setProperty(SandeshaClientConstants.AcksTo, rmsBean.getToEndpointReference().getAddress());
				}
			}
	
			// Update the rmsBean
			storageManager.getRMSBeanMgr().update(rmsBean);
			
			if(startPolling) {
				SandeshaUtil.startWorkersForSequence(msgContext.getConfigurationContext(), rmsBean);
			}
			
			
			int SOAPVersion = Sandesha2Constants.SOAPVersion.v1_1;
			if (!msgContext.isSOAP11())
				SOAPVersion = Sandesha2Constants.SOAPVersion.v1_2;
			if (msgContext.getEnvelope() == null) {
				try {
					msgContext.setEnvelope(SOAPAbstractFactory.getSOAPFactory(
							SOAPVersion).getDefaultEnvelope());
				} catch (AxisFault e) {
					throw new SandeshaException(e.getMessage());
				}
			}
	
			SOAPBody soapBody = rmMsgCtx.getSOAPEnvelope().getBody();
			if (soapBody == null) {
				String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.soapBodyNotPresent);
				log.debug(message);
				throw new SandeshaException(message);
			}
	
			
			if (rmMsgCtx.getMessageId() == null) {
				String messageId1 = SandeshaUtil.getUUID();
				rmMsgCtx.setMessageId(messageId1);
			}
	
			EndpointReference toEPR = msgContext.getTo();
	
			
			if (toEPR != null) {
				// setting default actions.
				String to = toEPR.getAddress();
				String operationName = msgContext.getOperationContext().getAxisOperation().getName().getLocalPart();
				if (msgContext.getWSAAction() == null) {
					msgContext.setWSAAction(to + "/" + operationName);
				}
				if (msgContext.getSoapAction() == null) {
					msgContext.setSoapAction("\"" + to + "/" + operationName + "\"");
				}
			}
			
			// processing the response if not an dummy.
			if (!dummyMessage){
				String storageKey = SandeshaUtil.getUUID(); 
				processResponseMessage(rmMsgCtx, rmsBean, internalSequenceId, outSequenceID, messageNumber, storageKey, storageManager, tran, hasUserTransaction);
			}
			//Users wont be able to get reliable response msgs in the back channel in the back channel of a 
			//reliable message. If he doesn't have a endpoint he should use polling mechanisms.
			msgContext.pause();
			
			if (appMsgProcTran != null && appMsgProcTran.isActive()) {
				appMsgProcTran.commit();
				appMsgProcTran = null;
			}
		}

		finally {
			if (appMsgProcTran != null && appMsgProcTran.isActive())
				appMsgProcTran.rollback();
		}
		
		if (log.isDebugEnabled())
			log.debug("Exit: ApplicationMsgProcessor::processOutMessage " + Boolean.TRUE);
		return true;
	}

	private RMSBean addCreateSequenceMessage(RMMsgContext applicationRMMsg, RMSBean rmsBean,
			StorageManager storageManager) throws AxisFault {

		if (log.isDebugEnabled())
			log.debug("Enter: ApplicationMsgProcessor::addCreateSequenceMessage, " + rmsBean);

		MessageContext applicationMsg = applicationRMMsg.getMessageContext();
		ConfigurationContext configCtx = applicationMsg.getConfigurationContext();

		// generating a new create sequence message.
		RMMsgContext createSeqRMMessage = RMMsgCreator.createCreateSeqMsg(rmsBean, applicationRMMsg);

		createSeqRMMessage.setFlow(MessageContext.OUT_FLOW);
		CreateSequence createSequencePart = createSeqRMMessage.getCreateSequence();

		SequenceOffer offer = createSequencePart.getSequenceOffer();
		if (offer != null) {
			String offeredSequenceId = offer.getIdentifer().getIdentifier();

			rmsBean.setOfferedSequence(offeredSequenceId);
		}

		MessageContext createSeqMsg = createSeqRMMessage.getMessageContext();
		createSeqMsg.setRelationships(null); // create seq msg does not
											 // relateTo anything
		
		String createSequenceMessageStoreKey = SandeshaUtil.getUUID(); // the key that will be used to store 
																	   //the create sequence message.
		
		rmsBean.setCreateSeqMsgID(createSeqMsg.getMessageID());
		rmsBean.setCreateSequenceMsgStoreKey(createSequenceMessageStoreKey);
		
		if (storageManager.getRMSBeanMgr().insert(rmsBean)) {
			//cloning the message and storing it as a reference.
			MessageContext clonedMessage = SandeshaUtil.cloneMessageContext(createSeqMsg);
			String clonedMsgStoreKey = SandeshaUtil.getUUID();
			storageManager.storeMessageContext(clonedMsgStoreKey, clonedMessage);
			rmsBean.setReferenceMessageStoreKey(clonedMsgStoreKey);
			
			SecurityToken token = (SecurityToken) createSeqRMMessage.getProperty(Sandesha2Constants.MessageContextProperties.SECURITY_TOKEN);
			if(token != null) {
				SecurityManager secManager = SandeshaUtil.getSecurityManager(configCtx);
				rmsBean.setSecurityTokenData(secManager.getTokenRecoveryData(token));
			}

      // Update the RMSBean 
      storageManager.getRMSBeanMgr().update(rmsBean);
			SenderBean createSeqEntry = new SenderBean();
			createSeqEntry.setMessageContextRefKey(createSequenceMessageStoreKey);
			createSeqEntry.setTimeToSend(System.currentTimeMillis());
			createSeqEntry.setMessageID(createSeqRMMessage.getMessageId());
			createSeqEntry.setInternalSequenceID(rmsBean.getInternalSequenceID());
			// this will be set to true in the sender
			createSeqEntry.setSend(true);
			// Indicate that this message is a create sequence
			createSeqEntry.setMessageType(Sandesha2Constants.MessageTypes.CREATE_SEQ);
			EndpointReference to = createSeqRMMessage.getTo();
			if (to!=null)
				createSeqEntry.setToAddress(to.getAddress());
			// If this message is targetted at an anonymous address then we must not have a transport
			// ready for it, as the create sequence is not a reply.
			if(to == null || to.hasAnonymousAddress())
				createSeqEntry.setTransportAvailable(false);
	
			createSeqMsg.setProperty(Sandesha2Constants.QUALIFIED_FOR_SENDING, Sandesha2Constants.VALUE_FALSE);
			
			SandeshaUtil.executeAndStore(createSeqRMMessage, createSequenceMessageStoreKey, storageManager);
	
			storageManager.getSenderBeanMgr().insert(createSeqEntry);
			
			if(appMsgProcTran != null && createSeqRMMessage.getMessageId() != null && !storageManager.hasUserTransaction(createSeqMsg)) {

				// Lock the sender bean before we insert it, if we are planning to send it ourselves
				String workId = createSeqEntry.getMessageID() + createSeqEntry.getTimeToSend();
				SandeshaThread sender = storageManager.getSender();

				ConfigurationContext context = createSeqMsg.getConfigurationContext();
				WorkerLock lock = sender.getWorkerLock();
		
				SenderWorker worker = new SenderWorker(context, createSeqEntry, rmsBean.getRMVersion());
				worker.setLock(lock);
				worker.setWorkId(workId);
				// Actually take the lock
				lock.addWork(workId, worker);
			  
				// Commit the transaction, so that the sender worker starts with a clean slate.
				if(appMsgProcTran.isActive()) appMsgProcTran.commit();				
						
				if(worker != null) {
					try {
						worker.run();
					} catch(Exception e)  {
						
						log.debug("Caught exception running SandeshaWorker", e);
					}
				}
		
				//Create transaction
				appMsgProcTran = storageManager.getTransaction();
			
				//Find RMSBean
				RMSBeanMgr rmsBeanMgr = storageManager.getRMSBeanMgr();
				RMSBean tempRMSBean = new RMSBean();
				tempRMSBean.setInternalSequenceID(rmsBean.getInternalSequenceID());
				rmsBean = rmsBeanMgr.findUnique(tempRMSBean);
			
				// If the RMSBean has been terminated this means that we may 
				// well have encountered a problem sending this message
				if (rmsBean == null || rmsBean.isTerminated()){
					
					if (log.isDebugEnabled())
						log.debug("Exit: ApplicationMsgProcessor::addCreateSequenceMessage, Failed to establish sequence " + rmsBean);
					
					if (rmsBean != null && rmsBean.getLastSendError() != null) {
						if (rmsBean.getLastSendError() instanceof AxisFault)
							throw (AxisFault)rmsBean.getLastSendError();
						
						throw new AxisFault(SandeshaMessageHelper.getMessage(SandeshaMessageKeys.createSequenceRefused), 
								rmsBean.getLastSendError());
					}
					
					throw new AxisFault(SandeshaMessageHelper.getMessage(SandeshaMessageKeys.createSequenceRefused));						
				}
			}
			// Setup enough of the workers to get this create sequence off the box.
			SandeshaUtil.startWorkersForSequence(configCtx, rmsBean);
		} else {
			rmsBean = null;
		}
				
		if (log.isDebugEnabled())
			log.debug("Exit: ApplicationMsgProcessor::addCreateSequenceMessage, " + rmsBean);
		
		return rmsBean;
	}

	private void processResponseMessage(RMMsgContext rmMsg, RMSBean rmsBean, String internalSequenceId, String outSequenceID, long messageNumber,
		    String storageKey, StorageManager storageManager, Transaction tran, boolean hasUserTransaction) throws AxisFault {

		if (log.isDebugEnabled())
			log.debug("Enter: ApplicationMsgProcessor::processResponseMessage, " + internalSequenceId + ", " + outSequenceID);

		MessageContext msg = rmMsg.getMessageContext();

		SenderBeanMgr retransmitterMgr = storageManager.getSenderBeanMgr();

		// setting last message
		boolean lastMessage = false;
		if (msg.isServerSide()) {
			Boolean inboundLast = (Boolean) msg.getProperty(Sandesha2Constants.MessageContextProperties.INBOUND_LAST_MESSAGE);
			if (inboundLast != null && inboundLast.booleanValue()) {
				lastMessage = true;
			}

		} else {
			// client side
			Object obj = msg.getProperty(SandeshaClientConstants.LAST_MESSAGE);
			if (obj != null && "true".equals(obj)) {
				lastMessage = true;
			}
		}

		boolean sendingNow = false;
		if(outSequenceID != null && !hasUserTransaction) {
		  sendingNow = true;
		}
		
		// Now that we have decided which sequence to use for the message, make sure that we secure
		// it with the correct token.
		RMMsgCreator.secureOutboundMessage(rmsBean, msg);

		// Retransmitter bean entry for the application message
		SenderBean appMsgEntry = new SenderBean();

		appMsgEntry.setMessageContextRefKey(storageKey);

		appMsgEntry.setTimeToSend(System.currentTimeMillis());
		appMsgEntry.setMessageID(rmMsg.getMessageId());
		appMsgEntry.setMessageNumber(messageNumber);
		appMsgEntry.setLastMessage(lastMessage);
		
		SOAPEnvelope envelope = rmMsg.getSOAPEnvelope();
		if (lastMessage && envelope!=null && envelope.getBody().getFirstOMChild()==null)
			appMsgEntry.setMessageType(Sandesha2Constants.MessageTypes.LAST_MESSAGE);
		else
			appMsgEntry.setMessageType(Sandesha2Constants.MessageTypes.APPLICATION);
		
		appMsgEntry.setInboundSequenceId(inboundSequence);
		appMsgEntry.setInboundMessageNumber(inboundMessageNumber);
		if (outSequenceID == null) {
			appMsgEntry.setSend(false);
		} else {
			appMsgEntry.setSend(true);
			// Send will be set to true at the sender.
			msg.setProperty(Sandesha2Constants.SET_SEND_TO_TRUE, Sandesha2Constants.VALUE_TRUE);
			appMsgEntry.setSequenceID(outSequenceID);
		}
		
		EndpointReference to = rmMsg.getTo();
		if (to!=null)
			appMsgEntry.setToAddress(to.getAddress());
		
		appMsgEntry.setInternalSequenceID(internalSequenceId);

		msg.setProperty(Sandesha2Constants.QUALIFIED_FOR_SENDING, Sandesha2Constants.VALUE_FALSE);

		// increasing the current handler index, so that the message will not be
		// going throught the SandeshaOutHandler again.
		msg.setCurrentHandlerIndex(msg.getCurrentHandlerIndex() + 1);

		SandeshaUtil.executeAndStore(rmMsg, storageKey, storageManager);
		
		// Insert the SenderBean
		retransmitterMgr.insert(appMsgEntry);

		// Lock the sender bean before we insert it, if we are planning to send it ourselves
		SenderWorker worker = null;
		if(sendingNow) {
		  String workId = appMsgEntry.getMessageID() + appMsgEntry.getTimeToSend();
		  SandeshaThread sender = storageManager.getSender();
		  ConfigurationContext context = msg.getConfigurationContext();
		  WorkerLock lock = sender.getWorkerLock();
      
		  worker = new SenderWorker(context, appMsgEntry, rmsBean.getRMVersion());
		  worker.setLock(lock);
		  worker.setWorkId(workId);
		  // Actually take the lock
		  lock.addWork(workId, worker);
		}
		
		// Commit the transaction, so that the sender worker starts with a clean slate.
		if(appMsgProcTran != null && appMsgProcTran.isActive()) appMsgProcTran.commit();
		 
		if(worker != null) {
		  try {
		    worker.run();
		  } catch(Exception e)  {
		    log.error("Caught exception running SandeshaWorker", e);
		  }
		}
		if (log.isDebugEnabled())
			log.debug("Exit: ApplicationMsgProcessor::processResponseMessage");
	}

}
