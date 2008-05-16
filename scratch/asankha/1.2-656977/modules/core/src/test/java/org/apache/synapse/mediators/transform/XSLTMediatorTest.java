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
import org.apache.axiom.om.util.StAXUtils;
import org.apache.axiom.soap.impl.builder.StAXSOAPModelBuilder;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.synapse.MessageContext;
import org.apache.synapse.TestMessageContext;
import org.apache.synapse.config.Entry;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.mediators.MediatorProperty;
import org.apache.synapse.mediators.TestUtils;
import org.apache.synapse.transport.base.BaseConstants;
import org.apache.synapse.util.xpath.SynapseXPath;

import java.io.FileInputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XSLTMediatorTest extends TestCase {

    private static final String SOURCE =
        "<m0:CheckPriceRequest xmlns:m0=\"http://services.samples/xsd\">\n" +
        "<m0:Code>String</m0:Code>\n" +
        "</m0:CheckPriceRequest>";

    private static final String ENCLOSING_SOURCE =
        "<m:someOtherElement xmlns:m=\"http://someother\">" +
        "<m0:CheckPriceRequest xmlns:m0=\"http://services.samples/xsd\">\n" +
        "<m0:Code>String</m0:Code>\n" +
        "</m0:CheckPriceRequest>" +
        "</m:someOtherElement>";

    XSLTMediator transformMediator = null;

    public void testTransformXSLTCustomSource() throws Exception {

        // create a new switch mediator
        transformMediator = new XSLTMediator();

        // set xpath condition to select source
        SynapseXPath xpath = new SynapseXPath("//m0:CheckPriceRequest");
        xpath.addNamespace("m0", "http://services.samples/xsd");
        transformMediator.setSource(xpath);

        // set XSLT transformation URL
        transformMediator.setXsltKey("xslt-key");
        List<MediatorProperty> list = new ArrayList<MediatorProperty>();
        MediatorProperty mp = new MediatorProperty();
        mp.setName("parama1");
        mp.setValue("value1");
        list.add(mp);
        transformMediator.addAllProperties(list);
        Map props = new HashMap();
        Entry prop = new Entry();
        prop.setType(Entry.URL_SRC);
        prop.setSrc(new URL("file:./../../repository/conf/sample/resources/transform/transform_unittest.xslt"));
        props.put("xslt-key", prop);

        // invoke transformation, with static enveope
        MessageContext synCtx = TestUtils.getTestContextForXSLTMediator(SOURCE, props);
        transformMediator.mediate(synCtx);

        // validate result
        OMContainer body = synCtx.getEnvelope().getBody();
        if (body.getFirstOMChild().getNextOMSibling() instanceof OMElement) {

            OMElement getQuoteElem = (OMElement) body.getFirstOMChild().getNextOMSibling();
            assertTrue("GetQuote".equals(getQuoteElem.getLocalName()));
            assertTrue("http://www.webserviceX.NET/".equals(getQuoteElem.getNamespace().getNamespaceURI()));

            OMElement symbolElem = getQuoteElem.getFirstElement();
            assertTrue("symbol".equals(symbolElem.getLocalName()));
            assertTrue("http://www.webserviceX.NET/".equals(symbolElem.getNamespace().getNamespaceURI()));

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
        Entry prop = new Entry();
        prop.setType(Entry.URL_SRC);
        prop.setSrc(new URL("file:./../../repository/conf/sample/resources/transform/transform_unittest.xslt"));
        props.put("xslt-key", prop);

        // invoke transformation, with static enveope
        MessageContext synCtx = TestUtils.getTestContextForXSLTMediator(SOURCE, props);
        transformMediator.mediate(synCtx);

        // validate result
        OMContainer body = synCtx.getEnvelope().getBody();
        if (body.getFirstOMChild().getNextOMSibling() instanceof OMElement) {

            OMElement getQuoteElem = (OMElement) body.getFirstOMChild().getNextOMSibling();
            assertTrue("GetQuote".equals(getQuoteElem.getLocalName()));
            assertTrue("http://www.webserviceX.NET/".equals(getQuoteElem.getNamespace().getNamespaceURI()));

            OMElement symbolElem = getQuoteElem.getFirstElement();
            assertTrue("symbol".equals(symbolElem.getLocalName()));
            assertTrue("http://www.webserviceX.NET/".equals(symbolElem.getNamespace().getNamespaceURI()));

            assertTrue("String".equals(symbolElem.getText()));
        } else {
            fail("Unexpected element found in SOAP body");
        }
    }

    public void testTransformXSLTLargeMessagesCSV() throws Exception {

        // create a new switch mediator
        transformMediator = new XSLTMediator();
        // set XSLT transformation URL
        transformMediator.setXsltKey("xslt-key");

        for (int i=0; i<2; i++) {
            Map props = new HashMap();
            Entry prop = new Entry();
            prop.setType(Entry.URL_SRC);
            prop.setSrc(new URL("file:./../../repository/conf/sample/resources/transform/transform_load.xml"));
            props.put("xslt-key", prop);

            // invoke transformation, with static enveope
            MessageContext synCtx = TestUtils.getTestContextForXSLTMediatorUsingFile("./../../repository/conf/sample/resources/transform/message.xml", props);
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
            Map props = new HashMap();
            Entry prop = new Entry();
            prop.setType(Entry.URL_SRC);
            prop.setSrc(new URL("file:./../../repository/conf/sample/resources/transform/transform_load_3.xml"));
            props.put("xslt-key", prop);

            // invoke transformation, with static enveope
            MessageContext synCtx = TestUtils.getTestContextForXSLTMediatorUsingFile("./../../repository/conf/sample/resources/transform/message.xml", props);
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

        Map props = new HashMap();
        Entry prop = new Entry();
        prop.setType(Entry.URL_SRC);
        prop.setSrc(new URL("file:./../../repository/conf/sample/resources/transform/transform_load_2.xml"));
        props.put("xslt-key", prop);

        // invoke transformation, with static enveope
        MessageContext synCtx = TestUtils.getTestContextForXSLTMediatorUsingFile("./../../repository/conf/sample/resources/transform/med_message.xml", props);
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
            Map props = new HashMap();
            Entry prop = new Entry();
            prop.setType(Entry.URL_SRC);
            prop.setSrc(new URL("file:./../../repository/conf/sample/resources/transform/transform_load_2.xml"));
            props.put("xslt-key", prop);

            // invoke transformation, with static enveope
            MessageContext synCtx = TestUtils.getTestContextForXSLTMediatorUsingFile("./../../repository/conf/sample/resources/transform/small_message.xml", props);
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

        Map props = new HashMap();
        Entry prop = new Entry();
        prop.setType(Entry.URL_SRC);
        prop.setSrc(new URL("file:./../../repository/conf/sample/resources/transform/transform_unittest.xslt"));
        props.put("xslt-key", prop);
        
        // invoke transformation, with static enveope
        MessageContext synCtx = TestUtils.getTestContextForXSLTMediator(ENCLOSING_SOURCE, props);
        transformMediator.mediate(synCtx);

        // validate result
        OMContainer body = synCtx.getEnvelope().getBody();
        if (body.getFirstOMChild().getNextOMSibling() instanceof OMElement) {

            OMElement someOtherElem = (OMElement) body.getFirstOMChild().getNextOMSibling();
            assertTrue("someOtherElement".equals(someOtherElem.getLocalName()));
            assertTrue("http://someother".equals(someOtherElem.getNamespace().getNamespaceURI()));

            OMElement getQuoteElem = (OMElement) someOtherElem.getFirstOMChild();
            assertTrue("GetQuote".equals(getQuoteElem.getLocalName()));
            assertTrue("http://www.webserviceX.NET/".equals(getQuoteElem.getNamespace().getNamespaceURI()));

            OMElement symbolElem = getQuoteElem.getFirstElement();
            assertTrue("symbol".equals(symbolElem.getLocalName()));
            assertTrue("http://www.webserviceX.NET/".equals(symbolElem.getNamespace().getNamespaceURI()));

            assertTrue("String".equals(symbolElem.getText()));
        } else {
            fail("Unexpected element found in SOAP body");
        }
    }

    public void testTextEncoding() throws Exception {
        Entry xsltEntry = new Entry();
        xsltEntry.setType(Entry.URL_SRC);
        xsltEntry.setSrc(new URL("file:./../../repository/conf/sample/resources/transform/encoding_test.xslt"));
        
        SynapseConfiguration cfg = new SynapseConfiguration();
        cfg.addEntry("xslt-key", xsltEntry);
        
        transformMediator = new XSLTMediator();
        transformMediator.setXsltKey("xslt-key");
        
        MessageContext mc = new TestMessageContext();
        mc.setConfiguration(cfg);
        mc.setEnvironment(new Axis2SynapseEnvironment(new ConfigurationContext(new AxisConfiguration()), cfg));
        mc.setEnvelope(new StAXSOAPModelBuilder(StAXUtils.createXMLStreamReader(new FileInputStream("./../../repository/conf/sample/resources/transform/encoding_test.xml"))).getSOAPEnvelope());
        
        transformMediator.mediate(mc);
        
        OMElement resultElement = mc.getEnvelope().getBody().getFirstElement();
        assertEquals(BaseConstants.DEFAULT_TEXT_WRAPPER, resultElement.getQName());
        assertEquals("\u00e0 peine arriv\u00e9s nous entr\u00e2mes dans sa chambre", resultElement.getText());
    }
}
