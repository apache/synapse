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

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.util.SpecSpecificConstants;

/**
 * Represent the CreateSequence body element.
 */

public class CreateSequence implements IOMRMPart {
	
	private AcksTo acksTo = null;
	
	private Expires expires = null;
	
	private SequenceOffer sequenceOffer = null;
	
	private OMFactory defaultFactory;
	
	private String rmNamespaceValue = null;
	
	private String addressingNamespaceValue = null;
	
	private String secNamespaceValue = null;
	
	private OMElement securityTokenReference = null;
	
	private OMElement element;
	
	public CreateSequence(OMFactory factory,String rmNamespaceValue,String addressingNamespaceValue) throws SandeshaException {
		if (!isNamespaceSupported(rmNamespaceValue))
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.unknownSpec,
					rmNamespaceValue));
		
		this.defaultFactory = factory;
		this.rmNamespaceValue = rmNamespaceValue;
		this.addressingNamespaceValue = addressingNamespaceValue;
		this.secNamespaceValue = SpecSpecificConstants.getSecurityNamespace(rmNamespaceValue);
	}
	
	public CreateSequence (AcksTo acksTo,SOAPFactory factory,String rmNamespaceValue,String addressingNamespaceValue) throws SandeshaException {
		this (factory,rmNamespaceValue,addressingNamespaceValue);
		this.acksTo = acksTo;
	}

	public String getNamespaceValue() {
		return rmNamespaceValue;
	}

	public Object fromOMElement(OMElement bodyElement) throws OMException,SandeshaException {

		OMElement createSequencePart = bodyElement
				.getFirstChildWithName(new QName(rmNamespaceValue,
						                         Sandesha2Constants.WSRM_COMMON.CREATE_SEQUENCE));
		if (createSequencePart == null)
			throw new OMException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.noCreateSeqPartInElement,
					bodyElement.toString()));
		
		element = bodyElement;
		
		acksTo = new AcksTo(defaultFactory,rmNamespaceValue,addressingNamespaceValue);
		acksTo.fromOMElement(createSequencePart);

		OMElement offerPart = createSequencePart.getFirstChildWithName(new QName(rmNamespaceValue,
																	   Sandesha2Constants.WSRM_COMMON.SEQUENCE_OFFER));
		if (offerPart != null) {
			sequenceOffer = new SequenceOffer(defaultFactory,rmNamespaceValue);
			sequenceOffer.fromOMElement(createSequencePart);
		}

		OMElement expiresPart = createSequencePart.getFirstChildWithName(
						new QName(rmNamespaceValue,
						Sandesha2Constants.WSRM_COMMON.EXPIRES));
		if (expiresPart != null) {
			expires = new Expires(defaultFactory,rmNamespaceValue);
			expires.fromOMElement(createSequencePart);
		}
		
		if(secNamespaceValue != null) {
			securityTokenReference = createSequencePart.getFirstChildWithName(
				new QName(secNamespaceValue, "SecurityTokenReference"));
		}
		return this;
	}

	public OMElement toOMElement(OMElement bodyElement) throws OMException {

		if (bodyElement == null || !(bodyElement instanceof SOAPBody))
			throw new OMException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.createSeqCannotBeAddedToNonBody));

		if (acksTo == null)
			throw new OMException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.createSeqNullAcksTo));

		SOAPBody soapBody = (SOAPBody) bodyElement;
		
		OMFactory factory = bodyElement.getOMFactory();
		if (factory==null)
			factory = defaultFactory;
		OMNamespace rmNamespace = factory.createOMNamespace(rmNamespaceValue,Sandesha2Constants.WSRM_COMMON.NS_PREFIX_RM);
		OMElement createSequenceElement = factory.createOMElement(
				Sandesha2Constants.WSRM_COMMON.CREATE_SEQUENCE, rmNamespace);
		
		acksTo.toOMElement(createSequenceElement);

		if (sequenceOffer != null) {
			sequenceOffer.toOMElement(createSequenceElement);
		}

		if (expires != null) {
			expires.toOMElement(createSequenceElement);
		}
		
		if(securityTokenReference != null) {
			createSequenceElement.addChild(securityTokenReference);
		}

		soapBody.addChild(createSequenceElement);
		return soapBody;
	}

	public void setAcksTo(AcksTo acksTo) {
		this.acksTo = acksTo;
	}

	public void setSequenceOffer(SequenceOffer sequenceOffer) {
		this.sequenceOffer = sequenceOffer;
	}

	public AcksTo getAcksTo() {
		return acksTo;
	}

	public SequenceOffer getSequenceOffer() {
		return sequenceOffer;
	}

	public void toSOAPEnvelope(SOAPEnvelope envelope) {
		SOAPBody body = envelope.getBody();
		
		//detach if already exist.
		OMElement elem = body.getFirstChildWithName(new QName(rmNamespaceValue,
				Sandesha2Constants.WSRM_COMMON.CREATE_SEQUENCE));
		if (elem!=null)
			elem.detach();
		
		toOMElement(body);
	}
	
	public boolean isNamespaceSupported (String namespaceName) {
		if (Sandesha2Constants.SPEC_2005_02.NS_URI.equals(namespaceName))
			return true;
		
		if (Sandesha2Constants.SPEC_2005_10.NS_URI.equals(namespaceName))
			return true;
		
		return false;
	}
	
	public OMElement getSecurityTokenReference() {
		return securityTokenReference;
	}

	public void setSecurityTokenReference(OMElement theSTR) {
		this.securityTokenReference = theSTR;
	}

	public OMElement getOMElement() {
		return element;
	}

}
