/*
 *  Copyright (c) 2005-2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.apache.synapse.config.xml;

import junit.framework.TestCase;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseConstants;

import javax.xml.stream.XMLStreamException;

public class HeaderMediatorConfigurationTest extends TestCase {

    public void testNamespaceUnqualifiedScenarioOne() {
        try {
            String inputXml = "<header xmlns=\"http://synapse.apache.org/ns/2010/04/configuration\" name=\"MyHeader\" value=\"MyValue\"/>";
            HeaderMediatorFactory fac = new HeaderMediatorFactory();
            fac.createMediator(AXIOMUtil.stringToOM(inputXml));
            fail("HeaderMediator created with namespace unqualified SOAP header");
        } catch (XMLStreamException e) {
            fail("Error while parsing header mediator configuration");
        } catch (SynapseException ignored) {

        }

        try {
            String inputXml = "<header xmlns=\"http://synapse.apache.org/ns/2010/04/configuration\" name=\"MyHeader\" action=\"remove\"/>";
            HeaderMediatorFactory fac = new HeaderMediatorFactory();
            fac.createMediator(AXIOMUtil.stringToOM(inputXml));
            fail("HeaderMediator created with namespace unqualified SOAP header");
        } catch (XMLStreamException e) {
            fail("Error while parsing header mediator configuration");
        } catch (SynapseException ignored) {

        }
    }

    public void testNamespaceUnqualifiedScenarioTwo() {
        try {
            String inputXml = "<header xmlns=\"http://synapse.apache.org/ns/2010/04/configuration\" name=\"m:MyHeader\" value=\"MyValue\"/>";
            HeaderMediatorFactory fac = new HeaderMediatorFactory();
            fac.createMediator(AXIOMUtil.stringToOM(inputXml));
            fail("HeaderMediator created with namespace unqualified SOAP header");
        } catch (XMLStreamException e) {
            fail("Error while parsing header mediator configuration");
        } catch (SynapseException ignored) {

        }

        try {
            String inputXml = "<header xmlns=\"http://synapse.apache.org/ns/2010/04/configuration\" name=\"m:MyHeader\" action=\"remove\"/>";
            HeaderMediatorFactory fac = new HeaderMediatorFactory();
            fac.createMediator(AXIOMUtil.stringToOM(inputXml));
            fail("HeaderMediator created with namespace unqualified SOAP header");
        } catch (XMLStreamException e) {
            fail("Error while parsing header mediator configuration");
        } catch (SynapseException ignored) {

        }
    }

    public void testPredefinedHeaders() {
        predefinedHeaderTest(SynapseConstants.HEADER_TO);
        predefinedHeaderTest(SynapseConstants.HEADER_ACTION);
        predefinedHeaderTest(SynapseConstants.HEADER_FROM);
        predefinedHeaderTest(SynapseConstants.HEADER_RELATES_TO);
        predefinedHeaderTest(SynapseConstants.HEADER_REPLY_TO);
        predefinedHeaderTest(SynapseConstants.HEADER_FAULT);
    }

    private void predefinedHeaderTest(String header) {
        try {
            String inputXml = "<header xmlns=\"http://synapse.apache.org/ns/2010/04/configuration\" name=\"" + header + "\" value=\"MyValue\"/>";
            HeaderMediatorFactory fac = new HeaderMediatorFactory();
            fac.createMediator(AXIOMUtil.stringToOM(inputXml));
        } catch (XMLStreamException e) {
            fail("Error while parsing header mediator configuration");
        }

        try {
            String inputXml = "<header xmlns=\"http://synapse.apache.org/ns/2010/04/configuration\" name=\"" + header + "\" action=\"remove\"/>";
            HeaderMediatorFactory fac = new HeaderMediatorFactory();
            fac.createMediator(AXIOMUtil.stringToOM(inputXml));
        } catch (XMLStreamException e) {
            fail("Error while parsing header mediator configuration");
        }
    }

    public void testNamespaceQualifiedScenario() {
        try {
            String inputXml = "<header xmlns=\"http://synapse.apache.org/ns/2010/04/configuration\" xmlns:m=\"http://synapse.apache.org\" name=\"m:MyHeader\" value=\"MyValue\"/>";
            HeaderMediatorFactory fac = new HeaderMediatorFactory();
            fac.createMediator(AXIOMUtil.stringToOM(inputXml));
        } catch (XMLStreamException e) {
            fail("Error while parsing header mediator configuration");
        }

        try {
            String inputXml = "<header xmlns=\"http://synapse.apache.org/ns/2010/04/configuration\" xmlns:m=\"http://synapse.apache.org\" name=\"m:MyHeader\" action=\"remove\"/>";
            HeaderMediatorFactory fac = new HeaderMediatorFactory();
            fac.createMediator(AXIOMUtil.stringToOM(inputXml));
        } catch (XMLStreamException e) {
            fail("Error while parsing header mediator configuration");
        }
    }

}
