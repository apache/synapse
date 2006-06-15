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
 * Time: 3:36:59 AM
 * To change this template use File | Settings | File Templates.
 */
public class TerminateSequenceTest extends SandeshaTestCase {

	SOAPFactory factory = OMAbstractFactory.getSOAP11Factory();
	String rmNamespace = Sandesha2Constants.SPEC_2005_02.NS_URI;
	
    public TerminateSequenceTest() {
        super("TerminateSequenceTest");
    }

    public void testFromOMElement() throws SandeshaException {
        TerminateSequence terminateSequence =  new TerminateSequence(factory,rmNamespace);
        SOAPEnvelope env = getSOAPEnvelope("", "TerminateSequence.xml");
        terminateSequence.fromOMElement(env.getBody());

        Identifier identifier = terminateSequence.getIdentifier();
        assertEquals("uuid:59b0c910-1625-11da-bdfc-b09ed76a1f06", identifier.getIdentifier());
    }

    public void testToSOAPEnvelope() throws SandeshaException {
        TerminateSequence terminateSequence = new TerminateSequence(factory,rmNamespace);
        Identifier identifier = new Identifier(factory,rmNamespace);
        identifier.setIndentifer("uuid:59b0c910-1625-11da-bdfc-b09ed76a1f06");
        terminateSequence.setIdentifier(identifier);

        SOAPEnvelope env = getEmptySOAPEnvelope();
        terminateSequence.toSOAPEnvelope(env);

        OMElement terminateSeqPart = env.getBody().getFirstChildWithName(
                new QName(rmNamespace, Sandesha2Constants.WSRM_COMMON.TERMINATE_SEQUENCE));
        OMElement identifierPart = terminateSeqPart.getFirstChildWithName(
                new QName(rmNamespace, Sandesha2Constants.WSRM_COMMON.IDENTIFIER));
        assertEquals("uuid:59b0c910-1625-11da-bdfc-b09ed76a1f06", identifierPart.getText());
    }
}
