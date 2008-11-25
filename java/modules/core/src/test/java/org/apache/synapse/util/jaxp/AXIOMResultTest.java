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

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.axiom.om.impl.builder.SAXOMBuilder;
import org.custommonkey.xmlunit.XMLTestCase;
import org.xml.sax.InputSource;

public class AXIOMResultTest extends XMLTestCase {
    private InputStream getInputStream() {
        return AXIOMSourceTest.class.getResourceAsStream("test.xml");
    }
    
    public void test() throws Exception {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        SAXOMBuilder builder = new SAXOMBuilder();
        StreamSource source = new StreamSource(getInputStream());
        SAXResult result = new SAXResult(builder);
        transformer.transform(source, result);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        builder.getRootElement().serialize(out);
//        System.out.write(out.toByteArray());
        assertXMLEqual(new InputSource(getInputStream()), new InputSource(new ByteArrayInputStream(out.toByteArray())));
    }
}
