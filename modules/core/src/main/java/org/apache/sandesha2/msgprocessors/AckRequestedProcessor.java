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
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.beanmanagers.SenderBeanMgr;
import org.apache.sandesha2.storage.beans.RMDBean;
import org.apache.sandesha2.storage.beans.SenderBean;
import org.apache.sandesha2.util.AcknowledgementManager;
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
		Iterator<AckRequested> ackRequests = message.getAckRequests();
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

		//it is a piggybacked ackrequest so we can ignore as we will piggyback acks at every opportunity anyway
		if(piggybackedAckRequest){
			if (log.isDebugEnabled())
			log.debug("Exit: AckRequestedProcessor::processAckRequestedHeader, it is a piggybacked ackrequest for seq " +
					"so we can ignore as we will piggyback an ack " + Boolean.FALSE);
			//No need to suspend. Just proceed.
			return false;
		}
		
		String sequenceId = ackRequested.getIdentifier().getIdentifier();

		MessageContext msgContext = rmMsgCtx.getMessageContext();
		ConfigurationContext configurationContext = msgContext.getConfigurationContext();

		StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(configurationContext,
				configurationContext.getAxisConfiguration());
		
		// Check that the sender of this AckRequest holds the correct token
		RMDBean rmdBean = SandeshaUtil.getRMDBeanFromSequenceId(storageManager, sequenceId);
		
		//check security credentials
		if(rmdBean!=null){
			SandeshaUtil.assertProofOfPossession(rmdBean, msgContext, soapHeader);
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
		EndpointReference acksTo = rmdBean.getAcksToEndpointReference();

		if (acksTo == null || acksTo.getAddress()==null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(SandeshaMessageKeys.acksToStrNotSet));
		
		//Getting the operation for ack messages.
		AxisOperation ackOperation = SpecSpecificConstants.getWSRMOperation(
				Sandesha2Constants.MessageTypes.ACK,
				rmdBean.getRMVersion(),
				msgContext.getAxisService());
		
		//creating the ack message. If the ackRequest was a standalone this will be a out (response) message 
		MessageContext ackMsgCtx = null;
		ackMsgCtx = SandeshaUtil.createNewRelatedMessageContext(rmMsgCtx, ackOperation);
			
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
		RMMsgCreator.addAckMessage(ackRMMsgCtx, sequenceId, rmdBean, true);
		
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
				TransportUtils.setResponseWritten(rmMsgCtx.getMessageContext(), true);
			} catch (AxisFault e1) {
				throw new SandeshaException(e1.getMessage());
			}

		} else {
			SandeshaPolicyBean propertyBean = SandeshaUtil.getPropertyBean(msgContext.getAxisOperation());

			long ackInterval = propertyBean.getAcknowledgementInterval();

			// Ack will be sent as stand alone, only after the acknowledgement interval
			long timeToSend = System.currentTimeMillis() + ackInterval;

			AcknowledgementManager.addAckBeanEntry(ackRMMsgCtx, sequenceId, timeToSend, storageManager);
		}
		
		if (log.isDebugEnabled())
			log.debug("Exit: AckRequestedProcessor::processAckRequestedHeader " + Boolean.FALSE);

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
		
		Iterator<AckRequested> iterator = ackRequestRMMsg.getAckRequests();
		
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

		sendOutgoingMessage(ackRequestRMMsg, Sandesha2Constants.MessageTypes.ACK_REQUEST, 0, null);
		
		// Pause the message context
		ackRequestRMMsg.pause();

		if (log.isDebugEnabled())
			log.debug("Exit: AckRequestedProcessor::processOutgoingAckRequestMessage " + Boolean.TRUE);
		
		return true;

	}

}
