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
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;

/**
 * This represent the wsrm:final element that may be present withing a sequence acknowledgement.
 */
public class AckFinal implements IOMRMElement {

	private OMFactory defaultfactory;
	
	private String namespaceValue = null;
	
	public AckFinal(OMFactory factory,String namespaceValue) throws SandeshaException {
		if (!isNamespaceSupported(namespaceValue))
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.unknownSpec,
					namespaceValue));
		
		this.defaultfactory = factory;
		this.namespaceValue = namespaceValue;
	}

	public String getNamespaceValue(){
		return namespaceValue;
	}

	public Object fromOMElement(OMElement element) throws OMException {
		
		OMFactory factory = element.getOMFactory();
		if (factory==null)
			factory = defaultfactory;
		
		OMElement finalPart = element.getFirstChildWithName(new QName(
				namespaceValue, Sandesha2Constants.WSRM_COMMON.FINAL));
		if (finalPart == null)
			throw new OMException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.noFinalPartInElement,
					element.toString()));

		return this;
	}

	public OMElement toOMElement(OMElement sequenceAckElement) throws OMException {
		//soapheaderblock element will be given

		OMFactory factory = sequenceAckElement.getOMFactory();
		if (factory==null)
			factory = defaultfactory;
		
		OMNamespace rmNamespace = factory.createOMNamespace(namespaceValue,Sandesha2Constants.WSRM_COMMON.NS_PREFIX_RM);
		OMElement finalElement = factory.createOMElement(Sandesha2Constants.WSRM_COMMON.FINAL, rmNamespace);
		sequenceAckElement.addChild(finalElement);

		return sequenceAckElement;
	}

	
	//this element is only supported in 2005_05_10 spec.
	public boolean isNamespaceSupported (String namespaceName) {
		if (Sandesha2Constants.SPEC_2005_02.NS_URI.equals(namespaceName))
			return false;
		
		if (Sandesha2Constants.SPEC_2005_10.NS_URI.equals(namespaceName))
			return true;
		
		return false;
	}
}
