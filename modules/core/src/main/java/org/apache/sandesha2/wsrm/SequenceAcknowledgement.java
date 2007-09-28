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
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.util.SpecSpecificConstants;

/**
 * Adds the SequenceAcknowledgement header block.
 */

public class SequenceAcknowledgement implements IOMRMPart {
	
	private Identifier identifier;
	private ArrayList acknowledgementRangeList;
	private ArrayList nackList;
	private String namespaceValue = null;
	private OMNamespace omNamespace = null;
	private AckNone ackNone = null;
	private AckFinal ackFinal = null;
	
	public SequenceAcknowledgement(String namespaceValue) throws SandeshaException {
		if (!isNamespaceSupported(namespaceValue))
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.unknownSpec,
					namespaceValue));
		
		this.namespaceValue = namespaceValue;
		acknowledgementRangeList = new ArrayList();
		nackList = new ArrayList();
	}

	public String getNamespaceValue() {
		return namespaceValue;
	}

	public Object fromOMElement(OMElement sequenceAckElement) throws OMException,SandeshaException {

		identifier = new Identifier(namespaceValue);
		identifier.fromOMElement(sequenceAckElement);

		Iterator ackRangeParts = sequenceAckElement.getChildrenWithName(new QName(
				namespaceValue, Sandesha2Constants.WSRM_COMMON.ACK_RANGE));

		while (ackRangeParts.hasNext()) {
			OMElement ackRangePart = (OMElement) ackRangeParts.next();

			AcknowledgementRange ackRange = new AcknowledgementRange(namespaceValue);
			ackRange.fromOMElement(ackRangePart);
			acknowledgementRangeList.add(ackRange);
		}

		Iterator nackParts = sequenceAckElement.getChildrenWithName(new QName(
				namespaceValue, Sandesha2Constants.WSRM_COMMON.NACK));

		while (nackParts.hasNext()) {
			OMElement nackPart = (OMElement) nackParts.next();
			Nack nack = new Nack(namespaceValue);
			nack.fromOMElement(nackPart);
			nackList.add(nack);
		}

		String rmSpecVersion = SpecSpecificConstants.getSpecVersionString (namespaceValue);
		
		if (SpecSpecificConstants.isAckFinalAllowed(rmSpecVersion)) {
			OMElement ackFinalPart = sequenceAckElement.getFirstChildWithName(new QName (namespaceValue,Sandesha2Constants.WSRM_COMMON.FINAL));
			if (ackFinalPart!=null) {
				ackFinal = new AckFinal (namespaceValue);
				ackFinal.fromOMElement(sequenceAckElement);
			}
		}
		
		if (SpecSpecificConstants.isAckNoneAllowed(rmSpecVersion)) {
			OMElement ackNonePart = sequenceAckElement.getFirstChildWithName(new QName (namespaceValue,Sandesha2Constants.WSRM_COMMON.NONE));
			if (ackNonePart!=null) {
				ackNone = new AckNone (namespaceValue);
				ackNone.fromOMElement(sequenceAckElement);
			}
		}
		
    // Indicate that we have processed this part of the message.
    ((SOAPHeaderBlock)sequenceAckElement).setProcessed();

    
		return this;
	}

	public OMElement toOMElement(OMElement header) throws OMException,SandeshaException {

		if (header == null || !(header instanceof SOAPHeader))
			throw new OMException();

		//If there already is an ack for this sequence it will be removed. 
		//We do not allow to send two sequenceAcknowledgements for the same sequence in the same message.
		Iterator oldAckIter = header.getChildrenWithName(new QName (namespaceValue,Sandesha2Constants.WSRM_COMMON.SEQUENCE_ACK));
		while (oldAckIter.hasNext()) {
			
			OMElement oldAckElement = (OMElement) oldAckIter.next();
			
			SequenceAcknowledgement oldSequenceAcknowledgement = new SequenceAcknowledgement (namespaceValue);
			oldSequenceAcknowledgement.fromOMElement(oldAckElement);
			
			String oldAckIdentifier = oldSequenceAcknowledgement.getIdentifier().getIdentifier();
			if (oldAckIdentifier!=null && oldAckIdentifier.equals(this.identifier.getIdentifier())) {
				oldAckElement.detach();
			}
		}
		
		SOAPHeader SOAPHeader = (SOAPHeader) header;
		SOAPHeaderBlock sequenceAcknowledgementHeaderBlock = SOAPHeader.addHeaderBlock(
				Sandesha2Constants.WSRM_COMMON.SEQUENCE_ACK,omNamespace);
		
		if (sequenceAcknowledgementHeaderBlock == null)
			throw new OMException("Cant set sequence acknowledgement since the element is null");

		if (identifier == null)
			throw new OMException(
					SandeshaMessageHelper.getMessage(
							SandeshaMessageKeys.invalidIdentifier,
							header.toString()));

    // SequenceACK messages should always have the MustUnderstand flag set to true
		sequenceAcknowledgementHeaderBlock.setMustUnderstand(true);
		identifier.toOMElement(sequenceAcknowledgementHeaderBlock, omNamespace);

		Iterator ackRangeIt = acknowledgementRangeList.iterator();
		while (ackRangeIt.hasNext()) {
			AcknowledgementRange ackRange = (AcknowledgementRange) ackRangeIt
					.next();
			ackRange.toOMElement(sequenceAcknowledgementHeaderBlock);
		}

		Iterator nackIt = nackList.iterator();
		while (nackIt.hasNext()) {
			Nack nack = (Nack) nackIt.next();
			nack.toOMElement(sequenceAcknowledgementHeaderBlock);
		}
		
		String rmSpecVersion = SpecSpecificConstants.getSpecVersionString(namespaceValue);

		//setting a 'None' when nothing is there (for the correct RM version)
		if (ackNone==null && acknowledgementRangeList.size()==0 && nackList.size()==0 && SpecSpecificConstants.isAckNoneAllowed(rmSpecVersion)) {
			ackNone = new AckNone (namespaceValue);
		}
		
		if (ackNone!=null) {
			if (!SpecSpecificConstants.isAckNoneAllowed(rmSpecVersion)) {
				throw new SandeshaException (SandeshaMessageHelper.getMessage(
						SandeshaMessageKeys.noneNotAllowedNamespace,
						rmSpecVersion));
			}
			
			if (acknowledgementRangeList.size()>0) {
				throw new SandeshaException (SandeshaMessageHelper.getMessage(
						SandeshaMessageKeys.noneNotAllowedAckRangesPresent));
			}
			
			if (nackList.size()>0) {
				throw new SandeshaException (SandeshaMessageHelper.getMessage(
						SandeshaMessageKeys.noneNotAllowedNackPresent));
			}
			
			ackNone.toOMElement(sequenceAcknowledgementHeaderBlock);
		}
		
		if (ackFinal!=null) {
			if (!SpecSpecificConstants.isAckFinalAllowed(rmSpecVersion)) {
				throw new SandeshaException (SandeshaMessageHelper.getMessage(
						SandeshaMessageKeys.finalNotAllowedNamespace));
			}
			
			if (nackList.size()>0) {
				throw new SandeshaException (SandeshaMessageHelper.getMessage(
						SandeshaMessageKeys.cannotHaveFinalWithNack));
			}
			
			ackFinal.toOMElement(sequenceAcknowledgementHeaderBlock);
		}
		
		return header;
	}

	public void setIdentifier(Identifier identifier) {
		this.identifier = identifier;
	}

	public void setAckRanges(ArrayList acknowledgementRagngesList) {
		acknowledgementRangeList = acknowledgementRagngesList;
	}

	public Nack addNackRanges(Nack nack) {
		nackList.add(nack);
		return nack;
	}

	public AcknowledgementRange addAcknowledgementRanges(
			AcknowledgementRange ackRange) {
		acknowledgementRangeList.add(ackRange);
		return ackRange;
	}

	public Identifier getIdentifier() {
		return identifier;
	}

	public List getAcknowledgementRanges() {
		return acknowledgementRangeList;
	}

	public List getNackList() {
		return nackList;
	}

	public void toSOAPEnvelope(SOAPEnvelope envelope) throws SandeshaException {
		SOAPHeader header = envelope.getHeader();
		
		if (header==null) {
			SOAPFactory factory = (SOAPFactory)envelope.getOMFactory();
			header = factory.createSOAPHeader(envelope);
		}
		
		toOMElement(header);
	}

	public boolean isNamespaceSupported (String namespaceName) {
		if (Sandesha2Constants.SPEC_2005_02.NS_URI.equals(namespaceName)) {
			omNamespace = Sandesha2Constants.SPEC_2005_02.OM_NS_URI;
			return true;
		}		
		if (Sandesha2Constants.SPEC_2007_02.NS_URI.equals(namespaceName)) {
			omNamespace = Sandesha2Constants.SPEC_2007_02.OM_NS_URI;
			return true;
		}		
		
		return false;
	}

	public AckFinal getAckFinal() {
		return ackFinal;
	}

	public void setAckFinal(AckFinal ackFinal) {
		this.ackFinal = ackFinal;
	}

	public AckNone getAckNone() {
		return ackNone;
	}

	public void setAckNone(AckNone ackNone) {
		this.ackNone = ackNone;
	}
	
}
