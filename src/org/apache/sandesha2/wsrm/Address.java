/*
 * Created on Sep 1, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
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
 * Represents an Address element.
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
		
		OMNamespace rmNamespace = factory.createOMNamespace(rmNamespaceValue,Sandesha2Constants.WSRM_COMMON.NS_PREFIX_RM);
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
		if (Sandesha2Constants.SPEC_2005_02.NS_URI.equals(namespaceName))
			return true;
		
		if (Sandesha2Constants.SPEC_2006_08.NS_URI.equals(namespaceName))
			return true;
		
		return false;
	}
}
