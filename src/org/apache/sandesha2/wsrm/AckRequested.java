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

import java.util.Iterator;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;

/**
 * Represent the AckRequested header block.
 * The 2005/02 spec includes a 'MessageNumber' part in the ack request, but
 * the 2006/08 spec does not. As the message number was never used in our
 * implementation we simply ignore it.
 */

public class AckRequested implements IOMRMPart {
	
	private Identifier identifier;
	private String namespaceValue = null;
	private boolean mustUnderstand = false;
	
	public AckRequested(String namespaceValue) throws SandeshaException {
		if (!isNamespaceSupported(namespaceValue))
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.unknownSpec,
					namespaceValue));
		
		this.namespaceValue = namespaceValue;
	}

	public String getNamespaceValue() {
		return namespaceValue;
	}

	public Object fromOMElement(OMElement ackReqElement) throws OMException,SandeshaException {

		identifier = new Identifier(namespaceValue);
		identifier.fromOMElement(ackReqElement);
		
		return this;
	}

	public OMElement toOMElement(OMElement header) throws OMException {

		if (header == null || !(header instanceof SOAPHeader))
			throw new OMException(
					SandeshaMessageHelper.getMessage(
							SandeshaMessageKeys.ackRequestedCannotBeAddedToNonHeader));

		if (identifier == null)
			throw new OMException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.ackRequestNullID));
		
		OMFactory factory = header.getOMFactory();
		OMNamespace rmNamespace = factory.createOMNamespace(namespaceValue,Sandesha2Constants.WSRM_COMMON.NS_PREFIX_RM);

		Iterator iter = header.getChildrenWithName(new QName (namespaceValue,Sandesha2Constants.WSRM_COMMON.ACK_REQUESTED));
		while (iter.hasNext()) {
			OMElement ackRequestedElement = (OMElement) iter.next();
			
			OMElement identifierElement = ackRequestedElement.getFirstChildWithName(new QName (namespaceValue,
					Sandesha2Constants.WSRM_COMMON.IDENTIFIER));
			String identifierVal = null;
			if (identifierElement!=null)
				identifierVal = identifierElement.getText();
			
			if (identifierVal!=null && 
					(identifierVal.equals(identifier.getIdentifier()) || identifierVal.equals(Sandesha2Constants.TEMP_SEQUENCE_ID)))
				ackRequestedElement.detach();
			
		}
		
		SOAPHeader SOAPHdr = (SOAPHeader) header;
		SOAPHeaderBlock ackReqHdrBlock = SOAPHdr.addHeaderBlock(Sandesha2Constants.WSRM_COMMON.ACK_REQUESTED, rmNamespace);
		ackReqHdrBlock.setMustUnderstand(isMustUnderstand());

		identifier.toOMElement(ackReqHdrBlock);

		return header;
	}

	public void setIdentifier(Identifier identifier) {
		this.identifier = identifier;
	}

	public Identifier getIdentifier() {
		return identifier;
	}

	public void toSOAPEnvelope(SOAPEnvelope envelope) {
		SOAPHeader header = envelope.getHeader();
		
		if (header==null) {
			SOAPFactory factory = (SOAPFactory)envelope.getOMFactory();
			header = factory.createSOAPHeader(envelope);
		}
		
		toOMElement(header);
	}
	
	public boolean isMustUnderstand() {
		return mustUnderstand;
	}

	public void setMustUnderstand(boolean mustUnderstand) {
		this.mustUnderstand = mustUnderstand;
	}
	
	public boolean isNamespaceSupported (String namespaceName) {
		if (Sandesha2Constants.SPEC_2005_02.NS_URI.equals(namespaceName))
			return true;
		
		if (Sandesha2Constants.SPEC_2007_02.NS_URI.equals(namespaceName))
			return true;
		
		return false;
	}
	
}
