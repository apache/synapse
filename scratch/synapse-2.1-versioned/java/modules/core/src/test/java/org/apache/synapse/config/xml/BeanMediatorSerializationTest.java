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
 * Tests Bean mediator serialization scenarios.
 */
public class BeanMediatorSerializationTest extends AbstractTestCase {

    private BeanMediatorFactory beanMediatorFactory = new BeanMediatorFactory();
    private BeanMediatorSerializer beanMediatorSerializer = new BeanMediatorSerializer();

    public BeanMediatorSerializationTest() {
        super(AbstractTestCase.class.getName());
    }

    public void testBeanMediatorCreateActionSerialization() throws Exception {

        String inputXml = "<bean action='CREATE' class='org.apache.synapse.mediators.bean.Quote' var='test_quote' " +
                " xmlns='http://ws.apache.org/ns/synapse'/>";

        assertTrue(serialization(inputXml, beanMediatorFactory, beanMediatorSerializer));
        assertTrue(serialization(inputXml, beanMediatorSerializer));
    }

    public void testBeanMediatorCreateNoReplaceActionSerialization() throws Exception {

        String inputXml = "<bean action='CREATE' class='org.apache.synapse.mediators.bean.Quote' var='test_quote' " +
                "replace='false' xmlns='http://ws.apache.org/ns/synapse'/>";

        assertTrue(serialization(inputXml, beanMediatorFactory, beanMediatorSerializer));
        assertTrue(serialization(inputXml, beanMediatorSerializer));
    }

    public void testBeanMediatorRemoveActionSerialization() throws Exception {

        String inputXml = "<bean action='REMOVE' var='test_quote' xmlns='http://ws.apache.org/ns/synapse'/>";

        assertTrue(serialization(inputXml, beanMediatorFactory, beanMediatorSerializer));
        assertTrue(serialization(inputXml, beanMediatorSerializer));
    }

    public void testSetPropertyFromStaticValueActionSerialization() throws Exception {

        String inputXml = "<bean action='SET_PROPERTY' var='test_quote' property='symbol' value='IBM' " +
                "xmlns='http://ws.apache.org/ns/synapse'/>";

        assertTrue(serialization(inputXml, beanMediatorFactory, beanMediatorSerializer));
        assertTrue(serialization(inputXml, beanMediatorSerializer));
    }

    public void testSetPropertyFromXPathActionSerialization() throws Exception {

        String inputXml = "<bean action='SET_PROPERTY' var='test_quote' property='price' value='{//m:price}' " +
                "xmlns:m='http://services.samples' xmlns='http://ws.apache.org/ns/synapse'/>";

        assertTrue(serialization(inputXml, beanMediatorFactory, beanMediatorSerializer));
        assertTrue(serialization(inputXml, beanMediatorSerializer));
    }

    public void testGetPropertyToMCPropertyActionSerialization() throws Exception {

        String inputXml = "<bean action='GET_PROPERTY' var='test_quote' property='price' target='quote_price' " +
                "xmlns='http://ws.apache.org/ns/synapse'/>";

        assertTrue(serialization(inputXml, beanMediatorFactory, beanMediatorSerializer));
        assertTrue(serialization(inputXml, beanMediatorSerializer));
    }

    public void testGetPropertyToXPathActionSerialization() throws Exception {

        String inputXml = "<bean action='GET_PROPERTY' var='test_quote' property='symbol' " +
                "target='{//m:QuoteResponse/m:symbol}' " +
                "xmlns:m='http://services.samples' xmlns='http://ws.apache.org/ns/synapse'/>";

        assertTrue(serialization(inputXml, beanMediatorFactory, beanMediatorSerializer));
        assertTrue(serialization(inputXml, beanMediatorSerializer));
    }
}
