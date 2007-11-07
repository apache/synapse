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

import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.SandeshaTestCase;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;

import javax.xml.namespace.QName;

public class TerminateSequenceTest extends SandeshaTestCase {

	SOAPFactory factory = OMAbstractFactory.getSOAP11Factory();
	String rmNamespace = Sandesha2Constants.SPEC_2007_02.NS_URI;
	
    public TerminateSequenceTest() {
        super("TerminateSequenceTest");
    }

    
    
    public void testFromOMElement() throws SandeshaException {
        TerminateSequence terminateSequence =  new TerminateSequence(rmNamespace);
        SOAPEnvelope env = getSOAPEnvelope("", "TerminateSequence.xml");
        terminateSequence.fromOMElement(env.getBody());

        Identifier identifier = terminateSequence.getIdentifier();
        assertEquals("uuid:59b0c910-1625-11da-bdfc-b09ed76a1f06", identifier.getIdentifier());
        assertEquals(1, terminateSequence.getLastMessageNumber().getMessageNumber());
    }

    public void testToSOAPEnvelope() throws SandeshaException {
        TerminateSequence terminateSequence = new TerminateSequence(rmNamespace);
        Identifier identifier = new Identifier(rmNamespace);
        identifier.setIndentifer("uuid:59b0c910-1625-11da-bdfc-b09ed76a1f06");
        terminateSequence.setIdentifier(identifier);

        if(TerminateSequence.isLastMsgNumberRequired(Sandesha2Constants.SPEC_2007_02.NS_URI)){
        	LastMessageNumber lastMessageNumber = new LastMessageNumber(Sandesha2Constants.SPEC_2007_02.NS_URI);
        	lastMessageNumber.setMessageNumber(1);
        	terminateSequence.setLastMessageNumber(lastMessageNumber);
        }
        
        SOAPEnvelope env = getEmptySOAPEnvelope();
        terminateSequence.toSOAPEnvelope(env);

        OMElement terminateSeqPart = env.getBody().getFirstChildWithName(
                new QName(rmNamespace, Sandesha2Constants.WSRM_COMMON.TERMINATE_SEQUENCE));
        OMElement identifierPart = terminateSeqPart.getFirstChildWithName(
                new QName(rmNamespace, Sandesha2Constants.WSRM_COMMON.IDENTIFIER));
        assertEquals("uuid:59b0c910-1625-11da-bdfc-b09ed76a1f06", identifierPart.getText());
    }
}
