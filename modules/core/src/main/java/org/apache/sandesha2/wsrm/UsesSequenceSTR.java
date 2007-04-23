/*
 * Copyright 2006 The Apache Software Foundation.
 * Copyright 2006 International Business Machines Corp.
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
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;

/**
 * Class which handles the UsesSequenceSTR header block
 */
public class UsesSequenceSTR implements IOMRMPart {
	
	private SOAPFactory defaultFactory;
	private String namespaceValue = null;
	
	public UsesSequenceSTR(SOAPFactory factory,String namespaceValue) throws SandeshaException {
		if (!isNamespaceSupported(namespaceValue))
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.unknownSpec,
					namespaceValue));
		
		this.namespaceValue = namespaceValue;
		this.defaultFactory = factory;
	}

	public String getNamespaceValue() {
		return namespaceValue;
	}

	public Object fromOMElement(OMElement header) throws OMException {

		OMFactory factory = header.getOMFactory();
		if (factory==null)
			factory = defaultFactory;
		
    // Set that we have processed the must understand
    ((SOAPHeaderBlock)header).setProcessed();
    
		return this;
	}

	public OMElement toOMElement(OMElement header) throws OMException {

		if (header == null || !(header instanceof SOAPHeader))
			throw new OMException();

		OMFactory factory = header.getOMFactory();
		if (factory==null)
			factory = defaultFactory;
		
		OMNamespace rmNamespace = factory.createOMNamespace(namespaceValue,Sandesha2Constants.WSRM_COMMON.NS_PREFIX_RM);
		
		SOAPHeader SOAPHeader = (SOAPHeader) header;
		SOAPHeaderBlock sequenceAcknowledgementHeaderBlock = SOAPHeader.addHeaderBlock(
				Sandesha2Constants.WSRM_COMMON.USES_SEQUENCE_STR,rmNamespace);
		
		if (sequenceAcknowledgementHeaderBlock == null)
			throw new OMException("Cant set UsesSequenceSTR since the element is null");

		// This header _must_ always be understood
		sequenceAcknowledgementHeaderBlock.setMustUnderstand(true);

		SOAPHeader.addChild(sequenceAcknowledgementHeaderBlock);

		return header;
	}

	public void toSOAPEnvelope(SOAPEnvelope envelope) {
		SOAPHeader header = envelope.getHeader();

		if (header==null) {
			SOAPFactory factory = (SOAPFactory)envelope.getOMFactory();
			header = factory.createSOAPHeader(envelope);
		}
		
		//detach if already exist.
		OMElement elem = header.getFirstChildWithName(new QName(
				namespaceValue, Sandesha2Constants.WSRM_COMMON.USES_SEQUENCE_STR));
		if (elem != null)
			elem.detach();

		toOMElement(header);
	}

	public boolean isNamespaceSupported (String namespaceName) {
		// This is only supported using the new namespace
		if (Sandesha2Constants.SPEC_2007_02.NS_URI.equals(namespaceName))
			return true;
		
		return false;
	}

}