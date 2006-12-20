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

package org.apache.sandesha2.wsrm;

import java.util.ArrayList;
import java.util.Iterator;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMException;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axis2.AxisFault;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.util.SOAPAbstractFactory;

/**
 * All RM model classes should implement this.
 * RM model classes include all the class that represent the elements 
 * defined by RM specification. 
 */

public class RMElements {

	private Sequence sequence = null;
	
	//there can be more than one sequence ack or ack request in a single message.
	private ArrayList sequenceAcknowledgements = null;
	private ArrayList ackRequests = null;
	
	private CreateSequence createSequence = null;
	private CreateSequenceResponse createSequenceResponse = null;
	private TerminateSequence terminateSequence = null;
	private TerminateSequenceResponse terminateSequenceResponse = null;
	private CloseSequence closeSequence = null;
	private CloseSequenceResponse closeSequenceResponse = null;
	private UsesSequenceSTR usesSequenceSTR = null;
	private MessagePending messagePending = null;
	private MakeConnection makeConnection = null;
	private String rmNamespaceValue = null;
	
	public RMElements () {
		sequenceAcknowledgements = new ArrayList ();
		ackRequests = new ArrayList();
	}
	
	public RMElements (String addressingNamespace) {
		this ();
	}
	
	public void fromSOAPEnvelope(SOAPEnvelope envelope, String action) throws AxisFault {

		if (envelope == null)
			throw new OMException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.nullPassedElement));

		SOAPFactory factory;

		//Yep, I know. Could hv done it directly :D (just to make it consistent)
		if (envelope.getNamespace().getNamespaceURI().equals(SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI))
			factory = SOAPAbstractFactory.getSOAPFactory(Sandesha2Constants.SOAPVersion.v1_1);
		else
			factory = SOAPAbstractFactory.getSOAPFactory(Sandesha2Constants.SOAPVersion.v1_2);
			
		
		//finding out the rm version.
		rmNamespaceValue = getRMNamespaceValue (envelope,action);
		if (rmNamespaceValue==null) {
			return;
		}
		
		OMElement sequenceElement = envelope.getHeader().getFirstChildWithName(
				new QName(rmNamespaceValue, Sandesha2Constants.WSRM_COMMON.SEQUENCE));
		if (sequenceElement != null) {
			sequence = new Sequence(rmNamespaceValue);
			sequence.fromOMElement(envelope.getHeader());
		}

		OMElement createSeqElement = envelope.getBody().getFirstChildWithName(
				new QName(rmNamespaceValue,
						Sandesha2Constants.WSRM_COMMON.CREATE_SEQUENCE));
		
		if (createSeqElement != null) {
			createSequence = new CreateSequence(rmNamespaceValue);
			createSequence.fromOMElement(envelope.getBody());
		}

		OMElement createSeqResElement = envelope.getBody()
				.getFirstChildWithName(
						new QName(rmNamespaceValue,
								Sandesha2Constants.WSRM_COMMON.CREATE_SEQUENCE_RESPONSE));
		if (createSeqResElement != null) {
			createSequenceResponse = new CreateSequenceResponse(rmNamespaceValue);
			createSequenceResponse.fromOMElement(envelope.getBody());
		}

		OMElement terminateSeqElement = envelope.getBody()
				.getFirstChildWithName(
						new QName(rmNamespaceValue,
								Sandesha2Constants.WSRM_COMMON.TERMINATE_SEQUENCE));
		if (terminateSeqElement != null) {
			terminateSequence = new TerminateSequence(rmNamespaceValue);
			terminateSequence.fromOMElement(envelope.getBody());
		}
		
		OMElement terminateSeqResponseElement = envelope.getBody()
				.getFirstChildWithName(
						new QName(rmNamespaceValue,
								Sandesha2Constants.WSRM_COMMON.TERMINATE_SEQUENCE_RESPONSE));
		if (terminateSeqResponseElement != null) {
				terminateSequenceResponse = new TerminateSequenceResponse (rmNamespaceValue);
				terminateSequenceResponse.fromOMElement(envelope.getBody());
		}

		OMElement closeSequenceElement = envelope.getBody()
			.getFirstChildWithName(
				new QName(rmNamespaceValue,
						Sandesha2Constants.WSRM_COMMON.CLOSE_SEQUENCE));
		if (closeSequenceElement != null) {
			closeSequence = new CloseSequence (rmNamespaceValue);
			closeSequence.fromOMElement(envelope.getBody());
		}

		OMElement closeSequenceResponseElement = envelope.getBody()
			.getFirstChildWithName(
					new QName(rmNamespaceValue,
							Sandesha2Constants.WSRM_COMMON.CLOSE_SEQUENCE_RESPONSE));
		if (closeSequenceResponseElement != null) {
			closeSequenceResponse = new CloseSequenceResponse  (rmNamespaceValue);
			closeSequenceResponse.fromOMElement(envelope.getBody());
		}
		
		Iterator sequenceAcknowledgementIter = envelope.getHeader()
				.getChildrenWithName (new QName(rmNamespaceValue,
						Sandesha2Constants.WSRM_COMMON.SEQUENCE_ACK));
		while (sequenceAcknowledgementIter.hasNext()) {
			OMElement sequenceAckElement = (OMElement) sequenceAcknowledgementIter.next();
			SequenceAcknowledgement sequenceAcknowledgement = new SequenceAcknowledgement  (rmNamespaceValue);
			sequenceAcknowledgement.fromOMElement(sequenceAckElement);
			
			sequenceAcknowledgements.add(sequenceAcknowledgement);
		}

		Iterator ackRequestIter = envelope.getHeader()
				.getChildrenWithName (new QName(rmNamespaceValue,
						Sandesha2Constants.WSRM_COMMON.ACK_REQUESTED));
		while (ackRequestIter.hasNext()) {
			OMElement ackRequestElement = (OMElement) ackRequestIter.next();
			AckRequested ackRequest = new AckRequested(rmNamespaceValue);
			ackRequest.fromOMElement(ackRequestElement);
			
			ackRequests.add(ackRequest);
		}

		OMElement usesSequenceSTRElement = envelope.getHeader()
		.getFirstChildWithName(
				new QName(rmNamespaceValue,
						Sandesha2Constants.WSRM_COMMON.USES_SEQUENCE_STR));
		if (usesSequenceSTRElement != null) {
			usesSequenceSTR = new UsesSequenceSTR(factory, rmNamespaceValue);
			usesSequenceSTR.fromOMElement(envelope.getHeader());
		}
		
		OMElement makeConnectionElement = envelope.getBody().getFirstChildWithName(
				new QName (rmNamespaceValue,
						Sandesha2Constants.WSRM_COMMON.MAKE_CONNECTION));
		if (makeConnectionElement!=null) {
			makeConnection = new MakeConnection (rmNamespaceValue);
			makeConnection.fromOMElement(makeConnectionElement);
		}
		
		OMElement messagePendingElement = envelope.getHeader().getFirstChildWithName(
				new QName (rmNamespaceValue,
						Sandesha2Constants.WSRM_COMMON.MESSAGE_PENDING));
		if (messagePendingElement!=null) {
			messagePending = new MessagePending (rmNamespaceValue);
			messagePending.fromOMElement(messagePendingElement);
		}
	}

	public SOAPEnvelope toSOAPEnvelope(SOAPEnvelope envelope) throws AxisFault  {
		if (sequence != null) {
			sequence.toOMElement(envelope.getHeader());
		}
		for (Iterator iter=sequenceAcknowledgements.iterator();iter.hasNext();) {
			SequenceAcknowledgement sequenceAck = (SequenceAcknowledgement) iter.next();
			sequenceAck.toOMElement(envelope.getHeader());
		}
		for (Iterator iter=ackRequests.iterator();iter.hasNext();) {
			AckRequested ackReq = (AckRequested) iter.next();
			ackReq.toOMElement(envelope.getHeader());
		}
		if (createSequence != null) {
			createSequence.toOMElement(envelope.getBody());
		}
		if (createSequenceResponse != null) {
			createSequenceResponse.toOMElement(envelope.getBody());
		}
		if (terminateSequence != null) {
			terminateSequence.toOMElement(envelope.getBody());
		}
		if (terminateSequenceResponse != null) {
			terminateSequenceResponse.toOMElement(envelope.getBody());
		}
		
		if (closeSequence != null) {
			closeSequence.toOMElement(envelope.getBody());
		}
		
		if (closeSequenceResponse != null) {
			closeSequenceResponse.toOMElement(envelope.getBody());
		}
		
		if (makeConnection!=null) {
			makeConnection.toOMElement(envelope.getBody());
		}
		
		if (messagePending!=null) {
			messagePending.toOMElement(envelope.getHeader());
		}
		
		return envelope;
	}

	public CreateSequence getCreateSequence() {
		return createSequence;
	}

	public CreateSequenceResponse getCreateSequenceResponse() {
		return createSequenceResponse;
	}

	public Sequence getSequence() {
		return sequence;
	}

	public Iterator getSequenceAcknowledgements() {
		return sequenceAcknowledgements.iterator();
	}

	public TerminateSequence getTerminateSequence() {
		return terminateSequence;
	}
	
	public TerminateSequenceResponse getTerminateSequenceResponse() {
		return terminateSequenceResponse;
	}

	public void setCreateSequence(CreateSequence createSequence) {
		this.createSequence = createSequence;
	}

	public void setCreateSequenceResponse(
			CreateSequenceResponse createSequenceResponse) {
		this.createSequenceResponse = createSequenceResponse;
	}

	public void setSequence(Sequence sequence) {
		this.sequence = sequence;
	}

	public void setSequenceAcknowledgements(
			ArrayList sequenceAcknowledgements) {
		this.sequenceAcknowledgements = sequenceAcknowledgements;
	}
	
	public void addSequenceAcknowledgement (SequenceAcknowledgement sequenceAcknowledgement) {
		sequenceAcknowledgements.add(sequenceAcknowledgement);
	}

	public void setTerminateSequence(TerminateSequence terminateSequence) {
		this.terminateSequence = terminateSequence;
	}
	
	public void setTerminateSequenceResponse(TerminateSequenceResponse terminateSequenceResponse) {
		this.terminateSequenceResponse = terminateSequenceResponse;
	}

	public Iterator getAckRequests() {
		return ackRequests.iterator();
	}

	public void setAckRequested(ArrayList ackRequests) {
		this.ackRequests = ackRequests;
	}
	
	public void addAckRequested(AckRequested ackRequested) {
		ackRequests.add(ackRequested);
	}
	
	public void setMakeConnection(MakeConnection makeConnection) {
		this.makeConnection = makeConnection;
	}
	
	public void setMessagePending(MessagePending messagePending) {
		this.messagePending = messagePending;
	}
	
	private String getRMNamespaceValue (SOAPEnvelope envelope, String action) {
		
		SOAPHeader header = envelope.getHeader();
		if (header!=null) {
			ArrayList headers = header.getHeaderBlocksWithNSURI(Sandesha2Constants.SPEC_2005_02.NS_URI);
			if (headers!=null && headers.size()>0)
				return Sandesha2Constants.SPEC_2005_02.NS_URI;
			
			headers = header.getHeaderBlocksWithNSURI(Sandesha2Constants.SPEC_2006_08.NS_URI);
			if (headers!=null && headers.size()>0)
				return Sandesha2Constants.SPEC_2006_08.NS_URI;
		}
		
		//rm control messages with parts in the body will be identified by the wsa:action,
		//except when ws-addressing headers are turned off
		if (action!=null) {
			if(action.startsWith(Sandesha2Constants.SPEC_2005_02.NS_URI)) {
				return Sandesha2Constants.SPEC_2005_02.NS_URI;
			}
			
			if (action.startsWith(Sandesha2Constants.SPEC_2006_08.NS_URI))
				return Sandesha2Constants.SPEC_2006_08.NS_URI;
		}
		
		// As a final resort check the body namespace
		SOAPBody body = envelope.getBody();
		if(body != null) {
			Iterator elements = body.getChildElements();
			if(elements.hasNext()) {
				OMElement firstBodyElement = (OMElement) elements.next();
				String namespace = firstBodyElement.getNamespace().getNamespaceURI();
				if(namespace.equals(Sandesha2Constants.SPEC_2005_02.NS_URI) ||
				   namespace.equals(Sandesha2Constants.SPEC_2006_08.NS_URI)  ) {
					return namespace;
				}
			}
		}
		
		return null;   //a version could not be found
	}
	
	public CloseSequence getCloseSequence() {
		return closeSequence;
	}

	public void setCloseSequence(CloseSequence closeSequence) {
		this.closeSequence = closeSequence;
	}

	public CloseSequenceResponse getCloseSequenceResponse() {
		return closeSequenceResponse;
	}

	public void setCloseSequenceResponse(CloseSequenceResponse closeSequenceResponse) {
		this.closeSequenceResponse = closeSequenceResponse;
	}
	
	public UsesSequenceSTR getUsesSequenceSTR() {
		return usesSequenceSTR;
	}
	
	public void setUsesSequenceSTR(UsesSequenceSTR header) {
		usesSequenceSTR = header;
	}

	public MakeConnection getMakeConnection() {
		return makeConnection;
	}

	public MessagePending getMessagePending() {
		return messagePending;
	}
}
