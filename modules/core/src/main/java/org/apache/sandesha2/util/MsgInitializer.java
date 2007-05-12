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

import java.util.Iterator;

import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.context.MessageContext;
import org.apache.sandesha2.RMMsgContext;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.wsrm.AckRequested;
import org.apache.sandesha2.wsrm.CloseSequence;
import org.apache.sandesha2.wsrm.CloseSequenceResponse;
import org.apache.sandesha2.wsrm.CreateSequence;
import org.apache.sandesha2.wsrm.CreateSequenceResponse;
import org.apache.sandesha2.wsrm.MakeConnection;
import org.apache.sandesha2.wsrm.RMElements;
import org.apache.sandesha2.wsrm.Sequence;
import org.apache.sandesha2.wsrm.SequenceAcknowledgement;
import org.apache.sandesha2.wsrm.TerminateSequence;
import org.apache.sandesha2.wsrm.TerminateSequenceResponse;

/**
 * This class is used to create an RMMessageContext out of an MessageContext.
 */

public class MsgInitializer {

	/**
	 * Called to create a rmMessageContext out of an message context. Finds out
	 * things like rm version and message type as well.
	 * 
	 * @param ctx
	 * @param assumedRMNamespace
	 *            this is used for validation (to find out weather the
	 *            rmNamespace of the current message is equal to the regietered
	 *            rmNamespace of the sequence). If null validation will not
	 *            happen.
	 * 
	 * @return
	 * @throws SandeshaException
	 */
	public static RMMsgContext initializeMessage(MessageContext ctx) throws AxisFault {
		RMMsgContext rmMsgCtx = new RMMsgContext(ctx);

		populateRMMsgContext(ctx, rmMsgCtx);
		validateMessage(rmMsgCtx);
		return rmMsgCtx;
	}

	/**
	 * Adds the message parts the the RMMessageContext.
	 * 
	 * @param msgCtx
	 * @param rmMsgContext
	 */
	private static void populateRMMsgContext(MessageContext msgCtx, RMMsgContext rmMsgContext) throws AxisFault {

		// if client side and the addressing version is not set. assuming the
		// default addressing version
		String addressingNamespace = (String) msgCtx.getProperty(AddressingConstants.WS_ADDRESSING_VERSION);
		if (addressingNamespace == null && !msgCtx.isServerSide())
			addressingNamespace = AddressingConstants.Final.WSA_NAMESPACE;

		RMElements elements = new RMElements(addressingNamespace);
		elements.fromSOAPEnvelope(msgCtx.getEnvelope(), msgCtx.getWSAAction());

		String rmNamespace = null;

		if (elements.getCreateSequence() != null) {
			rmMsgContext.setMessagePart(Sandesha2Constants.MessageParts.CREATE_SEQ, elements.getCreateSequence());
			rmNamespace = elements.getCreateSequence().getNamespaceValue();
		}

		if (elements.getCreateSequenceResponse() != null) {
			rmMsgContext.setMessagePart(Sandesha2Constants.MessageParts.CREATE_SEQ_RESPONSE, elements
					.getCreateSequenceResponse());
			rmNamespace = elements.getCreateSequenceResponse().getNamespaceValue();
		}

		if (elements.getSequence() != null) {
			rmMsgContext.setMessagePart(Sandesha2Constants.MessageParts.SEQUENCE, elements.getSequence());
			rmNamespace = elements.getSequence().getNamespaceValue();
		}

		//In case of ack messages RM Namespace is decided based on the sequenceId of the last 
		//sequence Ack. In other words Sandesha2 does not expect to receive two SequenceAcknowledgements
		//of different RM specifications in the same incoming message
		for (Iterator iter = elements.getSequenceAcknowledgements();iter.hasNext();) {
			SequenceAcknowledgement sequenceAck = (SequenceAcknowledgement) iter.next();
			rmMsgContext.setMessagePart(Sandesha2Constants.MessageParts.SEQ_ACKNOWLEDGEMENT, sequenceAck);
			rmNamespace = sequenceAck.getNamespaceValue();
		}

		if (elements.getTerminateSequence() != null) {
			rmMsgContext.setMessagePart(Sandesha2Constants.MessageParts.TERMINATE_SEQ, elements.getTerminateSequence());
			rmNamespace = elements.getTerminateSequence().getNamespaceValue();
		}

		if (elements.getTerminateSequenceResponse() != null) {
			rmMsgContext.setMessagePart(Sandesha2Constants.MessageParts.TERMINATE_SEQ_RESPONSE, elements
					.getTerminateSequenceResponse());
			rmNamespace = elements.getTerminateSequenceResponse().getNamespaceValue();
		}

		//In case of ack request messages RM Namespace is decided based on the sequenceId of the last 
		//ack request.
		for (Iterator iter = elements.getAckRequests();iter.hasNext();) {
			AckRequested ackRequest = (AckRequested) iter.next();
			rmMsgContext.setMessagePart(Sandesha2Constants.MessageParts.ACK_REQUEST, ackRequest);
			rmNamespace = ackRequest.getNamespaceValue();
		}

		if (elements.getCloseSequence() != null) {
			rmMsgContext.setMessagePart(Sandesha2Constants.MessageParts.CLOSE_SEQUENCE, elements.getCloseSequence());
			rmNamespace = elements.getCloseSequence().getNamespaceValue();
		}

		if (elements.getCloseSequenceResponse() != null) {
			rmMsgContext.setMessagePart(Sandesha2Constants.MessageParts.CLOSE_SEQUENCE_RESPONSE, elements
					.getCloseSequenceResponse());
			rmNamespace = elements.getCloseSequenceResponse().getNamespaceValue();
		}
		
		if (elements.getUsesSequenceSTR() != null) {
			rmMsgContext.setMessagePart(Sandesha2Constants.MessageParts.USES_SEQUENCE_STR, elements
					.getUsesSequenceSTR());
		}
		
		if (elements.getMakeConnection() != null) {
			rmMsgContext.setMessagePart(Sandesha2Constants.MessageParts.MAKE_CONNECTION,
					elements.getMakeConnection());
			rmNamespace = elements.getMakeConnection().getNamespaceValue();
		}
		
		if (elements.getMessagePending() != null) {
			rmMsgContext.setMessagePart(Sandesha2Constants.MessageParts.MESSAGE_PENDING,
					elements.getMessagePending());
			rmNamespace = elements.getMessagePending().getNamespaceValue();
		}
		
		if (elements.getSequenceFault() != null) {
			rmMsgContext.setMessagePart(Sandesha2Constants.MessageParts.SEQUENCE_FAULT,
					elements.getSequenceFault());
		}

		rmMsgContext.setRMNamespaceValue(rmNamespace);

	}

	/**
	 * This is used to validate the message. Also set an Message type. Possible
	 * types are given in the Sandesha2Constants.MessageTypes interface.
	 * 
	 * @param rmMsgCtx
	 * @return
	 * @throws SandeshaException
	 */
	private static boolean validateMessage(RMMsgContext rmMsgCtx) throws SandeshaException {

		String sequenceID = null;

		CreateSequence createSequence = (CreateSequence) rmMsgCtx.getMessagePart(
				Sandesha2Constants.MessageParts.CREATE_SEQ);
		CreateSequenceResponse createSequenceResponse = (CreateSequenceResponse) rmMsgCtx.getMessagePart(
				Sandesha2Constants.MessageParts.CREATE_SEQ_RESPONSE);
		TerminateSequence terminateSequence = (TerminateSequence) rmMsgCtx.getMessagePart(
				Sandesha2Constants.MessageParts.TERMINATE_SEQ);
		TerminateSequenceResponse terminateSequenceResponse = (TerminateSequenceResponse) rmMsgCtx.getMessagePart(
				Sandesha2Constants.MessageParts.TERMINATE_SEQ_RESPONSE);
		Iterator sequenceAcknowledgementsIter = rmMsgCtx.getMessageParts(
				Sandesha2Constants.MessageParts.SEQ_ACKNOWLEDGEMENT);
		Sequence sequence = (Sequence) rmMsgCtx.getMessagePart(
				Sandesha2Constants.MessageParts.SEQUENCE);
		Iterator ackRequestedIter = rmMsgCtx.getMessageParts(
				Sandesha2Constants.MessageParts.ACK_REQUEST);
		CloseSequence closeSequence = (CloseSequence) rmMsgCtx.getMessagePart(
				Sandesha2Constants.MessageParts.CLOSE_SEQUENCE);
		CloseSequenceResponse closeSequenceResponse = (CloseSequenceResponse) rmMsgCtx.getMessagePart(
				Sandesha2Constants.MessageParts.CLOSE_SEQUENCE_RESPONSE);
		MakeConnection makeConnection = (MakeConnection) rmMsgCtx.getMessagePart(
				Sandesha2Constants.MessageParts.MAKE_CONNECTION);

		// Setting message type.
		if (createSequence != null) {
			rmMsgCtx.setMessageType(Sandesha2Constants.MessageTypes.CREATE_SEQ);
		} else if (createSequenceResponse != null) {
			rmMsgCtx.setMessageType(Sandesha2Constants.MessageTypes.CREATE_SEQ_RESPONSE);
			sequenceID = createSequenceResponse.getIdentifier().getIdentifier();
		} else if (terminateSequence != null) {
			rmMsgCtx.setMessageType(Sandesha2Constants.MessageTypes.TERMINATE_SEQ);
			sequenceID = terminateSequence.getIdentifier().getIdentifier();
		} else if (terminateSequenceResponse != null) {
			rmMsgCtx.setMessageType(Sandesha2Constants.MessageTypes.TERMINATE_SEQ_RESPONSE);
			sequenceID = terminateSequenceResponse.getIdentifier().getIdentifier();
		} else if (rmMsgCtx.getMessagePart(Sandesha2Constants.MessageParts.SEQUENCE) != null) {
			rmMsgCtx.setMessageType(Sandesha2Constants.MessageTypes.APPLICATION);
			sequenceID = sequence.getIdentifier().getIdentifier();
		} else if (sequenceAcknowledgementsIter.hasNext()) {
			rmMsgCtx.setMessageType(Sandesha2Constants.MessageTypes.ACK);
			SequenceAcknowledgement sequenceAcknowledgement = (SequenceAcknowledgement) sequenceAcknowledgementsIter.next();
			
			//if there is only on sequenceAck, sequenceId will be set. Otherwise it will not be.
			if (!sequenceAcknowledgementsIter.hasNext())
				sequenceID = sequenceAcknowledgement.getIdentifier().getIdentifier();
		} else if (ackRequestedIter.hasNext()) {
			rmMsgCtx.setMessageType(Sandesha2Constants.MessageTypes.ACK_REQUEST);
			AckRequested ackRequest = (AckRequested) ackRequestedIter.next();

			//if there is only on sequenceAck, sequenceId will be set. Otherwise it will not be.
			if (!ackRequestedIter.hasNext())
				sequenceID = ackRequest.getIdentifier().getIdentifier();
		} else if (closeSequence != null) {
			rmMsgCtx.setMessageType(Sandesha2Constants.MessageTypes.CLOSE_SEQUENCE);
			sequenceID = closeSequence.getIdentifier().getIdentifier();
		} else if (closeSequenceResponse != null) {
			rmMsgCtx.setMessageType(Sandesha2Constants.MessageTypes.CLOSE_SEQUENCE_RESPONSE);
			sequenceID = closeSequenceResponse.getIdentifier().getIdentifier(); 
		} else if (makeConnection != null){
			rmMsgCtx.setMessageType(Sandesha2Constants.MessageTypes.MAKE_CONNECTION_MSG);
			if (makeConnection.getIdentifier()!=null) {
				sequenceID = makeConnection.getIdentifier().getIdentifier();
			} else if (makeConnection.getAddress()!=null){
				//TODO get sequenceId based on the anonymous address.
			} else {
				throw new SandeshaException (
						"Invalid MakeConnection message. Either Address or Identifier must be present");
			}
		} else
			rmMsgCtx.setMessageType(Sandesha2Constants.MessageTypes.UNKNOWN);
		
		if (sequenceID!=null)
			rmMsgCtx.setProperty(Sandesha2Constants.MessageContextProperties.SEQUENCE_ID,sequenceID);

		return true;
	}

}
