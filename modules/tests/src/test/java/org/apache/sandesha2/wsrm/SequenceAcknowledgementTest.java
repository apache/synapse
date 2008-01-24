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
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;

import javax.xml.namespace.QName;
import java.util.Iterator;

public class SequenceAcknowledgementTest extends SandeshaTestCase {

	SOAPFactory factory = OMAbstractFactory.getSOAP11Factory();
	String rmNamespace = Sandesha2Constants.SPEC_2005_02.NS_URI;
	
    public SequenceAcknowledgementTest() {
        super("SequenceAcknowledgementTest");
    }

    public void testFromOMElement()  throws SandeshaException {
    	  QName name = new QName(rmNamespace, "SequenceAcknowledgement");
        SequenceAcknowledgement sequenceAck = new SequenceAcknowledgement(rmNamespace);
        SOAPEnvelope env = getSOAPEnvelope("", "SequenceAcknowledgement.xml");
        sequenceAck.fromHeaderBlock((SOAPHeaderBlock) env.getHeader().getFirstChildWithName(name));

        Identifier identifier = sequenceAck.getIdentifier();
        assertEquals("uuid:897ee740-1624-11da-a28e-b3b9c4e71445", identifier.getIdentifier());

        Iterator iterator = sequenceAck.getAcknowledgementRanges().iterator();
        while (iterator.hasNext()) {
            AcknowledgementRange ackRange = (AcknowledgementRange) iterator.next();
            if (ackRange.getLowerValue() == 1){
                assertEquals(2, ackRange.getUpperValue());

            } else if (ackRange.getLowerValue() == 4) {
                assertEquals(6, ackRange.getUpperValue());

            } else if (ackRange.getLowerValue() == 8) {
                assertEquals(10, ackRange.getUpperValue());
            }
        }

        iterator = sequenceAck.getNackList().iterator();
        while (iterator.hasNext()) {
            Nack nack = (Nack) iterator.next();
            if (nack.getNackNumber() == 3) {

            } else if (nack.getNackNumber() == 7) {

            } else {
                fail("invalid nack : " +  nack.getNackNumber());
            }
        }


    }

    public void testToOMElement()  throws Exception {
        SequenceAcknowledgement seqAck = new SequenceAcknowledgement(rmNamespace);
        Identifier identifier = new Identifier(rmNamespace);
        identifier.setIndentifer("uuid:897ee740-1624-11da-a28e-b3b9c4e71445");
        seqAck.setIdentifier(identifier);

        SOAPEnvelope env = getEmptySOAPEnvelope();
        seqAck.toHeader(env.getHeader());

        OMElement sequenceAckPart = env.getHeader().getFirstChildWithName(
                new QName(rmNamespace, Sandesha2Constants.WSRM_COMMON.SEQUENCE_ACK));
        OMElement identifierPart = sequenceAckPart.getFirstChildWithName(
                new QName(rmNamespace, Sandesha2Constants.WSRM_COMMON.IDENTIFIER));
        assertEquals("uuid:897ee740-1624-11da-a28e-b3b9c4e71445", identifierPart.getText());




    }
}
