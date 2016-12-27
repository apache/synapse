/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.mediators.elementary;

import junit.framework.TestCase;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.TestUtils;
import org.apache.synapse.util.xpath.SynapseXPath;

import javax.xml.namespace.QName;

public class EnrichMediatorTest extends TestCase {

    public void testEnrich1() throws Exception {
        OMFactory fac = OMAbstractFactory.getOMFactory();
        OMElement element = fac.createOMElement(new QName("test"));
        element.setText("12345");

        EnrichMediator mediator = new EnrichMediator();
        Source source = new Source();
        source.setSourceType(EnrichMediator.INLINE);
        source.setInlineOMNode(element);

        Target target = new Target();
        target.setTargetType(EnrichMediator.BODY);
        mediator.setSource(source);
        mediator.setTarget(target);

        MessageContext msgContext = TestUtils.getTestContext("<empty/>");
        mediator.mediate(msgContext);
        OMElement firstElement = msgContext.getEnvelope().getBody().getFirstElement();
        assertEquals("test", firstElement.getLocalName());
        assertEquals("12345", firstElement.getText());
    }

    public void testEnrich2() throws Exception {
        OMFactory fac = OMAbstractFactory.getOMFactory();
        OMElement element = fac.createOMElement(new QName("test"));
        element.setText("12345");

        OMElement propElement = fac.createOMElement(new QName("property"));

        EnrichMediator mediator = new EnrichMediator();
        Source source = new Source();
        source.setSourceType(EnrichMediator.INLINE);
        source.setInlineOMNode(element);

        Target target = new Target();
        target.setTargetType(EnrichMediator.PROPERTY);
        target.setProperty("foo");
        target.setAction(Target.ACTION_ADD_CHILD);
        mediator.setSource(source);
        mediator.setTarget(target);

        MessageContext msgContext = TestUtils.getTestContext("<empty/>");
        msgContext.setProperty("foo", propElement);

        mediator.mediate(msgContext);
        OMElement firstElement = msgContext.getEnvelope().getBody().getFirstElement();
        assertEquals("empty", firstElement.getLocalName());

        OMElement result = (OMElement) msgContext.getProperty("foo");
        assertEquals("property", result.getLocalName());
        assertEquals("test", result.getFirstElement().getLocalName());
    }

    public void testEnrich3() throws Exception {
        EnrichMediator mediator = new EnrichMediator();
        Source source = new Source();
        source.setSourceType(EnrichMediator.PROPERTY);
        source.setProperty("gender");

        Target target = new Target();
        target.setTargetType(EnrichMediator.CUSTOM);
        target.setXpath(new SynapseXPath("//student/@gender"));
        mediator.setSource(source);
        mediator.setTarget(target);

        MessageContext msgContext = TestUtils.getTestContext("<student gender=\"female\"><name>John</name><age>15</age></student>");
        msgContext.setProperty("gender", "male");

        mediator.mediate(msgContext);
        OMElement element = msgContext.getEnvelope().getBody().getFirstElement();
        String result = element.getAttributeValue(new QName("gender"));
        assertEquals("male", result);
    }

    /**
     * Test for SYNAPSE-1007. Check whether message enrichment works when source is set to PROPERTY and
     * source is not cloned.
     *
     * @throws Exception
     */
    public void testEnrich4() throws Exception {

        String xml = "<student gender=\"female\"><name>John</name><age>15</age></student>";
        OMElement omElement = TestUtils.createOMElement(xml);

        EnrichMediator mediator = new EnrichMediator();
        Source source = new Source();
        source.setSourceType(EnrichMediator.PROPERTY);
        source.setProperty("msg_body");
        source.setClone(false);

        Target target = new Target();
        target.setTargetType(EnrichMediator.BODY);
        mediator.setSource(source);
        mediator.setTarget(target);

        MessageContext msgContext = TestUtils.getTestContext("<empty/>");
        msgContext.setProperty("msg_body", omElement);

        mediator.mediate(msgContext);
        OMElement element = msgContext.getEnvelope().getBody().getFirstElement();
        assertEquals("student", element.getLocalName());
    }
}
