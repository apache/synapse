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
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaTestCase;

public class CreateSequenceTest extends SandeshaTestCase {

	SOAPFactory factory = OMAbstractFactory.getSOAP11Factory();
	String rmNamespaceValue = Sandesha2Constants.SPEC_2005_02.NS_URI;
	String addressingNamespaceValue = Sandesha2Constants.SPEC_2005_02.ADDRESSING_NS_URI;
	
    public CreateSequenceTest() {
        super("CreateSequenceTest");
    }

    public void testfromOMElement()  throws AxisFault {
    	
        CreateSequence createSequence = new CreateSequence(rmNamespaceValue);
        createSequence.fromOMElement(getSOAPEnvelope("", "CreateSequence.xml").getBody());

        AcksTo acksTo = createSequence.getAcksTo();
        assertEquals("http://127.0.0.1:9090/axis/services/RMService", acksTo.getEPR().getAddress());

        SequenceOffer offer = createSequence.getSequenceOffer();
        Identifier identifier = offer.getIdentifer();
        assertEquals("uuid:c3671020-15e0-11da-9b3b-f0439d4867bd", identifier.getIdentifier());

    }

    public void testToSOAPEnvelope()  throws AxisFault {
        CreateSequence createSequence = new CreateSequence(rmNamespaceValue);

        EndpointReference epr = new EndpointReference("http://127.0.0.1:9090/axis/services/RMService");
        AcksTo acksTo = new AcksTo(epr, rmNamespaceValue, addressingNamespaceValue);
        createSequence.setAcksTo(acksTo);

        SequenceOffer sequenceOffer = new SequenceOffer(rmNamespaceValue);
        Identifier identifier = new Identifier(rmNamespaceValue);
        identifier.setIndentifer("uuid:c3671020-15e0-11da-9b3b-f0439d4867bd");
        sequenceOffer.setIdentifier(identifier);
        createSequence.setSequenceOffer(sequenceOffer);

        SOAPEnvelope envelope = getEmptySOAPEnvelope();
        createSequence.toSOAPEnvelope(envelope);

        OMElement createSequencePart = envelope.getBody().getFirstChildWithName(new QName(rmNamespaceValue,
                        Sandesha2Constants.WSRM_COMMON.CREATE_SEQUENCE));
        OMElement acksToPart = createSequencePart.getFirstChildWithName(new QName(
        		rmNamespaceValue, Sandesha2Constants.WSRM_COMMON.ACKS_TO));
		OMElement addressPart = acksToPart.getFirstChildWithName(new QName(
                addressingNamespaceValue, Sandesha2Constants.WSA.ADDRESS));
        assertEquals("http://127.0.0.1:9090/axis/services/RMService", addressPart.getText());

        OMElement offerPart = createSequencePart.getFirstChildWithName(
                new QName(rmNamespaceValue, Sandesha2Constants.WSRM_COMMON.SEQUENCE_OFFER));
        OMElement identifierPart = offerPart.getFirstChildWithName(
                new QName(rmNamespaceValue, Sandesha2Constants.WSRM_COMMON.IDENTIFIER));
        assertEquals("uuid:c3671020-15e0-11da-9b3b-f0439d4867bd", identifierPart.getText());
    }
}
