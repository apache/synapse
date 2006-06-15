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

public class FaultCode implements IOMRMElement {

	private String faultCode = null;
	
	private SOAPFactory defaultFactory;
	
	private String namespaceValue = null;
	
	public FaultCode(SOAPFactory factory, String namespaceValue) throws SandeshaException {
		if (!isNamespaceSupported(namespaceValue))
			throw new SandeshaException ("Unsupported namespace");
		
		this.defaultFactory = factory;
		this.namespaceValue = namespaceValue;
	}

	public String getNamespaceValue() {
		return namespaceValue;
	}

	public Object fromOMElement(OMElement sequenceFault) throws OMException {

		if (sequenceFault == null)
			throw new OMException("Can't add Fault Code part since the passed element is null");

		OMElement faultCodePart = sequenceFault
				.getFirstChildWithName(new QName(namespaceValue,
						Sandesha2Constants.WSRM_COMMON.FAULT_CODE));

		if (faultCodePart == null)
			throw new OMException("Passed element does not contain a Fauld Code part");

		this.faultCode = faultCodePart.getText();

		return sequenceFault;

	}

	public OMElement toOMElement(OMElement sequenceFault) throws OMException {

		if (sequenceFault == null)
			throw new OMException(
					"Can't add Fault Code part since the passed element is null");

		if (faultCode == null || faultCode == "")
			throw new OMException(
					"Cant add fault code since the the value is not set correctly.");

		OMFactory factory = sequenceFault.getOMFactory();
		if (factory==null)
			factory = defaultFactory;
		
		OMNamespace rmNamespace = factory.createOMNamespace(namespaceValue,Sandesha2Constants.WSRM_COMMON.NS_PREFIX_RM);
		OMElement faultCodeElement = factory.createOMElement(Sandesha2Constants.WSRM_COMMON.FAULT_CODE,rmNamespace);

		faultCodeElement.setText(faultCode);
		sequenceFault.addChild(faultCodeElement);

		return sequenceFault;
	}
    
    public void setFaultCode(String faultCode) {
        this.faultCode = faultCode;
    }
    
    public String getFaultCode() {
        return faultCode;
    }

	public boolean isNamespaceSupported (String namespaceName) {
		if (Sandesha2Constants.SPEC_2005_02.NS_URI.equals(namespaceName))
			return true;
		
		if (Sandesha2Constants.SPEC_2005_10.NS_URI.equals(namespaceName))
			return true;
		
		return false;
	}
}
