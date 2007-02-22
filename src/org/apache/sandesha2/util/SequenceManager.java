/*
 * Copyright  1999-2004 The Apache Software Foundation.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.sandesha2.util;

import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.Parameter;
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
import org.apache.sandesha2.storage.beanmanagers.RMDBeanMgr;
import org.apache.sandesha2.storage.beans.RMDBean;
import org.apache.sandesha2.storage.beans.RMSBean;
import org.apache.sandesha2.workers.SequenceEntry;
import org.apache.sandesha2.wsrm.CreateSequence;

/**
 * This is used to set up a new sequence, both at the sending side and the
 * receiving side.
 */

public class SequenceManager {

	private static Log log = LogFactory.getLog(SequenceManager.class);

	/**
	 * Set up a new inbound sequence, triggered by the arrival of a create sequence message. As this
	 * is an inbound sequence, the sequencePropertyKey is the sequenceId.
	 */
	public static RMDBean setupNewSequence(RMMsgContext createSequenceMsg, StorageManager storageManager, SecurityManager securityManager, SecurityToken token)
			throws AxisFault {
		if (log.isDebugEnabled())
			log.debug("Enter: SequenceManager::setupNewSequence");
		
		String sequenceId = SandeshaUtil.getUUID();

		// Generate the new RMD Bean
		RMDBean rmdBean = new RMDBean();

		EndpointReference to = createSequenceMsg.getTo();
		if (to == null) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.toEPRNotValid, null);
			log.debug(message);
			throw new AxisFault(message);
		}

		EndpointReference replyTo = createSequenceMsg.getReplyTo();

		CreateSequence createSequence = (CreateSequence) createSequenceMsg
				.getMessagePart(Sandesha2Constants.MessageParts.CREATE_SEQ);
		if (createSequence == null) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.createSeqEntryNotFound);
			log.debug(message);
			throw new AxisFault(message);
		}

		EndpointReference acksTo = createSequence.getAcksTo().getEPR();

		if (acksTo == null) {
			FaultManager.makeCreateSequenceRefusedFault(createSequenceMsg, SandeshaMessageHelper.getMessage(SandeshaMessageKeys.noAcksToPartInCreateSequence), new Exception());
		} else if (acksTo.getAddress().equals(AddressingConstants.Final.WSA_NONE_URI)){
			FaultManager.makeCreateSequenceRefusedFault(createSequenceMsg, "AcksTo can not be " + AddressingConstants.Final.WSA_NONE_URI, new Exception());
		}

		MessageContext createSeqContext = createSequenceMsg.getMessageContext();
		
		// If this create is the result of a MakeConnection, then we must have a related
		// outbound sequence.
		SequenceEntry entry = (SequenceEntry) createSeqContext.getProperty(Sandesha2Constants.MessageContextProperties.MAKECONNECTION_ENTRY);
		if(entry != null && entry.isRmSource()) {
			rmdBean.setOutboundSequence(entry.getSequenceId());
		}

		rmdBean.setServerCompletedMessages(new RangeString());
		
		rmdBean.setReplyToEPR(to.getAddress());
		rmdBean.setAcksToEPR(acksTo.getAddress());

		// If no replyTo value. Send responses as sync.
		if (replyTo != null)
			rmdBean.setToEPR(replyTo.getAddress());

		// Store the security token alongside the sequence
		if(token != null) {
			String tokenData = securityManager.getTokenRecoveryData(token);
			rmdBean.setSecurityTokenData(tokenData);
		}		

		RMDBeanMgr nextMsgMgr = storageManager.getRMDBeanMgr();
		
		rmdBean.setSequenceID(sequenceId);
		rmdBean.setNextMsgNoToProcess(1);
		
		rmdBean.setToAddress(to.getAddress());
		
		// If this sequence has a 'To' address that is anonymous then we must have got the
		// message as a response to a poll. We need to make sure that we keep polling until
		// the sequence is closed.
		if(to.hasAnonymousAddress()) {
			String newKey = SandeshaUtil.getUUID();
			rmdBean.setPollingMode(true);
			rmdBean.setReferenceMessageKey(newKey);
			storageManager.storeMessageContext(newKey, createSeqContext);
		}

		String messageRMNamespace = createSequence.getNamespaceValue();

		String specVersion = null;
		if (Sandesha2Constants.SPEC_2005_02.NS_URI.equals(messageRMNamespace)) {
			specVersion = Sandesha2Constants.SPEC_VERSIONS.v1_0;
		} else if (Sandesha2Constants.SPEC_2007_02.NS_URI.equals(messageRMNamespace)) {
			specVersion = Sandesha2Constants.SPEC_VERSIONS.v1_1;
		} else {
			throw new SandeshaException(SandeshaMessageHelper.getMessage(SandeshaMessageKeys.cannotDecideRMVersion));
		}

		rmdBean.setRMVersion(specVersion);

		nextMsgMgr.insert(rmdBean);

		// TODO get the SOAP version from the create seq message.

		if (log.isDebugEnabled())
			log.debug("Exit: SequenceManager::setupNewSequence, " + rmdBean);
		return rmdBean;
	}

	public void removeSequence(String sequence) {

	}

	public static RMSBean setupNewClientSequence(MessageContext firstAplicationMsgCtx,
			String internalSequenceId, StorageManager storageManager) throws SandeshaException {
		if (log.isDebugEnabled())
			log.debug("Enter: SequenceManager::setupNewClientSequence");
		
		RMSBean rmsBean = new RMSBean();
		rmsBean.setInternalSequenceID(internalSequenceId);

		// If we are server-side, we use the details from the inbound sequence to help set
		// up the reply sequence.
		String inboundSequence = null;
		RMDBean inboundBean = null;
		if(firstAplicationMsgCtx.isServerSide()) {
			inboundSequence = (String) firstAplicationMsgCtx.getProperty(Sandesha2Constants.MessageContextProperties.INBOUND_SEQUENCE_ID);
			if(inboundSequence != null) {
				inboundBean = SandeshaUtil.getRMDBeanFromSequenceId(storageManager, inboundSequence);
			}
		}
		
		// Finding the spec version
		String specVersion = null;
		if (firstAplicationMsgCtx.isServerSide()) {
			// in the server side, get the RM version from the request sequence.
			if(inboundBean == null || inboundBean.getRMVersion() == null) {
				String beanInfo = (inboundBean == null) ? "null" : inboundBean.toString();
				String message = SandeshaMessageHelper.getMessage(
						SandeshaMessageKeys.cannotChooseSpecLevel, inboundSequence, beanInfo );
				SandeshaException e = new SandeshaException(message);
				if(log.isDebugEnabled()) log.debug("Throwing", e);
				throw e;
			}

			specVersion = inboundBean.getRMVersion();
		} else {
			// in the client side, user will set the RM version.
			specVersion = (String) firstAplicationMsgCtx.getProperty(SandeshaClientConstants.RM_SPEC_VERSION);
			
			// If the spec version is null, look in the axis operation to see value has been set
			Parameter opLevel = firstAplicationMsgCtx.getAxisOperation().getParameter(SandeshaClientConstants.RM_SPEC_VERSION);
			if (specVersion == null && opLevel != null)	specVersion = (String) opLevel.getValue();						
		}

		if (specVersion == null)
			// TODO change the default to v1_1
			specVersion = SpecSpecificConstants.getDefaultSpecVersion(); 
		
		rmsBean.setRMVersion(specVersion);

		// Set up the To EPR
		EndpointReference toEPR = firstAplicationMsgCtx.getTo();

		if (toEPR == null) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.toEPRNotValid, null);
			log.debug(message);
			throw new SandeshaException(message);
		}

		rmsBean.setToEPR(toEPR.getAddress());

		// Discover the correct acksTo and replyTo EPR for this RMSBean
		EndpointReference acksToEPR = null;
		EndpointReference replyToEPR = null;

		if (firstAplicationMsgCtx.isServerSide()) {
			// Server side, we want the replyTo and AcksTo EPRs to point into this server.
			// We can work that out by looking at the RMD bean that pulled the message in,
			// and copying its 'ReplyTo' address.
			if(inboundBean != null && inboundBean.getReplyToEPR() != null) {
				acksToEPR = new EndpointReference(inboundBean.getReplyToEPR());
				replyToEPR = new EndpointReference(inboundBean.getReplyToEPR());
			} else {
				String beanInfo = (inboundBean == null) ? "null" : inboundBean.toString();
				String message = SandeshaMessageHelper.getMessage(
						SandeshaMessageKeys.cannotChooseAcksTo, inboundSequence, beanInfo);
				SandeshaException e = new SandeshaException(message);
				if(log.isDebugEnabled()) log.debug("Throwing", e);
				throw e;
			}

		} else {
			replyToEPR = firstAplicationMsgCtx.getReplyTo();

			// For client-side sequences there are 3 options:
			// 1) An explict AcksTo, set via the client API
			// 2) The replyTo from the app message
			// 3) The anonymous URI (for which we can leave a null EPR)
			String acksTo = (String) firstAplicationMsgCtx.getProperty(SandeshaClientConstants.AcksTo);
			if (acksTo != null) {
				if (log.isDebugEnabled())
					log.debug("Using explicit AcksTo, addr=" + acksTo);
				acksToEPR = new EndpointReference(acksTo);
			} else if(replyToEPR != null) {
				if (log.isDebugEnabled())
					log.debug("Using replyTo EPR as AcksTo, addr=" + replyToEPR.getAddress());
				acksToEPR = replyToEPR;
			}
		}
		// In case either of the replyTo or AcksTo is anonymous, rewrite them using the AnonURI template
		replyToEPR = SandeshaUtil.rewriteEPR(replyToEPR, firstAplicationMsgCtx);
		acksToEPR = SandeshaUtil.rewriteEPR(acksToEPR, firstAplicationMsgCtx);
		
		// Store both the acksTo and replyTo 
		if(replyToEPR != null) rmsBean.setReplyToEPR(replyToEPR.getAddress());
		if(acksToEPR  != null) rmsBean.setAcksToEPR(acksToEPR.getAddress());
		
		// New up the client completed message ranges list
		rmsBean.setClientCompletedMessages(new RangeString());

		// saving transportTo value;
		String transportTo = (String) firstAplicationMsgCtx.getProperty(Constants.Configuration.TRANSPORT_URL);
		if (transportTo != null) {
			rmsBean.setTransportTo(transportTo);
		}

		// updating the last activated time.
		rmsBean.setLastActivatedTime(System.currentTimeMillis());
		
		if (log.isDebugEnabled())
			log.debug("Exit: SequenceManager::setupNewClientSequence " + rmsBean);
		return rmsBean;
	}

	public static boolean hasSequenceTimedOut(String internalSequenceId, RMMsgContext rmMsgCtx, StorageManager storageManager)
			throws SandeshaException {

		// operation is the lowest level, Sandesha2 could be engaged.
		SandeshaPolicyBean propertyBean = SandeshaUtil.getPropertyBean(rmMsgCtx.getMessageContext()
				.getAxisOperation());

		if (propertyBean.getInactivityTimeoutInterval() <= 0)
			return false;

		boolean sequenceTimedOut = false;

		RMSBean rmsBean = SandeshaUtil.getRMSBeanFromInternalSequenceId(storageManager, internalSequenceId);
		
		if (rmsBean != null) {
			long lastActivatedTime = rmsBean.getLastActivatedTime();
			long timeNow = System.currentTimeMillis();
			if (lastActivatedTime > 0 && (lastActivatedTime + propertyBean.getInactivityTimeoutInterval() < timeNow))
				sequenceTimedOut = true;
		}
		return sequenceTimedOut;
	}
}
