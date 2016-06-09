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

package org.apache.synapse.mediators.transform;

import junit.framework.TestCase;

import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFault;
import org.apache.axiom.soap.SOAPFaultDetail;
import org.apache.synapse.MessageContext;
import org.apache.synapse.config.Entry;
import org.apache.synapse.mediators.TestUtils;
import org.apache.synapse.util.xpath.SynapseXPath;

import javax.xml.namespace.QName;
import java.net.URI;
import java.util.HashMap;

public class FaultMediatorTest extends TestCase {

    private static final QName F_CODE = new QName("http://namespace", "somefaultcode");
    private static final String F_STRING = "Some fault string";
    private static final String F_ACTOR_URI = "http://actor";
    private static final String F_DETAIL = "Some detail text";

    public void testSOAP11Fault() throws Exception {
        FaultMediator faultMediator = new FaultMediator();
        faultMediator.setSoapVersion(FaultMediator.SOAP11);
        faultMediator.setFaultCodeValue(F_CODE);
        faultMediator.setFaultReasonValue(F_STRING);
        faultMediator.setFaultRole(new URI(F_ACTOR_URI));
        faultMediator.setFaultDetail(F_DETAIL);

        // invoke transformation, with static envelope
        MessageContext synCtx = TestUtils.getAxis2MessageContext(
                "<empty/>", new HashMap<String, Entry>());
        faultMediator.mediate(synCtx);

        SOAPEnvelope envelope = synCtx.getEnvelope();
        SOAPFault fault = envelope.getBody().getFault();
        assertTrue(F_CODE.equals(fault.getCode().getTextAsQName()));
        assertTrue(F_STRING.equals(fault.getReason().getText()));
        assertTrue(F_ACTOR_URI.equals(fault.getRole().getRoleValue()));
        assertTrue(F_DETAIL.equals(fault.getDetail().getText()));
        assertEquals(SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI, envelope.getNamespace().getNamespaceURI());
    }

    public void testSOAP11FaultWithExpression() throws Exception {
        FaultMediator faultMediator = new FaultMediator();
        faultMediator.setSoapVersion(FaultMediator.SOAP11);
        faultMediator.setFaultCodeValue(F_CODE);
        faultMediator.setFaultReasonValue(F_STRING);
        faultMediator.setFaultRole(new URI(F_ACTOR_URI));
        faultMediator.setFaultDetailExpr(new SynapseXPath("get-property('foo')"));

        // invoke transformation, with static envelope
        MessageContext synCtx = TestUtils.getAxis2MessageContext(
                "<empty/>", new HashMap<String, Entry>());
        synCtx.setProperty("foo", "<test>some text</test>");
        faultMediator.mediate(synCtx);

        SOAPEnvelope envelope = synCtx.getEnvelope();
        SOAPFault fault = envelope.getBody().getFault();
        assertTrue(F_CODE.equals(fault.getCode().getTextAsQName()));
        assertTrue(F_STRING.equals(fault.getReason().getText()));
        assertTrue(F_ACTOR_URI.equals(fault.getRole().getRoleValue()));
        SOAPFaultDetail detail = fault.getDetail();
        assertNotNull(detail.getFirstElement());
        assertEquals("test", detail.getFirstElement().getLocalName());
        assertEquals("some text", detail.getFirstElement().getText());
    }

    public void testSOAP11FaultWithInvalidOutputExpression() throws Exception {
        FaultMediator faultMediator = new FaultMediator();
        faultMediator.setSoapVersion(FaultMediator.SOAP11);
        faultMediator.setFaultCodeValue(F_CODE);
        faultMediator.setFaultReasonValue(F_STRING);
        faultMediator.setFaultRole(new URI(F_ACTOR_URI));
        faultMediator.setFaultDetailExpr(new SynapseXPath("get-property('foo')"));

        // invoke transformation, with static envelope
        MessageContext synCtx = TestUtils.getAxis2MessageContext(
                "<empty/>", new HashMap<String, Entry>());
        synCtx.setProperty("foo", F_DETAIL);
        faultMediator.mediate(synCtx);

        SOAPEnvelope envelope = synCtx.getEnvelope();
        SOAPFault fault = envelope.getBody().getFault();
        assertTrue(F_CODE.equals(fault.getCode().getTextAsQName()));
        assertTrue(F_STRING.equals(fault.getReason().getText()));
        assertTrue(F_ACTOR_URI.equals(fault.getRole().getRoleValue()));
        SOAPFaultDetail detail = fault.getDetail();
        assertNull(detail.getFirstElement());
    }
}
