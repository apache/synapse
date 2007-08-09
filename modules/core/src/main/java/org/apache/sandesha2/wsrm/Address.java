/*
 * Copyright  2005-2006 The Apache Software Foundation.
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
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;

/**
 * Represents an Address element, that is contained within the MakeConnection message.
 */

public class Address implements IOMRMElement {

	private String address = null;
	
	private String rmNamespaceValue = null;
	
	public Address(String rmNamespaceValue) {
		this.rmNamespaceValue = rmNamespaceValue;
	}

	public Object fromOMElement(OMElement element) throws OMException {

		OMElement addressPart = element.getFirstChildWithName(new QName(
				rmNamespaceValue, Sandesha2Constants.WSRM_COMMON.ADDRESS));
		if (addressPart == null)
			throw new OMException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.cannotFindAddressElement,
					element.toString()));
		
		String addressText = addressPart.getText();
		if (addressText == null || "".equals(addressText))
			throw new OMException(
					SandeshaMessageHelper.getMessage(
							SandeshaMessageKeys.cannotFindAddressText,
							element.toString()));

		this.address = addressText;
		return this;
	}

	public String getNamespaceValue(){
		return rmNamespaceValue;
	}

	public OMElement toOMElement(OMElement element) throws OMException {

		if (address == null ||  "".equals(address))
			throw new OMException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.addressNotValid));

		OMFactory factory = element.getOMFactory();
		
		OMNamespace rmNamespace = factory.createOMNamespace(rmNamespaceValue,Sandesha2Constants.WSRM_COMMON.NS_PREFIX_MC);
		OMElement addressElement = factory.createOMElement(Sandesha2Constants.WSRM_COMMON.ADDRESS, rmNamespace);
		
		addressElement.setText(address);
		element.addChild(addressElement);

		return element;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}
	
	public boolean isNamespaceSupported (String namespaceName) {
		if (Sandesha2Constants.SPEC_2007_02.MC_NS_URI.equals(namespaceName))
			return true;
		
		return false;
	}
}
