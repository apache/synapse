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
package org.apache.synapse.mediators.builtin;

import junit.framework.TestCase;
import org.apache.synapse.SynapseMessageContext;
import org.apache.synapse.Util;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.mediators.TestUtils;
import org.apache.synapse.mediators.MediatorProperty;
import org.apache.axiom.om.xpath.AXIOMXPath;

public class PropertyMediatorTest extends TestCase {

    public void testSetAndReadContextProperty() throws Exception {

        PropertyMediator propMediator = new PropertyMediator();
        propMediator.setName("name");
        propMediator.setValue("value");

        SynapseMessageContext synCtx = TestUtils.getTestContext("<empty/>");
        propMediator.mediate(synCtx);

        assertTrue(
            "value".equals(Util.getStringValue(
                new AXIOMXPath("synapse:get-property('name')"), synCtx)));
    }

    /**
     * property being searched does not exist in context, and lookup should go up into the config
     * @throws Exception
     */
    public void testSetAndReadGlobalProperty() throws Exception {

        SynapseMessageContext synCtx = TestUtils.getTestContext("<empty/>");

        SynapseConfiguration synCfg = new SynapseConfiguration();
        synCfg.addProperty("name", "value");
        synCtx.setConfiguration(synCfg);

        assertTrue(
            "value".equals(Util.getStringValue(
                new AXIOMXPath("synapse:get-property('name')"), synCtx)));
    }

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

        SynapseMessageContext synCtx = TestUtils.getTestContext("<empty/>");
        propMediator.mediate(synCtx);

        // read property through a mediator property
        MediatorProperty medProp = new MediatorProperty();
        medProp.setExpression(new AXIOMXPath("synapse:get-property('name')"));

        assertTrue(
            "value".equals(medProp.getEvaluatedExpression(synCtx)));
    }

}


