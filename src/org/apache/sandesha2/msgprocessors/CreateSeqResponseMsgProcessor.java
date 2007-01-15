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

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.addressing.RelatesTo;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.wsdl.WSDLConstants;
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
import org.apache.sandesha2.storage.beanmanagers.RMSBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.RMDBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.SenderBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.SequencePropertyBeanMgr;
import org.apache.sandesha2.storage.beans.RMSBean;
import org.apache.sandesha2.storage.beans.RMDBean;
import org.apache.sandesha2.storage.beans.SenderBean;
import org.apache.sandesha2.storage.beans.SequencePropertyBean;
import org.apache.sandesha2.util.MsgInitializer;
import org.apache.sandesha2.util.SandeshaUtil;
import org.apache.sandesha2.util.SpecSpecificConstants;
import org.apache.sandesha2.wsrm.Accept;
import org.apache.sandesha2.wsrm.AckRequested;
import org.apache.sandesha2.wsrm.CloseSequence;
import org.apache.sandesha2.wsrm.CreateSequenceResponse;
import org.apache.sandesha2.wsrm.Identifier;
import org.apache.sandesha2.wsrm.Sequence;
import org.apache.sandesha2.wsrm.TerminateSequence;

/**
 * Responsible for processing an incoming Create Sequence Response message.
 */

public class CreateSeqResponseMsgProcessor implements MsgProcessor {

	private static final Log log = LogFactory.getLog(CreateSeqResponseMsgProcessor.class);

	public boolean processInMessage(RMMsgContext createSeqResponseRMMsgCtx) throws AxisFault {

		if (log.isDebugEnabled())
			log.debug("Enter: CreateSeqResponseMsgProcessor::processInMessage");

		ConfigurationContext configCtx = createSeqResponseRMMsgCtx.getMessageContext().getConfigurationContext();

		StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(configCtx, configCtx
				.getAxisConfiguration());

		// Processing the create sequence response.

		CreateSequenceResponse createSeqResponsePart = (CreateSequenceResponse) createSeqResponseRMMsgCtx
				.getMessagePart(Sandesha2Constants.MessageParts.CREATE_SEQ_RESPONSE);
		if (createSeqResponsePart == null) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.noCreateSeqResponse);
			log.debug(message);
			throw new SandeshaException(message);
		}

		String newOutSequenceId = createSeqResponsePart.getIdentifier().getIdentifier();
		if (newOutSequenceId == null) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.newSeqIdIsNull);
			log.debug(message);
			throw new SandeshaException(message);
		}

		RelatesTo relatesTo = createSeqResponseRMMsgCtx.getMessageContext().getRelatesTo();
		String createSeqMsgId = null;
		if (relatesTo != null) {
			createSeqMsgId = relatesTo.getValue();
		} else {
			// Work out the related message from the operation context
			OperationContext context = createSeqResponseRMMsgCtx.getMessageContext().getOperationContext();
			MessageContext createSeq = context.getMessageContext(WSDLConstants.MESSAGE_LABEL_OUT_VALUE);
			if(createSeq != null) createSeqMsgId = createSeq.getMessageID();
		}
		if(createSeqMsgId == null) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.relatesToNotAvailable);
			log.error(message);
			throw new SandeshaException(message);
		}

		SenderBeanMgr retransmitterMgr = storageManager.getSenderBeanMgr();
		RMSBeanMgr rmsBeanMgr = storageManager.getRMSBeanMgr();

		RMSBean rmsBean = rmsBeanMgr.retrieve(createSeqMsgId);
		if (rmsBean == null) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.createSeqEntryNotFound);
			log.debug(message);
			throw new SandeshaException(message);
		}

		// Check that the create sequence response message proves possession of the correct token
		String tokenData = rmsBean.getSecurityTokenData();
		if(tokenData != null) {
			SecurityManager secManager = SandeshaUtil.getSecurityManager(configCtx);
			MessageContext crtSeqResponseCtx = createSeqResponseRMMsgCtx.getMessageContext();
			OMElement body = crtSeqResponseCtx.getEnvelope().getBody();
			SecurityToken token = secManager.recoverSecurityToken(tokenData);
			secManager.checkProofOfPossession(token, body, crtSeqResponseCtx);
		}

		String internalSequenceId = rmsBean.getInternalSequenceID();
		if (internalSequenceId == null || "".equals(internalSequenceId)) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.tempSeqIdNotSet);
			log.debug(message);
			throw new SandeshaException(message);
		}
		createSeqResponseRMMsgCtx.setProperty(Sandesha2Constants.MessageContextProperties.INTERNAL_SEQUENCE_ID,internalSequenceId);
		
		String sequencePropertyKey = SandeshaUtil.getSequencePropertyKey(createSeqResponseRMMsgCtx);
		
		rmsBean.setSequenceID(newOutSequenceId);

		// We must poll for any reply-to that uses the anonymous URI. If it is a ws-a reply to then
		// the create must include an offer (or this client cannot be identified). If the reply-to
		// is the RM anon URI template then the offer is not required.
		if (Sandesha2Constants.SPEC_VERSIONS.v1_1.equals(createSeqResponseRMMsgCtx.getRMSpecVersion())) {
			String replyToAddress = rmsBean.getReplyToEPR();
			if(SandeshaUtil.isWSRMAnonymous(replyToAddress)) {
				rmsBean.setPollingMode(true);
				SandeshaUtil.startPollingManager(configCtx);
			}
		}

		SenderBean createSequenceSenderBean = retransmitterMgr.retrieve(createSeqMsgId);
		if (createSequenceSenderBean == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(SandeshaMessageKeys.createSeqEntryNotFound));

		// deleting the create sequence entry.
		retransmitterMgr.delete(createSeqMsgId);

		SequencePropertyBeanMgr sequencePropMgr = storageManager.getSequencePropertyBeanMgr();
		
		// Store the security token under the new sequence id
		if(tokenData != null) {
			SequencePropertyBean newToken = new SequencePropertyBean(sequencePropertyKey,
					Sandesha2Constants.SequenceProperties.SECURITY_TOKEN, tokenData);
			sequencePropMgr.insert(newToken);
		}
		
		// processing for accept (offer has been sent)
		Accept accept = createSeqResponsePart.getAccept();
		if (accept != null) {

			// TODO this should be detected in the Fault manager.
			if (rmsBean.getOfferedSequence() == null) {
				String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.accptButNoSequenceOffered);
				log.debug(message);
				throw new SandeshaException(message);
			}

			RMDBean rMDBean = new RMDBean();
			
			EndpointReference acksToEPR = accept.getAcksTo().getEPR();
			rMDBean.setAcksToEPR(acksToEPR.getAddress());
			rMDBean.setSequenceID(rmsBean.getOfferedSequence());
			rMDBean.setNextMsgNoToProcess(1);

			//Storing the referenceMessage of the sending side sequence as the reference message
			//of the receiving side as well.
			//This can be used when creating new outgoing messages.
			
			String referenceMsgStoreKey = rmsBean.getReferenceMessageStoreKey();
			MessageContext referenceMsg = storageManager.retrieveMessageContext(referenceMsgStoreKey, configCtx);
			
			String newMessageStoreKey = SandeshaUtil.getUUID();
			storageManager.storeMessageContext(newMessageStoreKey,referenceMsg);
			
			rMDBean.setReferenceMessageKey(newMessageStoreKey);

			// If this is an offered sequence that needs polling then we need to setup the
			// rmdBean for polling too, so that it still gets serviced after the outbound
			// sequence terminates.
			if (Sandesha2Constants.SPEC_VERSIONS.v1_1.equals(createSeqResponseRMMsgCtx.getRMSpecVersion())) {
				String replyToAddress = rmsBean.getReplyToEPR();
				EndpointReference ref = new EndpointReference(replyToAddress);
				if(!rmsBean.isPollingMode() && (replyToAddress == null || ref.hasAnonymousAddress())) {
					rMDBean.setPollingMode(true);
					SandeshaUtil.startPollingManager(configCtx);
				}
			}
			
			String rmSpecVersion = createSeqResponseRMMsgCtx.getRMSpecVersion();

			SequencePropertyBean specVersionBean = new SequencePropertyBean(rmsBean.getOfferedSequence(),
					Sandesha2Constants.SequenceProperties.RM_SPEC_VERSION, rmSpecVersion);
			sequencePropMgr.insert(specVersionBean);

			rMDBean.setServerCompletedMessages(new ArrayList());

			RMDBeanMgr rmdBeanMgr = storageManager.getRMDBeanMgr();
			rmdBeanMgr.insert(rMDBean);

			rmsBean.setLastActivatedTime(System.currentTimeMillis());
			rmsBeanMgr.update(rmsBean);

			// Store the security token for the offered sequence
			if(tokenData != null) {
				SequencePropertyBean newToken = new SequencePropertyBean(rmsBean.getOfferedSequence(),
						Sandesha2Constants.SequenceProperties.SECURITY_TOKEN, tokenData);
				sequencePropMgr.insert(newToken);
			}
			
			// Add the offered sequence into the inbound sequences list
			SequencePropertyBean incomingSequenceListBean = sequencePropMgr.retrieve(
					Sandesha2Constants.SequenceProperties.ALL_SEQUENCES,
					Sandesha2Constants.SequenceProperties.INCOMING_SEQUENCE_LIST);

			if (incomingSequenceListBean == null) {
				incomingSequenceListBean = new SequencePropertyBean();
				incomingSequenceListBean.setSequencePropertyKey(Sandesha2Constants.SequenceProperties.ALL_SEQUENCES);
				incomingSequenceListBean.setName(Sandesha2Constants.SequenceProperties.INCOMING_SEQUENCE_LIST);
				incomingSequenceListBean.setValue(null);

				// this get inserted before
				sequencePropMgr.insert(incomingSequenceListBean);
			}

			ArrayList incomingSequenceList = SandeshaUtil.getArrayListFromString(incomingSequenceListBean.getValue());
			incomingSequenceList.add(rmsBean.getOfferedSequence());
			incomingSequenceListBean.setValue(incomingSequenceList.toString());
			sequencePropMgr.update(incomingSequenceListBean);
		}

		SenderBean target = new SenderBean();
		target.setInternalSequenceID(internalSequenceId);
		target.setSend(false);

		Iterator iterator = retransmitterMgr.find(target).iterator();
		while (iterator.hasNext()) {
			SenderBean tempBean = (SenderBean) iterator.next();

			// updating the application message
			String key = tempBean.getMessageContextRefKey();
			MessageContext applicationMsg = storageManager.retrieveMessageContext(key, configCtx);

			// TODO make following exception message more understandable to the
			// user (probably some others exceptions messages as well)
			if (applicationMsg == null)
				throw new SandeshaException(SandeshaMessageHelper.getMessage(SandeshaMessageKeys.unavailableAppMsg));

			String rmVersion = SandeshaUtil.getRMVersion(sequencePropertyKey, storageManager);
			if (rmVersion == null)
				throw new SandeshaException(SandeshaMessageHelper.getMessage(SandeshaMessageKeys.cannotDecideRMVersion));

			String assumedRMNamespace = SpecSpecificConstants.getRMNamespaceValue(rmVersion);

			RMMsgContext applicaionRMMsg = MsgInitializer.initializeMessage(applicationMsg);

			if (tempBean.getMessageType() == Sandesha2Constants.MessageTypes.APPLICATION) {
				
				Sequence sequencePart = (Sequence) applicaionRMMsg.getMessagePart(Sandesha2Constants.MessageParts.SEQUENCE);
				if (sequencePart == null) {
					String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.seqPartIsNull);
					log.debug(message);
					throw new SandeshaException(message);
				}
	
				Identifier identifier = new Identifier(assumedRMNamespace);
				identifier.setIndentifer(newOutSequenceId);
	
				sequencePart.setIdentifier(identifier);
				
			} else if (tempBean.getMessageType() == Sandesha2Constants.MessageTypes.TERMINATE_SEQ) {
				
				TerminateSequence sequencePart = (TerminateSequence) applicaionRMMsg.getMessagePart(Sandesha2Constants.MessageParts.TERMINATE_SEQ);
				if (sequencePart == null) {
					String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.seqPartIsNull);
					log.debug(message);
					throw new SandeshaException(message);
				}
	
				Identifier identifier = new Identifier(assumedRMNamespace);
				identifier.setIndentifer(newOutSequenceId);
	
				sequencePart.setIdentifier(identifier);

			} else if (tempBean.getMessageType() == Sandesha2Constants.MessageTypes.CLOSE_SEQUENCE) {
			
				CloseSequence sequencePart = (CloseSequence) applicaionRMMsg.getMessagePart(Sandesha2Constants.MessageParts.CLOSE_SEQUENCE);
				if (sequencePart == null) {
					String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.seqPartIsNull);
					log.debug(message);
					throw new SandeshaException(message);
				}
	
				Identifier identifier = new Identifier(assumedRMNamespace);
				identifier.setIndentifer(newOutSequenceId);
	
				sequencePart.setIdentifier(identifier);
				
			} else if (tempBean.getMessageType() == Sandesha2Constants.MessageTypes.ACK_REQUEST) {

				Iterator headerIterator = applicaionRMMsg.getMessageParts(Sandesha2Constants.MessageParts.ACK_REQUEST);
								
				AckRequested sequencePart = null;
				while (headerIterator.hasNext()) {
					sequencePart = (AckRequested) headerIterator.next(); 
				}
				
				if (headerIterator.hasNext()) {
					throw new SandeshaException (SandeshaMessageHelper.getMessage(SandeshaMessageKeys.ackRequestMultipleParts));
				}
				
				if (sequencePart == null) {
					String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.seqPartIsNull);
					log.debug(message);
					throw new SandeshaException(message);
				}
				
				sequencePart.getIdentifier().setIndentifer(newOutSequenceId);
					
			}

			try {
				applicaionRMMsg.addSOAPEnvelope();
			} catch (AxisFault e) {
				throw new SandeshaException(e.getMessage(), e);
			}

			// asking to send the application msssage
			tempBean.setSend(true);
			tempBean.setSequenceID(newOutSequenceId);
			retransmitterMgr.update(tempBean);

			// updating the message. this will correct the SOAP envelope string.
			storageManager.updateMessageContext(key, applicationMsg);
		}

		createSeqResponseRMMsgCtx.getMessageContext().getOperationContext().setProperty(
				org.apache.axis2.Constants.RESPONSE_WRITTEN, "false");

		createSeqResponseRMMsgCtx.pause();

		if (log.isDebugEnabled())
			log.debug("Exit: CreateSeqResponseMsgProcessor::processInMessage " + Boolean.TRUE);
		return true;
	}

	public boolean processOutMessage(RMMsgContext rmMsgCtx) {
		if (log.isDebugEnabled()) {
			log.debug("Enter: CreateSeqResponseMsgProcessor::processOutMessage");
			log.debug("Exit: CreateSeqResponseMsgProcessor::processOutMessage " + Boolean.FALSE);
		}
		return false;

	}
}
