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

package org.apache.sandesha2.util;

import java.util.Iterator;

import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
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
		return rmMsgCtx;
	}

	/**
	 * Adds the message parts the the RMMessageContext.
	 * 
	 * @param msgCtx
	 * @param rmMsgContext
	 */
	private static void populateRMMsgContext(MessageContext msgCtx, RMMsgContext rmMsgContext) throws AxisFault {

	
		rmMsgContext.fromSOAPEnvelope(msgCtx.getEnvelope(), msgCtx.getWSAAction());

		String sequenceID = null;

		CreateSequence createSequence = rmMsgContext.getCreateSequence();
		CreateSequenceResponse createSequenceResponse = rmMsgContext.getCreateSequenceResponse();
		TerminateSequence terminateSequence = rmMsgContext.getTerminateSequence();
		TerminateSequenceResponse terminateSequenceResponse = rmMsgContext.getTerminateSequenceResponse();
		Iterator<SequenceAcknowledgement> sequenceAcknowledgementsIter = rmMsgContext.getSequenceAcknowledgements();
		Sequence sequence = rmMsgContext.getSequence();
		Iterator<AckRequested> ackRequestedIter = rmMsgContext.getAckRequests();
		CloseSequence closeSequence = rmMsgContext.getCloseSequence();
		CloseSequenceResponse closeSequenceResponse = rmMsgContext.getCloseSequenceResponse();
		MakeConnection makeConnection = rmMsgContext.getMakeConnection();
		
		String rmNamespace = null;

		if (createSequence != null) {
			rmNamespace = createSequence.getNamespaceValue();
		}

		if (createSequenceResponse != null) {
			rmNamespace = createSequenceResponse.getNamespaceValue();
		}

		if (sequence != null) {
			rmNamespace = sequence.getNamespaceValue();
		}

		//In case of ack messages RM Namespace is decided based on the sequenceId of the last 
		//sequence Ack. In other words Sandesha2 does not expect to receive two SequenceAcknowledgements
		//of different RM specifications in the same incoming message
		while(sequenceAcknowledgementsIter.hasNext()){
			SequenceAcknowledgement sequenceAck = (SequenceAcknowledgement) sequenceAcknowledgementsIter.next();
			rmNamespace = sequenceAck.getNamespaceValue();
		}

		if (terminateSequence != null) {
			rmNamespace = terminateSequence.getNamespaceValue();
		}

		if (terminateSequenceResponse != null) {
			rmNamespace = terminateSequenceResponse.getNamespaceValue();
		}

		//In case of ack request messages RM Namespace is decided based on the sequenceId of the last 
		//ack request.
		while(ackRequestedIter.hasNext()){
			AckRequested ackRequest = (AckRequested) ackRequestedIter.next();
			rmNamespace = ackRequest.getNamespaceValue();
		}

		if (closeSequence != null) {
			rmNamespace = closeSequence.getNamespaceValue();
		}

		if (closeSequenceResponse != null) {
			rmNamespace = closeSequenceResponse.getNamespaceValue();
		}
		
		if (makeConnection != null) {
			if (Sandesha2Constants.SPEC_2007_02.MC_NS_URI.equals(makeConnection.getNamespaceValue()))
				rmNamespace = Sandesha2Constants.SPEC_2007_02.NS_URI;
		}
		
		if (rmMsgContext.getMessagePending() != null) {
			//MessagePending only supported in 1.1 namespace... no need to check the namespace value
			rmNamespace = Sandesha2Constants.SPEC_2007_02.NS_URI;
		}
		if (rmNamespace!=null)
			rmMsgContext.setRMNamespaceValue(rmNamespace);
			
		sequenceAcknowledgementsIter = rmMsgContext.getSequenceAcknowledgements();
		ackRequestedIter = rmMsgContext.getAckRequests();
		
		// Setting message type.
		if (createSequence != null) {
			rmMsgContext.setMessageType(Sandesha2Constants.MessageTypes.CREATE_SEQ);
		} else if (createSequenceResponse != null) {
			rmMsgContext.setMessageType(Sandesha2Constants.MessageTypes.CREATE_SEQ_RESPONSE);
			sequenceID = createSequenceResponse.getIdentifier().getIdentifier();
		} else if (terminateSequence != null) {
			rmMsgContext.setMessageType(Sandesha2Constants.MessageTypes.TERMINATE_SEQ);
			sequenceID = terminateSequence.getIdentifier().getIdentifier();
		} else if (terminateSequenceResponse != null) {
			rmMsgContext.setMessageType(Sandesha2Constants.MessageTypes.TERMINATE_SEQ_RESPONSE);
			sequenceID = terminateSequenceResponse.getIdentifier().getIdentifier();
		} else if (sequence != null) {
			
			Sequence seq = rmMsgContext.getSequence();
			boolean lastMessage = seq.getLastMessage();
			SOAPEnvelope envelope = rmMsgContext.getSOAPEnvelope();
			
			if (lastMessage && envelope.getBody().getFirstOMChild()==null) {
				//the message is an empty body last message
				rmMsgContext.setMessageType(Sandesha2Constants.MessageTypes.LAST_MESSAGE);
			}else
				rmMsgContext.setMessageType(Sandesha2Constants.MessageTypes.APPLICATION);
			
			sequenceID = sequence.getIdentifier().getIdentifier();
		} else if (makeConnection != null){
			rmMsgContext.setMessageType(Sandesha2Constants.MessageTypes.MAKE_CONNECTION_MSG);
			if (makeConnection.getIdentifier()!=null) {
				sequenceID = makeConnection.getIdentifier().getIdentifier();
			} else if (makeConnection.getAddress()!=null){
				//TODO get sequenceId based on the anonymous address.
			} 
		} else if (ackRequestedIter.hasNext()) {
			rmMsgContext.setMessageType(Sandesha2Constants.MessageTypes.ACK_REQUEST);
			AckRequested ackRequest = (AckRequested) ackRequestedIter.next();

			//if there is only on sequenceAck, sequenceId will be set. Otherwise it will not be.
			if (!ackRequestedIter.hasNext())
				sequenceID = ackRequest.getIdentifier().getIdentifier();
		} else if (closeSequence != null) {
			rmMsgContext.setMessageType(Sandesha2Constants.MessageTypes.CLOSE_SEQUENCE);
			sequenceID = closeSequence.getIdentifier().getIdentifier();
		} else if (closeSequenceResponse != null) {
			rmMsgContext.setMessageType(Sandesha2Constants.MessageTypes.CLOSE_SEQUENCE_RESPONSE);
			sequenceID = closeSequenceResponse.getIdentifier().getIdentifier(); 
			
			// As an ACK can be piggybacked on all the other message types - check for ACK last.
		} else if (sequenceAcknowledgementsIter.hasNext()) {
			rmMsgContext.setMessageType(Sandesha2Constants.MessageTypes.ACK);
			SequenceAcknowledgement sequenceAcknowledgement = (SequenceAcknowledgement) sequenceAcknowledgementsIter.next();
			
			//if there is only on sequenceAck, sequenceId will be set. Otherwise it will not be.
			if (!sequenceAcknowledgementsIter.hasNext())
				sequenceID = sequenceAcknowledgement.getIdentifier().getIdentifier();
		} else
			rmMsgContext.setMessageType(Sandesha2Constants.MessageTypes.UNKNOWN);
		
		if (sequenceID!=null)
			rmMsgContext.setProperty(Sandesha2Constants.MessageContextProperties.SEQUENCE_ID,sequenceID);

	}


}
