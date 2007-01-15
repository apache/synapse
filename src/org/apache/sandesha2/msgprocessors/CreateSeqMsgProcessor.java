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
import java.util.Collection;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.engine.AxisEngine;
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
import org.apache.sandesha2.storage.beanmanagers.RMSBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.SequencePropertyBeanMgr;
import org.apache.sandesha2.storage.beans.RMDBean;
import org.apache.sandesha2.storage.beans.RMSBean;
import org.apache.sandesha2.storage.beans.SequencePropertyBean;
import org.apache.sandesha2.util.FaultManager;
import org.apache.sandesha2.util.RMMsgCreator;
import org.apache.sandesha2.util.SandeshaUtil;
import org.apache.sandesha2.util.SequenceManager;
import org.apache.sandesha2.wsrm.Accept;
import org.apache.sandesha2.wsrm.CreateSequence;
import org.apache.sandesha2.wsrm.CreateSequenceResponse;
import org.apache.sandesha2.wsrm.Endpoint;
import org.apache.sandesha2.wsrm.SequenceOffer;

/**
 * Responsible for processing an incoming Create Sequence message.
 */

public class CreateSeqMsgProcessor implements MsgProcessor {

	private static final Log log = LogFactory.getLog(CreateSeqMsgProcessor.class);

	public boolean processInMessage(RMMsgContext createSeqRMMsg) throws AxisFault {
		if (log.isDebugEnabled())
			log.debug("Enter: CreateSeqMsgProcessor::processInMessage");

		MessageContext createSeqMsg = createSeqRMMsg.getMessageContext();
		CreateSequence createSeqPart = (CreateSequence) createSeqRMMsg
				.getMessagePart(Sandesha2Constants.MessageParts.CREATE_SEQ);
		if (createSeqPart == null) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.noCreateSeqParts);
			log.debug(message);
			throw new SandeshaException(message);
		}

		ConfigurationContext context = createSeqMsg.getConfigurationContext();
		StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(context, context.getAxisConfiguration());

		FaultManager.checkForCreateSequenceRefused(createSeqMsg, storageManager);
		
		// If the inbound CreateSequence includes a SecurityTokenReference then
		// ask the security manager to resolve that to a token for us. We also
		// check that the Create was secured using the token.
		SecurityManager secManager = SandeshaUtil.getSecurityManager(context);
		OMElement theSTR = createSeqPart.getSecurityTokenReference();
		SecurityToken token = null;
		if(theSTR != null) {
			MessageContext msgcontext = createSeqRMMsg.getMessageContext();
			token = secManager.getSecurityToken(theSTR, msgcontext);
			
			// The create must be the body part of this message, so we check the
			// security of that element.
			OMElement body = msgcontext.getEnvelope().getBody();
			secManager.checkProofOfPossession(token, body, msgcontext);
		}

		MessageContext outMessage = null;
		SequencePropertyBeanMgr seqPropMgr = storageManager.getSequencePropertyBeanMgr();

		// Create the new sequence id, as well as establishing the beans that handle the
		// sequence state.
		RMDBean rmdBean = SequenceManager.setupNewSequence(createSeqRMMsg, storageManager, secManager, token);
			
		RMMsgContext createSeqResponse = RMMsgCreator.createCreateSeqResponseMsg(createSeqRMMsg, rmdBean.getSequenceID());
		outMessage = createSeqResponse.getMessageContext();
			
		createSeqResponse.setFlow(MessageContext.OUT_FLOW);

		// for making sure that this won't be processed again
		createSeqResponse.setProperty(Sandesha2Constants.APPLICATION_PROCESSING_DONE, "true"); 
		
		CreateSequenceResponse createSeqResPart = (CreateSequenceResponse) createSeqResponse
				.getMessagePart(Sandesha2Constants.MessageParts.CREATE_SEQ_RESPONSE);

			// OFFER PROCESSING
		SequenceOffer offer = createSeqPart.getSequenceOffer();
		if (offer != null) {
			Accept accept = createSeqResPart.getAccept();
			if (accept == null) {
				String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.noAcceptPart);
				log.debug(message);
				throw new SandeshaException(message);
			}

			// offered seq id
			String offeredSequenceID = offer.getIdentifer().getIdentifier(); 
			
			boolean offerEcepted = offerAccepted(offeredSequenceID, context, createSeqRMMsg, storageManager);

			if (offerEcepted) {
				// Setting the CreateSequence table entry for the outgoing
				// side.
				RMSBean rMSBean = new RMSBean();
				rMSBean.setSequenceID(offeredSequenceID);
				String outgoingSideInternalSequenceId = SandeshaUtil
						.getOutgoingSideInternalSequenceID(rmdBean.getSequenceID());
				rMSBean.setInternalSequenceID(outgoingSideInternalSequenceId);
				// this is a dummy value
				rMSBean.setCreateSeqMsgID(SandeshaUtil.getUUID()); 
					
				rMSBean.setToEPR(rmdBean.getToEPR());
				rMSBean.setAcksToEPR(rmdBean.getAcksToEPR());
				rMSBean.setReplyToEPR(rmdBean.getReplyToEPR());
				rMSBean.setLastActivatedTime(System.currentTimeMillis());
				
				String outgoingSideSequencePropertyKey = outgoingSideInternalSequenceId;

				// Setting sequence properties for the outgoing sequence.
				// Only will be used by the server side response path. Will
				// be wasted properties for the client side.
					
				Endpoint endpoint = offer.getEndpoint();
				if (endpoint!=null) {
					// setting the OfferedEndpoint
					rMSBean.setOfferedEndPoint(endpoint.getEPR().getAddress());
				}
	
				RMSBeanMgr rmsBeanMgr = storageManager.getRMSBeanMgr();
				rmsBeanMgr.insert(rMSBean);

				// Store the inbound token (if any) with the new sequence
				if(token != null) {
					String tokenData = secManager.getTokenRecoveryData(token);
					SequencePropertyBean tokenBean = new SequencePropertyBean(
							outgoingSideSequencePropertyKey,
							Sandesha2Constants.SequenceProperties.SECURITY_TOKEN,
							tokenData);
					seqPropMgr.insert(tokenBean);
				}
			} else {
				// removing the accept part.
				createSeqResPart.setAccept(null);
				createSeqResponse.addSOAPEnvelope();
			}
		}
			
			// Add this sequence to the list of inbound sequences
		SequencePropertyBean incomingSequenceListBean = seqPropMgr.retrieve(
				Sandesha2Constants.SequenceProperties.ALL_SEQUENCES,
				Sandesha2Constants.SequenceProperties.INCOMING_SEQUENCE_LIST);

		if (incomingSequenceListBean == null) {
			incomingSequenceListBean = new SequencePropertyBean();
			incomingSequenceListBean.setSequencePropertyKey(Sandesha2Constants.SequenceProperties.ALL_SEQUENCES);
			incomingSequenceListBean.setName(Sandesha2Constants.SequenceProperties.INCOMING_SEQUENCE_LIST);
			incomingSequenceListBean.setValue(null);

			// this get inserted before
			seqPropMgr.insert(incomingSequenceListBean);
		}

		ArrayList incomingSequenceList = SandeshaUtil.getArrayListFromString(incomingSequenceListBean.getValue());
		incomingSequenceList.add(rmdBean.getSequenceID());
		incomingSequenceListBean.setValue(incomingSequenceList.toString());
		seqPropMgr.update(incomingSequenceListBean);
			
		//TODO add createSequenceResponse message as the referenceMessage to the RMDBean.

		outMessage.setResponseWritten(true);

		rmdBean.setLastActivatedTime(System.currentTimeMillis());
		storageManager.getRMDBeanMgr().update(rmdBean);

		AxisEngine engine = new AxisEngine(context);
		try{
			engine.send(outMessage);				
		}
		catch(AxisFault e){
			throw new SandeshaException(
					SandeshaMessageHelper.getMessage(SandeshaMessageKeys.couldNotSendCreateSeqResponse, e.toString()), 
					e);
		}

		boolean anon = true;
		if (rmdBean.getToEPR() != null) {
			EndpointReference toEPR = new EndpointReference(rmdBean.getToEPR());
			if (!toEPR.hasAnonymousAddress()) anon = false;
		}
		if(anon) {
			createSeqMsg.getOperationContext().setProperty(org.apache.axis2.Constants.RESPONSE_WRITTEN, "true");
		} else {
				createSeqMsg.getOperationContext().setProperty(org.apache.axis2.Constants.RESPONSE_WRITTEN, "false");
		}
			
		createSeqRMMsg.pause();

		if (log.isDebugEnabled())
			log.debug("Exit: CreateSeqMsgProcessor::processInMessage " + Boolean.TRUE);
		return true;
	}

	private boolean offerAccepted(String sequenceId, ConfigurationContext configCtx, RMMsgContext createSeqRMMsg,
			StorageManager storageManager) throws SandeshaException {
		if (log.isDebugEnabled())
			log.debug("Enter: CreateSeqMsgProcessor::offerAccepted, " + sequenceId);

		if ("".equals(sequenceId)) {
			if (log.isDebugEnabled())
				log.debug("Exit: CreateSeqMsgProcessor::offerAccepted, " + false);
			return false;
		}

		RMSBeanMgr createSeqMgr = storageManager.getRMSBeanMgr();

		RMSBean createSeqFindBean = new RMSBean();
		createSeqFindBean.setSequenceID(sequenceId);
		Collection arr = createSeqMgr.find(createSeqFindBean);

		if (arr.size() > 0) {
			if (log.isDebugEnabled())
				log.debug("Exit: CreateSeqMsgProcessor::offerAccepted, " + false);
			return false;
		}
		if (sequenceId.length() <= 1) {
			if (log.isDebugEnabled())
				log.debug("Exit: CreateSeqMsgProcessor::offerAccepted, " + false);
			return false; // Single character offers are NOT accepted.
		}

		if (log.isDebugEnabled())
			log.debug("Exit: CreateSeqMsgProcessor::offerAccepted, " + true);
		return true;
	}

	public boolean processOutMessage(RMMsgContext rmMsgCtx) throws SandeshaException {
		if (log.isDebugEnabled())
			log.debug("Enter: CreateSeqMsgProcessor::processOutMessage");

		MessageContext msgCtx = rmMsgCtx.getMessageContext();

		// adding the SANDESHA_LISTENER
		SandeshaListener faultCallback = (SandeshaListener) msgCtx.getOptions().getProperty(
				SandeshaClientConstants.SANDESHA_LISTENER);
		if (faultCallback != null) {
			OperationContext operationContext = msgCtx.getOperationContext();
			if (operationContext != null) {
				operationContext.setProperty(SandeshaClientConstants.SANDESHA_LISTENER, faultCallback);
			}
		}
		if (log.isDebugEnabled())
			log.debug("Exit: CreateSeqMsgProcessor::processOutMessage " + Boolean.FALSE);
		return false;
	}
	
}
