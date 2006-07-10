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
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;

/**
 * Represents the RM Accept element which may come within the 
 * Create Sequence Response.
 */

public class Accept implements IOMRMElement {

	private AcksTo acksTo;
	
	private OMFactory defaultFactory;
	
	private String rmNamespaceValue;
	
	private String addressingNamespaceValue;


	public Accept(OMFactory factory, String rmNamespaceValue, String addressingNamespaceValue) throws SandeshaException {
		if (!isNamespaceSupported(rmNamespaceValue))
			throw new SandeshaException ("Unsupported namespace");
		
		this.defaultFactory = factory;
		this.addressingNamespaceValue = addressingNamespaceValue;
		this.rmNamespaceValue = rmNamespaceValue;
	}

	public String getNamespaceValue(){
		return rmNamespaceValue;
	}

	public Object fromOMElement(OMElement element) throws OMException,SandeshaException {

		OMFactory factory = element.getOMFactory();
		if (factory==null)
			factory = defaultFactory;
		
		OMElement acceptPart = element.getFirstChildWithName(new QName(
				rmNamespaceValue, Sandesha2Constants.WSRM_COMMON.ACCEPT));
		if (acceptPart == null)
			throw new OMException("Passed element does not contain an Accept part");

		acksTo = new AcksTo(defaultFactory,rmNamespaceValue,addressingNamespaceValue);
		acksTo.fromOMElement(acceptPart);

		return this;
	}

	public OMElement toOMElement(OMElement element) throws OMException {

		OMFactory factory = element.getOMFactory();
		if (factory==null)
			factory = defaultFactory;
		
		if (acksTo == null)
			throw new OMException("Cant add Accept part since AcksTo object is null");

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
		
		if (Sandesha2Constants.SPEC_2005_10.NS_URI.equals(namespaceName))
			return true;
		
		return false;
	}
}