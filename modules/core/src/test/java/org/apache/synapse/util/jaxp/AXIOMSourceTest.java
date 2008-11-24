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

package org.apache.synapse.util.jaxp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;

import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.custommonkey.xmlunit.XMLTestCase;
import org.xml.sax.InputSource;

public class AXIOMSourceTest extends XMLTestCase {
    private InputStream getInputStream() {
        return AXIOMSourceTest.class.getResourceAsStream("test.xml");
    }
    
    public void test() throws Exception {
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        // Turn off coalescing mode so that CDATA sections are reported by the StAX parser
        inputFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.FALSE);
        XMLStreamReader reader = inputFactory.createXMLStreamReader(getInputStream());
        StAXOMBuilder builder = new StAXOMBuilder(reader);
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        AXIOMSource source = new AXIOMSource(builder.getDocumentElement());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        StreamResult result = new StreamResult(out);
        transformer.transform(source, result);
//        System.out.write(out.toByteArray());
        assertXMLEqual(new InputSource(getInputStream()), new InputSource(new ByteArrayInputStream(out.toByteArray())));
    }
}
