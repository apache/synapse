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
import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axis2.AxisFault;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.Sandesha2Constants.SPEC_2005_02;
import org.apache.sandesha2.Sandesha2Constants.SPEC_2007_02;
import org.apache.sandesha2.Sandesha2Constants.WSRM_COMMON;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;

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

		// Check for RM defined elements, using either spec version
		OMElement header = envelope.getHeader();
		SOAPBody body = envelope.getBody();

		if(header != null){
			processHeaders(envelope);
		}
		if(body != null){
			processBody(body);
		}
	}
	
	private void processBody(SOAPBody body) throws AxisFault{
		// The body messages
		OMElement firstBodyElement = body.getFirstElement();
		if(firstBodyElement != null) {
			QName firstBodyQName = firstBodyElement.getQName();
			String namespace = firstBodyQName.getNamespaceURI();
			String localName = firstBodyQName.getLocalPart();

			boolean isSPEC2007_02 = SPEC_2007_02.NS_URI.equals(namespace);
			boolean isSPEC2005_02 = false;
			if(!isSPEC2007_02){
				isSPEC2005_02 = SPEC_2005_02.NS_URI.equals(namespace);
			}

			if(isSPEC2005_02 || isSPEC2007_02){
				if(Sandesha2Constants.SPEC_2007_02.QNames.CreateSequence.equals(firstBodyQName)) {
					createSequence = new CreateSequence(namespace);
					createSequence.fromOMElement(body);
				} else if(Sandesha2Constants.SPEC_2005_02.QNames.CreateSequence.equals(firstBodyQName)) {
					createSequence = new CreateSequence(namespace);
					createSequence.fromOMElement(body);
				} else if(Sandesha2Constants.SPEC_2007_02.QNames.CreateSequenceResponse.equals(firstBodyQName)) {
					createSequenceResponse = new CreateSequenceResponse(namespace);
					createSequenceResponse.fromOMElement(body);
				} else if(Sandesha2Constants.SPEC_2005_02.QNames.CreateSequenceResponse.equals(firstBodyQName)) {
					createSequenceResponse = new CreateSequenceResponse(namespace);
					createSequenceResponse.fromOMElement(body);
				} else if(Sandesha2Constants.SPEC_2007_02.QNames.CloseSequence.equals(firstBodyQName)) {
					closeSequence = new CloseSequence(namespace);
					closeSequence.fromOMElement(body);
				} else if(Sandesha2Constants.SPEC_2005_02.QNames.CloseSequence.equals(firstBodyQName)) {
					closeSequence = new CloseSequence(namespace);
					closeSequence.fromOMElement(body);
				} else if(Sandesha2Constants.SPEC_2007_02.QNames.CloseSequenceResponse.equals(firstBodyQName)) {
					closeSequenceResponse = new CloseSequenceResponse(namespace);
					closeSequenceResponse.fromOMElement(body);
				} else if(Sandesha2Constants.SPEC_2005_02.QNames.CloseSequenceResponse.equals(firstBodyQName)) {
					closeSequenceResponse = new CloseSequenceResponse(namespace);
					closeSequenceResponse.fromOMElement(body);
				} else if(Sandesha2Constants.SPEC_2007_02.QNames.TerminateSequence.equals(firstBodyQName)) {
					terminateSequence = new TerminateSequence(namespace);
					terminateSequence.fromOMElement(body);
				} else if(Sandesha2Constants.SPEC_2005_02.QNames.TerminateSequence.equals(firstBodyQName)) {
					terminateSequence = new TerminateSequence(namespace);
					terminateSequence.fromOMElement(body);
				} else if(Sandesha2Constants.SPEC_2007_02.QNames.TerminateSequenceResponse.equals(firstBodyQName)) {
					terminateSequenceResponse = new TerminateSequenceResponse(namespace);
					terminateSequenceResponse.fromOMElement(body);
				} else if(Sandesha2Constants.SPEC_2005_02.QNames.TerminateSequenceResponse.equals(firstBodyQName)) {
					terminateSequenceResponse = new TerminateSequenceResponse(namespace);
					terminateSequenceResponse.fromOMElement(body);
				}
			}else if(Sandesha2Constants.SPEC_2007_02.QNames.MakeConnection.equals(firstBodyQName)) {
				makeConnection = new MakeConnection(namespace);
				makeConnection.fromOMElement(firstBodyElement);
			}
		}
	}
	
	private void processHeaders(SOAPEnvelope envelope) throws AxisFault {

		if (envelope == null)
			throw new OMException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.nullPassedElement));

		SOAPFactory factory = (SOAPFactory)envelope.getOMFactory();
		OMElement header = envelope.getHeader();

		if(header!=null)
		{
			Iterator headers = header.getChildElements();
			while(headers.hasNext()){
				OMElement element = (OMElement)headers.next();
				QName elementName = element.getQName();
				String namespace = elementName.getNamespaceURI();
				String localName = elementName.getLocalPart();
				
				boolean isSPEC2007_02 = SPEC_2007_02.NS_URI.equals(namespace);
				boolean isSPEC2005_02 = false;
				if(!isSPEC2007_02){
					isSPEC2005_02 = SPEC_2005_02.NS_URI.equals(namespace);
				}
				
				if(isSPEC2005_02 || isSPEC2007_02){
					boolean isProcessed = false;
					if(isSPEC2007_02){
						if(WSRM_COMMON.USES_SEQUENCE_STR.equals(localName)){
							usesSequenceSTR = new UsesSequenceSTR(factory, namespace);
							usesSequenceSTR.fromOMElement(element);
							isProcessed = true;
						}else if(WSRM_COMMON.MESSAGE_PENDING.equals(localName)){
							messagePending = new MessagePending(namespace);
							messagePending.fromOMElement(element);
							isProcessed = true;
						}
					}
					
					if(!isProcessed){
						if(WSRM_COMMON.SEQUENCE.equals(localName)){
							sequence = new Sequence(namespace);
							sequence.fromOMElement(element);
						}else if(WSRM_COMMON.SEQUENCE_ACK.equals(localName)){
							SequenceAcknowledgement sequenceAcknowledgement = new SequenceAcknowledgement(namespace);
							sequenceAcknowledgement.fromOMElement(element);
							sequenceAcknowledgements.add(sequenceAcknowledgement);
						}else if(WSRM_COMMON.ACK_REQUESTED.equals(localName)){
							AckRequested ackRequest = new AckRequested(namespace);
							ackRequest.fromOMElement(element);
							ackRequests.add(ackRequest);
						}else if(WSRM_COMMON.SEQUENCE_FAULT.equals(localName)){
							sequenceFault = new SequenceFault(namespace);
							sequenceFault.fromOMElement(element);
						}	
					}
				}
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
