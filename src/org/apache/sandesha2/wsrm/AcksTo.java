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
import org.apache.axiom.soap.SOAPFactory;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;

/**
 * @author Chamikara Jayalath <chamikaramj@gmail.com>
 * @author Sanka Samaranayaka <ssanka@gmail.com>
 * @author Saminda Abeyruwan  <saminda@opensource.lk>
 */

public class AcksTo implements IOMRMElement {

	private Address address;
	
	private OMFactory defaultFactory;
	
	private String rmNamespaceValue = null;
	
	private String addressingNamespaceValue = null;

	public AcksTo(OMFactory factory,String rmNamespaceValue,String addressingNamespaceValue) throws SandeshaException {
		if (!isNamespaceSupported(rmNamespaceValue))
			throw new SandeshaException ("Unsupported namespace");
		
		this.defaultFactory = factory;
		this.rmNamespaceValue = rmNamespaceValue;
		this.addressingNamespaceValue = addressingNamespaceValue;
	}
	
	public AcksTo (Address address,SOAPFactory factory,String rmNamespaceValue, String addressingNamespaceValue) throws SandeshaException {
		this (factory,rmNamespaceValue,addressingNamespaceValue);
		this.address = address;
	}

	public String getNamespaceValue(){
		return rmNamespaceValue;
	}

	public Object fromOMElement(OMElement element) throws OMException,SandeshaException {
		OMElement acksToPart = element.getFirstChildWithName(new QName(
				rmNamespaceValue, Sandesha2Constants.WSRM_COMMON.ACKS_TO));

		if (acksToPart == null)
			throw new OMException("Passed element does not contain an acksTo part");

		address = new Address(defaultFactory,addressingNamespaceValue);
		address.fromOMElement(acksToPart);

		return this;
	}

	public OMElement toOMElement(OMElement element) throws OMException {

		if (address == null)
			throw new OMException("Cannot set AcksTo. Address is null");

		OMFactory factory = element.getOMFactory();
		if (factory==null)
			factory = defaultFactory;
		
		OMNamespace rmNamespace = factory.createOMNamespace(rmNamespaceValue,Sandesha2Constants.WSRM_COMMON.NS_PREFIX_RM);
		OMElement acksToElement = factory.createOMElement(Sandesha2Constants.WSRM_COMMON.ACKS_TO, rmNamespace);
		
		address.toOMElement(acksToElement);
		
		element.addChild(acksToElement);
		return element;
	}

	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}
	
	public boolean isNamespaceSupported (String namespaceName) {
		if (Sandesha2Constants.SPEC_2005_02.NS_URI.equals(namespaceName))
			return true;
		
		if (Sandesha2Constants.SPEC_2005_10.NS_URI.equals(namespaceName))
			return true;
		
		return false;
	}
}