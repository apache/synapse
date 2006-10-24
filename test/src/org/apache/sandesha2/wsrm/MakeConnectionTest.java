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
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.SandeshaTestCase;

public class MakeConnectionTest extends SandeshaTestCase {
	
	SOAPFactory factory = OMAbstractFactory.getSOAP11Factory();
	String rmNamespaceValue = Sandesha2Constants.SPEC_2006_08.NS_URI;
	String addressingNamespaceValue = AddressingConstants.Final.WSA_NAMESPACE;
	
    public MakeConnectionTest() {
        super("MakeConnectionTest");
    }
    
    public void testfromOMElement()  throws AxisFault {
        MakeConnection makeConnection = new MakeConnection(rmNamespaceValue);
        SOAPEnvelope envelope = getSOAPEnvelope("", "MakeConnection.xml");
        OMElement makeConnectionElement = envelope.getBody().getFirstChildWithName(new QName (rmNamespaceValue,Sandesha2Constants.WSRM_COMMON.MAKE_CONNECTION));
        makeConnection.fromOMElement(makeConnectionElement);

        Identifier identifier = makeConnection.getIdentifier();
        assertNotNull(identifier);
        assertEquals(identifier.getIdentifier(),"urn:uuid:6367739C8350488CD411576188379313");
        
        Address address = makeConnection.getAddress();
        assertNotNull(address);
        assertEquals(address.getAddress(),"http://docs.oasis-open.org/wsrx/wsrm/200608/anonymous?id=550e8400-e29b-11d4-a716-446655440000");

    }

    public void testToSOAPEnvelope()  throws SandeshaException {
        MakeConnection makeConnection = new MakeConnection (rmNamespaceValue);

        Address address = new Address (rmNamespaceValue);
        address.setAddress("http://docs.oasis-open.org/wsrx/wsrm/200608/anonymous?id=550e8400-e29b-11d4-a716-446655440000");
        Identifier identifier = new Identifier (rmNamespaceValue);
        identifier.setIndentifer("uuid:c3671020-15e0-11da-9b3b-f0439d4867bd");
        
        makeConnection.setAddress(address);
        makeConnection.setIdentifier(identifier);

        SOAPEnvelope envelope = getEmptySOAPEnvelope();
        makeConnection.toSOAPEnvelope(envelope);

        OMElement makeConnectionElement = envelope.getBody().getFirstChildWithName(
        		new QName (rmNamespaceValue,Sandesha2Constants.WSRM_COMMON.MAKE_CONNECTION));
        assertNotNull(makeConnectionElement);
        
        OMElement addressElement = makeConnectionElement.getFirstChildWithName(
        		new QName (rmNamespaceValue,Sandesha2Constants.WSA.ADDRESS));
        assertNotNull(addressElement);
        String addressValue = addressElement.getText();
        assertEquals(addressValue,"http://docs.oasis-open.org/wsrx/wsrm/200608/anonymous?id=550e8400-e29b-11d4-a716-446655440000");
        
        
        OMElement identifierElement = makeConnectionElement.getFirstChildWithName(
        		new QName (rmNamespaceValue,Sandesha2Constants.WSRM_COMMON.IDENTIFIER));
        assertNotNull(identifierElement);
        String identifierValue = identifierElement.getText();
        assertEquals(identifierValue,"uuid:c3671020-15e0-11da-9b3b-f0439d4867bd");
    }

}
