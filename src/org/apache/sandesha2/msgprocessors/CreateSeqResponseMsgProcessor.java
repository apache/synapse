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

import java.util.Iterator;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.addressing.RelatesTo;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
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
import org.apache.sandesha2.storage.beanmanagers.CreateSeqBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.NextMsgBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.SenderBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.SequencePropertyBeanMgr;
import org.apache.sandesha2.storage.beans.CreateSeqBean;
import org.apache.sandesha2.storage.beans.NextMsgBean;
import org.apache.sandesha2.storage.beans.SenderBean;
import org.apache.sandesha2.storage.beans.SequencePropertyBean;
import org.apache.sandesha2.util.MsgInitializer;
import org.apache.sandesha2.util.SandeshaUtil;
import org.apache.sandesha2.util.SequenceManager;
import org.apache.sandesha2.util.SpecSpecificConstants;
import org.apache.sandesha2.wsrm.Accept;
import org.apache.sandesha2.wsrm.CreateSequenceResponse;
import org.apache.sandesha2.wsrm.Identifier;
import org.apache.sandesha2.wsrm.Sequence;

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
		if (relatesTo == null) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.relatesToNotAvailable);
			log.error(message);
			throw new SandeshaException(message);
		}
		String createSeqMsgId = relatesTo.getValue();

		SenderBeanMgr retransmitterMgr = storageManager.getRetransmitterBeanMgr();
		CreateSeqBeanMgr createSeqMgr = storageManager.getCreateSeqBeanMgr();

		CreateSeqBean createSeqBean = createSeqMgr.retrieve(createSeqMsgId);
		if (createSeqBean == null) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.createSeqEntryNotFound);
			log.debug(message);
			throw new SandeshaException(message);
		}

		// Check that the create sequence response message proves possession of the correct token
		String tokenData = createSeqBean.getSecurityTokenData();
		if(tokenData != null) {
			SecurityManager secManager = SandeshaUtil.getSecurityManager(configCtx);
			MessageContext crtSeqResponseCtx = createSeqResponseRMMsgCtx.getMessageContext();
			OMElement body = crtSeqResponseCtx.getEnvelope().getBody();
			SecurityToken token = secManager.recoverSecurityToken(tokenData);
			secManager.checkProofOfPossession(token, body, crtSeqResponseCtx);
		}

		String internalSequenceId = createSeqBean.getInternalSequenceID();
		if (internalSequenceId == null || "".equals(internalSequenceId)) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.tempSeqIdNotSet);
			log.debug(message);
			throw new SandeshaException(message);
		}
		createSeqResponseRMMsgCtx.setProperty(Sandesha2Constants.MessageContextProperties.INTERNAL_SEQUENCE_ID,internalSequenceId);
		
		String sequencePropertyKey = SandeshaUtil.getSequencePropertyKey(createSeqResponseRMMsgCtx);
		
		createSeqBean.setSequenceID(newOutSequenceId);
		createSeqMgr.update(createSeqBean);

		SenderBean createSequenceSenderBean = retransmitterMgr.retrieve(createSeqMsgId);
		if (createSequenceSenderBean == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(SandeshaMessageKeys.createSeqEntryNotFound));

		// deleting the create sequence entry.
		retransmitterMgr.delete(createSeqMsgId);

		// storing new out sequence id
		SequencePropertyBeanMgr sequencePropMgr = storageManager.getSequencePropertyBeanMgr();
		SequencePropertyBean outSequenceBean = new SequencePropertyBean(sequencePropertyKey,
				Sandesha2Constants.SequenceProperties.OUT_SEQUENCE_ID, newOutSequenceId);
		SequencePropertyBean internalSequenceBean = new SequencePropertyBean(newOutSequenceId,
				Sandesha2Constants.SequenceProperties.INTERNAL_SEQUENCE_ID, sequencePropertyKey);

		sequencePropMgr.insert(outSequenceBean);
		sequencePropMgr.insert(internalSequenceBean);
		
		// Store the security token under the new sequence id
		if(tokenData != null) {
			SequencePropertyBean newToken = new SequencePropertyBean(newOutSequenceId,
					Sandesha2Constants.SequenceProperties.SECURITY_TOKEN, tokenData);
			sequencePropMgr.insert(newToken);
		}

		// processing for accept (offer has been sent)
		Accept accept = createSeqResponsePart.getAccept();
		if (accept != null) {
			// Find offered sequence from internal sequence id.
			SequencePropertyBean offeredSequenceBean = sequencePropMgr.retrieve(sequencePropertyKey,
					Sandesha2Constants.SequenceProperties.OFFERED_SEQUENCE);

			// TODO this should be detected in the Fault manager.
			if (offeredSequenceBean == null) {
				String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.accptButNoSequenceOffered);
				log.debug(message);
				throw new SandeshaException(message);
			}

			String offeredSequenceId = (String) offeredSequenceBean.getValue();

			EndpointReference acksToEPR = accept.getAcksTo().getEPR();
			SequencePropertyBean acksToBean = new SequencePropertyBean();
			acksToBean.setName(Sandesha2Constants.SequenceProperties.ACKS_TO_EPR);
			acksToBean.setSequencePropertyKey(offeredSequenceId);
			acksToBean.setValue(acksToEPR.getAddress());

			sequencePropMgr.insert(acksToBean);

			NextMsgBean nextMsgBean = new NextMsgBean();
			nextMsgBean.setSequenceID(offeredSequenceId);
			nextMsgBean.setNextMsgNoToProcess(1);
			

			boolean pollingMode = false;
			if (Sandesha2Constants.SPEC_VERSIONS.v1_1.equals(createSeqResponseRMMsgCtx.getRMSpecVersion())) {
				String replyToAddress = SandeshaUtil.getSequenceProperty(sequencePropertyKey, 
								Sandesha2Constants.SequenceProperties.REPLY_TO_EPR, storageManager);
				if (replyToAddress!=null) {
					if (AddressingConstants.Submission.WSA_ANONYMOUS_URL.equals(replyToAddress))
						pollingMode = true;
					else if (AddressingConstants.Final.WSA_ANONYMOUS_URL.equals(replyToAddress))
						pollingMode = true;
					else if (replyToAddress.startsWith(Sandesha2Constants.WSRM_ANONYMOUS_URI_PREFIX))
						pollingMode = true;
				}
			}
			
			//Storing the referenceMessage of the sending side sequence as the reference message
			//of the receiving side as well.
			//This can be used when creating new outgoing messages.
			
			String referenceMsgStoreKey = createSeqBean.getReferenceMessageStoreKey();
			MessageContext referenceMsg = storageManager.retrieveMessageContext(referenceMsgStoreKey, configCtx);
			
			String newMessageStoreKey = SandeshaUtil.getUUID();
			storageManager.storeMessageContext(newMessageStoreKey,referenceMsg);
			
			nextMsgBean.setReferenceMessageKey(newMessageStoreKey);
			
			nextMsgBean.setPollingMode(pollingMode);
			
			//if PollingMode is true, starting the pollingmanager.
			if (pollingMode)
				SandeshaUtil.startPollingManager(configCtx);
			
			NextMsgBeanMgr nextMsgMgr = storageManager.getNextMsgBeanMgr();
			nextMsgMgr.insert(nextMsgBean);

			String rmSpecVersion = createSeqResponseRMMsgCtx.getRMSpecVersion();

			SequencePropertyBean specVersionBean = new SequencePropertyBean(offeredSequenceId,
					Sandesha2Constants.SequenceProperties.RM_SPEC_VERSION, rmSpecVersion);
			sequencePropMgr.insert(specVersionBean);

			SequencePropertyBean receivedMsgBean = new SequencePropertyBean(offeredSequenceId,
					Sandesha2Constants.SequenceProperties.SERVER_COMPLETED_MESSAGES, "");
			sequencePropMgr.insert(receivedMsgBean);

			SequencePropertyBean msgsBean = new SequencePropertyBean();
			msgsBean.setSequencePropertyKey(offeredSequenceId);
			msgsBean.setName(Sandesha2Constants.SequenceProperties.CLIENT_COMPLETED_MESSAGES);
			msgsBean.setValue("");
			sequencePropMgr.insert(msgsBean);

			// setting the addressing version.
			String addressingNamespace = createSeqResponseRMMsgCtx.getAddressingNamespaceValue();
			SequencePropertyBean addressingVersionBean = new SequencePropertyBean(offeredSequenceId,
					Sandesha2Constants.SequenceProperties.ADDRESSING_NAMESPACE_VALUE, addressingNamespace);
			sequencePropMgr.insert(addressingVersionBean);

			// Store the security token for the offered sequence
			if(tokenData != null) {
				SequencePropertyBean newToken = new SequencePropertyBean(offeredSequenceId,
						Sandesha2Constants.SequenceProperties.SECURITY_TOKEN, tokenData);
				sequencePropMgr.insert(newToken);
			}
		}

		SenderBean target = new SenderBean();
		target.setInternalSequenceID(internalSequenceId);
		target.setSend(false);
		target.setReSend(true);

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

			Sequence sequencePart = (Sequence) applicaionRMMsg.getMessagePart(Sandesha2Constants.MessageParts.SEQUENCE);
			if (sequencePart == null) {
				String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.seqPartIsNull);
				log.debug(message);
				throw new SandeshaException(message);
			}

			Identifier identifier = new Identifier(assumedRMNamespace);
			identifier.setIndentifer(newOutSequenceId);

			sequencePart.setIdentifier(identifier);

			try {
				applicaionRMMsg.addSOAPEnvelope();
			} catch (AxisFault e) {
				throw new SandeshaException(e.getMessage(), e);
			}

			// asking to send the application msssage
			tempBean.setSend(true);
			retransmitterMgr.update(tempBean);

			// updating the message. this will correct the SOAP envelope string.
			storageManager.updateMessageContext(key, applicationMsg);
		}

		SequenceManager.updateLastActivatedTime(sequencePropertyKey, storageManager);

		createSeqResponseRMMsgCtx.getMessageContext().getOperationContext().setProperty(
				org.apache.axis2.Constants.RESPONSE_WRITTEN, "false");

		createSeqResponseRMMsgCtx.pause();

		if (log.isDebugEnabled())
			log.debug("Exit: CreateSeqResponseMsgProcessor::processInMessage " + Boolean.TRUE);
		return true;
	}

	public boolean processOutMessage(RMMsgContext rmMsgCtx) throws SandeshaException {
		if (log.isDebugEnabled()) {
			log.debug("Enter: CreateSeqResponseMsgProcessor::processOutMessage");
			log.debug("Exit: CreateSeqResponseMsgProcessor::processOutMessage " + Boolean.FALSE);
		}
		return false;

	}
}
