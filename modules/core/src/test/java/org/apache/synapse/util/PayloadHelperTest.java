/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.synapse.util;

import java.io.StringReader;

import junit.framework.TestCase;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.OMXMLBuilderFactory;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;

public class PayloadHelperTest extends TestCase {
    /**
     * Tests {@link PayloadHelper#setXMLPayload(SOAPEnvelope, OMElement)} with a SOAP envelope that
     * has not been built completely.
     */
    public void testSetXMLPayloadWithIncompleteEnvelope() {
        SOAPFactory factory = OMAbstractFactory.getSOAP11Factory();
        SOAPEnvelope orgEnvelope = factory.getDefaultEnvelope();
        OMNamespace ns = factory.createOMNamespace("urn:test", "p");
        factory.createOMElement("test", ns, orgEnvelope.getBody()).setText("test");
        String message = orgEnvelope.toString();
        SOAPEnvelope envelope = OMXMLBuilderFactory.createSOAPModelBuilder(
                new StringReader(message)).getSOAPEnvelope();
        OMElement newPayload = factory.createOMElement("test2", ns);
        newPayload.setText("test2");
        PayloadHelper.setXMLPayload(envelope, newPayload);
        assertSame(newPayload, envelope.getBody().getFirstOMChild());
        assertNull(newPayload.getNextOMSibling());
    }
}
