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

package org.apache.synapse.mediators.bsf;

import junit.framework.TestCase;

import org.apache.axiom.om.OMElement;
import org.apache.synapse.mediators.TestUtils;
import org.apache.synapse.mediators.bsf.convertors.DefaultOMElementConvertor;

public class ScriptMessageContextTest extends TestCase {
    
    private static final String ENV = 
        "<?xml version='1.0' encoding='utf-8'?><soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Header /><soapenv:Body><foo /></soapenv:Body></soapenv:Envelope>";

    public void testGetPayloadXML() throws Exception {
        ScriptMessageContext smc = new ScriptMessageContext(TestUtils.getTestContext("<foo/>"), new DefaultOMElementConvertor());
        assertEquals("<foo />", smc.getPayloadXML());
    }

    public void testSetPayloadXML() throws Exception {
        ScriptMessageContext smc = new ScriptMessageContext(TestUtils.getTestContext("<foo/>"), new DefaultOMElementConvertor());
        smc.setPayloadXML("<petra/>");
        OMElement payload = smc.getEnvelope().getBody().getFirstElement();
        assertEquals("<petra />", payload.toString());
    }

    public void testGetEnvelopeXML() throws Exception {
        ScriptMessageContext smc = new ScriptMessageContext(TestUtils.getTestContext("<foo/>"), new DefaultOMElementConvertor());
        assertEquals(ENV, smc.getEnvelopeXML());
    }

}
