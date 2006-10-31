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

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.MessageContextConstants;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.context.OperationContextFactory;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.OutInAxisOperation;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.util.Utils;
import org.apache.axis2.wsdl.WSDLConstants.WSDL20_2004Constants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.RMMsgContext;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.client.SandeshaClientConstants;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.msgreceivers.RMMessageReceiver;
import org.apache.sandesha2.security.SecurityManager;
import org.apache.sandesha2.security.SecurityToken;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.beanmanagers.SenderBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.SequencePropertyBeanMgr;
import org.apache.sandesha2.storage.beans.SenderBean;
import org.apache.sandesha2.storage.beans.SequencePropertyBean;
import org.apache.sandesha2.util.AcknowledgementManager;
import org.apache.sandesha2.util.FaultManager;
import org.apache.sandesha2.util.RMMsgCreator;
import org.apache.sandesha2.util.SOAPAbstractFactory;
import org.apache.sandesha2.util.SandeshaUtil;
import org.apache.sandesha2.util.SpecSpecificConstants;
import org.apache.sandesha2.wsrm.CloseSequence;
import org.apache.sandesha2.wsrm.Identifier;
import org.apache.sandesha2.wsrm.SequenceAcknowledgement;
import org.apache.sandesha2.wsrm.TerminateSequence;

/**
 * Responsible for processing an incoming Close Sequence message. (As introduced
 * by the WSRM 1.1 specification)
 */

public class CloseSequenceProcessor implements MsgProcessor {

	private static final Log log = LogFactory.getLog(CloseSequenceProcessor.class);

	public boolean processInMessage(RMMsgContext rmMsgCtx) throws AxisFault {
		if (log.isDebugEnabled())
			log.debug("Enter: CloseSequenceProcessor::processInMessage");

		ConfigurationContext configCtx = rmMsgCtx.getMessageContext().getConfigurationContext();
		CloseSequence closeSequence = (CloseSequence) rmMsgCtx
				.getMessagePart(Sandesha2Constants.MessageParts.CLOSE_SEQUENCE);

		MessageContext msgCtx = rmMsgCtx.getMessageContext();

		String sequenceId = closeSequence.getIdentifier().getIdentifier();
		String sequencePropertyKey = SandeshaUtil.getSequencePropertyKey(rmMsgCtx);
		
		StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(configCtx, configCtx
				.getAxisConfiguration());
		SequencePropertyBeanMgr sequencePropMgr = storageManager.getSequencePropertyBeanMgr();
		
		// Check that the sender of this CloseSequence holds the correct token
		SequencePropertyBean tokenBean = sequencePropMgr.retrieve(sequenceId, Sandesha2Constants.SequenceProperties.SECURITY_TOKEN);
		if(tokenBean != null) {
			SecurityManager secManager = SandeshaUtil.getSecurityManager(msgCtx.getConfigurationContext());
			OMElement body = msgCtx.getEnvelope().getBody();
			SecurityToken token = secManager.recoverSecurityToken(tokenBean.getValue());
			secManager.checkProofOfPossession(token, body, msgCtx);
		}

		FaultManager faultManager = new FaultManager();
		SandeshaException fault = faultManager.checkForUnknownSequence(rmMsgCtx, sequenceId, storageManager);
		if (fault != null) {
			throw fault;
		}

		SequencePropertyBean sequenceClosedBean = new SequencePropertyBean();
		sequenceClosedBean.setSequencePropertyKey(sequencePropertyKey);
		sequenceClosedBean.setName(Sandesha2Constants.SequenceProperties.SEQUENCE_CLOSED);
		sequenceClosedBean.setValue(Sandesha2Constants.VALUE_TRUE);

		sequencePropMgr.insert(sequenceClosedBean);

		RMMsgContext ackRMMsgCtx = AcknowledgementManager.generateAckMessage(rmMsgCtx, sequencePropertyKey, sequenceId, storageManager);

		MessageContext ackMsgCtx = ackRMMsgCtx.getMessageContext();

		String rmNamespaceValue = rmMsgCtx.getRMNamespaceValue();
		ackRMMsgCtx.setRMNamespaceValue(rmNamespaceValue);

		SOAPFactory factory = SOAPAbstractFactory.getSOAPFactory(SandeshaUtil
				.getSOAPVersion(rmMsgCtx.getSOAPEnvelope()));

		// Setting new envelope
		SOAPEnvelope envelope = factory.getDefaultEnvelope();
		try {
			ackMsgCtx.setEnvelope(envelope);
		} catch (AxisFault e3) {
			throw new SandeshaException(e3.getMessage());
		}

		// adding the ack part to the envelope.
		Iterator sequenceAckIter = ackRMMsgCtx
				.getMessageParts(Sandesha2Constants.MessageParts.SEQ_ACKNOWLEDGEMENT);

		MessageContext closeSequenceMsg = rmMsgCtx.getMessageContext();

		MessageContext closeSequenceResponseMsg = null;

		try {
			closeSequenceResponseMsg = Utils.createOutMessageContext(closeSequenceMsg);
		} catch (AxisFault e1) {
			throw new SandeshaException(e1);
		}

		RMMsgContext closeSeqResponseRMMsg = RMMsgCreator.createCloseSeqResponseMsg(rmMsgCtx, closeSequenceResponseMsg,
				storageManager);

		while (sequenceAckIter.hasNext()) {
			SequenceAcknowledgement sequenceAcknowledgement = (SequenceAcknowledgement) sequenceAckIter.next();
			closeSeqResponseRMMsg.setMessagePart(Sandesha2Constants.MessageParts.SEQ_ACKNOWLEDGEMENT,
					sequenceAcknowledgement);
		}
		
		closeSeqResponseRMMsg.setFlow(MessageContext.OUT_FLOW);
		closeSeqResponseRMMsg.setProperty(Sandesha2Constants.APPLICATION_PROCESSING_DONE, "true");

		closeSequenceResponseMsg.setResponseWritten(true);

		closeSeqResponseRMMsg.addSOAPEnvelope();

		AxisEngine engine = new AxisEngine(closeSequenceMsg.getConfigurationContext());

		try {
			engine.send(closeSequenceResponseMsg);
		} catch (AxisFault e) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.couldNotSendCloseResponse,
					sequenceId, e.toString());
			throw new SandeshaException(message, e);
		}

		if (log.isDebugEnabled())
			log.debug("Exit: CloseSequenceProcessor::processInMessage " + Boolean.FALSE);
		return false;
	}

	public boolean processOutMessage(RMMsgContext rmMsgCtx) throws AxisFault {
		
		if (log.isDebugEnabled()) {
			log.debug("Enter: CloseSequenceProcessor::processOutMessage");
			log.debug("Exit: CloseSequenceProcessor::processOutMessage " + Boolean.FALSE);
		}
		
		MessageContext msgContext = rmMsgCtx.getMessageContext();
		ConfigurationContext configurationContext = msgContext.getConfigurationContext();
		Options options = msgContext.getOptions();

		StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(configurationContext,
				configurationContext.getAxisConfiguration());

		SequencePropertyBeanMgr seqPropMgr = storageManager.getSequencePropertyBeanMgr();

		String toAddress = rmMsgCtx.getTo().getAddress();
		String sequenceKey = (String) options.getProperty(SandeshaClientConstants.SEQUENCE_KEY);
		String internalSeqenceID = SandeshaUtil.getInternalSequenceID(toAddress, sequenceKey);

		String outSequenceID = SandeshaUtil.getSequenceProperty(internalSeqenceID,
				Sandesha2Constants.SequenceProperties.OUT_SEQUENCE_ID, storageManager);
		if (outSequenceID == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.couldNotSendCloseSeqNotFound, internalSeqenceID));


		// registring an InOutOperationContext for this.
		// since the serviceContext.fireAndForget only sets a inOnly One
		// this does not work when there is a closeSequnceResponse
		// TODO do processing of closeMessagesCorrectly., create a new
		// message instead of sendign the one given by the serviceClient
		// TODO important

		AxisOperation outInAxisOp = new OutInAxisOperation(new QName("temp"));

		AxisOperation referenceInOutOperation = msgContext.getAxisService()
				.getOperation(
						new QName(Sandesha2Constants.RM_IN_OUT_OPERATION_NAME));
		if (referenceInOutOperation == null) {
			String messge = "Cant find the recerence RM InOut operation";
			throw new SandeshaException(messge);
		}

		outInAxisOp.setParent(msgContext.getAxisService());
		// setting flows
		// outInAxisOp.setRemainingPhasesInFlow(referenceInOutOperation.getRemainingPhasesInFlow());
		outInAxisOp.setRemainingPhasesInFlow(referenceInOutOperation
				.getRemainingPhasesInFlow());

		outInAxisOp.setMessageReceiver(new RMMessageReceiver ());
		
		OperationContext opcontext = OperationContextFactory
				.createOperationContext(
						WSDL20_2004Constants.MEP_CONSTANT_OUT_IN, outInAxisOp);
		opcontext.setParent(msgContext.getServiceContext());
		configurationContext.registerOperationContext(rmMsgCtx.getMessageId(),
				opcontext);

		msgContext.setOperationContext(opcontext);
		msgContext.setAxisOperation(outInAxisOp);
		
		CloseSequence closeSequencePart = (CloseSequence) rmMsgCtx
				.getMessagePart(Sandesha2Constants.MessageParts.CLOSE_SEQUENCE);
		Identifier identifier = closeSequencePart.getIdentifier();
		if (identifier==null) {
			identifier = new Identifier (closeSequencePart.getNamespaceValue());
			closeSequencePart.setIdentifier(identifier);
		}
		
		identifier.setIndentifer(outSequenceID);

		rmMsgCtx.setFlow(MessageContext.OUT_FLOW);
		msgContext.setProperty(Sandesha2Constants.APPLICATION_PROCESSING_DONE, "true");

		rmMsgCtx.setTo(new EndpointReference(toAddress));

		String rmVersion = SandeshaUtil.getRMVersion(internalSeqenceID, storageManager);
		if (rmVersion == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(SandeshaMessageKeys.cannotDecideRMVersion));

		rmMsgCtx.setWSAAction(SpecSpecificConstants.getCloseSequenceAction(rmVersion));
		rmMsgCtx.setSOAPAction(SpecSpecificConstants.getCloseSequenceAction (rmVersion));

		String transportTo = SandeshaUtil.getSequenceProperty(internalSeqenceID,
				Sandesha2Constants.SequenceProperties.TRANSPORT_TO, storageManager);
		if (transportTo != null) {
			rmMsgCtx.setProperty(MessageContextConstants.TRANSPORT_URL, transportTo);
		}
		
		//setting msg context properties
		rmMsgCtx.setProperty(Sandesha2Constants.MessageContextProperties.SEQUENCE_ID, outSequenceID);
		rmMsgCtx.setProperty(Sandesha2Constants.MessageContextProperties.INTERNAL_SEQUENCE_ID, internalSeqenceID);
		rmMsgCtx.setProperty(Sandesha2Constants.MessageContextProperties.SEQUENCE_PROPERTY_KEY , sequenceKey);

		try {
			rmMsgCtx.addSOAPEnvelope();
		} catch (AxisFault e) {
			throw new SandeshaException(e.getMessage(),e);
		}

		String key = SandeshaUtil.getUUID();

		SenderBean closeBean = new SenderBean();
		closeBean.setMessageContextRefKey(key);

		storageManager.storeMessageContext(key, msgContext);

		closeBean.setTimeToSend(System.currentTimeMillis());

		closeBean.setMessageID(msgContext.getMessageID());
		
		EndpointReference to = msgContext.getTo();
		if (to!=null)
			closeBean.setToAddress(to.getAddress());
		
		// this will be set to true at the sender.
		closeBean.setSend(true);

		msgContext.setProperty(Sandesha2Constants.QUALIFIED_FOR_SENDING, Sandesha2Constants.VALUE_FALSE);

		closeBean.setReSend(false);

		SenderBeanMgr retramsmitterMgr = storageManager.getRetransmitterBeanMgr();

		retramsmitterMgr.insert(closeBean);


		rmMsgCtx.setProperty(Sandesha2Constants.SET_SEND_TO_TRUE, Sandesha2Constants.VALUE_TRUE);

		SandeshaUtil.executeAndStore(rmMsgCtx, key);

		if (log.isDebugEnabled())
			log.debug("Exit: CloseSeqMsgProcessor::processOutMessage " + Boolean.TRUE);
		
		return true;

	}

}
