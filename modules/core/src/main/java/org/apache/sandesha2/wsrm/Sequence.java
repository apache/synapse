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
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;

/**
 * Represents a Sequence element which get carried within a RM application 
 * message.
 */

public class Sequence implements IOMRMPart {

	private Identifier identifier;
	private MessageNumber messageNumber;
	private LastMessage lastMessage = null;
	private String namespaceValue = null;
	private OMNamespace omNamespace = null;
	
	public Sequence(String namespaceValue) throws SandeshaException {
		if (!isNamespaceSupported(namespaceValue))
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.unknownSpec,
					namespaceValue));
		
		this.namespaceValue = namespaceValue;
	}

	public String getNamespaceValue() {
		return namespaceValue;
	}

	public Object fromOMElement(OMElement ome) throws OMException,SandeshaException {
		SOAPHeaderBlock shb = (SOAPHeaderBlock)ome;

		if (shb == null)
			throw new OMException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.noSequencePartInElement,
					shb.toString()));
		
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
		messageNumber = new MessageNumber(namespaceValue);
		messageNumber.fromOMElement(msgNumberPart);
		if(lastMessageElement != null){
			lastMessage = new LastMessage(namespaceValue);
			lastMessage.fromOMElement(lastMessageElement);
		}

		// Indicate that we have processed this part of the message.
		shb.setProcessed();
    
		return this;
	}
	
	public OMElement toOMElement(OMElement headerElement) throws OMException {

		if (headerElement == null || !(headerElement instanceof SOAPHeader))
			throw new OMException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.seqElementCannotBeAddedToNonHeader));

		SOAPHeader soapHeader = (SOAPHeader) headerElement;

		if (identifier == null)
			throw new OMException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.nullMsgId));
		if (messageNumber == null)
			throw new OMException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.seqPartIsNull));

		SOAPHeaderBlock sequenceHeaderBlock = soapHeader.addHeaderBlock(
				Sandesha2Constants.WSRM_COMMON.SEQUENCE, omNamespace);
		
    // Always set the MustUnderstand to true for Sequence messages 
		sequenceHeaderBlock.setMustUnderstand(true);
		identifier.toOMElement(sequenceHeaderBlock, omNamespace);
		messageNumber.toOMElement(sequenceHeaderBlock, omNamespace);
		if (lastMessage != null)
			lastMessage.toOMElement(sequenceHeaderBlock);

		return headerElement;
	}

	public Identifier getIdentifier() {
		return identifier;
	}

	public LastMessage getLastMessage() {
		return lastMessage;
	}

	public MessageNumber getMessageNumber() {
		return messageNumber;
	}

	public void setIdentifier(Identifier identifier) {
		this.identifier = identifier;
	}

	public void setLastMessage(LastMessage lastMessage) {
		this.lastMessage = lastMessage;
	}

	public void setMessageNumber(MessageNumber messageNumber) {
		this.messageNumber = messageNumber;
	}

	public void toSOAPEnvelope(SOAPEnvelope envelope) {
		SOAPHeader header = envelope.getHeader();
		
		if (header==null) {
			SOAPFactory factory = (SOAPFactory)envelope.getOMFactory();
			header = factory.createSOAPHeader(envelope);
		}
		
		//detach if already exist.
		OMElement elem = header.getFirstChildWithName(new QName(namespaceValue,
				Sandesha2Constants.WSRM_COMMON.SEQUENCE));
		if (elem!=null)
			elem.detach();
		
		toOMElement(header);
	}
	
	public boolean isNamespaceSupported (String namespaceName) {
		if (Sandesha2Constants.SPEC_2005_02.NS_URI.equals(namespaceName)) {
			omNamespace = Sandesha2Constants.SPEC_2005_02.OM_NS_URI;
			return true;
		}
		
		if (Sandesha2Constants.SPEC_2007_02.NS_URI.equals(namespaceName)) {
			omNamespace = Sandesha2Constants.SPEC_2007_02.OM_NS_URI;
			return true;
		}
		
		return false;
	}

}
