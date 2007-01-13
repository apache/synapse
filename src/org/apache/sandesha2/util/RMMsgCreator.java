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

package org.apache.sandesha2.util;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.util.Utils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.RMMsgContext;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.client.SandeshaClientConstants;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.security.SecurityManager;
import org.apache.sandesha2.security.SecurityToken;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.beanmanagers.SequencePropertyBeanMgr;
import org.apache.sandesha2.storage.beans.RMDBean;
import org.apache.sandesha2.storage.beans.RMSBean;
import org.apache.sandesha2.storage.beans.SequencePropertyBean;
import org.apache.sandesha2.wsrm.Accept;
import org.apache.sandesha2.wsrm.AckFinal;
import org.apache.sandesha2.wsrm.AcknowledgementRange;
import org.apache.sandesha2.wsrm.AcksTo;
import org.apache.sandesha2.wsrm.Address;
import org.apache.sandesha2.wsrm.CloseSequence;
import org.apache.sandesha2.wsrm.CloseSequenceResponse;
import org.apache.sandesha2.wsrm.CreateSequence;
import org.apache.sandesha2.wsrm.CreateSequenceResponse;
import org.apache.sandesha2.wsrm.Endpoint;
import org.apache.sandesha2.wsrm.IOMRMPart;
import org.apache.sandesha2.wsrm.Identifier;
import org.apache.sandesha2.wsrm.MakeConnection;
import org.apache.sandesha2.wsrm.SequenceAcknowledgement;
import org.apache.sandesha2.wsrm.SequenceOffer;
import org.apache.sandesha2.wsrm.TerminateSequence;
import org.apache.sandesha2.wsrm.TerminateSequenceResponse;
import org.apache.sandesha2.wsrm.UsesSequenceSTR;

/**
 * Used to create new RM messages.
 */

public class RMMsgCreator {

	private static Log log = LogFactory.getLog(RMMsgCreator.class);

	/**
	 * Create a new CreateSequence message.
	 * 
	 * @param applicationRMMsg
	 * @param internalSequenceId
	 * @param acksToEPR
	 * @return
	 * @throws SandeshaException
	 */
	public static RMMsgContext createCreateSeqMsg(RMSBean rmsBean, RMMsgContext applicationRMMsg, String sequencePropertyKey,
			EndpointReference acksToEPR, StorageManager storageManager) throws AxisFault {

		MessageContext applicationMsgContext = applicationRMMsg.getMessageContext();
		if (applicationMsgContext == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(SandeshaMessageKeys.appMsgIsNull));
		ConfigurationContext context = applicationMsgContext.getConfigurationContext();
		if (context == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(SandeshaMessageKeys.configContextNotSet));

		MessageContext createSeqmsgContext;
		try {
			// creating by copying common contents. (this will not set contexts
			// except for configCtx).
			AxisOperation createSequenceOperation = SpecSpecificConstants.getWSRMOperation(
					Sandesha2Constants.MessageTypes.CREATE_SEQ,
					SandeshaUtil.getRMVersion(sequencePropertyKey, storageManager),
					applicationMsgContext.getAxisService());

			createSeqmsgContext = SandeshaUtil
					.createNewRelatedMessageContext(applicationRMMsg, createSequenceOperation);
			
			OperationContext createSeqOpCtx = createSeqmsgContext.getOperationContext();
			String createSeqMsgId = SandeshaUtil.getUUID();
			createSeqmsgContext.setMessageID(createSeqMsgId);
			context.registerOperationContext(createSeqMsgId, createSeqOpCtx);

		} catch (AxisFault e) {
			throw new SandeshaException(e.getMessage(), e);
		}
        
		RMMsgContext createSeqRMMsg = new RMMsgContext(createSeqmsgContext);

		String rmVersion = SandeshaUtil.getRMVersion(sequencePropertyKey, storageManager);
		if (rmVersion == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(SandeshaMessageKeys.cannotDecideRMVersion));

		String rmNamespaceValue = SpecSpecificConstants.getRMNamespaceValue(rmVersion);

		// Decide which addressing version to use. We copy the version that the application
		// is already using (if set), and fall back to the level in the spec if that isn't
		// found.
		String addressingNamespace = (String) applicationMsgContext.getProperty(AddressingConstants.WS_ADDRESSING_VERSION);
		Boolean disableAddressing = (Boolean) applicationMsgContext.getProperty(AddressingConstants.DISABLE_ADDRESSING_FOR_OUT_MESSAGES);
		if(addressingNamespace == null) {
			// Addressing may still be enabled, as it defaults to the final spec. The only time
			// we follow the RM spec is when addressing has been explicitly disabled.
			if(disableAddressing != null && disableAddressing.booleanValue())
				addressingNamespace = SpecSpecificConstants.getAddressingNamespace(rmNamespaceValue);
			else
				addressingNamespace = AddressingConstants.Final.WSA_NAMESPACE;
		}
		
		// If acksTo has not been set, then default to anonaymous, using the correct spec level
		String anon = SpecSpecificConstants.getAddressingAnonymousURI(addressingNamespace);
		if(acksToEPR == null) acksToEPR = new EndpointReference(anon);
		
		CreateSequence createSequencePart = new CreateSequence(rmNamespaceValue);

		// Adding sequence offer - if present
		OperationContext operationcontext = applicationMsgContext.getOperationContext();
		if (operationcontext != null) {
			String offeredSequence = (String) applicationMsgContext
					.getProperty(SandeshaClientConstants.OFFERED_SEQUENCE_ID);
			EndpointReference offeredEndpoint = (EndpointReference) applicationMsgContext
					.getProperty(SandeshaClientConstants.OFFERED_ENDPOINT);
			
			if (offeredEndpoint==null) {
				EndpointReference replyTo = applicationMsgContext.getReplyTo();  //using replyTo as the Endpoint if it is not specified
			
				if (replyTo!=null) {
					offeredEndpoint = SandeshaUtil.cloneEPR(replyTo);
				}
			}
			// Finally fall back to using an anonymous endpoint
			if (offeredEndpoint==null) {
				offeredEndpoint = new EndpointReference(anon);
			}
			if (offeredSequence != null && !"".equals(offeredSequence)) {
				SequenceOffer offerPart = new SequenceOffer(rmNamespaceValue);
				Identifier identifier = new Identifier(rmNamespaceValue);
				identifier.setIndentifer(offeredSequence);
				offerPart.setIdentifier(identifier);
				createSequencePart.setSequenceOffer(offerPart);
				
				if (Sandesha2Constants.SPEC_2006_08.NS_URI.equals(rmNamespaceValue)) {
					Endpoint endpoint = new Endpoint (offeredEndpoint, rmNamespaceValue, addressingNamespace);
					offerPart.setEndpoint(endpoint);
				}
			}
		}
		
		String to = rmsBean.getToEPR();
		String replyTo = rmsBean.getReplyToEPR();

		if (to == null) {
			String message = SandeshaMessageHelper
					.getMessage(SandeshaMessageKeys.toBeanNotSet);
			throw new SandeshaException(message);
		}

		// TODO store and retrieve a full EPR instead of just the address.
		EndpointReference toEPR = new EndpointReference(to);
		createSeqRMMsg.setTo(toEPR);

		if(replyTo != null) {
			EndpointReference replyToEPR = new EndpointReference(replyTo);
			createSeqRMMsg.setReplyTo(replyToEPR);
		}

		AcksTo acksTo = new AcksTo(acksToEPR, rmNamespaceValue, addressingNamespace);
		createSequencePart.setAcksTo(acksTo);
		
		createSeqRMMsg.setMessagePart(Sandesha2Constants.MessageParts.CREATE_SEQ, createSequencePart);

		// Find the token that should be used to secure this new sequence. If there is a token, then we
		// save it in the properties so that the caller can store the token within the create sequence
		// bean.
		SecurityManager secMgr = SandeshaUtil.getSecurityManager(context);
		SecurityToken token = secMgr.getSecurityToken(applicationMsgContext);
		if(token != null) {
			OMElement str = secMgr.createSecurityTokenReference(token, createSeqmsgContext);
			createSequencePart.setSecurityTokenReference(str);
			createSeqRMMsg.setProperty(Sandesha2Constants.SequenceProperties.SECURITY_TOKEN, token);
			
			// If we are using token based security, and the 1.1 spec level, then we
			// should introduce a UsesSequenceSTR header into the message.
			if(createSequencePart.getNamespaceValue().equals(Sandesha2Constants.SPEC_2006_08.NS_URI)) {
				UsesSequenceSTR header = new UsesSequenceSTR(null, Sandesha2Constants.SPEC_2006_08.NS_URI);
				header.toSOAPEnvelope(createSeqmsgContext.getEnvelope());
			}

			// Ensure that the correct token will be used to secure the outbound create sequence message.
			// We cannot use the normal helper method as we have not stored the token into the sequence bean yet.
			secMgr.applySecurityToken(token, createSeqRMMsg.getMessageContext());
		}

		createSeqRMMsg.setAction(SpecSpecificConstants.getCreateSequenceAction(SandeshaUtil.getRMVersion(
				sequencePropertyKey, storageManager)));
		createSeqRMMsg.setSOAPAction(SpecSpecificConstants.getCreateSequenceSOAPAction(SandeshaUtil.getRMVersion(
				sequencePropertyKey, storageManager)));

		createSeqRMMsg.addSOAPEnvelope();
		
		return createSeqRMMsg;
	}

	/**
	 * Creates a new TerminateSequence message.
	 * 
	 * @param referenceRMMessage
	 * @param sequenceId
	 * @return
	 * @throws SandeshaException
	 */
	public static RMMsgContext createTerminateSequenceMessage(RMMsgContext referenceRMMessage, String sequenceId,
			String sequencePropertyKey, StorageManager storageManager) throws AxisFault {
		MessageContext referenceMessage = referenceRMMessage.getMessageContext();
		if (referenceMessage == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(SandeshaMessageKeys.msgContextNotSet));

		AxisOperation terminateOperation = SpecSpecificConstants.getWSRMOperation(
				Sandesha2Constants.MessageTypes.TERMINATE_SEQ,
				SandeshaUtil.getRMVersion(sequencePropertyKey, storageManager),
				referenceMessage.getAxisService());

		ConfigurationContext configCtx = referenceMessage.getConfigurationContext();
		if (configCtx == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(SandeshaMessageKeys.configContextNotSet));

		MessageContext terminateMessage = SandeshaUtil.createNewRelatedMessageContext(referenceRMMessage,
				terminateOperation);

		OperationContext operationContext = terminateMessage.getOperationContext();
		configCtx.registerOperationContext(terminateMessage.getMessageID(), operationContext); // to
																								// receive
																								// terminate
																								// sequence
																								// response
																								// messages
																								// correctly.

		String rmVersion = SandeshaUtil.getRMVersion(sequencePropertyKey, storageManager);
		if (rmVersion == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(SandeshaMessageKeys.cannotDecideRMVersion));

		String rmNamespaceValue = SpecSpecificConstants.getRMNamespaceValue(rmVersion);

		if (!SpecSpecificConstants.isTerminateSequenceResponseRequired(rmVersion)) {
			terminateMessage.setProperty(MessageContext.TRANSPORT_IN, null);
		}

		RMMsgContext terminateRMMessage = MsgInitializer.initializeMessage(terminateMessage);

		if (terminateMessage == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(SandeshaMessageKeys.msgContextNotSet));

		terminateMessage.setMessageID(SandeshaUtil.getUUID());

		AxisOperation referenceMsgOperation = referenceMessage.getAxisOperation();
		if (referenceMsgOperation != null) {
			ArrayList outPhases = referenceMsgOperation.getPhasesOutFlow();
			if (outPhases != null) {
				terminateOperation.setPhasesOutFlow(outPhases);
				terminateOperation.setPhasesOutFaultFlow(outPhases);
			}
		}

		TerminateSequence terminateSequencePart = new TerminateSequence(rmNamespaceValue);
		Identifier identifier = new Identifier(rmNamespaceValue);
		identifier.setIndentifer(sequenceId);
		terminateSequencePart.setIdentifier(identifier);
		terminateRMMessage.setMessagePart(Sandesha2Constants.MessageParts.TERMINATE_SEQ, terminateSequencePart);

		terminateMessage.setProperty(MessageContext.TRANSPORT_IN, null); // no
																			// need
																			// for
																			// an
																			// incoming
																			// transport
																			// for
																			// an
																			// terminate
		// message. If this is put, sender will look for an response.

		// Ensure the correct token is used to secure the terminate sequence
		secureOutboundMessage(sequencePropertyKey, terminateMessage);
		
		return terminateRMMessage;
	}

	/**
	 * Create a new CreateSequenceResponse message.
	 * 
	 * @param createSeqMessage
	 * @param outMessage
	 * @param newSequenceID
	 * @return
	 * @throws AxisFault
	 */
	public static RMMsgContext createCreateSeqResponseMsg(RMMsgContext createSeqMessage, String newSequenceID) throws AxisFault {

		CreateSequence cs = (CreateSequence) createSeqMessage.getMessagePart(Sandesha2Constants.MessageParts.CREATE_SEQ);
		String namespace = createSeqMessage.getRMNamespaceValue();

		CreateSequenceResponse response = new CreateSequenceResponse(namespace);
		Identifier identifier = new Identifier(namespace);
		identifier.setIndentifer(newSequenceID);
		response.setIdentifier(identifier);

		SequenceOffer offer = cs.getSequenceOffer();
		if (offer != null) {
			String outSequenceId = offer.getIdentifer().getIdentifier();

			if (outSequenceId != null && !"".equals(outSequenceId)) {

				Accept accept = new Accept(namespace);

				// Putting the To EPR as the AcksTo for the response sequence. We echo back the
				// addressing version that the create used.
				String addressingNamespace = cs.getAddressingNamespaceValue();
				EndpointReference acksToEPR = createSeqMessage.getTo();
				if(acksToEPR != null) {
					acksToEPR = SandeshaUtil.cloneEPR(acksToEPR);
				} else {
					String anon = SpecSpecificConstants.getAddressingAnonymousURI(addressingNamespace);
					acksToEPR = new EndpointReference(anon);
				}
				
				AcksTo acksTo = new AcksTo(acksToEPR, namespace, cs.getAddressingNamespaceValue());
				accept.setAcksTo(acksTo);
				response.setAccept(accept);
			}
		}

		String version = SpecSpecificConstants.getSpecVersionString(namespace);
		String action = SpecSpecificConstants.getCreateSequenceResponseAction(version);

		return createResponseMsg(createSeqMessage, response,
				Sandesha2Constants.MessageParts.CREATE_SEQ_RESPONSE,
				newSequenceID, action);
	}

	public static RMMsgContext createTerminateSeqResponseMsg(RMMsgContext terminateSeqRMMsg) throws AxisFault {
        
		TerminateSequence terminateSequence = (TerminateSequence) terminateSeqRMMsg
				.getMessagePart(Sandesha2Constants.MessageParts.TERMINATE_SEQ);
		String sequenceID = terminateSequence.getIdentifier().getIdentifier();

		String namespace = terminateSeqRMMsg.getRMNamespaceValue();

		TerminateSequenceResponse terminateSequenceResponse = new TerminateSequenceResponse(namespace);
		Identifier identifier = new Identifier(namespace);
		identifier.setIndentifer(sequenceID);
		terminateSequenceResponse.setIdentifier(identifier);

		String version = SpecSpecificConstants.getSpecVersionString(namespace);
		String action = SpecSpecificConstants.getTerminateSequenceResponseAction(version);

		return createResponseMsg(terminateSeqRMMsg, terminateSequenceResponse,
				Sandesha2Constants.MessageParts.TERMINATE_SEQ_RESPONSE,
				sequenceID, action);
	}

	public static RMMsgContext createCloseSeqResponseMsg(RMMsgContext closeSeqRMMsg) throws AxisFault {

		CloseSequence closeSequence = (CloseSequence) closeSeqRMMsg
				.getMessagePart(Sandesha2Constants.MessageParts.CLOSE_SEQUENCE);
		String sequenceID = closeSequence.getIdentifier().getIdentifier();

		String namespace = closeSeqRMMsg.getRMNamespaceValue();

		CloseSequenceResponse closeSequenceResponse = new CloseSequenceResponse(namespace);
		Identifier identifier = new Identifier(namespace);
		identifier.setIndentifer(sequenceID);
		closeSequenceResponse.setIdentifier(identifier);

		String version = SpecSpecificConstants.getSpecVersionString(namespace);
		String action = SpecSpecificConstants.getCloseSequenceResponseAction(version);

		return createResponseMsg(closeSeqRMMsg, closeSequenceResponse,
				Sandesha2Constants.MessageParts.CLOSE_SEQUENCE_RESPONSE,
				sequenceID, action);
	}

	private static RMMsgContext createResponseMsg(RMMsgContext requestMsg, IOMRMPart part, int messagePartId,
			String sequenceID, String action) throws AxisFault {

		MessageContext outMessage = Utils.createOutMessageContext(requestMsg.getMessageContext());
		RMMsgContext responseRMMsg = new RMMsgContext(outMessage);
		SOAPFactory factory = SOAPAbstractFactory.getSOAPFactory(SandeshaUtil.getSOAPVersion(requestMsg.getSOAPEnvelope()));

		String namespace = requestMsg.getRMNamespaceValue();
		responseRMMsg.setRMNamespaceValue(namespace);

		SOAPEnvelope envelope = factory.getDefaultEnvelope();
		responseRMMsg.setSOAPEnvelop(envelope);
		responseRMMsg.setMessagePart(messagePartId, part);

		outMessage.setWSAAction(action);
		outMessage.setSoapAction(action);

		responseRMMsg.addSOAPEnvelope();
		responseRMMsg.getMessageContext().setServerSide(true);

		// Ensure the correct token is used to secure the message
		secureOutboundMessage(sequenceID, outMessage);
		
		return responseRMMsg;
	}

	/**
	 * Adds an ack message to the given application message.
	 * 
	 * @param applicationMsg
	 * @param sequenceId
	 * @throws SandeshaException
	 */
	public static void addAckMessage(RMMsgContext applicationMsg, String sequencePropertyKey ,String sequenceId, StorageManager storageManager)
			throws SandeshaException {
		if(log.isDebugEnabled())
			log.debug("Entry: RMMsgCreator::addAckMessage " + sequenceId);
		
		SOAPEnvelope envelope = applicationMsg.getSOAPEnvelope();

		String rmVersion = SandeshaUtil.getRMVersion(sequencePropertyKey, storageManager);
		if (rmVersion == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(SandeshaMessageKeys.cannotDecideRMVersion));

		String rmNamespaceValue = SpecSpecificConstants.getRMNamespaceValue(rmVersion);

		SequenceAcknowledgement sequenceAck = new SequenceAcknowledgement(rmNamespaceValue);
		Identifier id = new Identifier(rmNamespaceValue);
		id.setIndentifer(sequenceId);
		sequenceAck.setIdentifier(id);

		RMDBean rmdBean = SandeshaUtil.getRMDBeanFromSequenceId(storageManager, sequenceId);

		ArrayList ackRangeArrayList = SandeshaUtil.getAckRangeArrayList(rmdBean.getServerCompletedMessages(), rmNamespaceValue);
		Iterator iterator = ackRangeArrayList.iterator();
		while (iterator.hasNext()) {
			AcknowledgementRange ackRange = (AcknowledgementRange) iterator.next();
			sequenceAck.addAcknowledgementRanges(ackRange);
		}

		if (rmdBean.isClosed()) {
			// sequence is closed. so add the 'Final' part.
			if (SpecSpecificConstants.isAckFinalAllowed(rmVersion)) {
				AckFinal ackFinal = new AckFinal(rmNamespaceValue);
				sequenceAck.setAckFinal(ackFinal);
			}
		}

		applicationMsg.setMessagePart(Sandesha2Constants.MessageParts.SEQ_ACKNOWLEDGEMENT, sequenceAck);

		sequenceAck.toOMElement(envelope.getHeader());
		
		if (applicationMsg.getWSAAction()==null) {
			applicationMsg.setAction(SpecSpecificConstants.getSequenceAcknowledgementAction(SandeshaUtil.getRMVersion(
					sequenceId, storageManager)));
			applicationMsg.setSOAPAction(SpecSpecificConstants.getSequenceAcknowledgementSOAPAction(SandeshaUtil
					.getRMVersion(sequenceId, storageManager)));
		}
		
		applicationMsg.setMessageId(SandeshaUtil.getUUID());
		
		//generating the SOAP envelope.
		try {
			applicationMsg.addSOAPEnvelope();
		} catch(AxisFault e) {
			throw new SandeshaException(e);
		}
		
		// Ensure the message also contains the token that needs to be used
		secureOutboundMessage(sequencePropertyKey, applicationMsg.getMessageContext());
		
		if(log.isDebugEnabled()) 
			log.debug("Exit: RMMsgCreator::addAckMessage");
	}
	
	public static RMMsgContext createMakeConnectionMessage (RMMsgContext referenceRMMessage,  String makeConnectionSeqId,
			String makeConnectionAnonURI, StorageManager storageManager) throws AxisFault {
		
		MessageContext referenceMessage = referenceRMMessage.getMessageContext();
		String rmNamespaceValue = referenceRMMessage.getRMNamespaceValue();
		String rmVersion = referenceRMMessage.getRMSpecVersion();
		
		AxisOperation makeConnectionOperation = SpecSpecificConstants.getWSRMOperation(
				Sandesha2Constants.MessageTypes.MAKE_CONNECTION_MSG,
				rmVersion,
				referenceMessage.getAxisService());

		MessageContext makeConnectionMessageCtx = SandeshaUtil.createNewRelatedMessageContext(referenceRMMessage,makeConnectionOperation);
		RMMsgContext makeConnectionRMMessageCtx = MsgInitializer.initializeMessage(makeConnectionMessageCtx);
		
		MakeConnection makeConnection = new MakeConnection (rmNamespaceValue);
		if (makeConnectionSeqId!=null) {
			Identifier identifier = new Identifier (rmNamespaceValue);
			identifier.setIndentifer(makeConnectionSeqId);
			makeConnection.setIdentifier(identifier);
		}
		
		if (makeConnectionAnonURI!=null) {
			Address address = new Address (rmNamespaceValue);
			address.setAddress (makeConnectionAnonURI);
			makeConnection.setAddress(address);
		}
		
		// Setting the addressing properties. As this is a poll we must send it to an non-anon
		// EPR, so we check both To and ReplyTo from the reference message
		EndpointReference epr = referenceMessage.getTo();
		if(epr.hasAnonymousAddress()) epr = referenceMessage.getReplyTo();
		
		makeConnectionMessageCtx.setTo(epr);
		makeConnectionMessageCtx.setWSAAction(SpecSpecificConstants.getMakeConnectionAction(rmVersion));
		makeConnectionMessageCtx.setMessageID(SandeshaUtil.getUUID());
		
		makeConnectionRMMessageCtx.setMessagePart(Sandesha2Constants.MessageParts.MAKE_CONNECTION,
				makeConnection);
		
		//generating the SOAP Envelope.
		makeConnectionRMMessageCtx.addSOAPEnvelope();
		
		// TODO work out how to find the correct sequence property key to look up the token
		// that we should include in the makeConnection (assuming we need one)
		
		return makeConnectionRMMessageCtx;
	}

	private static void secureOutboundMessage(String sequenceKey, MessageContext message)
	throws SandeshaException
	{
		if(log.isDebugEnabled()) log.debug("Entry: RMMsgCreator::secureOutboundMessage");

		ConfigurationContext configCtx = message.getConfigurationContext();
		StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(configCtx, configCtx.getAxisConfiguration());
		SequencePropertyBeanMgr sequencePropMgr = storageManager.getSequencePropertyBeanMgr();

		SequencePropertyBean tokenBean = sequencePropMgr.retrieve(sequenceKey, Sandesha2Constants.SequenceProperties.SECURITY_TOKEN);
		if(tokenBean != null) {
			if(log.isDebugEnabled()) log.debug("Securing outbound message");
			SecurityManager secManager = SandeshaUtil.getSecurityManager(configCtx);
			SecurityToken token = secManager.recoverSecurityToken(tokenBean.getValue());
			secManager.applySecurityToken(token, message);
		}

		if(log.isDebugEnabled()) log.debug("Exit: RMMsgCreator::secureOutboundMessage");
	}

}
