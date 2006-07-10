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
import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;

/**
 * Adds the CreateSequenceResponse body part.
 */

public class CreateSequenceResponse implements IOMRMPart {
	
	private Identifier identifier;
	
	private Accept accept;
	
	private Expires expires;
	
	private OMFactory defaultFactory;
	
	private String rmNamespaceValue = null;
	
	private String addressingNamespaceValue = null;

	public CreateSequenceResponse(OMFactory factory, String rmNamespaceValue, String addressingNamespaceValue) throws SandeshaException {
		if (!isNamespaceSupported(rmNamespaceValue))
			throw new SandeshaException ("Unsupported namespace");
		
		this.defaultFactory = factory;
		this.rmNamespaceValue = rmNamespaceValue;
		this.addressingNamespaceValue = addressingNamespaceValue;
	}

	public String getNamespaceValue() {
		return rmNamespaceValue;
	}

	public Object fromOMElement(OMElement bodyElement) throws OMException,SandeshaException {

		if (bodyElement == null || !(bodyElement instanceof SOAPBody))
			throw new OMException("Cant get create sequnce response from a non-body element");

		SOAPBody SOAPBody = (SOAPBody) bodyElement;

		OMElement createSeqResponsePart = SOAPBody
				.getFirstChildWithName(new QName(rmNamespaceValue,Sandesha2Constants.WSRM_COMMON.CREATE_SEQUENCE_RESPONSE));
		if (createSeqResponsePart == null)
			throw new OMException("The passed element does not contain a create seqence response part");

		identifier = new Identifier(defaultFactory,rmNamespaceValue);
		identifier.fromOMElement(createSeqResponsePart);

		OMElement expiresPart = createSeqResponsePart.getFirstChildWithName(
					new QName(rmNamespaceValue,
					Sandesha2Constants.WSRM_COMMON.EXPIRES));
		if (expiresPart != null) {
			expires = new Expires(defaultFactory,rmNamespaceValue);
			expires.fromOMElement(createSeqResponsePart);
		}

		OMElement acceptPart = createSeqResponsePart.getFirstChildWithName(
						new QName(rmNamespaceValue,
						Sandesha2Constants.WSRM_COMMON.ACCEPT));
		if (acceptPart != null) {
			accept = new Accept(defaultFactory,rmNamespaceValue,addressingNamespaceValue);
			accept.fromOMElement(createSeqResponsePart);
		}

		return this;
	}

	public OMElement toOMElement(OMElement bodyElement) throws OMException {

		if (bodyElement == null || !(bodyElement instanceof SOAPBody))
			throw new OMException(
					"Cant get create sequnce response from a non-body element");

		SOAPBody SOAPBody = (SOAPBody) bodyElement;

		if (identifier == null)
			throw new OMException("cant set create sequnce response since the Identifier is not set");

		OMFactory factory = bodyElement.getOMFactory();
		if (factory==null)
			factory = defaultFactory;
		
		OMNamespace rmNamespace = factory.createOMNamespace(rmNamespaceValue,Sandesha2Constants.WSRM_COMMON.NS_PREFIX_RM);
		OMNamespace addressingNamespace = factory.createOMNamespace(addressingNamespaceValue,Sandesha2Constants.WSA.NS_PREFIX_ADDRESSING);
		
		OMElement createSequenceResponseElement = factory.createOMElement(
				Sandesha2Constants.WSRM_COMMON.CREATE_SEQUENCE_RESPONSE,
				rmNamespace);
		
		identifier.toOMElement(createSequenceResponseElement);

		if (expires != null) {
			expires.toOMElement(createSequenceResponseElement);
		}

		if (accept != null) {
			accept.toOMElement(createSequenceResponseElement);
		}

		SOAPBody.addChild(createSequenceResponseElement);

		

		return SOAPBody;
	}

	public void setIdentifier(Identifier identifier) {
		this.identifier = identifier;
	}

	public Identifier getIdentifier() {
		return identifier;
	}

	public void setAccept(Accept accept) {
		this.accept = accept;
	}

	public Accept getAccept() {
		return accept;
	}

	public Expires getExpires() {
		return expires;
	}

	public void setExpires(Expires expires) {
		this.expires = expires;
	}

	public void toSOAPEnvelope(SOAPEnvelope envelope) {
		SOAPBody body = envelope.getBody();
		
		//detach if already exist.
		OMElement elem = body.getFirstChildWithName(new QName(rmNamespaceValue,
				Sandesha2Constants.WSRM_COMMON.CREATE_SEQUENCE_RESPONSE));
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
}