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
package org.apache.synapse.mediators.builtin;

import junit.framework.TestCase;
import org.apache.axiom.om.*;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.synapse.SynapseContext;
import org.apache.synapse.TestSynapseMessage;
import org.apache.synapse.TestSynapseMessageContext;
import org.apache.synapse.mediators.TestMediateHandler;
import org.apache.synapse.mediators.TestMediator;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.StringReader;
import java.io.File;

public class ValidateMediatorTest extends TestCase implements TestMediateHandler {

    private static final String VALID_ENVELOPE =
            "<m0:CheckPriceRequest xmlns:m0=\"http://www.apache-synapse.org/test\">\n" +
            "\t<m0:Code>String</m0:Code>\n" +
            "</m0:CheckPriceRequest>\n";

    private static final String IN_VALID_ENVELOPE =
            "<m0:CheckPriceRequest xmlns:m0=\"http://www.apache-synapse.org/test\">\n" +
            "\t<m0:Codes>String</m0:Codes>\n" +
            "</m0:CheckPriceRequest>\n";

    private boolean onFailInvoked = false;
    private TestMediator testMediator = null;

    public void setUp() {
        testMediator = new TestMediator();
        testMediator.setHandler(this);
    }

    public void handle(SynapseContext synCtx) {
        onFailInvoked = true;
    }

    public void setOnFailInvoked(boolean onFailInvoked) {
        this.onFailInvoked = onFailInvoked;
    }

    public void testValidateMedaitorValidCase() throws Exception {
        setOnFailInvoked(false);

        // create a validate mediator
        ValidateMediator validate = new ValidateMediator();

        // set the schema url, source xpath and any name spaces
        System.out.println("Current Dir : " + new File(".").getAbsolutePath());
        validate.setSchemaUrl("test-resources/misc/validate.xsd");
        AXIOMXPath source = new AXIOMXPath("//m0:CheckPriceRequest");
        source.addNamespace("m0", "http://www.apache-synapse.org/test");
        validate.setSource(source);

        // set dummy mediator to be called on fail
        validate.addChild(testMediator);

        // test validate mediator, with static enveope
        validate.mediate(getTestContext(VALID_ENVELOPE));

        assertTrue(!onFailInvoked);
    }

    public void testValidateMedaitorInvalidCase() throws Exception {
        setOnFailInvoked(false);

        // create a validate mediator
        ValidateMediator validate = new ValidateMediator();

        // set the schema url, source xpath and any name spaces
        validate.setSchemaUrl("modules/core/test-resources/misc/validate.xsd");
        AXIOMXPath source = new AXIOMXPath("//m0:CheckPriceRequest");
        source.addNamespace("m0", "http://www.apache-synapse.org/test");
        validate.setSource(source);

        // set dummy mediator to be called on fail
        validate.addChild(testMediator);

        // test validate mediator, with static enveope
        validate.mediate(getTestContext(IN_VALID_ENVELOPE));

        assertTrue(onFailInvoked);
    }

    private TestSynapseMessageContext getTestContext(String bodyText) throws Exception {

        // create a test synapse context
        TestSynapseMessageContext synCtx = new TestSynapseMessageContext();
        TestSynapseMessage synMsg = new TestSynapseMessage();

        SOAPEnvelope envelope = OMAbstractFactory.getSOAP11Factory().getDefaultEnvelope();
        OMDocument omDoc = OMAbstractFactory.getSOAP11Factory().createOMDocument();
        omDoc.addChild(envelope);

        XMLStreamReader parser = XMLInputFactory.newInstance().
            createXMLStreamReader(new StringReader(bodyText));
        StAXOMBuilder builder = new StAXOMBuilder(parser);

        // set a dummy static message
        envelope.getBody().addChild(builder.getDocumentElement());

        synMsg.setEnvelope(envelope);
        synCtx.setSynapseMessage(synMsg);
        return synCtx;
    }

}
