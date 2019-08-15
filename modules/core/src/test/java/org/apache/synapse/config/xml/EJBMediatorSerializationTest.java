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

package org.apache.synapse.config.xml;

/**
 * Tests EJB mediator serialization scenarios.
 */
public class EJBMediatorSerializationTest extends AbstractTestCase {

    private EJBMediatorFactory ejbMediatorFactory = new EJBMediatorFactory();
    private EJBMediatorSerializer ejbMediatorSerializer = new EJBMediatorSerializer();

    public EJBMediatorSerializationTest() {
        super(AbstractTestCase.class.getName());
    }

    public void testEJBMediatorStatelessScenario1Serialization() throws Exception {

        String inputXml = "<ejb class='org.apache.synapse.mediators.bean.Quote' beanstalk='demo' method='setPrice' " +
                "jndiName='jndi' xmlns='http://ws.apache.org/ns/synapse'>" +
                "<args><arg value=\"{get-property('price')}\"/></args></ejb>";

        assertTrue(serialization(inputXml, ejbMediatorFactory, ejbMediatorSerializer));
        assertTrue(serialization(inputXml, ejbMediatorSerializer));
    }

    public void testEJBMediatorStatelessScenario2Serialization() throws Exception {

        String inputXml = "<ejb class='org.apache.synapse.mediators.bean.Quote' beanstalk='demo' method='getPrice' " +
                "target='{//m:Price}' xmlns:m='http://services.samples' xmlns='http://ws.apache.org/ns/synapse'/>";

        assertTrue(serialization(inputXml, ejbMediatorFactory, ejbMediatorSerializer));
        assertTrue(serialization(inputXml, ejbMediatorSerializer));
    }

    public void testEJBMediatorStatelessScenario3Serialization() throws Exception {

        String inputXml = "<ejb class='org.apache.synapse.mediators.bean.Quote' beanstalk='demo' method='testMethod' " +
                "target='bar' jndiName='jndi' xmlns='http://ws.apache.org/ns/synapse'>" +
                "<args><arg value='100'/><arg value=\"{get-property('batz')}\"/></args></ejb>";

        assertTrue(serialization(inputXml, ejbMediatorFactory, ejbMediatorSerializer));
        assertTrue(serialization(inputXml, ejbMediatorSerializer));
    }


    public void testEJBMediatorStatefulScenario1Serialization() throws Exception {

        String inputXml = "<ejb class='org.apache.synapse.mediators.bean.Quote' beanstalk='demo' method='testMethod' " +
                "target='bar' sessionId=\"{get-property('SESSION_ID')}\" jndiName='jndi' " +
                "xmlns='http://ws.apache.org/ns/synapse'>" +
                "<args><arg value='{//m:item//m:quantity}' xmlns:m='http://org.test.ejb'/>" +
                "<arg value='{//m:item//m:id}' xmlns:m='http://org.test.ejb'/></args></ejb>";

        assertTrue(serialization(inputXml, ejbMediatorFactory, ejbMediatorSerializer));
        assertTrue(serialization(inputXml, ejbMediatorSerializer));
    }

    public void testEJBMediatorStatefulScenario2Serialization() throws Exception {

        String inputXml = "<ejb class='org.apache.synapse.mediators.bean.Quote' beanstalk='demo' method='testMethod' " +
                "target='bar' sessionId='1234' remove='true' xmlns='http://ws.apache.org/ns/synapse'>" +
                "<args><arg value='100'/><arg value=\"{get-property('batz')}\"/></args></ejb>";

        assertTrue(serialization(inputXml, ejbMediatorFactory, ejbMediatorSerializer));
        assertTrue(serialization(inputXml, ejbMediatorSerializer));
    }
}
