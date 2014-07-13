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

import org.apache.axiom.om.impl.exception.XMLComparisonException;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.impl.llom.util.XMLComparator;
import org.apache.axiom.om.OMElement;
import org.apache.synapse.mediators.validate.ValidateMediatorSerializer;
import org.apache.synapse.mediators.validate.ValidateMediatorFactory;
import org.apache.synapse.config.xml.MediatorFactory;
import org.apache.synapse.config.xml.MediatorSerializer;
import org.apache.xmlbeans.xml.stream.XMLStreamException;
import junit.framework.TestCase;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLInputFactory;
import java.io.StringReader;

public class ValidateMediatorSerializationTest extends TestCase {

    private ValidateMediatorFactory validateMediatorFactory = null;
    private ValidateMediatorSerializer validateMediatorSerializer = null;

    public ValidateMediatorSerializationTest() {
        validateMediatorFactory = new ValidateMediatorFactory();
        validateMediatorSerializer = new ValidateMediatorSerializer();
    }

    public void testValidateMediatorSerialization() {

        String validateConfiguration = "<syn:validate xmlns:syn=\"http://ws.apache.org/ns/synapse\" source=\"//regRequest\">" +
                "<syn:schema key=\"file:synapse_repository/conf/sample/validate.xsd\"/>" +
                "<syn:on-fail>" +
                "<syn:drop/>" +
                "</syn:on-fail>" +
                "</syn:validate>";

        try {
            assertTrue(serialization(validateConfiguration, validateMediatorFactory, validateMediatorSerializer));
        } catch (Exception e) {
            fail("Exception in test.");
        }
    }

    private boolean serialization(String inputXml, MediatorFactory mediatorFactory, MediatorSerializer mediatorSerializer) throws XMLComparisonException {

        XMLComparator comparator = new XMLComparator();

        OMElement inputOM = createOMElement(inputXml);
        Mediator mediator = mediatorFactory.createMediator(inputOM);
        OMElement resultOM = mediatorSerializer.serializeMediator(null, mediator);
        return comparator.compare(resultOM, inputOM);
    }

    private OMElement createOMElement(String xml) {
        try {

            XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(xml));
            StAXOMBuilder builder = new StAXOMBuilder(reader);
            OMElement omElement = builder.getDocumentElement();
            return omElement;
        }
        catch (javax.xml.stream.XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }
}

