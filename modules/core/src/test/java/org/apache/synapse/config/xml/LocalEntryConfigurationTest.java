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

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.synapse.config.Entry;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.StringReader;

public class LocalEntryConfigurationTest extends AbstractTestCase {

    public void testSimpleTextEntry() {
        String text = "Apache Synapse";
        String entrySrc = "<localEntry xmlns=\"http://synapse.apache.org/ns/2010/04/configuration\" " +
                "key=\"foo\"><![CDATA[" + text + "]]></localEntry>";
        try {
            OMElement source = parseEntrySource(entrySrc);
            Entry entry = EntryFactory.createEntry(source);
            assertEquals(text, entry.getValue());

            OMElement serialization = EntrySerializer.serializeEntry(entry, null);
            assertTrue(compare(source, serialization));
        } catch (XMLStreamException e) {
            fail("Simple Text Entry test failed: " + e.getMessage());
        }
    }

    public void testSimpleXMLEntry() {
        String entrySrc = "<localEntry xmlns=\"http://synapse.apache.org/ns/2010/04/configuration\" " +
                "key=\"foo\"><project><id>001</id><name>Synapse</name></project></localEntry>";
        try {
            OMElement source = parseEntrySource(entrySrc);
            OMElement original = source.cloneOMElement();
            Entry entry = EntryFactory.createEntry(source);
            assertTrue(compare(original.getFirstElement(), (OMElement) entry.getValue()));

            OMElement serialization = EntrySerializer.serializeEntry(entry, null);
            assertTrue(compare(original, serialization));
        } catch (XMLStreamException e) {
            fail("Simple XML Entry test failed: " + e.getMessage());
        }
    }

    // TODO: Fix SYNAPSE-624
    /*public void testLargeTextEntry() {
        String largeText = "Apache Synapse is designed to be a simple, lightweight and high " +
                "performance Enterprise Service Bus (ESB) from Apache. Based on a small " +
                "asynchronous core, Apache Synapse has excellent support for XML and Web " +
                "services - as well as binary and text formats. The Synapse engine is configured " +
                "with a simple XML format and comes with a set of ready-to-use transports and " +
                "mediators. We recommend you start by reading the QuickStart and then trying out " +
                "the samples. Synapse is made available under the Apache Software License 2.0. " +
                "For more detailsplease visit http://synapse.apache.org";

        String entrySrc = "<localEntry xmlns=\"http://synapse.apache.org/ns/2010/04/configuration\" " +
                "key=\"foo\"><![CDATA[" + largeText + "]]></localEntry>";
        try {
            OMElement source = parseEntrySource(entrySrc);
            Entry entry = EntryFactory.createEntry(source);
            assertEquals(largeText, entry.getValue());
        } catch (XMLStreamException e) {
            fail("Simple Text Entry test failed: " + e.getMessage());
        }
    }*/

    public void testLargeXMLEntry() {

        String entrySrc =
                "<localEntry xmlns=\"http://synapse.apache.org/ns/2010/04/configuration\" key=\"foo\">" +
                "<wsdl:definitions name=\"Imported\"\n" +
                "                  targetNamespace=\"http://www.example.com/imported\"\n" +
                "                  xmlns:tns=\"http://www.example.com/imported\"\n" +
                "                  xmlns:s=\"http://www.example.com/schema\"\n" +
                "                  xmlns:wsdl=\"http://schemas.xmlsoap.org/wsdl/\"\n" +
                "                  xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">\n" +
                "    <wsdl:types>\n" +
                "        <xsd:schema targetNamespace=\"http://www.example.com/imported\">\n" +
                "            <xsd:import namespace=\"http://www.example.com/schema\" schemaLocation=\"imported.xsd\"/>\n" +
                "            <xsd:element name=\"getTestData\">\n" +
                "                <xsd:complexType>\n" +
                "                    <xsd:sequence>\n" +
                "                        <xsd:element name=\"test\" type=\"s:SomeType\" minOccurs=\"1\" maxOccurs=\"1\"/>\n" +
                "                    </xsd:sequence>\n" +
                "                </xsd:complexType>\n" +
                "            </xsd:element>\n" +
                "            <xsd:element name=\"getTestDataResponse\">\n" +
                "                <xsd:complexType>\n" +
                "                    <xsd:sequence>\n" +
                "                        <xsd:element name=\"test\" type=\"s:SomeType\" minOccurs=\"1\" maxOccurs=\"1\"/>\n" +
                "                    </xsd:sequence>\n" +
                "                </xsd:complexType>\n" +
                "            </xsd:element>\n" +
                "        </xsd:schema>\n" +
                "    </wsdl:types>\n" +
                "    <wsdl:message name=\"getTestDataRequest\">\n" +
                "        <wsdl:part name=\"parameters\" element=\"tns:getTestData\"/>\n" +
                "    </wsdl:message>\n" +
                "    <wsdl:message name=\"getTestDataResponse\">\n" +
                "        <wsdl:part name=\"parameters\" element=\"tns:getTestDataResponse\"/>\n" +
                "    </wsdl:message>\n" +
                "    <wsdl:portType name=\"Test\">\n" +
                "        <wsdl:operation name=\"getTestData\">\n" +
                "            <wsdl:input message=\"tns:getTestDataRequest\" name=\"getTestData\"/>\n" +
                "            <wsdl:output message=\"tns:getTestDataResponse\" name=\"getTestDataResponse\"/>\n" +
                "        </wsdl:operation>\n" +
                "    </wsdl:portType>\n" +
                "</wsdl:definitions>" +
                "</localEntry>";

        try {
            OMElement source = parseEntrySource(entrySrc);
            OMElement original = source.cloneOMElement();
            Entry entry = EntryFactory.createEntry(source);
            assertTrue(compare(original.getFirstElement(), (OMElement) entry.getValue()));

            OMElement serialization = EntrySerializer.serializeEntry(entry, null);
            assertTrue(compare(original, serialization));
        } catch (XMLStreamException e) {
            fail("Large XML Entry test failed: " + e.getMessage());
        }
    }

    private OMElement parseEntrySource(String src) throws XMLStreamException {
        StringReader strReader = new StringReader(src);
        XMLInputFactory xmlInFac = XMLInputFactory.newInstance();
        //Non-Coalescing parsing
        xmlInFac.setProperty("javax.xml.stream.isCoalescing", false);

        XMLStreamReader parser = xmlInFac.createXMLStreamReader(strReader);
        StAXOMBuilder builder = new StAXOMBuilder(parser);
        return builder.getDocumentElement();
    }
}
