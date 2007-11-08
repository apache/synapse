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

package org.apache.sandesha2.security.dummy;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisModule;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.security.SecurityManager;
import org.apache.sandesha2.security.SecurityToken;

/**
 * A null implementation of a security manager. This manager cannot create tokens, so the
 * messages sent using this manager will not be secured.
 */
public class DummySecurityManager extends SecurityManager {

	public DummySecurityManager(ConfigurationContext context) {
		super(context);
	}
	
	public void initSecurity(AxisModule moduleDesc) {
	}

	public void checkProofOfPossession(SecurityToken token, OMElement messagePart, MessageContext message)
	throws SandeshaException
	{
		String msg = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.secureDummyNoProof);
		throw new SandeshaException(msg);
	}

	public SecurityToken getSecurityToken(MessageContext message) {
		// Returning null will tell the rest of sandesha to continue without attempting to secure the sequence.
		return null;
	}

	public String getTokenRecoveryData(SecurityToken token)
	throws SandeshaException
	{
		String msg = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.secureDummyNoToken);
		throw new SandeshaException(msg);
	}

	public SecurityToken recoverSecurityToken(String tokenData)
	throws SandeshaException
	{
		String msg = SandeshaMessageHelper.getMessage("secureDummyNoToken");
		throw new SandeshaException(msg);
	}


	public SecurityToken getSecurityToken(OMElement theSTR, MessageContext message)
	throws SandeshaException
	{
		String msg = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.secureDummyNoSTR);
		throw new SandeshaException(msg);
	}

	public OMElement createSecurityTokenReference(SecurityToken token, MessageContext message) throws SandeshaException {
		String msg = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.secureDummyNoSTR);
		throw new SandeshaException(msg);
	}

	public void applySecurityToken(SecurityToken token, MessageContext outboundMessage) throws SandeshaException {
		String msg = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.secureDummyNoSTR);
		throw new SandeshaException(msg);
	}


}
