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

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.axiom.om.OMElement;
import org.apache.synapse.MessageContext;
import org.apache.synapse.config.Property;
import org.apache.synapse.mediators.TestUtils;
import org.apache.synapse.mediators.bsf.convertors.DefaultOMElementConvertor;
import org.apache.synapse.mediators.bsf.convertors.JSOMElementConvertor;
import org.apache.synapse.mediators.bsf.convertors.OMElementConvertor;
import org.apache.synapse.mediators.bsf.convertors.RBOMElementConvertor;

public class ScriptMediatorTest extends TestCase {

    private static final OMElement TRUE_MEDIATOR = TestUtils.createOMElement(
       "<x><![CDATA[ function mediate(mc) { return true;} ]]></x>");

    private static final OMElement FALSE_MEDIATOR = TestUtils.createOMElement(
       "<x><![CDATA[ function mediate(mc) { return false;} ]]></x>");

    private static final OMElement XML_MEDIATOR = TestUtils.createOMElement(
       "<x><![CDATA[ function mediate(mc) { return 'xml' == (typeof mc.getPayloadXML());} ]]></x>");

    public void testTrueMediator() throws Exception {

        Property prop = new Property();
        prop.setValue(TRUE_MEDIATOR);
        prop.setSrc(new URL("http://MyMediator.js"));
        Map props = new HashMap();
        props.put("TRUE_MEDIATOR", prop);
        MessageContext mc = TestUtils.getTestContext("<foo/>", props);

        ScriptMediator mediator = new ScriptMediator("TRUE_MEDIATOR", "mediate");
        assertTrue(mediator.mediate(mc));
    }

    public void testFalseMediator() throws Exception {
        Property prop = new Property();
        prop.setValue(FALSE_MEDIATOR);
        prop.setSrc(new URL("http://MyFooMediator.js"));
        Map props = new HashMap();
        props.put("FALSE_MEDIATOR", prop);
        MessageContext mc = TestUtils.getTestContext("<foo/>", props);

        ScriptMediator mediator = new ScriptMediator("FALSE_MEDIATOR", "mediate");
        assertFalse(mediator.mediate(mc));
    }

    public void testXMLMediator() throws Exception {

        Property prop = new Property();
        prop.setValue(XML_MEDIATOR);
        prop.setSrc(new URL("http://MyFooMediator.js"));
        Map props = new HashMap();
        props.put("XML_MEDIATOR", prop);
        MessageContext mc = TestUtils.getTestContext("<foo/>", props);

        ScriptMediator mediator = new ScriptMediator("XML_MEDIATOR", "mediate");
        assertTrue(mediator.mediate(mc));
    }
    
    public void testJSCreateOMElementConvertor() {
        ScriptMediator mediator = new ScriptMediator(null, null);
        OMElementConvertor convertor = mediator.createOMElementConvertor("foo.js");
        assertTrue(convertor instanceof JSOMElementConvertor);
    }

    public void testRBCreateOMElementConvertor() {
        ScriptMediator mediator = new ScriptMediator(null, null);
        OMElementConvertor convertor = mediator.createOMElementConvertor("foo.rb");
        assertTrue(convertor instanceof RBOMElementConvertor);
    }
    
    public void testDefaultCreateOMElementConvertor() {
        ScriptMediator mediator = new ScriptMediator(null, null);
        OMElementConvertor convertor = mediator.createOMElementConvertor("foo.bla");
        assertTrue(convertor instanceof DefaultOMElementConvertor);
    }

}
