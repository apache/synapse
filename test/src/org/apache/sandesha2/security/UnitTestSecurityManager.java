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

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisModule;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;

public class UnitTestSecurityManager extends SecurityManager {

	private static HashMap tokens = new HashMap();
	private static int id = 0;
	private static String secNamespace = Sandesha2Constants.SPEC_2005_02.SEC_NS_URI;
	
	public UnitTestSecurityManager(ConfigurationContext context) {
		super(context);
	}
	
	public void initSecurity(AxisModule moduleDesc) {
	}

	public SecurityToken getSecurityToken(MessageContext message)
	throws SandeshaException
	{
		UnitTestSecurityToken result = new UnitTestSecurityToken(id++);
		tokens.put(getTokenRecoveryData(result), result);
		return result;
	}

	public SecurityToken getSecurityToken(OMElement theSTR,	MessageContext message)
	throws SandeshaException
	{
		OMElement reference = theSTR.getFirstChildWithName(new QName(secNamespace, "Reference"));
		String securityTokenURI = reference.getAttributeValue(new QName("URI"));
		String key = securityTokenURI;
		return (SecurityToken) tokens.get(key);
	}

	public String getTokenRecoveryData(SecurityToken token) throws SandeshaException {
		String key = ((UnitTestSecurityToken)token).getURI();
		return key;
	}

	public SecurityToken recoverSecurityToken(String tokenData)
			throws SandeshaException {
		return (SecurityToken) tokens.get(tokenData);
	}

	public void checkProofOfPossession(SecurityToken token, OMElement messagePart,
			MessageContext message) throws SandeshaException {
		if(token == null) {
			throw new SandeshaException("Security manager was passed a null token");
		}
	}

	public OMElement createSecurityTokenReference(SecurityToken token, MessageContext message) throws SandeshaException {
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
		
		return str;
	}


}
