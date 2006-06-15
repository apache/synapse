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

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMException;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.util.SOAPAbstractFactory;

/**
 * @author Chamikara Jayalath <chamikaramj@gmail.com>
 * @author Sanka Samaranayaka <ssanka@gmail.com>
 * @author Saminda Abeyruwan  <saminda@opensource.lk>
 */

public class RMElements {

	private Sequence sequence = null;
	private SequenceAcknowledgement sequenceAcknowledgement = null;
	private CreateSequence createSequence = null;
	private CreateSequenceResponse createSequenceResponse = null;
	private TerminateSequence terminateSequence = null;
	private TerminateSequenceResponse terminateSequenceResponse = null;
	private CloseSequence closeSequence = null;
	private CloseSequenceResponse closeSequenceResponse = null;
	private AckRequested ackRequested = null;
	private SOAPFactory factory = null;
	private String rmNamespaceValue = null;
	private String addressingNamespaceValue = null;
	
	public RMElements () {
		
	}
	
	public RMElements (String addressingNamespace) {
		this.addressingNamespaceValue = addressingNamespace;
	}
	
	public void fromSOAPEnvelope(SOAPEnvelope envelope, String action) throws SandeshaException {

		if (envelope == null)
			throw new OMException("The passed envelope is null");

		SOAPFactory factory;

		//Yep, I know. Could hv done it directly :D (just to make it consistent)
		if (envelope.getNamespace().getName().equals(SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI))
			factory = SOAPAbstractFactory.getSOAPFactory(Sandesha2Constants.SOAPVersion.v1_1);
		else
			factory = SOAPAbstractFactory.getSOAPFactory(Sandesha2Constants.SOAPVersion.v1_2);
			
		
		//finding out the rm version.
		rmNamespaceValue = getRMNamespaceValue (envelope,action);
		if (rmNamespaceValue==null) {
			return;
		}
		
		String addressingNamespaceTmp = getAddressingNamespaceValue (envelope,action);
		if (addressingNamespaceTmp!=null) {
			addressingNamespaceValue = addressingNamespaceTmp;
		}
		
		if (addressingNamespaceValue==null) {
			String message = "Cant find the addressing version";
			throw new SandeshaException (message);
//			return;
		}
		
//		if (addressingNamespaceValue==null)
//			addressingNamespaceValue = AddressingConstants.Final.WSA_NAMESPACE;   //Final is the default version for addressing
	
		OMElement sequenceElement = envelope.getHeader().getFirstChildWithName(
				new QName(rmNamespaceValue, Sandesha2Constants.WSRM_COMMON.SEQUENCE));
		if (sequenceElement != null) {
			sequence = new Sequence(factory,rmNamespaceValue);
			sequence.fromOMElement(envelope.getHeader());
		}

		OMElement sequenceAckElement = envelope.getHeader()
				.getFirstChildWithName(
						new QName(rmNamespaceValue,
								Sandesha2Constants.WSRM_COMMON.SEQUENCE_ACK));
		if (sequenceAckElement != null) {
			sequenceAcknowledgement = new SequenceAcknowledgement(factory,rmNamespaceValue);
			sequenceAcknowledgement.fromOMElement(envelope.getHeader());
		}

		OMElement createSeqElement = envelope.getBody().getFirstChildWithName(
				new QName(rmNamespaceValue,
						Sandesha2Constants.WSRM_COMMON.CREATE_SEQUENCE));
		
		if (createSeqElement != null) {
			createSequence = new CreateSequence(factory,rmNamespaceValue,addressingNamespaceValue);
			createSequence.fromOMElement(envelope.getBody());
		}

		OMElement createSeqResElement = envelope.getBody()
				.getFirstChildWithName(
						new QName(rmNamespaceValue,
								Sandesha2Constants.WSRM_COMMON.CREATE_SEQUENCE_RESPONSE));
		if (createSeqResElement != null) {
			createSequenceResponse = new CreateSequenceResponse(factory,rmNamespaceValue,addressingNamespaceValue);
			createSequenceResponse.fromOMElement(envelope.getBody());
		}

		OMElement terminateSeqElement = envelope.getBody()
				.getFirstChildWithName(
						new QName(rmNamespaceValue,
								Sandesha2Constants.WSRM_COMMON.TERMINATE_SEQUENCE));
		if (terminateSeqElement != null) {
			terminateSequence = new TerminateSequence(factory,rmNamespaceValue);
			terminateSequence.fromOMElement(envelope.getBody());
		}
		
		OMElement terminateSeqResponseElement = envelope.getBody()
				.getFirstChildWithName(
						new QName(rmNamespaceValue,
								Sandesha2Constants.WSRM_COMMON.TERMINATE_SEQUENCE_RESPONSE));
		if (terminateSeqResponseElement != null) {
				terminateSequenceResponse = new TerminateSequenceResponse (factory,rmNamespaceValue);
				terminateSequenceResponse.fromOMElement(envelope.getBody());
		}

		OMElement ackRequestedElement = envelope.getHeader()
				.getFirstChildWithName(
						new QName(rmNamespaceValue,
								Sandesha2Constants.WSRM_COMMON.ACK_REQUESTED));
		if (ackRequestedElement != null) {
			ackRequested = new AckRequested(factory,rmNamespaceValue);
			ackRequested.fromOMElement(envelope.getHeader());
		}
		
		OMElement closeSequenceElement = envelope.getBody()
			.getFirstChildWithName(
				new QName(rmNamespaceValue,
						Sandesha2Constants.WSRM_COMMON.CLOSE_SEQUENCE));
		if (closeSequenceElement != null) {
			closeSequence = new CloseSequence (factory,rmNamespaceValue);
			closeSequence.fromOMElement(envelope.getBody());
		}

		OMElement closeSequenceResponseElement = envelope.getBody()
			.getFirstChildWithName(
					new QName(rmNamespaceValue,
							Sandesha2Constants.WSRM_COMMON.CLOSE_SEQUENCE_RESPONSE));
		if (closeSequenceResponseElement != null) {
			closeSequenceResponse = new CloseSequenceResponse  (factory,rmNamespaceValue);
			closeSequenceResponse.fromOMElement(envelope.getBody());
		}
	}

	public SOAPEnvelope toSOAPEnvelope(SOAPEnvelope envelope) throws SandeshaException  {
		if (sequence != null) {
			sequence.toOMElement(envelope.getHeader());
		}
		if (sequenceAcknowledgement != null) {
			sequenceAcknowledgement.toOMElement(envelope.getHeader());
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
		if (ackRequested != null) {
			ackRequested.toOMElement(envelope.getBody());
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

	public SequenceAcknowledgement getSequenceAcknowledgement() {
		return sequenceAcknowledgement;
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

	public void setSequenceAcknowledgement(
			SequenceAcknowledgement sequenceAcknowledgement) {
		this.sequenceAcknowledgement = sequenceAcknowledgement;
	}

	public void setTerminateSequence(TerminateSequence terminateSequence) {
		this.terminateSequence = terminateSequence;
	}
	
	public void setTerminateSequenceResponse(TerminateSequenceResponse terminateSequenceResponse) {
		this.terminateSequenceResponse = terminateSequenceResponse;
	}

	public AckRequested getAckRequested() {
		return ackRequested;
	}

	public void setAckRequested(AckRequested ackRequested) {
		this.ackRequested = ackRequested;
	}
	
	private String getRMNamespaceValue (SOAPEnvelope envelope, String action) {
		SOAPHeader header = envelope.getHeader();
		if (header!=null) {
			ArrayList headers = header.getHeaderBlocksWithNSURI(Sandesha2Constants.SPEC_2005_02.NS_URI);
			if (headers!=null && headers.size()>0)
				return Sandesha2Constants.SPEC_2005_02.NS_URI;
			
			headers = header.getHeaderBlocksWithNSURI(Sandesha2Constants.SPEC_2005_10.NS_URI);
			if (headers!=null && headers.size()>0)
				return Sandesha2Constants.SPEC_2005_10.NS_URI;
		}
		
		//rm control messages with parts in the body will be identified by the wsa:action.
		if (action==null)
			return null;
		
		if (action.equals(Sandesha2Constants.SPEC_2005_02.Actions.ACTION_CREATE_SEQUENCE))
			return Sandesha2Constants.SPEC_2005_02.NS_URI;
		if (action.equals(Sandesha2Constants.SPEC_2005_02.Actions.ACTION_CREATE_SEQUENCE_RESPONSE))
			return Sandesha2Constants.SPEC_2005_02.NS_URI;
		if (action.equals(Sandesha2Constants.SPEC_2005_02.Actions.ACTION_SEQUENCE_ACKNOWLEDGEMENT))
			return Sandesha2Constants.SPEC_2005_02.NS_URI;
		if (action.equals(Sandesha2Constants.SPEC_2005_02.Actions.ACTION_TERMINATE_SEQUENCE))
			return Sandesha2Constants.SPEC_2005_02.NS_URI;
		
		if (action.equals(Sandesha2Constants.SPEC_2005_10.Actions.ACTION_CREATE_SEQUENCE))
			return Sandesha2Constants.SPEC_2005_10.NS_URI;
		if (action.equals(Sandesha2Constants.SPEC_2005_10.Actions.ACTION_CREATE_SEQUENCE_RESPONSE))
			return Sandesha2Constants.SPEC_2005_10.NS_URI;
		if (action.equals(Sandesha2Constants.SPEC_2005_10.Actions.ACTION_SEQUENCE_ACKNOWLEDGEMENT))
			return Sandesha2Constants.SPEC_2005_10.NS_URI;
		if (action.equals(Sandesha2Constants.SPEC_2005_10.Actions.ACTION_TERMINATE_SEQUENCE))
			return Sandesha2Constants.SPEC_2005_10.NS_URI;
		if (action.equals(Sandesha2Constants.SPEC_2005_10.Actions.ACTION_CLOSE_SEQUENCE))
			return Sandesha2Constants.SPEC_2005_10.NS_URI;
		if (action.equals(Sandesha2Constants.SPEC_2005_10.Actions.ACTION_TERMINATE_SEQUENCE_RESPONSE))
			return Sandesha2Constants.SPEC_2005_10.NS_URI;
		if (action.equals(Sandesha2Constants.SPEC_2005_10.Actions.ACTION_CLOSE_SEQUENCE_RESPONSE))
			return Sandesha2Constants.SPEC_2005_10.NS_URI;
		
		return null;   //a version could not be found
	}
	
	private String getAddressingNamespaceValue (SOAPEnvelope envelope, String action) {
		SOAPHeader header = envelope.getHeader();
		if (header!=null) {
			ArrayList headers = header.getHeaderBlocksWithNSURI(AddressingConstants.Submission.WSA_NAMESPACE);
			if (headers!=null && headers.size()>0)
				return AddressingConstants.Submission.WSA_NAMESPACE;
			
			headers = header.getHeaderBlocksWithNSURI(AddressingConstants.Final.WSA_NAMESPACE);
			if (headers!=null && headers.size()>0)
				return AddressingConstants.Final.WSA_NAMESPACE;
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

	public String getAddressingNamespaceValue() {
		return addressingNamespaceValue;
	}
}