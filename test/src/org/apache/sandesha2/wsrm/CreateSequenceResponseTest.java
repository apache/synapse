package org.apache.sandesha2.wsrm;

import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.SandeshaTestCase;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;

import javax.xml.namespace.QName;

public class CreateSequenceResponseTest extends SandeshaTestCase {

	SOAPFactory factory = OMAbstractFactory.getSOAP11Factory();
	String rmNamespaceValue = Sandesha2Constants.SPEC_2005_02.NS_URI;
	String addressingNamespaceValue = AddressingConstants.Final.WSA_NAMESPACE;
	
    public CreateSequenceResponseTest() {
        super("CreateSequenceResponseTest");

    }

    public void testFromOMElement() throws SandeshaException {
        CreateSequenceResponse res = new CreateSequenceResponse(factory,rmNamespaceValue,addressingNamespaceValue);
        SOAPEnvelope env = getSOAPEnvelope("", "CreateSequenceResponse.xml");
        res.fromOMElement(env.getBody());

        Identifier identifier = res.getIdentifier();
        assertEquals("uuid:88754b00-161a-11da-b6d6-8198de3c47c5", identifier.getIdentifier());

        Accept accept = res.getAccept();
        AcksTo  acksTo = accept.getAcksTo();
        Address address = acksTo.getAddress();
        assertEquals("http://localhost:8070/axis/services/TestService", address.getEpr().getAddress());

    }

    public void testToSOAPEnvelope()  throws SandeshaException {
        CreateSequenceResponse res = new CreateSequenceResponse(factory,rmNamespaceValue,addressingNamespaceValue);

        Identifier identifier = new Identifier(factory,rmNamespaceValue);
        identifier.setIndentifer("uuid:88754b00-161a-11da-b6d6-8198de3c47c5");
        res.setIdentifier(identifier);

        Accept accept = new Accept(factory,rmNamespaceValue,addressingNamespaceValue);
        AcksTo acksTo = new AcksTo(factory,rmNamespaceValue,addressingNamespaceValue);
        Address address = new Address(factory,addressingNamespaceValue);
        address.setEpr(new EndpointReference("http://localhost:8070/axis/services/TestService"));
        acksTo.setAddress(address);
        accept.setAcksTo(acksTo);
        res.setAccept(accept);

        SOAPEnvelope env = getEmptySOAPEnvelope();
        res.toSOAPEnvelope(env);

        OMElement createSeqResponsePart = env.getBody().getFirstChildWithName(
                new QName(rmNamespaceValue, Sandesha2Constants.WSRM_COMMON.CREATE_SEQUENCE_RESPONSE));
        OMElement identifierPart = createSeqResponsePart.getFirstChildWithName(
                new QName(rmNamespaceValue, Sandesha2Constants.WSRM_COMMON.IDENTIFIER));
        assertEquals("uuid:88754b00-161a-11da-b6d6-8198de3c47c5", identifierPart.getText());

        OMElement acceptPart = createSeqResponsePart.getFirstChildWithName(
                new QName(rmNamespaceValue, Sandesha2Constants.WSRM_COMMON.ACCEPT));
        OMElement acksToPart = acceptPart.getFirstChildWithName(
                new QName(rmNamespaceValue, Sandesha2Constants.WSRM_COMMON.ACKS_TO));
        OMElement addressPart = acksToPart.getFirstChildWithName(new QName(
				addressingNamespaceValue, Sandesha2Constants.WSA.ADDRESS));
        assertEquals("http://localhost:8070/axis/services/TestService", addressPart.getText());
    }
}
