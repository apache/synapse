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

import java.util.Collection;
import java.util.Iterator;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.context.OperationContextFactory;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.transport.TransportUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.RMMsgContext;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.policy.SandeshaPolicyBean;
import org.apache.sandesha2.security.SecurityManager;
import org.apache.sandesha2.security.SecurityToken;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.beanmanagers.SenderBeanMgr;
import org.apache.sandesha2.storage.beans.RMDBean;
import org.apache.sandesha2.storage.beans.SenderBean;
import org.apache.sandesha2.util.FaultManager;
import org.apache.sandesha2.util.MsgInitializer;
import org.apache.sandesha2.util.RMMsgCreator;
import org.apache.sandesha2.util.SOAPAbstractFactory;
import org.apache.sandesha2.util.SandeshaUtil;
import org.apache.sandesha2.util.SpecSpecificConstants;
import org.apache.sandesha2.util.WSRMMessageSender;
import org.apache.sandesha2.wsrm.AckRequested;

/**
 * Responsible for processing ack requested headers on incoming messages.
 */

public class AckRequestedProcessor extends WSRMMessageSender {

	private static final Log log = LogFactory.getLog(AckRequestedProcessor.class);

	public boolean processAckRequestedHeaders(RMMsgContext message) throws AxisFault {
		if (log.isDebugEnabled())
			log.debug("Enter: AckRequestedProcessor::processAckRequestHeaders");

		boolean msgCtxPaused = false;
		Iterator ackRequests = message.getMessageParts(Sandesha2Constants.MessageParts.ACK_REQUEST);
		while(ackRequests.hasNext()){
			AckRequested ackReq = (AckRequested)ackRequests.next();
			boolean paused = processAckRequestedHeader(message, ackReq.getOriginalAckRequestedElement(), ackReq);
			//if not already paused we might be now
			if(!msgCtxPaused){
				msgCtxPaused = paused;
			}
		}

		if (log.isDebugEnabled())
			log.debug("Exit: AckRequestedProcessor::processAckRequestHeaders " + msgCtxPaused);
		return msgCtxPaused;
	}

	/**
	 * 
	 * @param msgContext
	 * @param soapHeader
	 * @param ackRequested
	 * @return true if the msg context was paused
	 * @throws AxisFault
	 */
	public boolean processAckRequestedHeader(RMMsgContext rmMsgCtx, OMElement soapHeader, AckRequested ackRequested) throws AxisFault {
		if (log.isDebugEnabled())
			log.debug("Enter: AckRequestedProcessor::processAckRequestedHeader " + soapHeader);

		//checks weather the ack request was a piggybacked one.
		boolean piggybackedAckRequest = !(rmMsgCtx.getMessageType()==Sandesha2Constants.MessageTypes.ACK_REQUEST);
		
		String sequenceId = ackRequested.getIdentifier().getIdentifier();

		MessageContext msgContext = rmMsgCtx.getMessageContext();
		ConfigurationContext configurationContext = msgContext.getConfigurationContext();

		StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(configurationContext,
				configurationContext.getAxisConfiguration());
		
		// Check that the sender of this AckRequest holds the correct token
		RMDBean rmdBean = SandeshaUtil.getRMDBeanFromSequenceId(storageManager, sequenceId);

		if(rmdBean != null && rmdBean.getSecurityTokenData() != null) {
			SecurityManager secManager = SandeshaUtil.getSecurityManager(configurationContext);
			SecurityToken token = secManager.recoverSecurityToken(rmdBean.getSecurityTokenData());
			
			secManager.checkProofOfPossession(token, soapHeader, msgContext);
		}

		// Check that the sequence requested exists
		if (FaultManager.checkForUnknownSequence(rmMsgCtx, sequenceId, storageManager, piggybackedAckRequest)) {
			if (log.isDebugEnabled())
				log.debug("Exit: AckRequestedProcessor::processAckRequestedHeader, Unknown sequence ");
			return false;
		}

		// throwing a fault if the sequence is terminated
		if (FaultManager.checkForSequenceTerminated(rmMsgCtx, sequenceId, rmdBean, piggybackedAckRequest)) {
			if (log.isDebugEnabled())
				log.debug("Exit: AckRequestedProcessor::processAckRequestedHeader, Sequence terminated");
			return false;
		}

		// Setting the ack depending on AcksTo.
		EndpointReference acksTo = new EndpointReference(rmdBean.getAcksToEPR());
		String acksToStr = acksTo.getAddress();

		if (acksToStr == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(SandeshaMessageKeys.acksToStrNotSet));

		//Getting the operation for ack messages.
		AxisOperation ackOperation = SpecSpecificConstants.getWSRMOperation(
				Sandesha2Constants.MessageTypes.ACK,
				rmdBean.getRMVersion(),
				msgContext.getAxisService());
		
		//creating the ack message. If the ackRequest was a standalone this will be a out (response) message 
		MessageContext ackMsgCtx = null;
//		if (piggybackedAckRequest)
			ackMsgCtx = SandeshaUtil.createNewRelatedMessageContext(rmMsgCtx, ackOperation);
//		else
//			ackMsgCtx =MessageContextBuilder.createOutMessageContext (msgContext);
			
		//setting up the RMMsgContext
		RMMsgContext ackRMMsgCtx = MsgInitializer.initializeMessage(ackMsgCtx);
		ackRMMsgCtx.setRMNamespaceValue(rmMsgCtx.getRMNamespaceValue());
		
		if (ackMsgCtx.getMessageID()==null)
			ackMsgCtx.setMessageID(SandeshaUtil.getUUID());
		
		//adding the SOAP Envelope
		SOAPFactory factory = SOAPAbstractFactory.getSOAPFactory(SandeshaUtil.getSOAPVersion(msgContext.getEnvelope()));
		SOAPEnvelope envelope = factory.getDefaultEnvelope();
		try {
			ackMsgCtx.setEnvelope(envelope);
		} catch (AxisFault e3) {
			throw new SandeshaException(e3.getMessage());
		}

		ackMsgCtx.setTo(acksTo);
		ackMsgCtx.setReplyTo(msgContext.getTo());
		RMMsgCreator.addAckMessage(ackRMMsgCtx, sequenceId, rmdBean);
		
		//this is not a client generated message. So set serverSide to true.
		ackMsgCtx.setServerSide(true);

		if (acksTo.hasAnonymousAddress()) {
			//If acksTo is anonymous we will be sending the ack here it self. Transport will use what ever mechanism to send the 
			//message. (for e.g. HTTP will use the back channel)

			// setting "response written" since acksto is anonymous
			
			//adding an OperationContext if one is not available. (for e.g. If we are in the SandeshaGlobalInHandler)
			if (rmMsgCtx.getMessageContext().getOperationContext() == null) {
				// operation context will be null when doing in a GLOBAL
				// handler.
				
				ServiceContext serviceCtx = msgContext.getServiceContext();
				OperationContext opCtx =  OperationContextFactory.createOperationContext(ackOperation.getAxisSpecificMEPConstant(), ackOperation, serviceCtx);

				rmMsgCtx.getMessageContext().setOperationContext(opCtx);
			}
			
			try {
				AxisEngine.send(ackMsgCtx);
				TransportUtils.setResponseWritten(ackMsgCtx, true);
			} catch (AxisFault e1) {
				throw new SandeshaException(e1.getMessage());
			}

		} else {
			//If AcksTo is non-anonymous we will be adding a senderBean entry here. The sender is responsible 
			//for sending it out.
			
			SenderBeanMgr senderBeanMgr = storageManager.getSenderBeanMgr();

			String key = SandeshaUtil.getUUID();

			// dumping to the storage will be done be Sandesha2 Transport Sender
			// storageManager.storeMessageContext(key,ackMsgCtx);

			SenderBean ackBean = new SenderBean();
			ackBean.setMessageContextRefKey(key);
			ackBean.setMessageID(ackMsgCtx.getMessageID());
			
			//acks are sent only once.
			ackBean.setReSend(false);
			
			ackBean.setSequenceID(sequenceId);
			
			EndpointReference to = ackMsgCtx.getTo();
			if (to!=null)
				ackBean.setToAddress(to.getAddress());

			// this will be set to true in the sender.
			ackBean.setSend(true);

			ackMsgCtx.setProperty(Sandesha2Constants.QUALIFIED_FOR_SENDING, Sandesha2Constants.VALUE_FALSE);
			ackBean.setMessageType(Sandesha2Constants.MessageTypes.ACK);
			SandeshaPolicyBean propertyBean = SandeshaUtil.getPropertyBean(msgContext.getAxisOperation());

			long ackInterval = propertyBean.getAcknowledgementInterval();

			// Ack will be sent as stand alone, only after the ackknowledgement interval
			long timeToSend = System.currentTimeMillis() + ackInterval;

			// removing old acks.
			SenderBean findBean = new SenderBean();
			findBean.setMessageType(Sandesha2Constants.MessageTypes.ACK);
			Collection coll = senderBeanMgr.find(findBean);
			Iterator it = coll.iterator();

			if (it.hasNext()) {
				SenderBean oldAckBean = (SenderBean) it.next();
				// If there is an old Ack. This Ack will be sent in the old timeToSend.
				timeToSend = oldAckBean.getTimeToSend(); 
				senderBeanMgr.delete(oldAckBean.getMessageID());
			}

			ackBean.setTimeToSend(timeToSend);

			msgContext.setProperty(Sandesha2Constants.QUALIFIED_FOR_SENDING, Sandesha2Constants.VALUE_FALSE);
			
			// passing the message through sandesha2sender
		    SandeshaUtil.executeAndStore(ackRMMsgCtx, key, storageManager);

			// inserting the new Ack.
			senderBeanMgr.insert(ackBean);

			msgContext.pause();

			if (log.isDebugEnabled())
				log.debug("Exit: AckRequestedProcessor::processAckRequestedHeader " + Boolean.TRUE);
			
		}
		
		//No need to suspend. Just proceed.
		return false;
	}
	
	/**
	 * This is used to capture AckRequest messages send by the SandeshaClient.
	 * This will send that message using the Sandesha2 Sender.
	 * 
	 * @param rmMsgContext
	 */
	public boolean processOutgoingAckRequestMessage (RMMsgContext ackRequestRMMsg) throws AxisFault {

		if (log.isDebugEnabled())
			log.debug("Enter: AckRequestedProcessor::processOutgoingAckRequestMessage");

		setupOutMessage(ackRequestRMMsg);

		AxisOperation ackOperation = SpecSpecificConstants.getWSRMOperation(
				Sandesha2Constants.MessageTypes.ACK,
				getRMVersion(),
				getMsgContext().getAxisService());
		getMsgContext().setAxisOperation(ackOperation);

		ServiceContext serviceCtx = getMsgContext().getServiceContext();
		OperationContext opcontext = OperationContextFactory.createOperationContext(ackOperation.getAxisSpecificMEPConstant(), ackOperation, serviceCtx);
		opcontext.setParent(getMsgContext().getServiceContext());

		getConfigurationContext().registerOperationContext(ackRequestRMMsg.getMessageId(), opcontext);
		getMsgContext().setOperationContext(opcontext);
		
		Iterator iterator = ackRequestRMMsg.getMessageParts(Sandesha2Constants.MessageParts.ACK_REQUEST);
		
		AckRequested ackRequested = null;
		while (iterator.hasNext()) {
			ackRequested = (AckRequested) iterator.next(); 
		}
		
		if (iterator.hasNext()) {
			throw new SandeshaException (SandeshaMessageHelper.getMessage(SandeshaMessageKeys.ackRequestMultipleParts));
		}
		
		if (ackRequested==null) {
			throw new SandeshaException (SandeshaMessageHelper.getMessage(SandeshaMessageKeys.noAckRequestPartFound));
		}
		
		ackRequestRMMsg.setWSAAction(SpecSpecificConstants.getAckRequestAction (getRMVersion()));
		ackRequestRMMsg.setSOAPAction(SpecSpecificConstants.getAckRequestSOAPAction (getRMVersion()));

		sendOutgoingMessage(ackRequestRMMsg, Sandesha2Constants.MessageTypes.ACK_REQUEST, 0);
		
		// Pause the message context
		ackRequestRMMsg.pause();

		if (log.isDebugEnabled())
			log.debug("Exit: AckRequestedProcessor::processOutgoingAckRequestMessage " + Boolean.TRUE);
		
		return true;

	}

}
