/*
 * Copyright 2006 The Apache Software Foundation.
 * Copyright 2006 International Business Machines Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.sandesha2.security;

import java.util.HashMap;
import java.util.Iterator;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisModule;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;

public class UnitTestSecurityManager extends SecurityManager {
	private static Log log = LogFactory.getLog(UnitTestSecurityManager.class);

	private static HashMap tokens = new HashMap();
	private static String secNamespace = Sandesha2Constants.SPEC_2005_02.SEC_NS_URI;
	private static QName unitTestHeader = new QName("http://unit.test.security", "tokenId");
	
	public UnitTestSecurityManager(ConfigurationContext context) {
		super(context);
		log.debug("UnitTestSecurityManager::UnitTestSecurityManager");
	}
	
	public void initSecurity(AxisModule moduleDesc) {
		log.debug("UnitTestSecurityManager::initSecurity");
	}

	public SecurityToken getSecurityToken(MessageContext message)
	{
		log.debug("Enter: UnitTestSecurityManager::getSecurityToken(MessageContext)");

		UnitTestSecurityToken result = new UnitTestSecurityToken(1); //use the same token for all messages in unit test
		tokens.put(getTokenRecoveryData(result), result);

		log.debug("Exit: UnitTestSecurityManager::getSecurityToken " + result);
		return result;
	}

	public SecurityToken getSecurityToken(OMElement theSTR,	MessageContext message)
	{
		log.debug("Enter: UnitTestSecurityManager::getSecurityToken(OMElement,MessageContext)");

		OMElement reference = theSTR.getFirstChildWithName(new QName(secNamespace, "Reference"));
		String securityTokenURI = reference.getAttributeValue(new QName("URI"));
		String key = securityTokenURI;
		SecurityToken result = (SecurityToken) tokens.get(key);
		
		log.debug("Exit: UnitTestSecurityManager::getSecurityToken " + result);
		return result;
	}

	public String getTokenRecoveryData(SecurityToken token)  {
		log.debug("Enter: UnitTestSecurityManager::getTokenRecoveryData");
		String key = ((UnitTestSecurityToken)token).getURI();
		log.debug("Exit: UnitTestSecurityManager::getTokenRecoveryData " + key);
		return key;
	}

	public SecurityToken recoverSecurityToken(String tokenData) {
		log.debug("Enter: UnitTestSecurityManager::recoverSecurityToken");
		SecurityToken result = (SecurityToken) tokens.get(tokenData);
		log.debug("Exit: UnitTestSecurityManager::recoverSecurityToken " + result);
		return result;
	}

	public void checkProofOfPossession(SecurityToken token, OMElement messagePart,
			MessageContext message) throws SandeshaException {
		log.debug("Enter: UnitTestSecurityManager::checkProofOfPossession");
		if(token == null) {
			throw new SandeshaException("Security manager was passed a null token");
		}
		
		// Look for the header that we should have introduced in the 'apply' method
		String key = ((UnitTestSecurityToken)token).getURI();
		boolean foundToken = false;
		SOAPEnvelope env = message.getEnvelope();
		SOAPHeader headers = env.getHeader();
		if(headers != null) {
			Iterator tokens = headers.getChildrenWithName(unitTestHeader);
			while(tokens.hasNext()) {
				OMElement myHeader = (OMElement) tokens.next();
				String text = myHeader.getText();
				if(key.equals(text)) {
					foundToken = true;
					break;
				}
			}
		}
		if(!foundToken) {
			SandeshaException e = new SandeshaException("Message was not secured with the correct token(s)");
			e.printStackTrace(System.err);
			throw e;
		}

		log.debug("Exit: UnitTestSecurityManager::checkProofOfPossession");
	}

	public OMElement createSecurityTokenReference(SecurityToken token, MessageContext message) {
		log.debug("Enter: UnitTestSecurityManager::createSecurityTokenReference");

		String uri = ((UnitTestSecurityToken)token).getURI();
		String type = ((UnitTestSecurityToken)token).getValueType();
		
		OMFactory factory = OMAbstractFactory.getOMFactory();
		OMNamespace secNS = factory.createOMNamespace(secNamespace, "wsse");
		OMElement str = factory.createOMElement("SecurityTokenReference", secNS);
		
		OMElement ref = factory.createOMElement("Reference", secNS);
		str.addChild(ref);
		
		OMAttribute uriAttr = factory.createOMAttribute("URI", null, uri);
		OMAttribute typeAttr = factory.createOMAttribute("ValueType", null, type);
		
		ref.addAttribute(uriAttr);
		ref.addAttribute(typeAttr);
		
		log.debug("Exit: UnitTestSecurityManager::createSecurityTokenReference " + str);
		return str;
	}

	public void applySecurityToken(SecurityToken token, MessageContext outboundMessage) throws SandeshaException {
		log.debug("Enter: UnitTestSecurityManager::applySecurityToken");
		if(token == null) {
			throw new SandeshaException("Security manager was passed a null token");
		}
		
		// Add the header that pretends to secure the message
		String key = ((UnitTestSecurityToken)token).getURI();
		SOAPEnvelope env = outboundMessage.getEnvelope();
		OMFactory factory = env.getOMFactory();

		SOAPHeader headers = env.getHeader();

		OMNamespace namespace = factory.createOMNamespace(unitTestHeader.getNamespaceURI(), "sec");
		OMElement header = headers.addHeaderBlock(unitTestHeader.getLocalPart(), namespace);
		header.setText(key);

		log.debug("Exit: UnitTestSecurityManager::applySecurityToken");
	}

}
