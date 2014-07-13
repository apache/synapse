/*
* Copyright 2004,2005 The Apache Software Foundation.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.apache.synapse.config.xml;

import junit.framework.TestCase;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.impl.exception.XMLComparisonException;
import org.apache.axiom.om.impl.llom.util.XMLComparator;
import org.apache.synapse.Mediator;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.StringReader;

/**
 *
 *
 */

public abstract class AbstractTestCase extends TestCase {

    XMLComparator comparator = null;

    public AbstractTestCase(String name) {
        super(name);
    }

    public AbstractTestCase() {
    }

    protected void setUp() throws Exception {
        super.setUp();
        comparator = new XMLComparator();
    }

    protected OMElement createOMElement(String xml) {
        try {

            XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(xml));
            StAXOMBuilder builder = new StAXOMBuilder(reader);
            OMElement omElement = builder.getDocumentElement();
            return omElement;

        }
        catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    protected boolean serialization(String inputXml, MediatorFactory mediatorFactory, MediatorSerializer mediatorSerializer) throws XMLComparisonException {

        OMElement inputOM = createOMElement(inputXml);
        Mediator mediator = mediatorFactory.createMediator(inputOM);
        OMElement resultOM = mediatorSerializer.serializeMediator(null, mediator);
        return comparator.compare(resultOM, inputOM);
    }

    protected boolean serialization(String inputXml, MediatorSerializer mediatorSerializer) throws XMLComparisonException {
        OMElement inputOM = createOMElement(inputXml);
        Mediator mediator = MediatorFactoryFinder.getInstance().getMediator(inputOM);
        OMElement resultOM = mediatorSerializer.serializeMediator(null, mediator);
        return comparator.compare(resultOM, inputOM);
    }

    protected OMElement getParent() {
        String parentXML = "<synapse xmlns=\"http://ws.apache.org/ns/synapse\"><definitions></definitions></synapse>";
        return createOMElement(parentXML);
    }

    protected boolean compare(OMElement inputElement, OMElement serializedElement) throws XMLComparisonException {
        return  comparator.compare(inputElement, serializedElement);
    }
}
