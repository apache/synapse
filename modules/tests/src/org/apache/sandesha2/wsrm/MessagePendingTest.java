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
import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.SandeshaTestCase;

public class MessagePendingTest extends SandeshaTestCase  {

	SOAPFactory factory = OMAbstractFactory.getSOAP11Factory();
	String mcNamespace = Sandesha2Constants.SPEC_2007_02.MC_NS_URI;
	
    public MessagePendingTest() {
        super("MessagePendingTest");
    }

    public void testFromOMElement()  throws SandeshaException {
    	
    	MessagePending messagePending = new MessagePending (mcNamespace);
    	SOAPEnvelope env = getSOAPEnvelope("", "MessagePending.xml");
    	
    	OMElement messagePendingElement = env.getHeader().getFirstChildWithName( 
    			new QName (mcNamespace,Sandesha2Constants.WSRM_COMMON.MESSAGE_PENDING));
    	messagePending.fromOMElement(messagePendingElement);
 
    	assertTrue(messagePending.isPending());
    }

    public void testToOMElement()  throws SandeshaException {
    	
    	MessagePending messagePending = new MessagePending (mcNamespace);
    	messagePending.setPending(true);
    	
    	
        SOAPEnvelope env = getEmptySOAPEnvelope();
        messagePending.toSOAPEnvelope(env);

        OMElement messagePendingElement = env.getHeader().getFirstChildWithName(
        		new QName (mcNamespace,Sandesha2Constants.WSRM_COMMON.MESSAGE_PENDING));
        assertNotNull(messagePendingElement);
        OMAttribute attribute = messagePendingElement.getAttribute(
        		new QName (Sandesha2Constants.WSRM_COMMON.PENDING));
        String value = attribute.getAttributeValue();
        assertEquals(value,"true");
        
    }
    
}
