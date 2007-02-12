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
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;

/**
 * Adds the SequenceFault header block.
 */

public class SequenceFault implements IOMRMPart {
	
	private FaultCode faultCode;
	
	private String namespaceValue = null;

	public SequenceFault(String namespaceValue) throws SandeshaException {
		if (!isNamespaceSupported(namespaceValue))
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.unknownSpec,
					namespaceValue));
		
		this.namespaceValue = namespaceValue;
	}

	public String getNamespaceValue() {
		return namespaceValue;
	}

	public Object fromOMElement(OMElement sequenceFaultPart) throws OMException,SandeshaException {


		if (sequenceFaultPart == null)
			throw new OMException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.noSeqFaultInElement,
					null));

		OMElement faultCodePart = sequenceFaultPart
				.getFirstChildWithName(new QName(namespaceValue,Sandesha2Constants.WSRM_COMMON.FAULT_CODE));

		if (faultCodePart != null) {
			faultCode = new FaultCode(namespaceValue);
			faultCode.fromOMElement(sequenceFaultPart);
		}

		return this;
	}

	public OMElement toOMElement(OMElement body) throws OMException {

		if (body == null || !(body instanceof SOAPHeader))
			throw new OMException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.seqFaultCannotBeExtractedToNonHeader));

		OMFactory factory = body.getOMFactory();

		OMNamespace rmNamespace = factory.createOMNamespace(namespaceValue,Sandesha2Constants.WSRM_COMMON.NS_PREFIX_RM);
		OMElement sequenceFaultElement =factory.createOMElement(
				Sandesha2Constants.WSRM_COMMON.SEQUENCE_FAULT, rmNamespace);
		if (faultCode != null)
			faultCode.toOMElement(sequenceFaultElement);

		body.addChild(sequenceFaultElement);

		return body;
	}

	public void setFaultCode(FaultCode faultCode) {
		this.faultCode = faultCode;
	}

	public FaultCode getFaultCode() {
		return faultCode;
	}
	
	public boolean isNamespaceSupported (String namespaceName) {
		if (Sandesha2Constants.SPEC_2005_02.NS_URI.equals(namespaceName))
			return true;
		
		if (Sandesha2Constants.SPEC_2006_08.NS_URI.equals(namespaceName))
			return true;
		
		return false;
	}

	public void toSOAPEnvelope(SOAPEnvelope envelope) {
		SOAPHeader header = envelope.getHeader();
		
		//detach if already exist.
		OMElement elem = header.getFirstChildWithName(new QName(namespaceValue,
				Sandesha2Constants.WSRM_COMMON.SEQUENCE_FAULT));
		if (elem!=null)
			elem.detach();
		
		toOMElement(header);
  }

}
