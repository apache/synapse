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

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;

/**
 * @author Chamikara Jayalath <chamikaramj@gmail.com>
 * @author Sanka Samaranayaka <ssanka@gmail.com>
 * @author Saminda Abeyruwan  <saminda@opensource.lk>
 */

public class AcknowledgementRange implements IOMRMElement {
	
	private long upperValue;
	
	private long lowerValue;
	
	private OMFactory defaultFactory;
	
	private String namespaceValue = null;
	
	public AcknowledgementRange(OMFactory factory, String namespaceValue) throws SandeshaException {
		if (!isNamespaceSupported(namespaceValue))
			throw new SandeshaException ("Unsupported namespace");
		
		this.defaultFactory = factory;
		this.namespaceValue = namespaceValue;
	}

	public String getNamespaceValue() {
		return namespaceValue;
	}

	public Object fromOMElement(OMElement ackRangePart) throws OMException {

		if (ackRangePart == null)
			throw new OMException("The passed element is null");

		OMAttribute lowerAttrib = ackRangePart.getAttribute(new QName(
				Sandesha2Constants.WSRM_COMMON.LOWER));
		OMAttribute upperAttrib = ackRangePart.getAttribute(new QName(
				Sandesha2Constants.WSRM_COMMON.UPPER));

		if (lowerAttrib == null || upperAttrib == null)
			throw new OMException(
					"Passed element does not contain upper or lower attributes");

		try {
			long lower = Long.parseLong(lowerAttrib.getAttributeValue());
			long upper = Long.parseLong(upperAttrib.getAttributeValue());
			upperValue = upper;
			lowerValue = lower;
		} catch (Exception ex) {
			throw new OMException(
					"The ack range does not have proper long values for Upper and Lower attributes");
		}

		return this;
	}

	public OMElement toOMElement(OMElement sequenceAckElement)
			throws OMException {

		if (sequenceAckElement == null)
			throw new OMException("Cant set Ack Range part since element is null");

		if (upperValue <= 0 || lowerValue <= 0 || lowerValue > upperValue)
			throw new OMException(
					"Cant set Ack Range part since Upper or Lower is not set to the correct value");

		OMFactory factory = sequenceAckElement.getOMFactory();
		if (factory==null)
			factory = defaultFactory;
		
		OMAttribute lowerAttrib = factory.createOMAttribute(
				Sandesha2Constants.WSRM_COMMON.LOWER, null, Long.toString(lowerValue));
		OMAttribute upperAttrib = factory.createOMAttribute(
				Sandesha2Constants.WSRM_COMMON.UPPER, null, Long.toString(upperValue));

		OMNamespace rmNamespace = factory.createOMNamespace(namespaceValue,Sandesha2Constants.WSRM_COMMON.NS_PREFIX_RM);
		OMElement acknowledgementRangeElement = factory.createOMElement(Sandesha2Constants.WSRM_COMMON.ACK_RANGE, rmNamespace);
		
		acknowledgementRangeElement.addAttribute(lowerAttrib);
		acknowledgementRangeElement.addAttribute(upperAttrib);
		sequenceAckElement.addChild(acknowledgementRangeElement);

		return sequenceAckElement;
	}

	public long getLowerValue() {
		return lowerValue;
	}

	public void setLowerValue(long lowerValue) {
		this.lowerValue = lowerValue;
	}

	public long getUpperValue() {
		return upperValue;
	}

	public void setUpperValue(long upperValue) {
		this.upperValue = upperValue;
	}
	
	public boolean isNamespaceSupported (String namespaceName) {
		if (Sandesha2Constants.SPEC_2005_02.NS_URI.equals(namespaceName))
			return true;
		
		if (Sandesha2Constants.SPEC_2005_10.NS_URI.equals(namespaceName))
			return true;
		
		return false;
	}
}