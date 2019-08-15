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

package org.apache.synapse.config.xml.templates;

import org.apache.axiom.om.OMElement;
import org.apache.synapse.config.xml.AbstractTestCase;
import org.apache.synapse.config.xml.TemplateMediatorSerializer;
import org.apache.synapse.config.xml.XMLToTemplateMapper;
import org.apache.synapse.config.xml.endpoints.TemplateSerializer;
import org.apache.synapse.endpoints.Template;
import org.apache.synapse.mediators.template.TemplateMediator;

public class TemplateSerializationTest extends AbstractTestCase {

    public void testEndpointTemplateScenarioOne() throws Exception {
        String inputXML = "<template name=\"t1\" xmlns=\"http://ws.apache.org/ns/synapse\">" +
                "<parameter name=\"name\"/><parameter name=\"uri\"/><endpoint/>" +
                "</template>" ;

        OMElement inputElement = createOMElement(inputXML);
        XMLToTemplateMapper mapper = new XMLToTemplateMapper();
        Template template = (Template) mapper.getObjectFromOMNode(inputElement, null);

        OMElement serializedOut = new TemplateSerializer().serializeEndpointTemplate(template, null);
        assertTrue(compare(serializedOut, inputElement));
    }

    public void testEndpointTemplateScenarioTwo() throws Exception {
        String inputXML = "<template name=\"t1\" xmlns=\"http://ws.apache.org/ns/synapse\">" +
                "<parameter name=\"name\"/><parameter name=\"uri\"/>" +
                "<endpoint><timeout><duration>10000</duration></timeout></endpoint>" +
                "</template>" ;

        OMElement inputElement = createOMElement(inputXML);
        XMLToTemplateMapper mapper = new XMLToTemplateMapper();
        Template template = (Template) mapper.getObjectFromOMNode(inputElement, null);

        OMElement serializedOut = new TemplateSerializer().serializeEndpointTemplate(template, null);
        assertTrue(compare(serializedOut,inputElement));
    }

    public void testSequenceTemplateScenarioOne() throws Exception {
        String inputXML = "<template name=\"t2\" xmlns=\"http://ws.apache.org/ns/synapse\">" +
                "<sequence/>" +
                "</template>" ;

        OMElement inputElement = createOMElement(inputXML);
        XMLToTemplateMapper mapper = new XMLToTemplateMapper();
        TemplateMediator template = (TemplateMediator) mapper.getObjectFromOMNode(inputElement, null);

        OMElement serializedOut = new TemplateMediatorSerializer().serializeMediator(null, template);
        assertTrue(compare(serializedOut,inputElement));
    }

    public void testSequenceTemplateScenarioTwo() throws Exception {
        String inputXML = "<template name=\"t2\" xmlns=\"http://ws.apache.org/ns/synapse\">" +
                "<sequence><log/><drop/></sequence>" +
                "</template>" ;

        OMElement inputElement = createOMElement(inputXML);
        XMLToTemplateMapper mapper = new XMLToTemplateMapper();
        TemplateMediator template = (TemplateMediator) mapper.getObjectFromOMNode(inputElement, null);

        OMElement serializedOut = new TemplateMediatorSerializer().serializeMediator(null, template);
        assertTrue(compare(serializedOut,inputElement));
    }

    public void testSequenceTemplateScenarioThree() throws Exception {
        String inputXML = "<template name=\"t2\" xmlns=\"http://ws.apache.org/ns/synapse\">" +
                "<parameter name=\"foo\"/><parameter name=\"bar\"/>" +
                "<sequence><log/><drop/></sequence>" +
                "</template>" ;

        OMElement inputElement = createOMElement(inputXML);
        XMLToTemplateMapper mapper = new XMLToTemplateMapper();
        TemplateMediator template = (TemplateMediator) mapper.getObjectFromOMNode(inputElement, null);

        OMElement serializedOut = new TemplateMediatorSerializer().serializeMediator(null, template);
        assertTrue(compare(serializedOut,inputElement));
    }
}
