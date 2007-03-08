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

package org.apache.synapse.mediators.builtin;

import junit.framework.TestCase;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.MediatorProperty;
import org.apache.synapse.mediators.TestUtils;

public class PropertyMediatorTest extends TestCase {

    public void testSetAndReadContextProperty() throws Exception {

        PropertyMediator propMediator = new PropertyMediator();
        propMediator.setName("name");
        propMediator.setValue("value");

         // set a local property to the synapse context
        PropertyMediator propMediatorTwo = new PropertyMediator();
        propMediatorTwo.setName("nameTwo");
        propMediatorTwo.setValue("valueTwo");

        MessageContext synCtx = TestUtils.getTestContext("<empty/>");
        propMediator.mediate(synCtx);
        propMediatorTwo.mediate(synCtx);
        assertTrue(
            "value".equals(Axis2MessageContext.getStringValue(
                new AXIOMXPath("synapse:get-property('name')"), synCtx)));
        assertTrue(
            "valueTwo".equals(Axis2MessageContext.getStringValue(
                new AXIOMXPath("synapse:get-property('nameTwo')"), synCtx)));

        PropertyMediator propMediatorThree = new PropertyMediator();
        propMediatorThree.setName("name");
        propMediatorThree.setValue("value");
        propMediatorThree.setAction(PropertyMediator.ACTION_REMOVE);
        propMediatorThree.mediate(synCtx) ;
        assertNull(Axis2MessageContext.getStringValue(
                new AXIOMXPath("synapse:get-property('name')"), synCtx));
        assertTrue(
                   "valueTwo".equals(Axis2MessageContext.getStringValue(
                       new AXIOMXPath("synapse:get-property('nameTwo')"), synCtx)));
                
    }

    /**
     * property being searched does not exist in context, and lookup should go up into the config
     * @throws Exception
     */
    /*TODO ACP public void testSetAndReadGlobalProperty() throws Exception {

        MessageContext synCtx = TestUtils.getTestContext("<empty/>");

        SynapseConfiguration synCfg = new SynapseConfiguration();
        Entry prop = new Entry();
        prop.setKey("name");
        prop.setType(Entry.VALUE_TYPE);
        prop.setValue("value");
        synCfg.addEntry("name", prop);
        synCtx.setConfiguration(synCfg);

        assertTrue(
            "value".equals(Axis2MessageContext.getStringValue(
                new AXIOMXPath("synapse:get-property('name')"), synCtx)));
    }*/

    public void testMediatorPropertiesLiteral() throws Exception {

        MediatorProperty medProp = new MediatorProperty();
        medProp.setName("name");
        medProp.setValue("value");
        assertTrue("value".equals(medProp.getValue()));
    }

    public void testMediatorPropertiesExpression() throws Exception {

        // set a local property to the synapse context
        PropertyMediator propMediator = new PropertyMediator();
        propMediator.setName("name");
        propMediator.setValue("value");

        MessageContext synCtx = TestUtils.getTestContext("<empty/>");
        propMediator.mediate(synCtx);

        // read property through a mediator property
        MediatorProperty medProp = new MediatorProperty();
        medProp.setExpression(new AXIOMXPath("synapse:get-property('name')"));

        assertTrue(
            "value".equals(medProp.getEvaluatedExpression(synCtx)));
    }

}


