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

package org.apache.synapse.mediators.validate;

import junit.framework.TestCase;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.synapse.mediators.TestUtils;
import org.apache.synapse.mediators.builtin.ValidateMediator;
import org.apache.synapse.config.xml.ValidateMediatorFactory;
import org.apache.synapse.config.Entry;
import org.apache.synapse.TestMediator;
import org.apache.synapse.TestMediateHandler;
import org.apache.synapse.MessageContext;

import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.net.URL;

public class ValidateMediatorTest extends TestCase {

    private static final String SCHEMA_FULL_CHECKING_FEATURE_ID = 
        "http://apache.org/xml/features/validation/schema-full-checking";

    private static final String HONOUR_ALL_SCHEMA_LOCATIONS_FEATURE_ID = 
        "http://apache.org/xml/features/honour-all-schemaLocations";

    public static final String FEATURE_SECURE_PROCESSING =
         "http://javax.xml.XMLConstants/feature/secure-processing";

    private static final String VALID_ENVELOPE_TWO_SCHEMAS =
            "<Outer xmlns=\"http://services.samples/xsd2\">" +
            "<m0:CheckPriceRequest xmlns:m0=\"http://services.samples/xsd\">\n" +
            "<m0:Code>String</m0:Code>\n" +
            "</m0:CheckPriceRequest>\n" +
            "<m1:CheckPriceRequest2 xmlns:m1=\"http://services.samples/xsd2\">\n" +
            "<m1:Code2>String</m1:Code2>\n" +
            "</m1:CheckPriceRequest2>\n" +
            "</Outer>";

    private static final String INVALID_ENVELOPE_TWO_SCHEMAS =
            "<Outer xmlns=\"http://services.samples/xsd2\">" +
            "<m1:CheckPriceRequest2 xmlns:m1=\"http://services.samples/xsd2\">\n" +
            "<m1:Code2>String</m1:Code2>\n" +
            "</m1:CheckPriceRequest2>\n" +
            "<m0:CheckPriceRequest xmlns:m0=\"http://services.samples/xsd\">\n" +
            "<m0:Code>String</m0:Code>\n" +
            "</m0:CheckPriceRequest>\n" +
            "</Outer>";

    private static final String VALID_ENVELOPE =
            "<m0:CheckPriceRequest xmlns:m0=\"http://services.samples/xsd\">\n" +
            "\t<m0:Code>String</m0:Code>\n" +
            "</m0:CheckPriceRequest>\n";

    private static final String IN_VALID_ENVELOPE =
            "<m0:CheckPriceRequest xmlns:m0=\"http://services.samples/xsd\">\n" +
            "\t<m0:Codes>String</m0:Codes>\n" +
            "</m0:CheckPriceRequest>\n";

    private static final String VALID_ENVELOPE_NO_NS =
            "<CheckPriceRequest xmlns=\"http://services.samples/xsd\">\n" +
            "<Code>String</Code>\n" +
            "</CheckPriceRequest>\n";

    private static final String IN_VALID_ENVELOPE_NO_NS =
            "<CheckPriceRequest xmlns=\"http://services.samples/xsd\">\n" +
            "<Codes>String</Codes>\n" +
            "</CheckPriceRequest>\n";

    private static final String DEFAULT_FEATURES_MEDIATOR_CONFIG = 
            "<validate xmlns=\"http://ws.apache.org/ns/synapse\">" +
            "   <schema key=\"file:synapse_repository/conf/sample/validate.xsd\"/>" +
            "   <on-fail>" +
            "       <makefault>" +
            "           <code value=\"tns:Receiver\" xmlns:tns=\"http://www.w3.org/2003/05/soap-envelope\"/>" +
            "           <reason value=\"Invalid request\"/>" +
            "       </makefault>" +
            "   </on-fail>" +
            "</validate>";

    private static final String CUSTOM_FEATURES_MEDIATOR_CONFIG = 
            "<validate xmlns=\"http://ws.apache.org/ns/synapse\">" +
            "   <schema key=\"file:synapse_repository/conf/sample/validate.xsd\"/>" +
            "   <feature name=\"" + SCHEMA_FULL_CHECKING_FEATURE_ID + "\" value=\"false\"/>" +
            "   <feature name=\"" + HONOUR_ALL_SCHEMA_LOCATIONS_FEATURE_ID + "\" value=\"true\"/>" +
            "   <on-fail>" +
            "       <makefault>" +
            "           <code value=\"tns:Receiver\" xmlns:tns=\"http://www.w3.org/2003/05/soap-envelope\"/>" +
            "           <reason value=\"Invalid request\"/>" +
            "       </makefault>" +
            "   </on-fail>" +
            "</validate>";

    private static final String REG_KEY =
            "<validate xmlns=\"http://ws.apache.org/ns/synapse\">" +
            "   <schema key=\"file:synapse_repository/conf/sample/validate.xsd\"/>" +
            "   <on-fail>" +
            "       <makefault>" +
            "           <code value=\"tns:Receiver\" xmlns:tns=\"http://www.w3.org/2003/05/soap-envelope\"/>" +
            "           <reason value=\"Invalid request\"/>" +
            "       </makefault>" +
            "   </on-fail>" +
            "</validate>";

    private boolean onFailInvoked = false;
    private TestMediator testMediator = null;

    public void setUp() {
        testMediator = new TestMediator();
        testMediator.setHandler(
            new TestMediateHandler() {
                public void handle(MessageContext synCtx) {
                    setOnFailInvoked(true);
                }
            });
    }

    public void setOnFailInvoked(boolean onFailInvoked) {
        this.onFailInvoked = onFailInvoked;
    }

    public void testValidateMediatorValidCase() throws Exception {
        setOnFailInvoked(false);

        // create a validate mediator
        ValidateMediator validate = new ValidateMediator();

        // set the schema url, source xpath and any name spaces
        List keys = new ArrayList();
        keys.add("xsd-key");
        validate.setSchemaKeys(keys);
        AXIOMXPath source = new AXIOMXPath("//m0:CheckPriceRequest");
        source.addNamespace("m0", "http://services.samples/xsd");
        validate.setSource(source);

        // set dummy mediator to be called on fail
        validate.addChild(testMediator);

        Map props = new HashMap();
        Entry prop = new Entry();
        prop.setType(Entry.URL_SRC);
        prop.setSrc(new URL("file:./../../repository/conf/sample/resources/validate/validate.xsd"));
        props.put("xsd-key", prop);

        // test validate mediator, with static enveope
        validate.mediate(TestUtils.getTestContext(VALID_ENVELOPE, props));

        assertFalse(onFailInvoked);
    }

    public void testValidateMediatorValidCaseTwoSchemas() throws Exception {
        setOnFailInvoked(false);

        // create a validate mediator
        ValidateMediator validate = new ValidateMediator();

        // set the schema url, source xpath and any name spaces
        List keys = new ArrayList();
        keys.add("xsd-key-1");
        keys.add("xsd-key-2");
        validate.setSchemaKeys(keys);
        AXIOMXPath source = new AXIOMXPath("//m0:Outer");
        source.addNamespace("m0", "http://services.samples/xsd2");
        validate.setSource(source);

        // set dummy mediator to be called on fail
        validate.addChild(testMediator);

        Map props = new HashMap();
        Entry prop = new Entry();
        prop.setType(Entry.URL_SRC);
        prop.setSrc(new URL("file:./../../repository/conf/sample/resources/validate/validate.xsd"));
        props.put("xsd-key-1", prop);
        Entry prop2 = new Entry();
        prop2.setType(Entry.URL_SRC);
        prop2.setSrc(new URL("file:./../../repository/conf/sample/resources/validate/validate2.xsd"));
        props.put("xsd-key-2", prop2);

        // test validate mediator, with static enveope
        validate.mediate(TestUtils.getTestContext(VALID_ENVELOPE_TWO_SCHEMAS, props));

        assertFalse(onFailInvoked);
    }

    public void testValidateMediatorInvalidCaseTwoSchemas() throws Exception {
        setOnFailInvoked(false);

        // create a validate mediator
        ValidateMediator validate = new ValidateMediator();

        // set the schema url, source xpath and any name spaces
        List keys = new ArrayList();
        keys.add("xsd-key-1");
        keys.add("xsd-key-2");
        validate.setSchemaKeys(keys);
        AXIOMXPath source = new AXIOMXPath("//m0:Outer");
        source.addNamespace("m0", "http://services.samples/xsd2");
        validate.setSource(source);

        // set dummy mediator to be called on fail
        validate.addChild(testMediator);

        Map props = new HashMap();
        Entry prop = new Entry();
        prop.setType(Entry.URL_SRC);
        prop.setSrc(new URL("file:./../../repository/conf/sample/resources/validate/validate.xsd"));
        props.put("xsd-key-1", prop);
        Entry prop2 = new Entry();
        prop2.setType(Entry.URL_SRC);
        prop2.setSrc(new URL("file:./../../repository/conf/sample/resources/validate/validate2.xsd"));
        props.put("xsd-key-2", prop2);

        // test validate mediator, with static enveope
        validate.mediate(TestUtils.getTestContext(INVALID_ENVELOPE_TWO_SCHEMAS, props));

        assertTrue(onFailInvoked);
    }

    public void testValidateMediatorInvalidCase() throws Exception {
        setOnFailInvoked(false);

        // create a validate mediator
        ValidateMediator validate = new ValidateMediator();

        // set the schema url, source xpath and any name spaces
        List keys = new ArrayList();
        keys.add("xsd-key-1");
        validate.setSchemaKeys(keys);
        AXIOMXPath source = new AXIOMXPath("//m0:CheckPriceRequest");
        source.addNamespace("m0", "http://services.samples/xsd");
        validate.setSource(source);

        // set dummy mediator to be called on fail
        validate.addChild(testMediator);

        Map props = new HashMap();
        Entry prop = new Entry();
        prop.setType(Entry.URL_SRC);
        prop.setSrc(new URL("file:./../../repository/conf/sample/resources/validate/validate.xsd"));
        props.put("xsd-key-1", prop);

        // test validate mediator, with static enveope
        validate.mediate(TestUtils.getTestContext(IN_VALID_ENVELOPE, props));

        assertTrue(onFailInvoked);
    }

    public void testValidateMediatorValidCaseNoNS() throws Exception {
        setOnFailInvoked(false);

        // create a validate mediator
        ValidateMediator validate = new ValidateMediator();

        // set the schema url, source xpath and any name spaces
        List keys = new ArrayList();
        keys.add("xsd-key-1");
        validate.setSchemaKeys(keys);
        AXIOMXPath source = new AXIOMXPath("//m0:CheckPriceRequest");
        source.addNamespace("m0", "http://services.samples/xsd");
        validate.setSource(source);

        // set dummy mediator to be called on fail
        validate.addChild(testMediator);

        Map props = new HashMap();
        Entry prop = new Entry();
        prop.setType(Entry.URL_SRC);
        prop.setSrc(new URL("file:./../../repository/conf/sample/resources/validate/validate.xsd"));
        props.put("xsd-key-1", prop);

        // test validate mediator, with static enveope
        validate.mediate(TestUtils.getTestContext(VALID_ENVELOPE_NO_NS, props));

        assertFalse(onFailInvoked);
    }

    public void testValidateMediatorInvalidCaseNoNS() throws Exception {
        setOnFailInvoked(false);

        // create a validate mediator
        ValidateMediator validate = new ValidateMediator();

        // set the schema url, source xpath and any name spaces
        List keys = new ArrayList();
        keys.add("xsd-key-1");
        validate.setSchemaKeys(keys);
        AXIOMXPath source = new AXIOMXPath("//m0:CheckPriceRequest");
        source.addNamespace("m0", "http://services.samples/xsd");
        validate.setSource(source);

        // set dummy mediator to be called on fail
        validate.addChild(testMediator);

        Map props = new HashMap();
        Entry prop = new Entry();
        prop.setType(Entry.URL_SRC);
        prop.setSrc(new URL("file:./../../repository/conf/sample/resources/validate/validate.xsd"));
        props.put("xsd-key-1", prop);

        // test validate mediator, with static enveope
        validate.mediate(TestUtils.getTestContext(IN_VALID_ENVELOPE_NO_NS, props));

        assertTrue(onFailInvoked);
    }

    public void testValidateMediatorDefaultFeatures() throws Exception {

        ValidateMediatorFactory mf = new ValidateMediatorFactory();
        ValidateMediator validate = (ValidateMediator)mf.createMediator(
            createOMElement(DEFAULT_FEATURES_MEDIATOR_CONFIG));

        assertNull(validate.getFeature(SCHEMA_FULL_CHECKING_FEATURE_ID));
        assertNull(validate.getFeature(HONOUR_ALL_SCHEMA_LOCATIONS_FEATURE_ID));

        makeValidInvocation(validate);
    }

    public void testValidateMediatorCustomFeatures() throws Exception 
    {
        ValidateMediatorFactory mf = new ValidateMediatorFactory();
        ValidateMediator validate = (ValidateMediator)mf.createMediator(
            createOMElement(CUSTOM_FEATURES_MEDIATOR_CONFIG));

        assertNotNull(validate.getFeature(SCHEMA_FULL_CHECKING_FEATURE_ID));
        assertFalse("true".equals((String)validate.getFeature(SCHEMA_FULL_CHECKING_FEATURE_ID)));
        assertNotNull(validate.getFeature(HONOUR_ALL_SCHEMA_LOCATIONS_FEATURE_ID));
        assertTrue("true".equals((String)validate.getFeature(HONOUR_ALL_SCHEMA_LOCATIONS_FEATURE_ID)));

        makeValidInvocation(validate);
    }

    private void makeValidInvocation(ValidateMediator validate) throws Exception {

        setOnFailInvoked(false);

        // set the schema url, source xpath and any name spaces
        List keys = new ArrayList();
        keys.add("xsd-key-1");
        validate.setSchemaKeys(keys);
        AXIOMXPath source = new AXIOMXPath("//m0:CheckPriceRequest");
        source.addNamespace("m0", "http://services.samples/xsd");
        validate.setSource(source);

        // set dummy mediator to be called on fail
        validate.removeChild(0);
        validate.addChild(testMediator);

        Map props = new HashMap();
        Entry prop = new Entry();
        prop.setType(Entry.URL_SRC);
        prop.setSrc(new URL("file:./../../repository/conf/sample/resources/validate/validate.xsd"));
        props.put("xsd-key-1", prop);


        // test validate mediator, with static enveope
        validate.mediate(TestUtils.getTestContext(VALID_ENVELOPE, props));

        assertFalse(onFailInvoked);
    }

    private static OMElement createOMElement(String xml) {
        try {
            StAXOMBuilder builder = new StAXOMBuilder(new ByteArrayInputStream(xml.getBytes()));
            OMElement omElement = builder.getDocumentElement();
            return omElement;
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }
}
