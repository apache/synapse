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
package org.apache.synapse.mediators.ext;

import junit.framework.TestCase;
import org.apache.synapse.TestSynapseMessageContext;
import org.apache.synapse.mediators.MediatorProperty;
import org.apache.axiom.om.xpath.AXIOMXPath;

/**
 * Tests the class mediator instantiation and setting of literal and
 * XPath parameters at runtime.
 */
public class ClassMediatorTest extends TestCase {

    public void testCreationWithoutProperties() throws Exception {
        ClassMediator cm = new ClassMediator();
        cm.setClazz(ClassMediatorTestMediator.class);
        cm.mediate(new TestSynapseMessageContext());
        assertTrue(ClassMediatorTestMediator.invoked);
    }

    public void testCreationWithLiteralProperties() throws Exception {
        ClassMediator cm = new ClassMediator();
        MediatorProperty mp = new MediatorProperty();
        mp.setName("testProp");
        mp.setValue("testValue");
        cm.addProperty(mp);
        cm.setClazz(ClassMediatorTestMediator.class);
        cm.mediate(new TestSynapseMessageContext());
        assertTrue(ClassMediatorTestMediator.testProp.equals("testValue"));
    }

    public void testCreationWithXPathProperties() throws Exception {
        ClassMediator cm = new ClassMediator();
        MediatorProperty mp = new MediatorProperty();
        mp.setName("testProp");
        mp.setExpression(new AXIOMXPath("concat('XPath ','is ','FUN!')"));
        cm.addProperty(mp);
        cm.setClazz(ClassMediatorTestMediator.class);
        cm.mediate(new TestSynapseMessageContext());
        assertTrue(ClassMediatorTestMediator.testProp.equals("XPath is FUN!"));
    }

}
