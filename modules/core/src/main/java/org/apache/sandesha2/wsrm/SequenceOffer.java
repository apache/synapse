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

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axis2.AxisFault;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;

/**
 * Represents an SequenceOffer element which may be present within a 
 * Create Sequence message.
 */

public class SequenceOffer implements IOMRMElement {
	
	private Identifier identifier = null;
	
	private Expires expires = null;
	
	private Endpoint endpoint = null;
	
	private String namespaceValue = null;
	
	public SequenceOffer(String namespaceValue) throws SandeshaException {
		if (!isNamespaceSupported(namespaceValue))
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.unknownSpec,
					namespaceValue));
		
		this.namespaceValue = namespaceValue;
	}

	public String getNamespaceValue() {
		return namespaceValue;
	}

	public Object fromOMElement(OMElement createSequenceElement)
			throws OMException,AxisFault {
		
		OMElement sequenceOfferPart = createSequenceElement
				.getFirstChildWithName(new QName(namespaceValue,Sandesha2Constants.WSRM_COMMON.SEQUENCE_OFFER));
		if (sequenceOfferPart == null)
			throw new OMException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.noSeqOfferInElement,
					createSequenceElement.toString()));

		identifier = new Identifier(namespaceValue);
		identifier.fromOMElement(sequenceOfferPart);

		OMElement expiresPart = sequenceOfferPart
				.getFirstChildWithName(new QName(namespaceValue,Sandesha2Constants.WSRM_COMMON.EXPIRES));
		if (expiresPart != null) {
			expires = new Expires(namespaceValue);
			expires.fromOMElement(sequenceOfferPart);
		}
		
		OMElement endpointPart = sequenceOfferPart
				.getFirstChildWithName(new QName (namespaceValue,Sandesha2Constants.WSRM_COMMON.ENDPOINT));
		if (endpointPart != null) {
			endpoint = new Endpoint (namespaceValue);
			endpoint.fromOMElement (endpointPart);
		}

		return this;
	}

	public OMElement toOMElement(OMElement createSequenceElement)
			throws OMException,AxisFault {

		if (identifier == null)
			throw new OMException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.nullMsgId));

		OMFactory factory = createSequenceElement.getOMFactory();
		
		OMNamespace rmNamespace = factory.createOMNamespace(namespaceValue,Sandesha2Constants.WSRM_COMMON.NS_PREFIX_RM);
		OMElement sequenceOfferElement = factory.createOMElement(Sandesha2Constants.WSRM_COMMON.SEQUENCE_OFFER, rmNamespace);
		
		identifier.toOMElement(sequenceOfferElement, rmNamespace);

		if (endpoint!=null) {
			endpoint.toOMElement(sequenceOfferElement);
		} else {
			if (Sandesha2Constants.SPEC_2007_02.NS_URI.equals(namespaceValue)) {
				String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.elementMustForSpec,
						Sandesha2Constants.WSRM_COMMON.ENDPOINT,
						Sandesha2Constants.SPEC_2007_02.NS_URI);
				throw new SandeshaException (message);
			}
		}
		
		if (expires != null) {
			expires.toOMElement(sequenceOfferElement);
		}

		createSequenceElement.addChild(sequenceOfferElement);

		return createSequenceElement;
	}

	public Identifier getIdentifer() {
		return identifier;
	}

	public void setIdentifier(Identifier identifier) {
		this.identifier = identifier;
	}
	
	public Endpoint getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(Endpoint endpoint) {
		this.endpoint = endpoint;
	}

	public boolean isNamespaceSupported (String namespaceName) {
		if (Sandesha2Constants.SPEC_2005_02.NS_URI.equals(namespaceName))
			return true;
		
		if (Sandesha2Constants.SPEC_2007_02.NS_URI.equals(namespaceName))
			return true;
		
		return false;
	}

}
