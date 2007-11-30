/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.sandesha2.wsrm;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;

/**
 * Represents an MessageNumber element.
 */

public class MessageNumber {
	
	private long messageNumber;
	
	private String namespaceValue = null;
	
	public MessageNumber(String namespaceValue) throws SandeshaException {
		if (!isNamespaceSupported(namespaceValue))
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.unknownSpec,
					namespaceValue));
		
		this.namespaceValue = namespaceValue;
	}
	
	public long getMessageNumber(){
		return messageNumber;
	}
	public void setMessageNumber(long messageNumber){
		this.messageNumber = messageNumber;
	}
	
	public Object fromOMElement(OMElement msgNumberPart) throws OMException {
		if (msgNumberPart==null)
			throw new OMException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.noMessageNumberPartInElement));
		
		String msgNoStr = msgNumberPart.getText();
		messageNumber = Long.parseLong(msgNoStr);
		return this;
	}
	
	public OMElement toOMElement(OMElement element, OMNamespace rmNamespace) throws OMException {
		if (messageNumber <= 0 ){
			throw new OMException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.setAValidMsgNumber,
					Long.toString(messageNumber)));
		}
		
		OMFactory factory = element.getOMFactory();
		
		OMElement messageNoElement = factory.createOMElement(Sandesha2Constants.WSRM_COMMON.MSG_NUMBER,rmNamespace);
		messageNoElement.setText(Long.toString(messageNumber));
		element.addChild(messageNoElement);
		
		return element;
	}
	
	public String getNamespaceValue() throws OMException {
		return namespaceValue;
	}

	public boolean isNamespaceSupported (String namespaceName) {
		if (Sandesha2Constants.SPEC_2005_02.NS_URI.equals(namespaceName))
			return true;
		
		if (Sandesha2Constants.SPEC_2007_02.NS_URI.equals(namespaceName))
			return true;
		
		return false;
	}

}
