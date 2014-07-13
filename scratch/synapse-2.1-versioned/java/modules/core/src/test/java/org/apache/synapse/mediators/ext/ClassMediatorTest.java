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

package org.apache.synapse.mediators.ext;

import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.Mediator;
import org.apache.synapse.TestMessageContext;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.config.xml.MediatorFactoryFinder;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.mediators.AbstractMediatorTestCase;

import java.util.Properties;

/**
 * Tests the class mediator instantiation and setting of literal and
 * XPath parameters at runtime.
 */
public class ClassMediatorTest extends AbstractMediatorTestCase {

    public void testMediationWithoutProperties() throws Exception {
        Mediator cm = MediatorFactoryFinder.getInstance().getMediator(createOMElement(
                "<class name='org.apache.synapse.mediators.ext.ClassMediatorTestMediator' " +
                        "xmlns='http://ws.apache.org/ns/synapse'/>"), new Properties());
        cm.mediate(new TestMessageContext());
        assertTrue(ClassMediatorTestMediator.invoked);
    }

    public void testMediationWithLiteralProperties() throws Exception {
        Mediator cm = MediatorFactoryFinder.getInstance().getMediator(createOMElement(
                "<class name='org.apache.synapse.mediators.ext.ClassMediatorTestMediator' " +
                        "xmlns='http://ws.apache.org/ns/synapse'><property name='testProp' value='testValue'/></class>"), new Properties());
        cm.mediate(new TestMessageContext());
        assertTrue(ClassMediatorTestMediator.invoked);
        assertTrue(ClassMediatorTestMediator.testProp.equals("testValue"));
    }

    public void testInitializationAndMedition() throws Exception {
        Mediator cm = MediatorFactoryFinder.getInstance().getMediator(createOMElement(
                "<class name='org.apache.synapse.mediators.ext.ClassMediatorTestMediator' " +
                        "xmlns='http://ws.apache.org/ns/synapse'/>"), new Properties());
        ((ManagedLifecycle) cm).init(new Axis2SynapseEnvironment(new SynapseConfiguration()));
        assertTrue(ClassMediatorTestMediator.initialized);
        cm.mediate(new TestMessageContext());
        assertTrue(ClassMediatorTestMediator.invoked);
    }

    public void testDestroy() throws Exception {
        Mediator cm = MediatorFactoryFinder.getInstance().getMediator(createOMElement(
                "<class name='org.apache.synapse.mediators.ext.ClassMediatorTestMediator' " +
                        "xmlns='http://ws.apache.org/ns/synapse'/>"), new Properties());
        cm.mediate(new TestMessageContext());
        assertTrue(ClassMediatorTestMediator.invoked);
        ((ManagedLifecycle) cm).destroy();
        assertTrue(ClassMediatorTestMediator.destroyed);
    }

//    public void testCreationWithXPathProperties() throws Exception {
//        ClassMediator cm = new ClassMediator();
//        MediatorProperty mp = new MediatorProperty();
//        mp.setName("testProp");
//        mp.setExpression(new SynapseXPath("concat('XPath ','is ','FUN!')"));
//        cm.addProperty(mp);
//        cm.setClazz(ClassMediatorTestMediator.class);
//        cm.mediate(new TestMessageContext());
//        assertTrue(ClassMediatorTestMediator.testProp.equals("XPath is FUN!"));
//    }

}
