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

import junit.framework.TestCase;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.synapse.MessageContext;
import org.apache.synapse.TestMessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.TestUtils;

import java.util.HashMap;

/**
 * 
 */
public class SynapseXPathTest extends TestCase {

    String message = "This is XPath test";    

    public void testAbsoluteXPath() throws Exception {
        SynapseXPath xpath = new SynapseXPath("//test");
        MessageContext ctx =  TestUtils.getTestContext("<test>" + message + "</test>");
        assertEquals(xpath.getStringValue(ctx), message);
    }

    public void testBodyRelativeXPath() throws Exception {
        SynapseXPath xpath = new SynapseXPath("$body/test");
        MessageContext ctx =  TestUtils.getTestContext("<test>" + message + "</test>");
        assertEquals(xpath.getStringValue(ctx), message);
    }

    public void testHeaderRelativeXPath() throws Exception {
        SynapseXPath xpath = new SynapseXPath("$header/t:test");
        xpath.addNamespace("t", "http://test");
        MessageContext ctx =  TestUtils.getTestContext("<test>" + message + "</test>");
        OMFactory fac = ctx.getEnvelope().getOMFactory();
        SOAPHeaderBlock block = ctx.getEnvelope().getHeader().addHeaderBlock("test",
            fac.createOMNamespace("http://test", "t"));
        block.setText(message);
        assertEquals(xpath.getStringValue(ctx), message);
    }

    public void testContextProperties() throws Exception {
        SynapseXPath xpath = new SynapseXPath("$ctx:test");
        MessageContext synCtx = new TestMessageContext();
        synCtx.setProperty("test", message);
        assertEquals(xpath.evaluate(synCtx), message);
    }

    public void testAxis2ContextProperties() throws Exception {
        SynapseXPath xpath = new SynapseXPath("$axis2:test");
        HashMap props = new HashMap();
        Axis2MessageContext synCtx = TestUtils.getAxis2MessageContext("<test/>", props);
        synCtx.getAxis2MessageContext().setProperty("test", message);
        synCtx.setProperty("test", message);
        assertEquals(xpath.evaluate(synCtx), message);
    }
    
}
