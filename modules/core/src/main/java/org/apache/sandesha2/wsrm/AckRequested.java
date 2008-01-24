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

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.OMNamespace;
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
 * 
 * Either RM10 or RM11 namespace supported
 */

public class AckRequested implements RMHeaderPart {
	
	private Identifier identifier;
	private String namespaceValue = null;
	private boolean mustUnderstand = false;
	private OMNamespace omNamespace = null;
	private OMElement originalAckRequestedElement;
	
	public AckRequested(String namespaceValue) throws SandeshaException {
		this.namespaceValue = namespaceValue;
		if (Sandesha2Constants.SPEC_2005_02.NS_URI.equals(namespaceValue)) {
			omNamespace = Sandesha2Constants.SPEC_2005_02.OM_NS_URI;
		}else{
			omNamespace = Sandesha2Constants.SPEC_2007_02.OM_NS_URI;
		}
	}

	public String getNamespaceValue() {
		return namespaceValue;
	}

	public Object fromHeaderBlock(SOAPHeaderBlock ackReqElement) throws OMException,SandeshaException {
		originalAckRequestedElement = ackReqElement;
		identifier = new Identifier(namespaceValue);
		OMElement identifierPart = ackReqElement.getFirstChildWithName(new QName(
				namespaceValue, Sandesha2Constants.WSRM_COMMON.IDENTIFIER));
		if(identifierPart != null){
			identifier.fromOMElement(identifierPart);
		}

		// Indicate that we have processed this SOAPHeaderBlock
		ackReqElement.setProcessed();

		return this;
	}

	public void setIdentifier(Identifier identifier) {
		this.identifier = identifier;
	}

	public Identifier getIdentifier() {
		return identifier;
	}
	
	public boolean isMustUnderstand() {
		return mustUnderstand;
	}

	public void setMustUnderstand(boolean mustUnderstand) {
		this.mustUnderstand = mustUnderstand;
	}
		
	public OMElement getOriginalAckRequestedElement() {
		return originalAckRequestedElement;
	}

	public void toHeader(SOAPHeader header) throws SandeshaException {
		if (identifier == null)
			throw new OMException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.ackRequestNullID));
				
		SOAPHeaderBlock ackReqHdrBlock = header.addHeaderBlock(Sandesha2Constants.WSRM_COMMON.ACK_REQUESTED, omNamespace);
		ackReqHdrBlock.setMustUnderstand(isMustUnderstand());
		identifier.toOMElement(ackReqHdrBlock, omNamespace);
	}
}
