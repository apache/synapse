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
 
package org.apache.sandesha2.security;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisModule;
import org.apache.sandesha2.SandeshaException;

/**
 * SecurityManager interface. This manages the link between the RM layer and Security,
 * so that the CreateSequence message can be secured using a SecurityTokenReference.
 * Once the sequence is secured using the STR, each inbound messgae must be checked to
 * ensure the sender has demonstrated proof-of-possession of the referenced token.
 * 
 * See the WS-RM 1.0 spec for details.
 */
public abstract class SecurityManager {
	
	public SecurityManager(ConfigurationContext context) {
		
	}

	/**
	 * Initialize the security manager. This may perfom setup such as checking the set
	 * of sequences that have been persisted over restart, and bootstrapping their
	 * security state.
	 */
	public abstract void initSecurity(AxisModule moduleDesc);
	
	/**
	 * Get a security token to secure an outbound create sequence message. This token
	 * will be the one referenced with the body of the CreateSequence. This method may
	 * return null, in which case the CreateSequence message will not contain the
	 * SecurityTokenReference.
	 */
	public abstract SecurityToken getSecurityToken(MessageContext message)
	throws SandeshaException;
	
	/**
	 * Get a security token, from a SecurityTokenReference within an inbound message.
	 * This method should not return null, so if the Token cannot be found the implementation
	 * should throw an exception.
	 */
	public abstract SecurityToken getSecurityToken(OMElement theSTR, MessageContext message)
	throws SandeshaException;
	
	/**
	 * Create a SecurityTokenReference, suitable for inclusion in the given message.
	 * The imelementation may choose to insert security headers into the SOAP envelope at
	 * this point, or it may choose to simple place some state into the message context
	 * and defer the real work until the security handlers execute. 
	 */
	public abstract OMElement createSecurityTokenReference(SecurityToken token, MessageContext message)
	throws SandeshaException;
	
	/**
	 * Check that the given element of the message demonstrated proof of possession of
	 * the given token. This allows Sandesha to implement the checking required by the
	 * RM spec. Proof is normally demonstrated by signing or encrypting the the given
	 * part using the token.
	 * If the elements is not secured with the given token the SecurityManager must
	 * throw an exception. 
	 */
	public abstract void checkProofOfPossession(SecurityToken token, OMElement messagePart, MessageContext message)
	throws SandeshaException;

	/**
	 * Write the data from this token in to a String. This is here to simplify storing
	 * token data into the storage layer - rather than rely on Java serialization we
	 * use this method, and the matching SecurityManager method to rebuild the token
	 * object. 
	 */
	public abstract String getTokenRecoveryData(SecurityToken token)
	throws SandeshaException;

	/**
	 * Reconstruct a token from a String. This method should not return null - if the
	 * security manager is unable to recover the token from the correlation data then
	 * it should throw an exception.
	 */
	public abstract SecurityToken recoverSecurityToken(String tokenData)
	throws SandeshaException;

	/**
	 * Ensure that the given token will be associated with an outbound message.
	 * This gives the SecurityManager implementation an opportunity to decorate
	 * the message context with properties that will then be used by the security
	 * handlers.
	 */
	public abstract void applySecurityToken(SecurityToken token, MessageContext outboundMessage)
	throws SandeshaException;
}
