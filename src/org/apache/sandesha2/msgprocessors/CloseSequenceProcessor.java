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

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.util.Utils;
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
import org.apache.sandesha2.storage.beanmanagers.SequencePropertyBeanMgr;
import org.apache.sandesha2.storage.beans.SequencePropertyBean;
import org.apache.sandesha2.util.AcknowledgementManager;
import org.apache.sandesha2.util.FaultManager;
import org.apache.sandesha2.util.RMMsgCreator;
import org.apache.sandesha2.util.SOAPAbstractFactory;
import org.apache.sandesha2.util.SandeshaUtil;
import org.apache.sandesha2.wsrm.CloseSequence;
import org.apache.sandesha2.wsrm.SequenceAcknowledgement;

/**
 * Responsible for processing an incoming Close Sequence message. (As introduced
 * by the WSRM 1.1 specification)
 */

public class CloseSequenceProcessor implements MsgProcessor {

	private static final Log log = LogFactory.getLog(CloseSequenceProcessor.class);

	public void processInMessage(RMMsgContext rmMsgCtx) throws SandeshaException {
		if (log.isDebugEnabled())
			log.debug("Enter: CloseSequenceProcessor::processInMessage");

		ConfigurationContext configCtx = rmMsgCtx.getMessageContext().getConfigurationContext();
		CloseSequence closeSequence = (CloseSequence) rmMsgCtx
				.getMessagePart(Sandesha2Constants.MessageParts.CLOSE_SEQUENCE);

		MessageContext msgCtx = rmMsgCtx.getMessageContext();

		String sequenceID = closeSequence.getIdentifier().getIdentifier();

		StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(configCtx, configCtx
				.getAxisConfiguration());
		SequencePropertyBeanMgr sequencePropMgr = storageManager.getSequencePropertyBeanMgr();
		
		// Check that the sender of this CloseSequence holds the correct token
		SequencePropertyBean tokenBean = sequencePropMgr.retrieve(sequenceID, Sandesha2Constants.SequenceProperties.SECURITY_TOKEN);
		if(tokenBean != null) {
			SecurityManager secManager = SandeshaUtil.getSecurityManager(msgCtx.getConfigurationContext());
			OMElement body = msgCtx.getEnvelope().getBody();
			SecurityToken token = secManager.recoverSecurityToken(tokenBean.getValue());
			secManager.checkProofOfPossession(token, body, msgCtx);
		}

		FaultManager faultManager = new FaultManager();
		RMMsgContext faultMessageContext = faultManager.checkForUnknownSequence(rmMsgCtx, sequenceID, storageManager);
		if (faultMessageContext != null) {
			ConfigurationContext configurationContext = msgCtx.getConfigurationContext();
			AxisEngine engine = new AxisEngine(configurationContext);

			try {
				engine.sendFault(faultMessageContext.getMessageContext());
			} catch (AxisFault e) {
				throw new SandeshaException(SandeshaMessageHelper.getMessage(SandeshaMessageKeys.couldNotSendFault, e
						.toString()), e);
			}

			msgCtx.pause();
			return;
		}

		SequencePropertyBean sequenceClosedBean = new SequencePropertyBean();
		sequenceClosedBean.setSequencePropertyKey(sequenceID);
		sequenceClosedBean.setName(Sandesha2Constants.SequenceProperties.SEQUENCE_CLOSED);
		sequenceClosedBean.setValue(Sandesha2Constants.VALUE_TRUE);

		sequencePropMgr.insert(sequenceClosedBean);

		RMMsgContext ackRMMsgCtx = AcknowledgementManager.generateAckMessage(rmMsgCtx, sequenceID, storageManager);

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
		SequenceAcknowledgement sequenceAcknowledgement = (SequenceAcknowledgement) ackRMMsgCtx
				.getMessagePart(Sandesha2Constants.MessageParts.SEQ_ACKNOWLEDGEMENT);

		MessageContext closeSequenceMsg = rmMsgCtx.getMessageContext();

		MessageContext closeSequenceResponseMsg = null;

		try {
			closeSequenceResponseMsg = Utils.createOutMessageContext(closeSequenceMsg);
		} catch (AxisFault e1) {
			throw new SandeshaException(e1);
		}

		RMMsgContext closeSeqResponseRMMsg = RMMsgCreator.createCloseSeqResponseMsg(rmMsgCtx, closeSequenceResponseMsg,
				storageManager);

		closeSeqResponseRMMsg.setMessagePart(Sandesha2Constants.MessageParts.SEQ_ACKNOWLEDGEMENT,
				sequenceAcknowledgement);

		closeSeqResponseRMMsg.setFlow(MessageContext.OUT_FLOW);
		closeSeqResponseRMMsg.setProperty(Sandesha2Constants.APPLICATION_PROCESSING_DONE, "true");

		closeSequenceResponseMsg.setResponseWritten(true);

		closeSeqResponseRMMsg.addSOAPEnvelope();

		AxisEngine engine = new AxisEngine(closeSequenceMsg.getConfigurationContext());

		try {
			engine.send(closeSequenceResponseMsg);
		} catch (AxisFault e) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.couldNotSendTerminateResponse,
					sequenceID, e.toString());
			throw new SandeshaException(message, e);
		}

		if (log.isDebugEnabled())
			log.debug("Exit: CloseSequenceProcessor::processInMessage");
	}

	public void processOutMessage(RMMsgContext rmMsgCtx) throws SandeshaException {
		if (log.isDebugEnabled()) {
			log.debug("Enter: CloseSequenceProcessor::processOutMessage");
			log.debug("Exit: CloseSequenceProcessor::processOutMessage");
		}

	}

}
