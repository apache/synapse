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

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMDocument;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.synapse.MessageContext;
import org.apache.synapse.TestMessageContext;
import org.apache.synapse.config.Entry;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.registry.url.SimpleURLRegistry;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.StringReader;
import java.util.Iterator;
import java.util.Map;

public class TestUtils {

    public static TestMessageContext getTestContext(String bodyText, Map props) throws Exception {

        // create a test synapse context
        TestMessageContext synCtx = new TestMessageContext();
        SynapseConfiguration testConfig = new SynapseConfiguration();
        testConfig.setRegistry(new SimpleURLRegistry());

        if (props != null) {
            Iterator iter = props.keySet().iterator();
            while (iter.hasNext()) {
                String key = (String) iter.next();
                testConfig.addEntry(key, (Entry) props.get(key));
            }
        }
        synCtx.setConfiguration(testConfig);

        SOAPEnvelope envelope = OMAbstractFactory.getSOAP11Factory().getDefaultEnvelope();
        OMDocument omDoc = OMAbstractFactory.getSOAP11Factory().createOMDocument();
        omDoc.addChild(envelope);

        XMLStreamReader parser = XMLInputFactory.newInstance().
                createXMLStreamReader(new StringReader(bodyText));
        StAXOMBuilder builder = new StAXOMBuilder(parser);

        // set a dummy static message
        envelope.getBody().addChild(builder.getDocumentElement());

        synCtx.setEnvelope(envelope);
        return synCtx;
    }

    public static Axis2MessageContext getAxis2MessageContext(String bodyText,
                                                             Map props) throws Exception {
        // create a test synapse context
        SynapseConfiguration testConfig = new SynapseConfiguration();
        org.apache.axis2.context.MessageContext inContext =
                new org.apache.axis2.context.MessageContext();

        Axis2MessageContext synCtx = new Axis2MessageContext(inContext, testConfig, null);
        testConfig.setRegistry(new SimpleURLRegistry());

        if (props != null) {
            Iterator iter = props.keySet().iterator();
            while (iter.hasNext()) {
                String key = (String) iter.next();
                testConfig.addEntry(key, (Entry) props.get(key));
            }
        }
        synCtx.setConfiguration(testConfig);

        SOAPEnvelope envelope = OMAbstractFactory.getSOAP11Factory().getDefaultEnvelope();
        OMDocument omDoc = OMAbstractFactory.getSOAP11Factory().createOMDocument();
        omDoc.addChild(envelope);

        XMLStreamReader parser = XMLInputFactory.newInstance().
                createXMLStreamReader(new StringReader(bodyText));
        StAXOMBuilder builder = new StAXOMBuilder(parser);

        // set a dummy static message
        envelope.getBody().addChild(builder.getDocumentElement());

        synCtx.setEnvelope(envelope);
        return synCtx;
    }

    public static TestMessageContext getTestContextForXSLTMediator(String bodyText, Map props) throws Exception {

        // create a test synapse context
        TestMessageContext synCtx = new TestMessageContext();
        SynapseConfiguration testConfig = new SynapseConfiguration();
        testConfig.setRegistry(new SimpleURLRegistry());

        if (props != null) {
            Iterator iter = props.keySet().iterator();
            while (iter.hasNext()) {
                String key = (String) iter.next();
                testConfig.addEntry(key, (Entry) props.get(key));
            }
        }
        synCtx.setConfiguration(testConfig);

        SOAPEnvelope envelope = OMAbstractFactory.getSOAP11Factory().getDefaultEnvelope();
        OMDocument omDoc = OMAbstractFactory.getSOAP11Factory().createOMDocument();
        omDoc.addChild(envelope);

        XMLStreamReader parser = XMLInputFactory.newInstance().
                createXMLStreamReader(new StringReader(bodyText));
        StAXOMBuilder builder = new StAXOMBuilder(parser);

        // set a dummy static message
        OMFactory fac = OMAbstractFactory.getOMFactory();
        envelope.getBody().addChild(fac.createOMText("first text child "));
        envelope.getBody().addChild(builder.getDocumentElement());
        envelope.getBody().addChild(fac.createOMText("second text child "));

        synCtx.setEnvelope(envelope);
        return synCtx;
    }

    public static TestMessageContext getTestContextForXSLTMediatorUsingFile(String path, Map props) throws Exception {

        // create a test synapse context
        TestMessageContext synCtx = new TestMessageContext();
        SynapseConfiguration testConfig = new SynapseConfiguration();
        testConfig.setRegistry(new SimpleURLRegistry());

        if (props != null) {
            Iterator iter = props.keySet().iterator();
            while (iter.hasNext()) {
                String key = (String) iter.next();
                testConfig.addEntry(key, (Entry) props.get(key));
            }
        }
        synCtx.setConfiguration(testConfig);

        SOAPEnvelope envelope = OMAbstractFactory.getSOAP11Factory().getDefaultEnvelope();
        OMDocument omDoc = OMAbstractFactory.getSOAP11Factory().createOMDocument();
        omDoc.addChild(envelope);

        //XMLStreamReader parser = XMLInputFactory.newInstance().
        //        createXMLStreamReader(new FileReader(path));
        StAXOMBuilder builder = new StAXOMBuilder(path);

        // set a dummy static message
        OMFactory fac = OMAbstractFactory.getOMFactory();
        envelope.getBody().addChild(fac.createOMText("first text child "));
        envelope.getBody().addChild(builder.getDocumentElement());
        envelope.getBody().addChild(fac.createOMText("second text child "));

        synCtx.setEnvelope(envelope);
        return synCtx;
    }

    public static TestMessageContext getTestContext(String bodyText) throws Exception {
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
