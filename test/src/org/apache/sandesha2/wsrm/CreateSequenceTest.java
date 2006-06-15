package org.apache.sandesha2.wsrm;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.SandeshaTestCase;

/**
 * Created by IntelliJ IDEA.
 * User: sanka
 * Date: Oct 6, 2005
 * Time: 10:39:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class CreateSequenceTest extends SandeshaTestCase {

	SOAPFactory factory = OMAbstractFactory.getSOAP11Factory();
	String rmNamespaceValue = Sandesha2Constants.SPEC_2005_02.NS_URI;
	String addressingNamespaceValue = AddressingConstants.Final.WSA_NAMESPACE;
	
    public CreateSequenceTest() {
        super("CreateSequenceTest");
    }

    public void testfromOMElement()  throws SandeshaException {
        CreateSequence createSequence = new CreateSequence(factory,rmNamespaceValue,addressingNamespaceValue);
        createSequence.fromOMElement(getSOAPEnvelope("", "CreateSequence.xml").getBody());

        AcksTo acksTo = createSequence.getAcksTo();
        Address address = acksTo.getAddress();
        assertEquals("http://127.0.0.1:9090/axis/services/RMService", address.getEpr().getAddress());

        SequenceOffer offer = createSequence.getSequenceOffer();
        Identifier identifier = offer.getIdentifer();
        assertEquals("uuid:c3671020-15e0-11da-9b3b-f0439d4867bd", identifier.getIdentifier());

    }

    public void testToSOAPEnvelope()  throws SandeshaException {
        CreateSequence createSequence = new CreateSequence(factory,rmNamespaceValue,addressingNamespaceValue);

        AcksTo acksTo = new AcksTo(factory,rmNamespaceValue,addressingNamespaceValue);
        Address address = new Address(factory,addressingNamespaceValue);
        address.setEpr(new EndpointReference("http://127.0.0.1:9090/axis/services/RMService"));
        acksTo.setAddress(address);
        createSequence.setAcksTo(acksTo);

        SequenceOffer sequenceOffer = new SequenceOffer(factory,rmNamespaceValue);
        Identifier identifier = new Identifier(factory,rmNamespaceValue);
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
