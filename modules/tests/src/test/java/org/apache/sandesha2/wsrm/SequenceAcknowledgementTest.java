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

import java.util.Iterator;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.SandeshaTestCase;
import org.apache.sandesha2.util.Range;

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
        	Range ackRange = (Range) iterator.next();
            if (ackRange.lowerValue == 1){
                assertEquals(2, ackRange.upperValue);

            } else if (ackRange.lowerValue == 4) {
                assertEquals(6, ackRange.upperValue);

            } else if (ackRange.lowerValue == 8) {
                assertEquals(10, ackRange.upperValue);
            }
        }

        iterator = sequenceAck.getNackList().iterator();
        while (iterator.hasNext()) {
            Long nack = (Long) iterator.next();
            if (nack.longValue() == 3) {

            } else if (nack.longValue() == 7) {

            } else {
                fail("invalid nack : " +  nack);
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
