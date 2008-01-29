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

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.transport.TransportUtils;
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
import org.apache.sandesha2.storage.Transaction;
import org.apache.sandesha2.storage.beanmanagers.RMDBeanMgr;
import org.apache.sandesha2.storage.beans.RMDBean;
import org.apache.sandesha2.storage.beans.RMSBean;
import org.apache.sandesha2.util.FaultManager;
import org.apache.sandesha2.util.MsgInitializer;
import org.apache.sandesha2.util.RMMsgCreator;
import org.apache.sandesha2.util.RangeString;
import org.apache.sandesha2.util.SandeshaUtil;
import org.apache.sandesha2.util.SequenceManager;
import org.apache.sandesha2.util.SpecSpecificConstants;
import org.apache.sandesha2.util.TerminateManager;
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

	public boolean processInMessage(RMMsgContext createSeqRMMsg, Transaction transaction) throws AxisFault {
		if (log.isDebugEnabled())
			log.debug("Enter: CreateSeqMsgProcessor::processInMessage");

		try {
			
			CreateSequence createSeqPart = createSeqRMMsg.getCreateSequence();
			if (createSeqPart == null) {
				if (log.isDebugEnabled())
					log.debug(SandeshaMessageHelper.getMessage(SandeshaMessageKeys.noCreateSeqParts));
				FaultManager.makeCreateSequenceRefusedFault(createSeqRMMsg, SandeshaMessageHelper.getMessage(SandeshaMessageKeys.noCreateSeqParts), 
																										new Exception(), null);
				// Return false if an Exception hasn't been thrown.
				if (log.isDebugEnabled())
					log.debug("Exit: CreateSeqMsgProcessor::processInMessage " + Boolean.FALSE);				
				return false;

			}
	
			MessageContext createSeqMsg = createSeqRMMsg.getMessageContext();
			ConfigurationContext context = createSeqMsg.getConfigurationContext();
			StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(context, context.getAxisConfiguration());
			
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
	
			//if toAddress is RMAnon we may need to terminate the request side sequence here.
			EndpointReference toEPR = createSeqMsg.getTo();
			if (toEPR.hasAnonymousAddress()) {
	
				RMSBean findBean = new RMSBean ();
				findBean.setReplyToEndpointReference(toEPR);
				findBean.setTerminationPauserForCS(true);
				
				//TODO recheck
				RMSBean rmsBean = storageManager.getRMSBeanMgr().findUnique(findBean);
				if (rmsBean!=null) {					
					//AckManager hs not done the termination. Do the termination here.
					MessageContext requestSideRefMessage = storageManager.retrieveMessageContext(rmsBean.getReferenceMessageStoreKey(),context);
					if (requestSideRefMessage==null) {
						FaultManager.makeCreateSequenceRefusedFault(createSeqRMMsg, 
								SandeshaMessageHelper.getMessage(SandeshaMessageKeys.referencedMessageNotFound, rmsBean.getInternalSequenceID()),
								new Exception(), null);						
						// Return false if an Exception hasn't been thrown.
						if (log.isDebugEnabled())
							log.debug("Exit: CreateSeqMsgProcessor::processInMessage " + Boolean.FALSE);				
						return false;
					}
					
					RMMsgContext requestSideRefRMMessage = MsgInitializer.initializeMessage(requestSideRefMessage);
					TerminateManager.addTerminateSequenceMessage(requestSideRefRMMessage, rmsBean.getInternalSequenceID(), rmsBean.getSequenceID(), storageManager);
				}
			}
				

			MessageContext outMessage = null;
	
			// Create the new sequence id, as well as establishing the beans that handle the
			// sequence state.
			RMDBean rmdBean = SequenceManager.setupNewSequence(createSeqRMMsg, storageManager, secManager, token);
				
			RMMsgContext createSeqResponse = RMMsgCreator.createCreateSeqResponseMsg(createSeqRMMsg, rmdBean);
			outMessage = createSeqResponse.getMessageContext();
			// Set a message ID for this Create Sequence Response message
			outMessage.setMessageID(SandeshaUtil.getUUID());
				
			createSeqResponse.setFlow(MessageContext.OUT_FLOW);
	
			// for making sure that this won't be processed again
			createSeqResponse.setProperty(Sandesha2Constants.APPLICATION_PROCESSING_DONE, "true"); 
			
			CreateSequenceResponse createSeqResPart = createSeqResponse.getCreateSequenceResponse();
	
			// OFFER PROCESSING
			SequenceOffer offer = createSeqPart.getSequenceOffer();
			if (offer != null) {
				Accept accept = createSeqResPart.getAccept();
				if (accept == null) {
					if (log.isDebugEnabled())
						log.debug(SandeshaMessageHelper.getMessage(SandeshaMessageKeys.noAcceptPart));
					FaultManager.makeCreateSequenceRefusedFault(createSeqRMMsg, SandeshaMessageHelper.getMessage(SandeshaMessageKeys.noAcceptPart), 
																new Exception(),
																null);
					// Return false if an Exception hasn't been thrown.
					if (log.isDebugEnabled())
						log.debug("Exit: CreateSeqMsgProcessor::processInMessage " + Boolean.FALSE);				
					return false;
				}
	
				// offered seq id
				String offeredSequenceID = offer.getIdentifer().getIdentifier(); 
				
				boolean offerAccepted = offerAccepted(offeredSequenceID, context, createSeqRMMsg, storageManager);
	
				RMSBean rMSBean = null;
				//Before processing this offer any further we need to perform some extra checks 
				//on the offered EP if WS-RM Spec 1.1 is being used
				if(offerAccepted && Sandesha2Constants.SPEC_VERSIONS.v1_1.equals(rmdBean.getRMVersion())){
					Endpoint endpoint = offer.getEndpoint();
					if (endpoint!=null) {
						//Check to see if the offer endpoint has a value of WSA Anonymous
						String addressingNamespace = (String) createSeqRMMsg.getProperty(AddressingConstants.WS_ADDRESSING_VERSION);
						if(endpoint.equals(SpecSpecificConstants.getAddressingAnonymousURI(addressingNamespace))){
							//Don't accept the offer
							if (log.isDebugEnabled())
								log.debug("Offer Refused as it included a WSA Anonymous endpoint");	
							offerAccepted = false;
						} else {
							rMSBean = new RMSBean();
							//Set the offered EP
							rMSBean.setOfferedEndPoint(endpoint.getEPR().getAddress());
						}
					} else {
						//Don't accept the offer
						if (log.isDebugEnabled())
							log.debug("Offer Refused as it included a null endpoint");	
						offerAccepted = false;
					}
				} else if (offerAccepted && Sandesha2Constants.SPEC_VERSIONS.v1_0.equals(rmdBean.getRMVersion())){
					rMSBean = new RMSBean(); 
				}
				
				if (offerAccepted) {
					// Setting the CreateSequence table entry for the outgoing
					// side.
					rMSBean.setSequenceID(offeredSequenceID);
					String outgoingSideInternalSequenceId = SandeshaUtil
							.getOutgoingSideInternalSequenceID(rmdBean.getSequenceID());
					rMSBean.setInternalSequenceID(outgoingSideInternalSequenceId);
					// this is a dummy value
					rMSBean.setCreateSeqMsgID(SandeshaUtil.getUUID()); 
					
					if(rmdBean.getToEndpointReference() != null){
						rMSBean.setToEndpointReference(rmdBean.getToEndpointReference());
					} else {
						//It's Sync2Way so set to address to anonymous when spec 1.0 is used
						String specVersion = rmdBean.getRMVersion();
						if (Sandesha2Constants.SPEC_VERSIONS.v1_0.equals(specVersion)) {
							rMSBean.setToEndpointReference(new EndpointReference(AddressingConstants.Submission.WSA_ANONYMOUS_URL));
						} 	
					}
					
					rMSBean.setAcksToEndpointReference(rmdBean.getReplyToEndpointReference());  // The acks need to flow back into this endpoint
					rMSBean.setReplyToEndpointReference(rmdBean.getReplyToEndpointReference());
					rMSBean.setLastActivatedTime(System.currentTimeMillis());
					rMSBean.setRMVersion(rmdBean.getRMVersion());
					rMSBean.setClientCompletedMessages(new RangeString());
	
					// Setting sequence properties for the outgoing sequence.
					// Only will be used by the server side response path. Will
					// be wasted properties for the client side.
		
					rmdBean.setOutboundInternalSequence(outgoingSideInternalSequenceId);
					RMDBeanMgr rmdBeanMgr = storageManager.getRMDBeanMgr();
					rmdBeanMgr.update(rmdBean);
	
					// Store the inbound token (if any) with the new sequence
					rMSBean.setSecurityTokenData(rmdBean.getSecurityTokenData());
					
					// If this new sequence has anonymous acksTo, then we must poll for the acks
					// If the inbound sequence is targetted at the WSRM anonymous URI, we need to start
					// polling for this sequence.
					EndpointReference reference = rMSBean.getAcksToEndpointReference();
					if ((reference == null || reference.hasAnonymousAddress()) &&
						Sandesha2Constants.SPEC_VERSIONS.v1_1.equals(createSeqRMMsg.getRMSpecVersion())) {
						rMSBean.setPollingMode(true);
					}
					
					// Set the SOAP Version for this sequence.
					rMSBean.setSoapVersion(SandeshaUtil.getSOAPVersion(createSeqRMMsg.getSOAPEnvelope()));

					storageManager.getRMSBeanMgr().insert(rMSBean);
					
					SandeshaUtil.startWorkersForSequence(context, rMSBean);
					
				} else {
					// removing the accept part.
					createSeqResPart.setAccept(null);
					createSeqResponse.addSOAPEnvelope();
				}
			}
							
			//TODO add createSequenceResponse message as the referenceMessage to the RMDBean.
	
			outMessage.setResponseWritten(true);
	
			rmdBean.setLastActivatedTime(System.currentTimeMillis());
			
			// If the inbound sequence is targetted at the anonymous URI, we need to start
			// polling for this sequence.
			if (toEPR.hasAnonymousAddress()) {
				if (Sandesha2Constants.SPEC_VERSIONS.v1_1.equals(createSeqRMMsg.getRMSpecVersion())) {
					rmdBean.setPollingMode(true);
				}
			}
			
			storageManager.getRMDBeanMgr().update(rmdBean);
	
			SandeshaUtil.startWorkersForSequence(context, rmdBean);

			//
			// We have done all of our updates, so commit the transaction
			if(transaction != null && transaction.isActive()) transaction.commit();
			
			try{
				AxisEngine.send(outMessage);				
			}
			catch(AxisFault e){
				FaultManager.makeCreateSequenceRefusedFault(createSeqRMMsg, 
						SandeshaMessageHelper.getMessage(SandeshaMessageKeys.couldNotSendCreateSeqResponse, 
													     SandeshaUtil.getStackTraceFromException(e)), 
													     e,
													     rmdBean.getAcksToEndpointReference());
				// Return false if an Exception hasn't been thrown.
				if (log.isDebugEnabled())
					log.debug("Exit: CreateSeqMsgProcessor::processInMessage " + Boolean.FALSE);				
				return false;
			}
	
			EndpointReference replyTo = createSeqMsg.getReplyTo();
			if(replyTo == null || replyTo.hasAnonymousAddress()) {
				TransportUtils.setResponseWritten(createSeqMsg, true);
			}
			
	//		SequencePropertyBean findBean = new SequencePropertyBean ();
	//		findBean.setName (Sandesha2Constants.SequenceProperties.TERMINATE_ON_CREATE_SEQUENCE);
	//		findBean.setValue(createSeqMsg.getTo().getAddress());
			
			createSeqRMMsg.pause();
		}
		catch (Exception e) {
			if (log.isDebugEnabled())
				log.debug("Caught an exception processing CreateSequence message", e);
			// Does the message context already contain a fault ?
			// If it doesn't then we can add the CreateSequenceRefusedFault.
			if (createSeqRMMsg.getMessageContext().getProperty(SOAP12Constants.SOAP_FAULT_CODE_LOCAL_NAME) == null && 
					createSeqRMMsg.getMessageContext().getProperty(SOAP11Constants.SOAP_FAULT_CODE_LOCAL_NAME) == null) {
				// Add the fault details to the message
				FaultManager.makeCreateSequenceRefusedFault(createSeqRMMsg, SandeshaUtil.getStackTraceFromException(e), e, null);				
				
				// Return false if an Exception hasn't been thrown.
				if (log.isDebugEnabled())
					log.debug("Exit: CreateSeqMsgProcessor::processInMessage " + Boolean.FALSE);				
				return false;
			} 
				
			// If we are SOAP12 and we have already processed the fault - rethrow the exception
			if (createSeqRMMsg.getMessageContext().getProperty(SOAP12Constants.SOAP_FAULT_CODE_LOCAL_NAME) != null) {
					// throw the original exception
					if (e instanceof AxisFault)
						throw (AxisFault)e;
					 
					throw new SandeshaException(e);
			}
		}

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

		RMSBean createSeqFindBean = new RMSBean();
		createSeqFindBean.setSequenceID(sequenceId);
		Collection arr = storageManager.getRMSBeanMgr().find(createSeqFindBean);

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

	public boolean processOutMessage(RMMsgContext rmMsgCtx, Transaction transaction) {
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
