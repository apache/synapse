/*
 * Copyright 2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.sandesha2.wsrm;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.Constants;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;

public class MessagePending implements IOMRMPart {

	boolean pending = false;
	String namespaceValue = null;
	
	public MessagePending (String namespaceValue) throws SandeshaException {
		if (!isNamespaceSupported(namespaceValue))
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.unknownSpec,
					namespaceValue));
		
		this.namespaceValue = namespaceValue;
	}
	
	public void toSOAPEnvelope(SOAPEnvelope envelope) {
		SOAPHeader header = envelope.getHeader();
		
		if (header==null) {
			SOAPFactory factory = (SOAPFactory)envelope.getOMFactory();
			header = factory.createSOAPHeader(envelope);
		}
		
		//detach if already exist.
		OMElement elem = header.getFirstChildWithName(new QName(namespaceValue,
				Sandesha2Constants.WSRM_COMMON.MESSAGE_PENDING));
		if (elem!=null)
			elem.detach();
		
		toOMElement(header);
	}

	public String getNamespaceValue() {
		return namespaceValue;
	}

	public Object fromOMElement(OMElement messagePendingElement) throws OMException,
			SandeshaException {
		
		OMAttribute pendingAttr = messagePendingElement.getAttribute(new QName (Sandesha2Constants.WSRM_COMMON.PENDING));
		if (pendingAttr==null) {
			String message = "MessagePending header must have an attribute named 'pending'";
			throw new SandeshaException (message);
		}
		
		String value = pendingAttr.getAttributeValue();
		if (Constants.VALUE_TRUE.equals(value))
			pending = true;
		else if (Constants.VALUE_FALSE.equals(value))
			pending = false;
		else {
			String message = "Attribute 'pending' must have value 'true' or 'false'";
			throw new SandeshaException (message);
		}
		
		return messagePendingElement;
	}

	public OMElement toOMElement(OMElement headerElement) throws OMException {
		if (!(headerElement instanceof SOAPHeader)) {
			String message = "'MessagePending' element can only be added to a SOAP Header";
			throw new OMException(message);
		}
		
		SOAPHeader header = (SOAPHeader) headerElement;
		OMFactory factory = header.getOMFactory();
		OMNamespace namespace = factory.createOMNamespace(namespaceValue,Sandesha2Constants.WSRM_COMMON.NS_PREFIX_RM);
		
		SOAPHeaderBlock headerBlock = header.addHeaderBlock(Sandesha2Constants.WSRM_COMMON.MESSAGE_PENDING,namespace);
		
		OMAttribute attribute = factory.createOMAttribute(Sandesha2Constants.WSRM_COMMON.PENDING,null,new Boolean (pending).toString());
		headerBlock.addAttribute(attribute);
		
		return headerElement;
	}

	public boolean isNamespaceSupported(String namespaceName) {
		if (Sandesha2Constants.SPEC_2007_02.MC_NS_URI.equals(namespaceName))
			return true;
		
		return false;
	}

	public boolean isPending() {
		return pending;
	}

	public void setPending(boolean pending) {
		this.pending = pending;
	}

	
}
