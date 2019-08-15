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
import junit.framework.Test;
import junit.framework.TestSuite;
import junit.extensions.RepeatedTest;

import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.TestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ScriptMediatorTest extends TestCase {

    private static final String inlinescript = "var state=5;";

    private String threadsafetyscript = "var rno = mc.getPayloadXML().toString(); rno=rno*2; mc.setPayloadXML" +
            "(<randomNo>{rno}</randomNo>)";

    public void testInlineMediator() throws Exception {
        MessageContext mc = TestUtils.getTestContext("<foo/>", null);
        ScriptMediator mediator = new ScriptMediator("js", inlinescript,null);
        assertTrue(mediator.mediate(mc));
    }

    public void testThreadSafety() throws Exception {
        MessageContext mc = TestUtils.getTestContext("<randomNo/>", null);
        Random rand = new Random();
        String randomno = Integer.toString(rand.nextInt(200));
        mc.getEnvelope().getBody().getFirstElement().setText(randomno);
        ScriptMediator mediator = new ScriptMediator("js", threadsafetyscript,null);
        mediator.mediate(mc);
        assertEquals(Integer.parseInt(mc.getEnvelope().getBody().getFirstElement().getText()),
                Integer.parseInt(randomno) * 2);
    }

    public void testSetProperty() throws Exception {
        MessageContext mc = TestUtils.getAxis2MessageContext("<empty/>", null);

        // For default scope
        String script = "mc.setProperty(\"PROP_DEFAULT\", \"PROP_DEFAULT_VAL\");";
        ScriptMediator mediator = new ScriptMediator("js", script, null);
        mediator.mediate(mc);
        assertEquals("PROP_DEFAULT_VAL", mc.getProperty("PROP_DEFAULT").toString());

        // For Axis2 scope
        script = "mc.setProperty(\"PROP_AXIS2\", \"PROP_AXIS_VAL\", \"axis2\");";
        mediator = new ScriptMediator("js", script, null);
        mediator.mediate(mc);
        Axis2MessageContext axis2smc = (Axis2MessageContext) mc;
        org.apache.axis2.context.MessageContext axis2MessageCtx = axis2smc.getAxis2MessageContext();
        assertEquals("PROP_AXIS_VAL", axis2MessageCtx.getProperty("PROP_AXIS2").toString());

        // For Transport scope
        script = "mc.setProperty(\"PROP_TRP\", \"PROP_TRP_VAL\", \"transport\");";
        mediator = new ScriptMediator("js", script, null);
        mediator.mediate(mc);
        Object headers = axis2MessageCtx.getProperty(
                org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);

        String trpHeader = null;
        if (headers != null && headers instanceof Map) {
            Map headersMap = (Map) headers;
            trpHeader = (String) headersMap.get("PROP_TRP");
        }
        assertEquals("PROP_TRP_VAL", trpHeader);

    }

    public void testRemoveProperty() throws Exception {
        MessageContext mc = TestUtils.getAxis2MessageContext("<empty/>", null);

        // Setting properties
        mc.setProperty("PROP_DEFAULT", "PROP_DEFAULT_VAL");

        Axis2MessageContext axis2smc = (Axis2MessageContext) mc;
        org.apache.axis2.context.MessageContext axis2MessageCtx = axis2smc.getAxis2MessageContext();
        axis2MessageCtx.setProperty("PROP_AXIS2", "PROP_AXIS2_VAL");

        Map headersMap = new HashMap();
        headersMap.put("PROP_TRP", "PROP_TRP_VAL");
        axis2MessageCtx.setProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS, headersMap);

        // For default scope
        String removalScript = "mc.removeProperty(\"PROP_DEFAULT\");";
        ScriptMediator mediator = new ScriptMediator("js", removalScript, null);
        mediator.mediate(mc);
        assertNull(mc.getProperty("PROP_DEFAULT"));

        // For Axis2 scope
        removalScript = "mc.removeProperty(\"PROP_AXIS2\",\"axis2\");";
        mediator = new ScriptMediator("js", removalScript, null);
        mediator.mediate(mc);
        assertNull(axis2MessageCtx.getProperty("PROP_AXIS2"));

        // For Transport scope
        removalScript = "mc.removeProperty(\"PROP_TRP\",\"transport\");";
        mediator = new ScriptMediator("js", removalScript, null);
        mediator.mediate(mc);
        Object headers = axis2MessageCtx.getProperty(
                org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);

        headersMap = (Map) headers;
        assertNull(headersMap.get("PROP_TRP"));
    }

    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new ScriptMediatorTest("testInlineMediator"));
        for (int i = 0; i < 10; i++) {
            suite.addTest(new RepeatedTest(new ScriptMediatorTest("testThreadSafety"), 10));
        }
        suite.addTest(new ScriptMediatorTest("testSetProperty"));
        suite.addTest(new ScriptMediatorTest("testRemoveProperty"));
        return suite;
    }

    public ScriptMediatorTest(String name) {
        super(name);
    }
}
