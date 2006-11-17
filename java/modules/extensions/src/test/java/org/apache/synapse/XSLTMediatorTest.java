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

package org.apache.synapse;

import junit.framework.TestCase;
import org.apache.axiom.om.OMContainer;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.synapse.mediators.TestUtils;
import org.apache.synapse.mediators.transform.XSLTMediator;
import org.apache.synapse.config.Property;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.Map;

public class XSLTMediatorTest extends TestCase {

    private static final String SOURCE =
        "<m0:CheckPriceRequest xmlns:m0=\"http://www.apache-synapse.org/test\">\n" +
        "<m0:Code>String</m0:Code>\n" +
        "</m0:CheckPriceRequest>";

    private static final String ENCLOSING_SOURCE =
        "<m:someOtherElement xmlns:m=\"http://someother\">" +
        "<m0:CheckPriceRequest xmlns:m0=\"http://www.apache-synapse.org/test\">\n" +
        "<m0:Code>String</m0:Code>\n" +
        "</m0:CheckPriceRequest>" +
        "</m:someOtherElement>";

    XSLTMediator transformMediator = null;

    public void testTransformXSLTCustomSource() throws Exception {

        // create a new switch mediator
        transformMediator = new XSLTMediator();

        // set xpath condition to select source
        AXIOMXPath xpath = new AXIOMXPath("//m0:CheckPriceRequest");
        xpath.addNamespace("m0", "http://www.apache-synapse.org/test");
        transformMediator.setSource(xpath);

        // set XSLT transformation URL
        transformMediator.setXsltKey("xslt-key");

        Map props = new HashMap();
        Property prop = new Property();
        prop.setType(Property.DYNAMIC_TYPE);
        prop.setKey("file:./../../repository/conf/sample/resources/transform/transform_unittest.xslt");
        props.put("xslt-key", prop);

        // invoke transformation, with static enveope
        MessageContext synCtx = TestUtils.getTestContext(SOURCE, props);
        transformMediator.mediate(synCtx);

        // validate result
        OMContainer body = synCtx.getEnvelope().getBody();
        if (body.getFirstOMChild() instanceof OMElement) {

            OMElement getQuoteElem = (OMElement) body.getFirstOMChild();
            assertTrue("GetQuote".equals(getQuoteElem.getLocalName()));
            assertTrue("http://www.webserviceX.NET/".equals(getQuoteElem.getNamespace().getName()));

            OMElement symbolElem = getQuoteElem.getFirstElement();
            assertTrue("symbol".equals(symbolElem.getLocalName()));
            assertTrue("http://www.webserviceX.NET/".equals(symbolElem.getNamespace().getName()));

            assertTrue("String".equals(symbolElem.getText()));
        } else {
            fail("Unexpected element found in SOAP body");
        }
    }

    /**
     * If a source element for transformation is not found, default to soap body
     * @throws Exception
     */
    public void testTransformXSLTDefaultSource() throws Exception {

        // create a new switch mediator
        transformMediator = new XSLTMediator();

        // set XSLT transformation URL
        transformMediator.setXsltKey("xslt-key");

        Map props = new HashMap();
        Property prop = new Property();
        prop.setType(Property.DYNAMIC_TYPE);
        prop.setKey("file:./../../repository/conf/sample/resources/transform/transform_unittest.xslt");
        props.put("xslt-key", prop);

        // invoke transformation, with static enveope
        MessageContext synCtx = TestUtils.getTestContext(SOURCE, props);
        transformMediator.mediate(synCtx);

        // validate result
        OMContainer body = synCtx.getEnvelope().getBody();
        if (body.getFirstOMChild() instanceof OMElement) {

            OMElement getQuoteElem = (OMElement) body.getFirstOMChild();
            assertTrue("GetQuote".equals(getQuoteElem.getLocalName()));
            assertTrue("http://www.webserviceX.NET/".equals(getQuoteElem.getNamespace().getName()));

            OMElement symbolElem = getQuoteElem.getFirstElement();
            assertTrue("symbol".equals(symbolElem.getLocalName()));
            assertTrue("http://www.webserviceX.NET/".equals(symbolElem.getNamespace().getName()));

            assertTrue("String".equals(symbolElem.getText()));
        } else {
            fail("Unexpected element found in SOAP body");
        }
    }

    public void testTransformXSLTCustomSourceNonMainElement() throws Exception {

        // create a new switch mediator
        transformMediator = new XSLTMediator();

        // set xpath condition to select source
        AXIOMXPath xpath = new AXIOMXPath("//m0:CheckPriceRequest");
        xpath.addNamespace("m0", "http://www.apache-synapse.org/test");
        transformMediator.setSource(xpath);

        // set XSLT transformation URL
        transformMediator.setXsltKey("xslt-key");

        Map props = new HashMap();
        Property prop = new Property();
        prop.setType(Property.DYNAMIC_TYPE);
        prop.setKey("file:./../../repository/conf/sample/resources/transform/transform_unittest.xslt");
        props.put("xslt-key", prop);
        
        // invoke transformation, with static enveope
        MessageContext synCtx = TestUtils.getTestContext(ENCLOSING_SOURCE, props);
        transformMediator.mediate(synCtx);

        // validate result
        OMContainer body = synCtx.getEnvelope().getBody();
        if (body.getFirstOMChild() instanceof OMElement) {

            OMElement someOtherElem = (OMElement) body.getFirstOMChild();
            assertTrue("someOtherElement".equals(someOtherElem.getLocalName()));
            assertTrue("http://someother".equals(someOtherElem.getNamespace().getName()));

            OMElement getQuoteElem = (OMElement) someOtherElem.getFirstOMChild();
            assertTrue("GetQuote".equals(getQuoteElem.getLocalName()));
            assertTrue("http://www.webserviceX.NET/".equals(getQuoteElem.getNamespace().getName()));

            OMElement symbolElem = getQuoteElem.getFirstElement();
            assertTrue("symbol".equals(symbolElem.getLocalName()));
            assertTrue("http://www.webserviceX.NET/".equals(symbolElem.getNamespace().getName()));

            assertTrue("String".equals(symbolElem.getText()));
        } else {
            fail("Unexpected element found in SOAP body");
        }
    }

}
