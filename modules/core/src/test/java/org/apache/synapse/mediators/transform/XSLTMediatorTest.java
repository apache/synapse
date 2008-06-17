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

package org.apache.synapse.mediators.transform;

import junit.framework.TestCase;

import org.apache.axiom.om.OMContainer;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.TestMessageContextBuilder;
import org.apache.synapse.transport.base.BaseConstants;
import org.apache.synapse.util.xpath.SynapseXPath;

public class XSLTMediatorTest extends TestCase {

    private static final String SOURCE =
        "<m0:CheckPriceRequest xmlns:m0=\"http://services.samples/xsd\">\n" +
        "<m0:Code>String</m0:Code>\n" +
        "</m0:CheckPriceRequest>";

    private static final String ENCLOSING_SOURCE =
        "<m:someOtherElement xmlns:m=\"http://someother\">" +
        SOURCE +
        "</m:someOtherElement>";

    XSLTMediator transformMediator = null;

    /**
     * Check that the provided element is the result of the XSL transformation of
     * SOURCE by the stylesheet transform_unittest.xslt.
     * 
     * @param element
     */
    private void assertQuoteElement(OMNode node) {
        assertTrue(node instanceof OMElement);
        OMElement element = (OMElement)node;
        
        assertTrue("GetQuote".equals(element.getLocalName()));
        assertTrue("http://www.webserviceX.NET/".equals(element.getNamespace().getNamespaceURI()));

        OMElement symbolElem = element.getFirstElement();
        assertTrue("symbol".equals(symbolElem.getLocalName()));
        assertTrue("http://www.webserviceX.NET/".equals(symbolElem.getNamespace().getNamespaceURI()));

        assertTrue("String".equals(symbolElem.getText()));
    }
    
    public void testTransformXSLTCustomSource() throws Exception {

        // create a new switch mediator
        transformMediator = new XSLTMediator();

        // set xpath condition to select source
        SynapseXPath xpath = new SynapseXPath("//m0:CheckPriceRequest");
        xpath.addNamespace("m0", "http://services.samples/xsd");
        transformMediator.setSource(xpath);

        // set XSLT transformation URL
        transformMediator.setXsltKey("xslt-key");

        MessageContext synCtx = new TestMessageContextBuilder()
            .addFileEntry("xslt-key", "../../repository/conf/sample/resources/transform/transform_unittest.xslt")
            .setBodyFromString(SOURCE).addTextAroundBody().build();
        transformMediator.mediate(synCtx);

        // validate result
        assertQuoteElement(synCtx.getEnvelope().getBody().getFirstOMChild().getNextOMSibling());
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

        MessageContext synCtx = new TestMessageContextBuilder()
            .addFileEntry("xslt-key", "../../repository/conf/sample/resources/transform/transform_unittest.xslt")
            .setBodyFromString(SOURCE).addTextAroundBody().build();
        transformMediator.mediate(synCtx);

        // validate result
        assertQuoteElement(synCtx.getEnvelope().getBody().getFirstOMChild().getNextOMSibling());
    }

    public void testTransformXSLTLargeMessagesCSV() throws Exception {

        // create a new switch mediator
        transformMediator = new XSLTMediator();
        // set XSLT transformation URL
        transformMediator.setXsltKey("xslt-key");

        for (int i=0; i<2; i++) {

            // invoke transformation, with static enveope
            MessageContext synCtx = new TestMessageContextBuilder()
                .addFileEntry("xslt-key", "../../repository/conf/sample/resources/transform/transform_load.xml")
                .setBodyFromFile("../../repository/conf/sample/resources/transform/message.xml")
                .addTextAroundBody().build();
            //MessageContext synCtx = TestUtils.getTestContextForXSLTMediator(SOURCE, props);
            transformMediator.mediate(synCtx);
//            synCtx.getEnvelope().serializeAndConsume(new FileOutputStream("/tmp/out.xml"));
//            System.gc();
//            System.out.println("done : " + i + " :: " + Runtime.getRuntime().freeMemory());
        }
    }

    public void testTransformXSLTLargeMessagesXML() throws Exception {

        // create a new switch mediator
        transformMediator = new XSLTMediator();
        // set XSLT transformation URL
        transformMediator.setXsltKey("xslt-key");

        for (int i=0; i<2; i++) {

            // invoke transformation, with static enveope
            MessageContext synCtx = new TestMessageContextBuilder()
                .addFileEntry("xslt-key", "../../repository/conf/sample/resources/transform/transform_load_3.xml")
                .setBodyFromFile("../../repository/conf/sample/resources/transform/message.xml")
                .addTextAroundBody().build();
            //MessageContext synCtx = TestUtils.getTestContextForXSLTMediator(SOURCE, props);
            transformMediator.mediate(synCtx);
//            System.gc();
//            System.out.println("done : " + i + " :: " + Runtime.getRuntime().freeMemory());
        }
    }

     public void testSynapse242() throws Exception {

        // create a new switch mediator
        transformMediator = new XSLTMediator();
        // set XSLT transformation URL
        transformMediator.setXsltKey("xslt-key");

         // invoke transformation, with static enveope
         MessageContext synCtx = new TestMessageContextBuilder()
             .addFileEntry("xslt-key", "../../repository/conf/sample/resources/transform/transform_load_2.xml")
             .setBodyFromFile("../../repository/conf/sample/resources/transform/med_message.xml")
             .addTextAroundBody().build();
        transformMediator.mediate(synCtx);

         // validate result
         OMContainer body = synCtx.getEnvelope().getBody();
         assertTrue(body.getFirstOMChild().getNextOMSibling() instanceof OMElement);
         assertTrue( ((OMElement)body.getFirstOMChild().getNextOMSibling()).getText().length() > 0);
    }


    public void testTransformXSLTSmallMessages() throws Exception {

        // create a new switch mediator
        transformMediator = new XSLTMediator();
        // set XSLT transformation URL
        transformMediator.setXsltKey("xslt-key");

        for (int i=0; i<5; i++) {
            // invoke transformation, with static enveope
            MessageContext synCtx = new TestMessageContextBuilder()
                .addFileEntry("xslt-key", "../../repository/conf/sample/resources/transform/transform_load_2.xml")
                .setBodyFromFile("../../repository/conf/sample/resources/transform/small_message.xml")
                .addTextAroundBody().build();
            //MessageContext synCtx = TestUtils.getTestContextForXSLTMediator(SOURCE, props);
            transformMediator.mediate(synCtx);
            //System.out.println("done : " + i + " :: " + Runtime.getRuntime().freeMemory());
        }
    }

    public void testTransformXSLTCustomSourceNonMainElement() throws Exception {

        // create a new switch mediator
        transformMediator = new XSLTMediator();

        // set xpath condition to select source
        SynapseXPath xpath = new SynapseXPath("//m0:CheckPriceRequest");
        xpath.addNamespace("m0", "http://services.samples/xsd");
        transformMediator.setSource(xpath);

        // set XSLT transformation URL
        transformMediator.setXsltKey("xslt-key");

        // invoke transformation, with static enveope
        MessageContext synCtx = new TestMessageContextBuilder()
            .addFileEntry("xslt-key", "../../repository/conf/sample/resources/transform/transform_unittest.xslt")
            .setBodyFromString(ENCLOSING_SOURCE)
            .addTextAroundBody().build();
        transformMediator.mediate(synCtx);

        // validate result
        OMContainer body = synCtx.getEnvelope().getBody();
        if (body.getFirstOMChild().getNextOMSibling() instanceof OMElement) {

            OMElement someOtherElem = (OMElement) body.getFirstOMChild().getNextOMSibling();
            assertTrue("someOtherElement".equals(someOtherElem.getLocalName()));
            assertTrue("http://someother".equals(someOtherElem.getNamespace().getNamespaceURI()));

            assertQuoteElement(someOtherElem.getFirstOMChild());
        } else {
            fail("Unexpected element found in SOAP body");
        }
    }

    public void testTextEncoding() throws Exception {
        transformMediator = new XSLTMediator();
        transformMediator.setXsltKey("xslt-key");
        
        MessageContext mc = new TestMessageContextBuilder()
            .addFileEntry("xslt-key", "../../repository/conf/sample/resources/transform/encoding_test.xslt")
            .setEnvelopeFromFile("../../repository/conf/sample/resources/transform/encoding_test.xml").build();
        
        transformMediator.mediate(mc);
        
        OMElement resultElement = mc.getEnvelope().getBody().getFirstElement();
        assertEquals(BaseConstants.DEFAULT_TEXT_WRAPPER, resultElement.getQName());
        assertEquals("\u00e0 peine arriv\u00e9s nous entr\u00e2mes dans sa chambre", resultElement.getText());
    }
    
    // Test for SYNAPSE-307
    public void testInvalidStylesheet() throws Exception {
        transformMediator = new XSLTMediator();
        transformMediator.setXsltKey("xslt-key");
        
        MessageContext mc = new TestMessageContextBuilder()
            .addEntry("xslt-key", getClass().getResource("invalid.xslt"))
            .setBodyFromString("<root/>")
            .build();
        
        try {
            transformMediator.mediate(mc);
            fail("Expected a SynapseException to be thrown");
        } catch (SynapseException ex) {
            // this is what is expected
        }
    }
}
