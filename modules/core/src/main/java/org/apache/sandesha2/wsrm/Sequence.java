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

import java.util.Iterator;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;

/**
 * Represents a Sequence element which get carried within a RM application 
 * message.
 * 
 * Either RM10 or RM11 namespace supported
 */
public class Sequence implements RMHeaderPart {

	private Identifier identifier;
	private long messageNumber;
	private boolean lastMessage = false;
	private String namespaceValue = null;
	private OMNamespace omNamespace = null;
	
	public Sequence(String namespaceValue) throws SandeshaException {
		this.namespaceValue = namespaceValue;
		if (Sandesha2Constants.SPEC_2005_02.NS_URI.equals(namespaceValue)) {
			omNamespace = Sandesha2Constants.SPEC_2005_02.OM_NS_URI;
		}else{
			omNamespace = Sandesha2Constants.SPEC_2007_02.OM_NS_URI;
		}
	}

	public String getNamespaceValue() {
		return namespaceValue;
	}

	public Object fromHeaderBlock(SOAPHeaderBlock shb) throws OMException,SandeshaException {
		if (shb == null)
			throw new OMException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.noSequencePartInElement));
		
		OMElement identifierPart = null;
		OMElement msgNumberPart = null;
		OMElement lastMessageElement = null;
		
		Iterator iter = shb.getChildElements();
		while(iter.hasNext()){
			OMElement child = (OMElement)iter.next();
			QName qn = child.getQName();
			if(namespaceValue.equals(qn.getNamespaceURI())){
				if(Sandesha2Constants.WSRM_COMMON.IDENTIFIER.equals(qn.getLocalPart())){
					identifierPart = child;
				}else if(Sandesha2Constants.WSRM_COMMON.MSG_NUMBER.equals(qn.getLocalPart())){
					msgNumberPart = child;
				}else if(Sandesha2Constants.WSRM_COMMON.LAST_MSG.equals(qn.getLocalPart())){
					lastMessageElement = child;
				}
			}
		}
		
		identifier = new Identifier(namespaceValue);
		identifier.fromOMElement(identifierPart);
		
		if (msgNumberPart==null)
			throw new OMException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.noMessageNumberPartInElement));
		
		messageNumber = Long.parseLong(msgNumberPart.getText());
		
		if(lastMessageElement != null){
			lastMessage = true;
		}

		// Indicate that we have processed this part of the message.
		shb.setProcessed();
    
		return this;
	}
	
	public Identifier getIdentifier() {
		return identifier;
	}

	public boolean getLastMessage() {
		return lastMessage;
	}

	public long getMessageNumber() {
		return messageNumber;
	}

	public void setIdentifier(Identifier identifier) {
		this.identifier = identifier;
	}

	public void setLastMessage(boolean lastMessage) {
		this.lastMessage = lastMessage;
	}

	public void setMessageNumber(long messageNumber) {
		this.messageNumber = messageNumber;
	}
	
	public void toHeader(SOAPHeader header){
		if (identifier == null)
			throw new OMException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.nullMsgId));
		if (messageNumber <= 0 ){
			throw new OMException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.setAValidMsgNumber,
					Long.toString(messageNumber)));
		}

		SOAPHeaderBlock sequenceHeaderBlock = header.addHeaderBlock(
				Sandesha2Constants.WSRM_COMMON.SEQUENCE, omNamespace);
		
		// Always set the MustUnderstand to true for Sequence messages 
		sequenceHeaderBlock.setMustUnderstand(true);
		identifier.toOMElement(sequenceHeaderBlock, omNamespace);
		
		OMElement messageNoElement = sequenceHeaderBlock.getOMFactory().createOMElement(Sandesha2Constants.WSRM_COMMON.MSG_NUMBER,omNamespace);
		messageNoElement.setText(Long.toString(messageNumber));
		sequenceHeaderBlock.addChild(messageNoElement);
		
		if (lastMessage){		
			OMElement lastMessageElement = sequenceHeaderBlock.getOMFactory().createOMElement(Sandesha2Constants.WSRM_COMMON.LAST_MSG, omNamespace);
			sequenceHeaderBlock.addChild(lastMessageElement);
		}
	}
}