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
package org.apache.synapse.mediators;

import org.apache.synapse.TestMessageContext;
import org.apache.synapse.MessageContext;
import org.apache.synapse.registry.url.SimpleURLRegistry;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMDocument;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.config.SynapseConfiguration;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLInputFactory;
import java.io.StringReader;
import java.util.List;
import java.util.Iterator;
import java.util.Map;

public class TestUtils {

    public static TestMessageContext getTestContext(String bodyText, Map props) throws Exception {

        // create a test synapse context
        TestMessageContext synCtx = new TestMessageContext();
        SynapseConfiguration testConfig = new SynapseConfiguration();
        testConfig.addRegistry(null, new SimpleURLRegistry());

        if (props != null) {
            Iterator iter = props.keySet().iterator();
            while (iter.hasNext()) {
                String key = (String) iter.next();
                testConfig.addProperty(key, props.get(key));
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

    public static TestMessageContext getTestContext(String bodyText) throws Exception {
        return getTestContext(bodyText, null);
    }

    public static MessageContext createLightweightSynapseMessageContext(
            String paylod) throws Exception {
        org.apache.axis2.context.MessageContext mc =
                new org.apache.axis2.context.MessageContext();
        SynapseConfiguration config = new SynapseConfiguration();
        SynapseEnvironment env = new Axis2SynapseEnvironment();
        MessageContext synMc = new Axis2MessageContext(mc,config,env);
        SOAPEnvelope envelope =
                OMAbstractFactory.getSOAP11Factory().getDefaultEnvelope();
        OMDocument omDoc =
                OMAbstractFactory.getSOAP11Factory().createOMDocument();
        omDoc.addChild(envelope);

        XMLStreamReader parser = XMLInputFactory.newInstance().
                createXMLStreamReader(new StringReader(paylod));
        StAXOMBuilder builder = new StAXOMBuilder(parser);

        // set a dummy static message
        envelope.getBody().addChild(builder.getDocumentElement());

        synMc.setEnvelope(envelope);
        return synMc;
    }
}
