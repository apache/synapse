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
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
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
	private SequenceFault sequenceFault = null;
	
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
		
		// Check for RM defined elements, using either spec version
		OMElement header = envelope.getHeader();
		OMElement body = envelope.getBody();

		// The sequence header
		OMElement element = null;
		if(header!=null)
		{
			element = header.getFirstChildWithName(Sandesha2Constants.SPEC_2007_02.QNames.Sequence);
			if(element == null) {
				element = envelope.getHeader().getFirstChildWithName(Sandesha2Constants.SPEC_2005_02.QNames.Sequence);
			}
			if (element != null) {
				sequence = new Sequence(element.getNamespace().getNamespaceURI());
				sequence.fromOMElement(envelope.getHeader());
			}			
		}


		// The body messages
		OMElement firstBodyElement = body.getFirstElement();
		if(firstBodyElement != null) {
			QName firstBodyQName = firstBodyElement.getQName();
			
			if(Sandesha2Constants.SPEC_2007_02.QNames.CreateSequence.equals(firstBodyQName)) {
				createSequence = new CreateSequence(firstBodyQName.getNamespaceURI());
				createSequence.fromOMElement(body);
			} else if(Sandesha2Constants.SPEC_2005_02.QNames.CreateSequence.equals(firstBodyQName)) {
				createSequence = new CreateSequence(firstBodyQName.getNamespaceURI());
				createSequence.fromOMElement(body);

			} else if(Sandesha2Constants.SPEC_2007_02.QNames.CreateSequenceResponse.equals(firstBodyQName)) {
				createSequenceResponse = new CreateSequenceResponse(firstBodyQName.getNamespaceURI());
				createSequenceResponse.fromOMElement(body);
			} else if(Sandesha2Constants.SPEC_2005_02.QNames.CreateSequenceResponse.equals(firstBodyQName)) {
				createSequenceResponse = new CreateSequenceResponse(firstBodyQName.getNamespaceURI());
				createSequenceResponse.fromOMElement(body);

			} else if(Sandesha2Constants.SPEC_2007_02.QNames.CloseSequence.equals(firstBodyQName)) {
				closeSequence = new CloseSequence(firstBodyQName.getNamespaceURI());
				closeSequence.fromOMElement(body);
			} else if(Sandesha2Constants.SPEC_2005_02.QNames.CloseSequence.equals(firstBodyQName)) {
				closeSequence = new CloseSequence(firstBodyQName.getNamespaceURI());
				closeSequence.fromOMElement(body);

			} else if(Sandesha2Constants.SPEC_2007_02.QNames.CloseSequenceResponse.equals(firstBodyQName)) {
				closeSequenceResponse = new CloseSequenceResponse(firstBodyQName.getNamespaceURI());
				closeSequenceResponse.fromOMElement(body);
			} else if(Sandesha2Constants.SPEC_2005_02.QNames.CloseSequenceResponse.equals(firstBodyQName)) {
				closeSequenceResponse = new CloseSequenceResponse(firstBodyQName.getNamespaceURI());
				closeSequenceResponse.fromOMElement(body);

			} else if(Sandesha2Constants.SPEC_2007_02.QNames.TerminateSequence.equals(firstBodyQName)) {
				terminateSequence = new TerminateSequence(firstBodyQName.getNamespaceURI());
				terminateSequence.fromOMElement(body);
			} else if(Sandesha2Constants.SPEC_2005_02.QNames.TerminateSequence.equals(firstBodyQName)) {
				terminateSequence = new TerminateSequence(firstBodyQName.getNamespaceURI());
				terminateSequence.fromOMElement(body);

			} else if(Sandesha2Constants.SPEC_2007_02.QNames.TerminateSequenceResponse.equals(firstBodyQName)) {
				terminateSequenceResponse = new TerminateSequenceResponse(firstBodyQName.getNamespaceURI());
				terminateSequenceResponse.fromOMElement(body);
			} else if(Sandesha2Constants.SPEC_2005_02.QNames.TerminateSequenceResponse.equals(firstBodyQName)) {
				terminateSequenceResponse = new TerminateSequenceResponse(firstBodyQName.getNamespaceURI());
				terminateSequenceResponse.fromOMElement(body);

			} else if(Sandesha2Constants.SPEC_2007_02.QNames.MakeConnection.equals(firstBodyQName)) {
				makeConnection = new MakeConnection(firstBodyQName.getNamespaceURI());
				makeConnection.fromOMElement(firstBodyElement);
			}
		}
		
		// The other headers
		if(header!=null)
		{
			Iterator headers = header.getChildrenWithName(Sandesha2Constants.SPEC_2007_02.QNames.SequenceAck);
			while (headers.hasNext()) {
				OMElement sequenceAckElement = (OMElement) headers.next();
				SequenceAcknowledgement sequenceAcknowledgement = new SequenceAcknowledgement(Sandesha2Constants.SPEC_2007_02.NS_URI);
				sequenceAcknowledgement.fromOMElement(sequenceAckElement);
				sequenceAcknowledgements.add(sequenceAcknowledgement);
			}
			headers = header.getChildrenWithName(Sandesha2Constants.SPEC_2005_02.QNames.SequenceAck);
			while (headers.hasNext()) {
				OMElement sequenceAckElement = (OMElement) headers.next();
				SequenceAcknowledgement sequenceAcknowledgement = new SequenceAcknowledgement(Sandesha2Constants.SPEC_2005_02.NS_URI);
				sequenceAcknowledgement.fromOMElement(sequenceAckElement);
				sequenceAcknowledgements.add(sequenceAcknowledgement);
			}

			headers = header.getChildrenWithName(Sandesha2Constants.SPEC_2007_02.QNames.AckRequest);
			while (headers.hasNext()) {
				OMElement ackRequestElement = (OMElement) headers.next();
				AckRequested ackRequest = new AckRequested(Sandesha2Constants.SPEC_2007_02.NS_URI);
				ackRequest.fromOMElement(ackRequestElement);
				ackRequests.add(ackRequest);
			}
			headers = header.getChildrenWithName(Sandesha2Constants.SPEC_2005_02.QNames.AckRequest);
			while (headers.hasNext()) {
				OMElement ackRequestElement = (OMElement) headers.next();
				AckRequested ackRequest = new AckRequested(Sandesha2Constants.SPEC_2005_02.NS_URI);
				ackRequest.fromOMElement(ackRequestElement);
				ackRequests.add(ackRequest);
			}
			
			element = header.getFirstChildWithName(Sandesha2Constants.SPEC_2007_02.QNames.UsesSequenceSTR);
			if (element != null) {
				usesSequenceSTR = new UsesSequenceSTR(factory, Sandesha2Constants.SPEC_2007_02.NS_URI);
				usesSequenceSTR.fromOMElement(element);
			}
			
			element = header.getFirstChildWithName(Sandesha2Constants.SPEC_2007_02.QNames.MessagePending);
			if (element != null) {
				messagePending = new MessagePending(Sandesha2Constants.SPEC_2007_02.MC_NS_URI);
				messagePending.fromOMElement(element);
			}
			
			element = header.getFirstChildWithName(Sandesha2Constants.SPEC_2007_02.QNames.SequenceFault);
			if(element == null) {
				element = header.getFirstChildWithName(Sandesha2Constants.SPEC_2005_02.QNames.SequenceFault);
			}
			if (element !=null) {
				sequenceFault = new SequenceFault(element.getNamespace().getNamespaceURI());
				sequenceFault.fromOMElement(element);
			}
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
	
	public SequenceFault getSequenceFault() {
		return sequenceFault;
	}
}
