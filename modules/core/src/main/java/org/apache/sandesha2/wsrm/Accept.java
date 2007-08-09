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
 * Represents the RM Accept element which may come within the 
 * Create Sequence Response.
 */

public class Accept implements IOMRMElement {

	private AcksTo acksTo;
	
	private String rmNamespaceValue;
	
	// Constructor used during parsing
	public Accept(String rmNamespaceValue) throws SandeshaException {
		if (!isNamespaceSupported(rmNamespaceValue))
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.unknownNamespace,
					rmNamespaceValue));
		
		this.rmNamespaceValue = rmNamespaceValue;
	}
	
	// Constructor used during writing
	public Accept(String rmNamespace, AcksTo acksTo) throws SandeshaException {
		this(rmNamespace);
		this.acksTo = acksTo;
	}

	public String getNamespaceValue(){
		return rmNamespaceValue;
	}
	
	public String getAddressingNamespaceValue() {
		if(acksTo != null) return acksTo.getAddressingNamespaceValue();
		return null;
	}
	
	public Object fromOMElement(OMElement element) throws OMException,AxisFault {
		
		OMElement acceptPart = element.getFirstChildWithName(new QName(
				rmNamespaceValue, Sandesha2Constants.WSRM_COMMON.ACCEPT));
		if (acceptPart == null)
			throw new OMException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.noAcceptPartInElement,
					element.toString()));

		acksTo = new AcksTo(rmNamespaceValue);
		acksTo.fromOMElement(acceptPart);

		return this;
	}

	public OMElement toOMElement(OMElement element) throws OMException,AxisFault {

		OMFactory factory = element.getOMFactory();
		
		if (acksTo == null)
			throw new OMException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.acceptNullAcksTo));

		OMNamespace rmNamespace = factory.createOMNamespace(rmNamespaceValue,Sandesha2Constants.WSRM_COMMON.NS_PREFIX_RM);
		OMElement acceptElement = factory.createOMElement(Sandesha2Constants.WSRM_COMMON.ACCEPT, rmNamespace);
		
		acksTo.toOMElement(acceptElement);
		element.addChild(acceptElement);

		return element;
	}

	public void setAcksTo(AcksTo acksTo) {
		this.acksTo = acksTo;
	}

	public AcksTo getAcksTo() {
		return acksTo;
	}
	
	public boolean isNamespaceSupported (String namespaceName) {
		if (Sandesha2Constants.SPEC_2005_02.NS_URI.equals(namespaceName))
			return true;
		
		if (Sandesha2Constants.SPEC_2007_02.NS_URI.equals(namespaceName))
			return true;
		
		return false;
	}
}
