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
import org.apache.synapse.config.Entry;
import org.apache.synapse.mediators.TestUtils;
import org.apache.synapse.mediators.bsf.convertors.DefaultOMElementConvertor;
import org.apache.synapse.mediators.bsf.convertors.JSOMElementConvertor;
import org.apache.synapse.mediators.bsf.convertors.OMElementConvertor;
import org.apache.synapse.mediators.bsf.convertors.RBOMElementConvertor;

public class ScriptMediatorTest extends TestCase {

    private static final String inlinescript = "<x><![CDATA[ function mediate(mc) { return true;} ]]></x>";

    private static final String falsescript = "<x><![CDATA[ function mediate(mc) { return false;} ]]></x>";



    public void testTrueMediator() throws Exception {

        MessageContext mc = TestUtils.getTestContext("<foo/>", null);
        ScriptMediator mediator = new ScriptMediator("js",inlinescript);
        assertTrue(mediator.mediate(mc));
    }

    public void testFalseMediator() throws Exception {
        MessageContext mc = TestUtils.getTestContext("<foo/>", null);
        ScriptMediator mediator = new ScriptMediator("js",falsescript);
        assertTrue(mediator.mediate(mc));
    }

      
    public void testJSCreateOMElementConvertor() {
        ScriptMediator mediator = new ScriptMediator("js", "true;");
        OMElementConvertor convertor = mediator.getOMElementConvertor();
        assertTrue(convertor instanceof JSOMElementConvertor);
    }

//    public void testRBCreateOMElementConvertor() {
//        ScriptMediator mediator = new ScriptMediator("ruby", null);
//        OMElementConvertor convertor = mediator.getOMElementConvertor();
//        assertTrue(convertor instanceof RBOMElementConvertor);
//    }
//    
//    public void testDefaultCreateOMElementConvertor() {
//        ScriptMediator mediator = new ScriptMediator("foo.bar", null);
//        OMElementConvertor convertor = mediator.getOMElementConvertor();
//        assertTrue(convertor instanceof DefaultOMElementConvertor);
//    }

}
