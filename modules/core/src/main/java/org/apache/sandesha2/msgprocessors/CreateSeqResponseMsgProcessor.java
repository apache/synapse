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

import java.util.Iterator;

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
import org.apache.sandesha2.policy.SandeshaPolicyBean;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.Transaction;
import org.apache.sandesha2.storage.beanmanagers.RMSBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.RMDBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.SenderBeanMgr;
import org.apache.sandesha2.storage.beans.RMSBean;
import org.apache.sandesha2.storage.beans.RMDBean;
import org.apache.sandesha2.storage.beans.SenderBean;
import org.apache.sandesha2.util.RangeString;
import org.apache.sandesha2.util.SandeshaUtil;
import org.apache.sandesha2.util.TerminateManager;
import org.apache.sandesha2.wsrm.Accept;
import org.apache.sandesha2.wsrm.CreateSequenceResponse;

/**
 * Responsible for processing an incoming Create Sequence Response message.
 */

public class CreateSeqResponseMsgProcessor implements MsgProcessor {

	private static final Log log = LogFactory.getLog(CreateSeqResponseMsgProcessor.class);

	public boolean processInMessage(RMMsgContext createSeqResponseRMMsgCtx, Transaction transaction) throws AxisFault {

		if (log.isDebugEnabled())
			log.debug("Enter: CreateSeqResponseMsgProcessor::processInMessage");

		ConfigurationContext configCtx = createSeqResponseRMMsgCtx.getMessageContext().getConfigurationContext();

		StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(configCtx, configCtx
				.getAxisConfiguration());

		// Processing the create sequence response.

		CreateSequenceResponse createSeqResponsePart = createSeqResponseRMMsgCtx.getCreateSequenceResponse();
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
		
		//Need to see if the seqID is a dupe of one that already exists
		//If it is we can't accept the CSR, so will do nothing so that a new CreateSeq msg is sent
		RMSBean finderBean = new RMSBean ();
		finderBean.setSequenceID(newOutSequenceId);
		RMSBean rmsBeanWithDuplicateSeqID = storageManager.getRMSBeanMgr().findUnique(finderBean);
		if(rmsBeanWithDuplicateSeqID != null){
			if (log.isDebugEnabled())
				log.debug("Duplicate SeqID: " + newOutSequenceId + " so we can't accept the CSR, will terminate this seq and reallocate to a new seq");
			return false;
		}

		// Check that the create sequence response message proves possession of the correct token
		MessageContext msgCtx = createSeqResponseRMMsgCtx.getMessageContext();
		SandeshaUtil.assertProofOfPossession(rmsBean, msgCtx, msgCtx.getEnvelope().getBody());

		String internalSequenceId = rmsBean.getInternalSequenceID();
		if (internalSequenceId == null || "".equals(internalSequenceId)) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.tempSeqIdNotSet);
			log.debug(message);
			throw new SandeshaException(message);
		}
		createSeqResponseRMMsgCtx.setProperty(Sandesha2Constants.MessageContextProperties.INTERNAL_SEQUENCE_ID,internalSequenceId);
		
		// Check to see if we have already received a sequence id. If we have then this response is redundant,
		// and we can forget about it.
		// TODO: Should we terminate or close the extra sequence? We can only do that if the new sequence
		// id is different from the one we got back the first time. For now it should be enough to just
		// ignore it, and let it time out.
		if(rmsBean.getSequenceID() != null) {
			if(log.isDebugEnabled())
				log.debug("Exit: CreateSeqResponseMsgProcessor::processInMessage, sequence id is already set. " +
						"Existing id:" + rmsBean.getSequenceID() + ", new id:" + newOutSequenceId);
			return false;
		}
			
		// Store the new sequence id
		rmsBean.setSequenceID(newOutSequenceId);

		// We should poll for any reply-to that uses the anonymous URI, when MakeConnection
		// is enabled.
		if (Sandesha2Constants.SPEC_VERSIONS.v1_1.equals(createSeqResponseRMMsgCtx.getRMSpecVersion())) {
			SandeshaPolicyBean policy = SandeshaUtil.getPropertyBean(configCtx.getAxisConfiguration());
			if(policy.isEnableMakeConnection()) {
				EndpointReference reference = rmsBean.getAcksToEndpointReference();
				if(reference == null || reference.hasAnonymousAddress()) {
					rmsBean.setPollingMode(true);
				}
			}
		}
			
		rmsBean.setLastActivatedTime(System.currentTimeMillis());
		
		if(!rmsBeanMgr.update(rmsBean)){			
			//Im not setting the createSeqBean sender bean to resend true as the reallocation of msgs will do this
			try{
				TerminateManager.terminateSendingSide(rmsBean, storageManager, true, transaction);
			} catch(Exception e){
				if (log.isDebugEnabled())
					log.debug(e);					
			}
		} else {
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
				rMDBean.setAcksToEndpointReference(acksToEPR);
				rMDBean.setSequenceID(rmsBean.getOfferedSequence());
				rMDBean.setNextMsgNoToProcess(1);
				rMDBean.setOutboundInternalSequence(rmsBean.getInternalSequenceID());

				rMDBean.setServiceName(createSeqResponseRMMsgCtx.getMessageContext().getAxisService().getName());
				
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
					if(rmsBean.isPollingMode()) {
						rMDBean.setPollingMode(true);
						rMDBean.setReplyToEndpointReference(rmsBean.getReplyToEndpointReference());
					}
				}
				
				String rmSpecVersion = createSeqResponseRMMsgCtx.getRMSpecVersion();
				rMDBean.setRMVersion(rmSpecVersion);
				
				EndpointReference toEPR = createSeqResponseRMMsgCtx.getTo();
				if (toEPR==null) {
					//Most probably this is a sync response message, using the replyTo of the request message
					OperationContext operationContext = createSeqResponseRMMsgCtx.getMessageContext().getOperationContext();
					if (operationContext!=null) {
						MessageContext createSequnceMessage = operationContext.getMessageContext(WSDLConstants.MESSAGE_LABEL_OUT_VALUE);
						if (createSequnceMessage!=null)
							toEPR = createSequnceMessage.getReplyTo();
					}
				}
				
				if (toEPR!=null) 
					rMDBean.setToAddress(toEPR.getAddress());
				
				rMDBean.setServerCompletedMessages(new RangeString());
				RMDBeanMgr rmdBeanMgr = storageManager.getRMDBeanMgr();

				// Store the security token for the offered sequence
				rMDBean.setSecurityTokenData(rmsBean.getSecurityTokenData());
				
				rMDBean.setLastActivatedTime(System.currentTimeMillis());
				
				rmdBeanMgr.insert(rMDBean);
				SandeshaUtil.startWorkersForSequence(configCtx, rMDBean);
			}
			
			// Get the CreateSeqBean based on the message id to take a lock on the bean
		    SenderBean createSeqBean = retransmitterMgr.retrieve(createSeqMsgId);
		    
			// deleting the create sequence sender bean entry.
			retransmitterMgr.delete(createSeqBean.getMessageID());
				
			// Remove the create sequence message
			storageManager.removeMessageContext(rmsBean.getCreateSequenceMsgStoreKey());
			SandeshaUtil.startWorkersForSequence(configCtx, rmsBean);
	
			// Locate and update all of the messages for this sequence, now that we know
			// the sequence id.
			SenderBean target = new SenderBean();
			target.setInternalSequenceID(internalSequenceId);
			target.setSend(false);
			
			Iterator<SenderBean> iterator = retransmitterMgr.find(target).iterator();
			while (iterator.hasNext()) {
				SenderBean tempBean = (SenderBean) iterator.next();
	
				// asking to send the application msssage
				tempBean.setSend(true);
				tempBean.setSequenceID(newOutSequenceId);
				retransmitterMgr.update(tempBean);
			}
	
			// TODO - does this do anything?
			createSeqResponseRMMsgCtx.pause();
		}
		
		if (log.isDebugEnabled())
			log.debug("Exit: CreateSeqResponseMsgProcessor::processInMessage " + Boolean.TRUE);
		return true;
	}

	public boolean processOutMessage(RMMsgContext rmMsgCtx, Transaction transaction) {
		if (log.isDebugEnabled()) {
			log.debug("Enter: CreateSeqResponseMsgProcessor::processOutMessage");
			log.debug("Exit: CreateSeqResponseMsgProcessor::processOutMessage " + Boolean.FALSE);
		}
		return false;

	}
}
