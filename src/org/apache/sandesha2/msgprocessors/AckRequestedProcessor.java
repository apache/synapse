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

import java.util.Collection;
import java.util.Iterator;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.MessageContextConstants;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.engine.AxisEngine;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.RMMsgContext;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.client.SandeshaClientConstants;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.policy.SandeshaPolicyBean;
import org.apache.sandesha2.security.SecurityManager;
import org.apache.sandesha2.security.SecurityToken;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.beanmanagers.SenderBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.SequencePropertyBeanMgr;
import org.apache.sandesha2.storage.beans.SenderBean;
import org.apache.sandesha2.storage.beans.SequencePropertyBean;
import org.apache.sandesha2.util.MsgInitializer;
import org.apache.sandesha2.util.RMMsgCreator;
import org.apache.sandesha2.util.SOAPAbstractFactory;
import org.apache.sandesha2.util.SandeshaUtil;
import org.apache.sandesha2.util.SpecSpecificConstants;
import org.apache.sandesha2.wsrm.AckRequested;

/**
 * Responsible for processing ack requested headers on incoming messages.
 */

public class AckRequestedProcessor {

	private static final Log log = LogFactory.getLog(AckRequestedProcessor.class);

	public boolean processAckRequestedHeaders(RMMsgContext message) throws AxisFault {
		if (log.isDebugEnabled())
			log.debug("Enter: AckRequestedProcessor::processAckRequestHeaders");

		SOAPEnvelope envelope = message.getMessageContext().getEnvelope();
		SOAPHeader header = envelope.getHeader();
		boolean msgCtxPaused = false;
		if(header!=null)
		{
			for(int i = 0; i < Sandesha2Constants.SPEC_NS_URIS.length; i++) {
				QName headerName = new QName(Sandesha2Constants.SPEC_NS_URIS[i], Sandesha2Constants.WSRM_COMMON.ACK_REQUESTED);
				
				Iterator acks = header.getChildrenWithName(headerName);
				while(acks.hasNext()) {
					OMElement ack = (OMElement) acks.next();
					AckRequested ackReq = new AckRequested(headerName.getNamespaceURI());
					ackReq.fromOMElement(ack);
					boolean paused = processAckRequestedHeader(message, ack, ackReq);
					//if nto already paused we might be now
					if(!msgCtxPaused){
						msgCtxPaused = paused;
					}
				}
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

		String sequenceId = ackRequested.getIdentifier().getIdentifier();

		MessageContext msgContext = rmMsgCtx.getMessageContext();
		ConfigurationContext configurationContext = msgContext.getConfigurationContext();

		StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(configurationContext,
				configurationContext.getAxisConfiguration());

		SequencePropertyBeanMgr seqPropMgr = storageManager.getSequencePropertyBeanMgr();

		//not getting the sequencePropertyKey from the usual method since the ackRequest may be embedded in a message
		//of a different sequence. (usual method SandeshaUtil.getSequencePropertyKey)
		String sequencePropertyKey = sequenceId;
		
		// Check that the sender of this AckRequest holds the correct token
		SequencePropertyBean tokenBean = seqPropMgr.retrieve(sequencePropertyKey, Sandesha2Constants.SequenceProperties.SECURITY_TOKEN);
		if(tokenBean != null) {
			SecurityManager secManager = SandeshaUtil.getSecurityManager(configurationContext);
			SecurityToken token = secManager.recoverSecurityToken(tokenBean.getValue());
			
			secManager.checkProofOfPossession(token, soapHeader, msgContext);
		}

		// Setting the ack depending on AcksTo.
		SequencePropertyBean acksToBean = seqPropMgr.retrieve(sequencePropertyKey,
				Sandesha2Constants.SequenceProperties.ACKS_TO_EPR);
		SequencePropertyBean versionBean = seqPropMgr.retrieve(sequencePropertyKey,
				Sandesha2Constants.SequenceProperties.RM_SPEC_VERSION);

		EndpointReference acksTo = new EndpointReference(acksToBean.getValue());
		String acksToStr = acksTo.getAddress();

		if (acksToStr == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(SandeshaMessageKeys.acksToStrNotSet));

		AxisOperation ackOperation = SpecSpecificConstants.getWSRMOperation(
				Sandesha2Constants.MessageTypes.ACK,
				versionBean.getValue(),
				msgContext.getAxisService());
		MessageContext ackMsgCtx = SandeshaUtil.createNewRelatedMessageContext(rmMsgCtx, ackOperation);

		ackMsgCtx.setProperty(Sandesha2Constants.APPLICATION_PROCESSING_DONE, "true");

		RMMsgContext ackRMMsgCtx = MsgInitializer.initializeMessage(ackMsgCtx);
		ackRMMsgCtx.setRMNamespaceValue(rmMsgCtx.getRMNamespaceValue());

		ackMsgCtx.setMessageID(SandeshaUtil.getUUID());

		SOAPFactory factory = SOAPAbstractFactory.getSOAPFactory(SandeshaUtil.getSOAPVersion(msgContext.getEnvelope()));

		// Setting new envelope
		SOAPEnvelope envelope = factory.getDefaultEnvelope();
		try {
			ackMsgCtx.setEnvelope(envelope);
		} catch (AxisFault e3) {
			throw new SandeshaException(e3.getMessage());
		}

		ackMsgCtx.setTo(acksTo);
		ackMsgCtx.setReplyTo(msgContext.getTo());
		RMMsgCreator.addAckMessage(ackRMMsgCtx,sequencePropertyKey,sequenceId, storageManager);
		ackRMMsgCtx.getMessageContext().setServerSide(true);
		ackMsgCtx.setProperty(AddressingConstants.WS_ADDRESSING_VERSION, msgContext
				.getProperty(AddressingConstants.WS_ADDRESSING_VERSION)); // TODO
																			// do
																			// this
																			// in
																			// the
																			// RMMsgCreator

		String addressingNamespaceURI = SandeshaUtil.getSequenceProperty(sequencePropertyKey,
				Sandesha2Constants.SequenceProperties.ADDRESSING_NAMESPACE_VALUE, storageManager);
		String anonymousURI = SpecSpecificConstants.getAddressingAnonymousURI(addressingNamespaceURI);

		if (anonymousURI.equals(acksTo.getAddress())) {

			AxisEngine engine = new AxisEngine(ackRMMsgCtx.getMessageContext().getConfigurationContext());

			// setting CONTEXT_WRITTEN since acksto is anonymous
			if (rmMsgCtx.getMessageContext().getOperationContext() == null) {
				// operation context will be null when doing in a GLOBAL
				// handler.
				OperationContext opCtx = new OperationContext(ackOperation);
				rmMsgCtx.getMessageContext().setOperationContext(opCtx);
			}

			rmMsgCtx.getMessageContext().getOperationContext().setProperty(org.apache.axis2.Constants.RESPONSE_WRITTEN,
					Constants.VALUE_TRUE);

			rmMsgCtx.getMessageContext().setProperty(Sandesha2Constants.ACK_WRITTEN, "true");

			try {
				engine.send(ackRMMsgCtx.getMessageContext());
			} catch (AxisFault e1) {
				throw new SandeshaException(e1.getMessage());
			}

		} else {

			SenderBeanMgr retransmitterBeanMgr = storageManager.getRetransmitterBeanMgr();

			String key = SandeshaUtil.getUUID();

			// dumping to the storage will be done be Sandesha2 Transport Sender
			// storageManager.storeMessageContext(key,ackMsgCtx);

			SenderBean ackBean = new SenderBean();
			ackBean.setMessageContextRefKey(key);
			ackBean.setMessageID(ackMsgCtx.getMessageID());
			ackBean.setReSend(false);
			ackBean.setSequenceID(sequenceId);
			
			EndpointReference to = ackMsgCtx.getTo();
			if (to!=null)
				ackBean.setToAddress(to.getAddress());

			// this will be set to true in the sender.
			ackBean.setSend(true);

			ackMsgCtx.setProperty(Sandesha2Constants.QUALIFIED_FOR_SENDING, Sandesha2Constants.VALUE_FALSE);

			ackBean.setMessageType(Sandesha2Constants.MessageTypes.ACK);

			// the internalSequenceId value of the retransmitter Table for the
			// messages related to an incoming
			// sequence is the actual sequence ID

			// operation is the lowest level, Sandesha2 can be engaged.
			SandeshaPolicyBean propertyBean = SandeshaUtil.getPropertyBean(msgContext.getAxisOperation());

			long ackInterval = propertyBean.getAcknowledgementInterval();

			// Ack will be sent as stand alone, only after the retransmitter
			// interval.
			long timeToSend = System.currentTimeMillis() + ackInterval;

			// removing old acks.
			SenderBean findBean = new SenderBean();
			findBean.setMessageType(Sandesha2Constants.MessageTypes.ACK);

			// this will be set to true in the sandesha2TransportSender.
			findBean.setSend(true);
			findBean.setReSend(false);
			Collection coll = retransmitterBeanMgr.find(findBean);
			Iterator it = coll.iterator();

			if (it.hasNext()) {
				SenderBean oldAckBean = (SenderBean) it.next();
				timeToSend = oldAckBean.getTimeToSend(); // If there is an
															// old ack. This ack
															// will be sent in
															// the old
															// timeToSend.
				retransmitterBeanMgr.delete(oldAckBean.getMessageID());
			}

			ackBean.setTimeToSend(timeToSend);

			storageManager.storeMessageContext(key, ackMsgCtx);
			msgContext.setProperty(Sandesha2Constants.QUALIFIED_FOR_SENDING, Sandesha2Constants.VALUE_FALSE);
			
			// inserting the new ack.
			retransmitterBeanMgr.insert(ackBean);

			// passing the message through sandesha2sender

			SandeshaUtil.executeAndStore(ackRMMsgCtx, key);

			SandeshaUtil.startSenderForTheSequence(configurationContext, sequenceId);

			msgContext.pause();

			if (log.isDebugEnabled())
				log.debug("Exit: AckRequestedProcessor::processAckRequestedHeader " + Boolean.TRUE);
			return true;
		}
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

		MessageContext msgContext = ackRequestRMMsg.getMessageContext();
		ConfigurationContext configurationContext = msgContext.getConfigurationContext();
		Options options = msgContext.getOptions();

		StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(configurationContext,
				configurationContext.getAxisConfiguration());

		String toAddress = ackRequestRMMsg.getTo().getAddress();
		String sequenceKey = (String) options.getProperty(SandeshaClientConstants.SEQUENCE_KEY);
		String internalSeqenceID = SandeshaUtil.getInternalSequenceID(toAddress, sequenceKey);

		String outSequenceID = SandeshaUtil.getSequenceProperty(internalSeqenceID,
				Sandesha2Constants.SequenceProperties.OUT_SEQUENCE_ID, storageManager);
		if (outSequenceID == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.couldNotSendAckRequestSeqNotFound, internalSeqenceID));

		String rmVersion = SandeshaUtil.getRMVersion(internalSeqenceID, storageManager);
		if (rmVersion == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(SandeshaMessageKeys.cannotDecideRMVersion));

		AxisOperation ackOperation = SpecSpecificConstants.getWSRMOperation(
				Sandesha2Constants.MessageTypes.ACK,
				rmVersion,
				msgContext.getAxisService());
		msgContext.setAxisOperation(ackOperation);

		OperationContext opcontext = new OperationContext(ackOperation);
		opcontext.setParent(msgContext.getServiceContext());
		configurationContext.registerOperationContext(ackRequestRMMsg.getMessageId(), opcontext);
		msgContext.setOperationContext(opcontext);
		
		Iterator iterator = ackRequestRMMsg.getMessageParts(Sandesha2Constants.MessageParts.ACK_REQUEST);
		
		AckRequested ackRequested = null;
		while (iterator.hasNext()) {
			ackRequested = (AckRequested) iterator.next(); 
		}
		
		if (iterator.hasNext()) {
			String message = "Passed message has more than one AckRequest. You can have only one";
			throw new SandeshaException (message);
		}
		
		if (ackRequested==null) {
			String message = "No AckRequested part was present in the message";
			throw new SandeshaException (message);
		}
		
		ackRequested.getIdentifier().setIndentifer(outSequenceID);
		
		msgContext.setProperty(Sandesha2Constants.APPLICATION_PROCESSING_DONE, "true");


		ackRequestRMMsg.setWSAAction(SpecSpecificConstants.getAckRequestAction (rmVersion));
		ackRequestRMMsg.setSOAPAction(SpecSpecificConstants.getAckRequestSOAPAction (rmVersion));

		String transportTo = SandeshaUtil.getSequenceProperty(internalSeqenceID,
				Sandesha2Constants.SequenceProperties.TRANSPORT_TO, storageManager);
		if (transportTo != null) {
			ackRequestRMMsg.setProperty(MessageContextConstants.TRANSPORT_URL, transportTo);
		}
		
		//setting msg context properties
		ackRequestRMMsg.setProperty(Sandesha2Constants.MessageContextProperties.SEQUENCE_ID, outSequenceID);
		ackRequestRMMsg.setProperty(Sandesha2Constants.MessageContextProperties.INTERNAL_SEQUENCE_ID, internalSeqenceID);
		ackRequestRMMsg.setProperty(Sandesha2Constants.MessageContextProperties.SEQUENCE_PROPERTY_KEY , sequenceKey);

		ackRequestRMMsg.addSOAPEnvelope();
		
		// Ensure the outbound message us secured using the correct token
		String tokenData = SandeshaUtil.getSequenceProperty(internalSeqenceID,
				Sandesha2Constants.SequenceProperties.SECURITY_TOKEN,
				storageManager);
		if(tokenData != null) {
			SecurityManager secMgr = SandeshaUtil.getSecurityManager(configurationContext);
			SecurityToken token = secMgr.recoverSecurityToken(tokenData);
			secMgr.applySecurityToken(token, msgContext);
		}

		String key = SandeshaUtil.getUUID();

		SenderBean ackRequestBean = new SenderBean();
		ackRequestBean.setMessageContextRefKey(key);

		storageManager.storeMessageContext(key, msgContext);

		// Set a retransmitter lastSentTime so that terminate will be send with
		// some delay.
		// Otherwise this get send before return of the current request (ack).
		// TODO: refine the terminate delay.
		ackRequestBean.setTimeToSend(System.currentTimeMillis());

		ackRequestBean.setMessageID(msgContext.getMessageID());
		
		EndpointReference to = msgContext.getTo();
		if (to!=null)
			ackRequestBean.setToAddress(to.getAddress());
		
		// this will be set to true at the sender.
		ackRequestBean.setSend(true);

		msgContext.setProperty(Sandesha2Constants.QUALIFIED_FOR_SENDING, Sandesha2Constants.VALUE_FALSE);

		ackRequestBean.setReSend(false);

		SenderBeanMgr retramsmitterMgr = storageManager.getRetransmitterBeanMgr();

		retramsmitterMgr.insert(ackRequestBean);

		ackRequestRMMsg.setProperty(Sandesha2Constants.SET_SEND_TO_TRUE, Sandesha2Constants.VALUE_TRUE);

		SandeshaUtil.executeAndStore(ackRequestRMMsg, key);

		if (log.isDebugEnabled())
			log.debug("Exit: AckRequestedProcessor::processOutgoingAckRequestMessage " + Boolean.FALSE);
		
		return true;

	}

}
