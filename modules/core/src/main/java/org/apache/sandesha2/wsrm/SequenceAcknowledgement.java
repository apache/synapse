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

package org.apache.sandesha2.wsrm;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.util.SpecSpecificConstants;

/**
 * Adds the SequenceAcknowledgement header block.
 * 
 * Either RM10 or RM11 namespace supported
 */
public class SequenceAcknowledgement implements RMHeaderPart {
	
	private Identifier identifier;
	private ArrayList acknowledgementRangeList;
	private ArrayList nackList;
	private String namespaceValue = null;
	private OMNamespace omNamespace = null;
	private AckNone ackNone = null;
	private AckFinal ackFinal = null;
	
	private OMElement originalSequenceAckElement;
	
	public SequenceAcknowledgement(String namespaceValue) throws SandeshaException {
		this.namespaceValue = namespaceValue;
		if (Sandesha2Constants.SPEC_2005_02.NS_URI.equals(namespaceValue)) {
			omNamespace = Sandesha2Constants.SPEC_2005_02.OM_NS_URI;
		}else{
			omNamespace = Sandesha2Constants.SPEC_2007_02.OM_NS_URI;
		}
		acknowledgementRangeList = new ArrayList();
		nackList = new ArrayList();
	}

	public String getNamespaceValue() {
		return namespaceValue;
	}

	public Object fromHeaderBlock(SOAPHeaderBlock sequenceAckElement) throws OMException,SandeshaException {
		originalSequenceAckElement = sequenceAckElement;
		OMElement identifierPart = null;
		Iterator childElements = sequenceAckElement.getChildElements();
		while(childElements.hasNext()){
			OMElement element = (OMElement)childElements.next();
			String elementNamespace = element.getQName().getNamespaceURI();
			String elementLocalName = element.getQName().getLocalPart();
			if(namespaceValue.equals(elementNamespace)){
				if(Sandesha2Constants.WSRM_COMMON.ACK_RANGE.equals(elementLocalName)){
					AcknowledgementRange ackRange = new AcknowledgementRange(namespaceValue);
					ackRange.fromOMElement(element);
					acknowledgementRangeList.add(ackRange);
				}else if(Sandesha2Constants.WSRM_COMMON.NACK.equals(elementLocalName)){
					Nack nack = new Nack(namespaceValue);
					nack.fromOMElement(element);
					nackList.add(nack);
				}else if(Sandesha2Constants.WSRM_COMMON.IDENTIFIER.equals(elementLocalName)){
					identifierPart = element;
				}else {
					String rmSpecVersion = SpecSpecificConstants.getSpecVersionString (namespaceValue);
					if (SpecSpecificConstants.isAckFinalAllowed(rmSpecVersion)) {
						if(Sandesha2Constants.WSRM_COMMON.FINAL.equals(elementLocalName)){
							ackFinal = new AckFinal (namespaceValue);
							ackFinal.fromOMElement(element);
						}
					}
					if (SpecSpecificConstants.isAckNoneAllowed(rmSpecVersion)) {
						if(Sandesha2Constants.WSRM_COMMON.NONE.equals(elementLocalName)){
							ackNone = new AckNone (namespaceValue);
							ackNone.fromOMElement(element);
						}
					}
				}
			}
		}

		identifier = new Identifier(namespaceValue);
		identifier.fromOMElement(identifierPart);

		// Indicate that we have processed this part of the message.
		sequenceAckElement.setProcessed();
		return this;
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
	
	public OMElement getOriginalSequenceAckElement() {
		return originalSequenceAckElement;
	}

	public void toHeader(SOAPHeader header) throws SandeshaException{
		SOAPHeaderBlock sequenceAcknowledgementHeaderBlock = header.addHeaderBlock(
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
	}
}
