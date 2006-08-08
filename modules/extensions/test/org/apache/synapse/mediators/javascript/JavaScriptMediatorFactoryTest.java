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
package org.apache.synapse.mediators.javascript;

import junit.framework.TestCase;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.synapse.Mediator;

import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;

public class JavaScriptMediatorFactoryTest extends TestCase {

    private static final OMElement TRUE_MEDIATOR_CONFIG = createOMElement(
            "<javascript>" + 
            "   <![CDATA[function mediate(mc) {return true}]]>" +
            "</javascript>");

    private static final OMElement FALSE_MEDIATOR_CONFIG = createOMElement(
            "<javascript>" +
            "   <![CDATA[function mediate(mc) {return false}]]>" +
            "</javascript>");

    public void testJavaScriptMediatorFactory1() throws XMLStreamException {
        JavaScriptMediatorFactory mf = new JavaScriptMediatorFactory();
        Mediator mediator = mf.createMediator(TRUE_MEDIATOR_CONFIG);
        assertTrue(mediator.mediate(null));
    }

    public void testJavaScriptMediatorFactory2() throws XMLStreamException {
        JavaScriptMediatorFactory mf = new JavaScriptMediatorFactory();
        Mediator mediator = mf.createMediator(FALSE_MEDIATOR_CONFIG);
        assertFalse(mediator.mediate(null));
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
