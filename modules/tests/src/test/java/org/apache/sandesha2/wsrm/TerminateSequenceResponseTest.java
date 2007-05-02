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

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.sandesha2.Sandesha2Constants;

import junit.framework.TestCase;

public class TerminateSequenceResponseTest extends TestCase {

	SOAPFactory factory = OMAbstractFactory.getSOAP11Factory();
	String rmNamespaceValue = Sandesha2Constants.SPEC_2005_02.NS_URI;
	String addressingNamespaceValue = AddressingConstants.Final.WSA_NAMESPACE;
	
    public TerminateSequenceResponseTest() {
//        super("CreateSequenceResponseTest");

    }

    public void testFromOMElement() {
    	
    }

    public void testToSOAPEnvelope() {}
}
