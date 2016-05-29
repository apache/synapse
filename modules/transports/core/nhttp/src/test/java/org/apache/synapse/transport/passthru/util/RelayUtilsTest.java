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
package org.apache.synapse.transport.passthru.util;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.Charset;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMXMLBuilderFactory;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.io.input.ReaderInputStream;
import org.junit.Before;
import org.junit.Test;

public class RelayUtilsTest {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private final String xml =
            "<s:Envelope xmlns:s='http://schemas.xmlsoap.org/soap/envelope/'>"
            + "<s:Body><payload><data>value</data></payload></s:Body>"
            + "</s:Envelope>";

    private final QName payloadQName = new QName("payload");

    MessageContext msgCtx;

    @Before
    public void setUp() throws Exception {
        msgCtx = new MessageContext();

        AxisConfiguration configuration = new AxisConfiguration();
        ConfigurationContext context = new ConfigurationContext(configuration);
        msgCtx.setConfigurationContext(context);
    }

    @Test
    public void testSOAPBodyIsntFullyReadWhenNotUsingBinaryRelayBuilder()
            throws IOException, XMLStreamException {

        SOAPEnvelope envelope = OMXMLBuilderFactory
                .createSOAPModelBuilder(new StringReader(xml)).getSOAPEnvelope();

        msgCtx.setEnvelope(envelope);

        // Build message when using pass through pipe or binary relay builder
        RelayUtils.buildMessage(msgCtx);

        // Ensure that the payload element is accessible
        assertEquals(payloadQName, msgCtx.getEnvelope().getBody().getFirstElement().getQName());

        // Ensure that the body isn't fully build to support the use of deferred building
        assertFalse(msgCtx.getEnvelope().getBody().isComplete());
    }

    @Test
    public void testBinaryRelayPayloadExpandsToOriginalPayload()
            throws IOException, XMLStreamException {

        // Transform request soap message into a binary payload
        BinaryRelayBuilder builder = new BinaryRelayBuilder();
        InputStream stream = new ReaderInputStream(new StringReader(xml), UTF8);
        OMElement element = builder.processDocument(stream, "text/xml", msgCtx);
        msgCtx.setEnvelope((SOAPEnvelope)element);

        // Build message when using pass through pipe or binary relay builder
        RelayUtils.buildMessage(msgCtx);

        // Ensure that the binary payload is transformed to the appropriate element
        assertEquals(payloadQName, msgCtx.getEnvelope().getBody().getFirstElement().getQName());

        // Ensure that the body isn't fully build to support the use of deferred building
        assertFalse(msgCtx.getEnvelope().getBody().isComplete());
    }

}
