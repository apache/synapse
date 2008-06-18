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

package org.apache.synapse.mediators;

import java.io.StringReader;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMDocument;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.synapse.MessageContext;
import org.apache.synapse.TestMessageContextBuilder;
import org.apache.synapse.config.Entry;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;

public class TestUtils {

    public static MessageContext getTestContext(String bodyText, Map<String,Entry> props) throws Exception {
        TestMessageContextBuilder builder = new TestMessageContextBuilder();
        builder.setBodyFromString(bodyText);
        if (props != null) {
            for (Map.Entry<String,Entry> mapEntry : props.entrySet()) {
                builder.addEntry(mapEntry.getKey(), mapEntry.getValue());
            }
        }
        return builder.build();
    }

    public static Axis2MessageContext getAxis2MessageContext(String bodyText,
                                                             Map<String,Entry> props) throws Exception {
        TestMessageContextBuilder builder = new TestMessageContextBuilder();
        builder.setRequireAxis2MessageContext(true);
        builder.setBodyFromString(bodyText);
        if (props != null) {
            for (Map.Entry<String,Entry> mapEntry : props.entrySet()) {
                builder.addEntry(mapEntry.getKey(), mapEntry.getValue());
            }
        }
        return (Axis2MessageContext)builder.build();
    }

    public static MessageContext getTestContext(String bodyText) throws Exception {
        return getTestContext(bodyText, null);
    }

    public static MessageContext createLightweightSynapseMessageContext(
            String payload) throws Exception {
        org.apache.axis2.context.MessageContext mc =
                new org.apache.axis2.context.MessageContext();
        SynapseConfiguration config = new SynapseConfiguration();
        SynapseEnvironment env = new Axis2SynapseEnvironment(config);
        MessageContext synMc = new Axis2MessageContext(mc, config, env);
        SOAPEnvelope envelope =
                OMAbstractFactory.getSOAP11Factory().getDefaultEnvelope();
        OMDocument omDoc =
                OMAbstractFactory.getSOAP11Factory().createOMDocument();
        omDoc.addChild(envelope);

        envelope.getBody().addChild(createOMElement(payload));

        synMc.setEnvelope(envelope);
        return synMc;
    }

    public static OMElement createOMElement(String xml) {
        try {

            XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(xml));
            StAXOMBuilder builder = new StAXOMBuilder(reader);
            OMElement omElement = builder.getDocumentElement();
            return omElement;

        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

}
