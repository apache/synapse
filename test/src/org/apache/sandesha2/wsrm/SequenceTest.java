package org.apache.sandesha2.wsrm;

import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.SandeshaTestCase;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;

import javax.xml.namespace.QName;

/**
 * Created by IntelliJ IDEA.
 * User: sanka
 * Date: Oct 7, 2005
 * Time: 4:31:54 AM
 * To change this template use File | Settings | File Templates.
 */
public class SequenceTest extends SandeshaTestCase {

	SOAPFactory factory = OMAbstractFactory.getSOAP11Factory();
	String rmNamespace = Sandesha2Constants.SPEC_2005_02.NS_URI;
	
    public SequenceTest() {
        super("SequenceTest");

    }

    public void testFromOMElement()  throws SandeshaException {
        SOAPEnvelope env = getSOAPEnvelope("", "Sequence.xml");
        Sequence sequence = new Sequence(factory,rmNamespace);
        sequence.fromOMElement(env.getHeader());

        Identifier identifier = sequence.getIdentifier();
        assertEquals("uuid:879da420-1624-11da-bed9-84d13db13902", identifier.getIdentifier());

        MessageNumber msgNo = sequence.getMessageNumber();
        assertEquals(1, msgNo.getMessageNumber());
    }

    public void testToSOAPEnvelope()  throws SandeshaException {
        Sequence sequence = new Sequence(factory,rmNamespace);

        Identifier identifier = new Identifier(factory,rmNamespace);
        identifier.setIndentifer("uuid:879da420-1624-11da-bed9-84d13db13902");
        sequence.setIdentifier(identifier);

        MessageNumber msgNo = new MessageNumber(factory,rmNamespace);
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
