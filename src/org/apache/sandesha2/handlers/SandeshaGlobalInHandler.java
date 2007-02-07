/*
 * Copyright 1999-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *  
 */

package org.apache.sandesha2.handlers;

import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.handlers.AbstractHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.wsrm.Sequence;

/**
 * The Global handler of Sandesha2. This is only used to check for WSRM 1.0 messages
 * that have a particular way of signalling the last message in a sequence. These
 * checks have to be done before dispatch.
 */

public class SandeshaGlobalInHandler extends AbstractHandler {

	private static final long serialVersionUID = -7187928423123306156L;

	private static final Log log = LogFactory.getLog(SandeshaGlobalInHandler.class);
	
	public InvocationResponse invoke(MessageContext msgContext) throws AxisFault {

		if (log.isDebugEnabled())
			log.debug("Enter: SandeshaGlobalInHandler::invoke, " + msgContext.getEnvelope().getHeader());

		// The only work that this handler needs to do is identify messages which
		// follow the WSRM 1.0 convention for sending 'LastMessage' when the sender
		// doesn't have a reliable message to piggyback the last message marker onto.
		// Normally they will identify this scenario with an action marker, but if
		// there is no action at all then we have to check the soap body.
		// Either way, all that this handler need do is set the action back onto
		// the message, so that the dispatchers can allow it to continue. The real
		// processing will be done in the SequenceProcessor.
		String soapAction = msgContext.getSoapAction();
		String wsaAction = msgContext.getWSAAction();
		if(soapAction == null && wsaAction == null) {
			// Look for a WSRM 1.0 sequence header with the lastMessage marker
			SOAPEnvelope env = msgContext.getEnvelope();
			if(env != null) {
				boolean lastMessageHeader = false;
				try {
					SOAPHeader header = env.getHeader();
					if(header != null) {
						Sequence sequence = new Sequence(Sandesha2Constants.SPEC_2005_02.NS_URI);
						sequence.fromOMElement(header);
						if(sequence.getLastMessage() != null) {
							lastMessageHeader = true;
						}
					}
				} catch(Exception e) {
					// Do nothing, we failed to find a Sequence header
				}
				if(lastMessageHeader) {
					SOAPBody body = env.getBody();
					if(body != null && body.getFirstElement() == null) {
						// There is an empty body so we know this is the kind of message
						// that we are looking for.
						if(log.isDebugEnabled()) log.debug("Setting SOAP Action for a WSRM 1.0 last message");
						msgContext.setSoapAction(Sandesha2Constants.SPEC_2005_02.Actions.SOAP_ACTION_LAST_MESSAGE);
					}
				}
			}

		}
		
		if (log.isDebugEnabled())
			log.debug("Exit: SandeshaGlobalInHandler::invoke, continuing");
		return InvocationResponse.CONTINUE;
	}
	
}
