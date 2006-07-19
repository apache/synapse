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
import org.apache.axis2.addressing.EndpointReference;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;

/**
 * Represents an Address element.
 */

public class Address implements IOMRMElement {

	private EndpointReference epr = null;
	
	private OMFactory defaultFactory;
	
	private String addressingNamespaceValue = null;
	
	public Address(OMFactory factory, String addressingNamespaceValue) {
		this.defaultFactory = factory;
		this.addressingNamespaceValue = addressingNamespaceValue;
	}
	
	public Address (EndpointReference epr,OMFactory factory,String addressingNamespaceValue) {
		this(factory,addressingNamespaceValue);
		this.epr = epr;
	}

	public Object fromOMElement(OMElement element) throws OMException {

		OMElement addressPart = element.getFirstChildWithName(new QName(
				addressingNamespaceValue, Sandesha2Constants.WSA.ADDRESS));
		if (addressPart == null)
			throw new OMException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.cannotFindAddressElement,
					element.toString()));
		String addressText = addressPart.getText();
		if (addressText == null || addressText == "")
			throw new OMException(
					SandeshaMessageHelper.getMessage(
							SandeshaMessageKeys.cannotFindAddressText,
							element.toString()));

		epr = new EndpointReference(addressText);
		return this;
	}

	public String getNamespaceValue(){
		return addressingNamespaceValue;
	}

	public OMElement toOMElement(OMElement element) throws OMException {

		if (epr == null || epr.getAddress() == null || epr.getAddress() == "")
			throw new OMException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.addressNotValid));

		OMFactory factory = element.getOMFactory();
		if (factory==null)
			factory = defaultFactory;
		
		OMNamespace addressingNamespace = factory.createOMNamespace(addressingNamespaceValue,Sandesha2Constants.WSA.NS_PREFIX_ADDRESSING);
		OMElement addressElement = factory.createOMElement(Sandesha2Constants.WSA.ADDRESS, addressingNamespace);
		
		addressElement.setText(epr.getAddress());
		element.addChild(addressElement);

		return element;
	}

	public EndpointReference getEpr() {
		return epr;
	}

	public void setEpr(EndpointReference epr) {
		this.epr = epr;
	}
	
	public boolean isNamespaceSupported (String namespaceName) {
		if (Sandesha2Constants.SPEC_2005_02.NS_URI.equals(namespaceName))
			return true;
		
		if (Sandesha2Constants.SPEC_2005_10.NS_URI.equals(namespaceName))
			return true;
		
		return false;
	}
}
