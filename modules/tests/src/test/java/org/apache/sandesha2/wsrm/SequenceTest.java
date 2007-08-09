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

public class SequenceTest extends SandeshaTestCase {

	SOAPFactory factory = OMAbstractFactory.getSOAP11Factory();
	String rmNamespace = Sandesha2Constants.SPEC_2005_02.NS_URI;
	
    public SequenceTest() {
        super("SequenceTest");

    }

    public void testFromOMElement()  throws SandeshaException {
        SOAPEnvelope env = getSOAPEnvelope("", "Sequence.xml");
        Sequence sequence = new Sequence(rmNamespace);
        sequence.fromOMElement(env.getHeader());

        Identifier identifier = sequence.getIdentifier();
        assertEquals("uuid:879da420-1624-11da-bed9-84d13db13902", identifier.getIdentifier());

        MessageNumber msgNo = sequence.getMessageNumber();
        assertEquals(1, msgNo.getMessageNumber());
    }

    public void testToSOAPEnvelope()  throws SandeshaException {
        Sequence sequence = new Sequence(rmNamespace);

        Identifier identifier = new Identifier(rmNamespace);
        identifier.setIndentifer("uuid:879da420-1624-11da-bed9-84d13db13902");
        sequence.setIdentifier(identifier);

        MessageNumber msgNo = new MessageNumber(rmNamespace);
        msgNo.setMessageNumber(1);
        sequence.setMessageNumber(msgNo);

        SOAPEnvelope envelope = getEmptySOAPEnvelope();
        sequence.toSOAPEnvelope(envelope);

        OMElement sequencePart = envelope.getHeader().getFirstChildWithName(
                new QName(rmNamespace, Sandesha2Constants.WSRM_COMMON.SEQUENCE));
        OMElement identifierPart = sequencePart.getFirstChildWithName(
                new QName(rmNamespace, Sandesha2Constants.WSRM_COMMON.IDENTIFIER));
        assertEquals("uuid:879da420-1624-11da-bed9-84d13db13902", identifierPart.getText());

        OMElement msgNumberPart = sequencePart.getFirstChildWithName(
				new QName (rmNamespace,Sandesha2Constants.WSRM_COMMON.MSG_NUMBER));
        assertEquals(1, Long.parseLong(msgNumberPart.getText()));
    }
}
