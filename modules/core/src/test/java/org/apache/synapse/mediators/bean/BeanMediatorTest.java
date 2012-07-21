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

package org.apache.synapse.mediators.bean;

import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.TestMessageContext;
import org.apache.synapse.config.xml.MediatorFactoryFinder;
import org.apache.synapse.mediators.AbstractMediatorTestCase;
import org.apache.synapse.mediators.TestUtils;
import org.apache.synapse.util.xpath.SynapseXPath;

import java.util.Properties;

/**
 * Tests Bean mediator's runtime behavior.
 */
public class BeanMediatorTest extends AbstractMediatorTestCase {

    private static final String MESSAGE_BODY =
            "<m:QuoteResponse xmlns:m='http://services.samples'>" +
                    "<m:symbol>SUN</m:symbol>" +
                    "<m:price>20.5</m:price>" +
            "</m:QuoteResponse>";

    private static final String VAR_NAME = "test_quote";

    public void testCreateAction() throws Exception {

        Mediator beanMediator = MediatorFactoryFinder.getInstance().getMediator(
                createOMElement(
                        "<bean action='CREATE' class='org.apache.synapse.mediators.bean.Quote' var='test_quote' " +
                                "xmlns='http://ws.apache.org/ns/synapse'/>"),
                new Properties());

        MessageContext synCtx = new TestMessageContext();

        assertTrue(beanMediator.mediate(synCtx));

        assertNotNull(synCtx.getProperty(VAR_NAME));
        assertTrue(synCtx.getProperty(VAR_NAME) instanceof Quote);
    }

    public void testCreateNoReplaceAction() throws Exception {

        Mediator beanMediator = MediatorFactoryFinder.getInstance().getMediator(
                createOMElement(
                        "<bean action='CREATE' class='org.apache.synapse.mediators.bean.Quote' var='test_quote' " +
                                "replace='false' xmlns='http://ws.apache.org/ns/synapse'/>"),
                new Properties());

        MessageContext synCtx = new TestMessageContext();
        Quote existingQuote = new Quote();
        synCtx.setProperty(VAR_NAME, existingQuote);

        assertTrue(beanMediator.mediate(synCtx));

        assertEquals(existingQuote, synCtx.getProperty(VAR_NAME));
    }

    public void testRemoveAction() throws Exception {

        Mediator beanMediator = MediatorFactoryFinder.getInstance().getMediator(
                createOMElement(
                        "<bean action='REMOVE' var='test_quote' xmlns='http://ws.apache.org/ns/synapse'/>"),
                new Properties());

        MessageContext synCtx = new TestMessageContext();
        synCtx.setProperty(VAR_NAME, new Quote());

        assertTrue(beanMediator.mediate(synCtx));

        assertNull(synCtx.getProperty(VAR_NAME));
    }

    public void testSetPropertyFromStaticValueAction() throws Exception {

        Mediator beanMediator = MediatorFactoryFinder.getInstance().getMediator(
                createOMElement(
                        "<bean action='SET_PROPERTY' var='test_quote' property='symbol' value='IBM' " +
                                "xmlns='http://ws.apache.org/ns/synapse'/>"),
                new Properties());

        MessageContext synCtx = TestUtils.getTestContext(MESSAGE_BODY);
        synCtx.setProperty(VAR_NAME, new Quote());

        assertTrue(beanMediator.mediate(synCtx));

        assertEquals("IBM", ((Quote) synCtx.getProperty(VAR_NAME)).getSymbol());
    }

    public void testSetPropertyFromMCPropertyAction() throws Exception {

        Mediator beanMediator = MediatorFactoryFinder.getInstance().getMediator(
                createOMElement(
                        "<bean action='SET_PROPERTY' var='test_quote' property='price' " +
                                "value=\"{get-property('mc_property')}\" " +
                                "xmlns:m='http://services.samples' xmlns='http://ws.apache.org/ns/synapse'/>"),
                new Properties());

        MessageContext synCtx = TestUtils.getTestContext(MESSAGE_BODY);
        synCtx.setProperty(VAR_NAME, new Quote());
        float price = 30.0f;
        synCtx.setProperty("mc_property", price);

        assertTrue(beanMediator.mediate(synCtx));

        assertEquals(price, ((Quote) synCtx.getProperty(VAR_NAME)).getPrice());
    }

    public void testSetPropertyFromMessageBodyAction() throws Exception {

        Mediator beanMediator = MediatorFactoryFinder.getInstance().getMediator(
                createOMElement(
                        "<bean action='SET_PROPERTY' var='test_quote' property='price' value='{//m:price}' " +
                                "xmlns:m='http://services.samples' xmlns='http://ws.apache.org/ns/synapse'/>"),
                new Properties());

        MessageContext synCtx = TestUtils.getTestContext(MESSAGE_BODY);
        synCtx.setProperty(VAR_NAME, new Quote());

        assertTrue(beanMediator.mediate(synCtx));

        assertEquals(new Float("20.5"), ((Quote) synCtx.getProperty(VAR_NAME)).getPrice());
    }

    public void testGetPropertyToMCPropertyAction() throws Exception {

        Mediator beanMediator = MediatorFactoryFinder.getInstance().getMediator(
                createOMElement(
                        "<bean action='GET_PROPERTY' var='test_quote' property='price' target='mc_property' " +
                                "xmlns='http://ws.apache.org/ns/synapse'/>"),
                new Properties());

        MessageContext synCtx = TestUtils.getTestContext(MESSAGE_BODY);
        Quote quote = new Quote();
        Float price = 10.5f;
        quote.setPrice(price);
        synCtx.setProperty(VAR_NAME, quote);

        assertTrue(beanMediator.mediate(synCtx));

        assertEquals(price, synCtx.getProperty("mc_property"));
    }

    public void testGetPropertyToMessageBodyAction() throws Exception {

        Mediator beanMediator = MediatorFactoryFinder.getInstance().getMediator(
                createOMElement(
                        "<bean action='GET_PROPERTY' var='test_quote' property='symbol' " +
                                "target='{//m:QuoteResponse/m:symbol/text()}' " +
                                "xmlns:m='http://services.samples' xmlns='http://ws.apache.org/ns/synapse'/>"),
                new Properties());

        MessageContext synCtx = TestUtils.getTestContext(MESSAGE_BODY);
        Quote quote = new Quote();
        String symbol = "IBM";
        quote.setSymbol(symbol);
        synCtx.setProperty(VAR_NAME, quote);

        assertTrue(beanMediator.mediate(synCtx));

        SynapseXPath xpath = new SynapseXPath("//m:QuoteResponse/m:symbol");
        xpath.addNamespace("m", "http://services.samples");

        assertEquals(symbol, xpath.stringValueOf(synCtx));
    }
}
