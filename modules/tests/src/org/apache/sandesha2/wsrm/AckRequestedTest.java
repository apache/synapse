/*
 * Copyright 2004,2005 The Apache Software Foundation.
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

package org.apache.sandesha2.wsrm;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.SandeshaTestCase;

public class AckRequestedTest extends SandeshaTestCase {

	SOAPFactory factory = OMAbstractFactory.getSOAP11Factory();
	String rmNamespace = Sandesha2Constants.SPEC_2005_02.NS_URI;
	
	public AckRequestedTest() {
		super("AckRequestedTest");
	}

	public void testFromOMElement() throws SandeshaException {
		QName name = new QName(rmNamespace, "AckRequested");
		AckRequested ackReq = new AckRequested(rmNamespace);
		SOAPEnvelope env = getSOAPEnvelope("", "AckRequested.xml");
		ackReq.fromOMElement(env.getHeader().getFirstChildWithName(name));
		
		Identifier identifier = ackReq.getIdentifier();
		assertEquals("uuid:897ee740-1624-11da-a28e-b3b9c4e71445", identifier.getIdentifier());
	}

	public void testToSOAPEnvelope()  throws SandeshaException {
		AckRequested ackReq = new AckRequested(rmNamespace);
		Identifier identifier = new Identifier(rmNamespace);
		identifier.setIndentifer("uuid:897ee740-1624-11da-a28e-b3b9c4e71445");
		ackReq.setIdentifier(identifier);
		
		SOAPEnvelope env = getEmptySOAPEnvelope();
		ackReq.toSOAPEnvelope(env);
		
		OMElement ackReqPart = env.getHeader().getFirstChildWithName(
				new QName(rmNamespace, Sandesha2Constants.WSRM_COMMON.ACK_REQUESTED));
		OMElement identifierPart = ackReqPart.getFirstChildWithName(
				new QName(rmNamespace, Sandesha2Constants.WSRM_COMMON.IDENTIFIER));
		assertEquals("uuid:897ee740-1624-11da-a28e-b3b9c4e71445", identifierPart.getText());
	}
}
