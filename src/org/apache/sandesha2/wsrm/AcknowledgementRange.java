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
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;

/**
 * Represents and AcknowledgementRange element.
 */

public class AcknowledgementRange implements IOMRMElement {
	
	private long upperValue;
	
	private long lowerValue;
	
	private String namespaceValue = null;
	
	public AcknowledgementRange(String namespaceValue) throws SandeshaException {
		if (!isNamespaceSupported(namespaceValue))
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.unknownSpec,
					namespaceValue));
		
		this.namespaceValue = namespaceValue;
	}

	public String getNamespaceValue() {
		return namespaceValue;
	}

	public Object fromOMElement(OMElement ackRangePart) throws OMException {

		if (ackRangePart == null)
			throw new OMException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.nullPassedElement));

		OMAttribute lowerAttrib = ackRangePart.getAttribute(new QName(
				Sandesha2Constants.WSRM_COMMON.LOWER));
		OMAttribute upperAttrib = ackRangePart.getAttribute(new QName(
				Sandesha2Constants.WSRM_COMMON.UPPER));

		if (lowerAttrib == null || upperAttrib == null)
			throw new OMException(
					SandeshaMessageHelper.getMessage(
							SandeshaMessageKeys.noUpperOrLowerAttributesInElement,
							ackRangePart.toString()));

		try {
			long lower = Long.parseLong(lowerAttrib.getAttributeValue());
			long upper = Long.parseLong(upperAttrib.getAttributeValue());
			upperValue = upper;
			lowerValue = lower;
		} catch (Exception ex) {
			throw new OMException(
					SandeshaMessageHelper.getMessage(
							SandeshaMessageKeys.ackRandDoesNotHaveCorrectValues,
							ackRangePart.toString()));
		}

		return this;
	}

	public OMElement toOMElement(OMElement sequenceAckElement)
			throws OMException {

		if (sequenceAckElement == null)
			throw new OMException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.cannotSetAckRangeNullElement));

		if (upperValue <= 0 || lowerValue <= 0 || lowerValue > upperValue)
			throw new OMException(
					SandeshaMessageHelper.getMessage(
							SandeshaMessageKeys.ackRandDoesNotHaveCorrectValues,
							upperValue + ":" + lowerValue));

		OMFactory factory = sequenceAckElement.getOMFactory();
		
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
		
		if (Sandesha2Constants.SPEC_2006_08.NS_URI.equals(namespaceName))
			return true;
		
		return false;
	}
}
