package org.apache.synapse.util;

import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.InOutAxisOperation;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.synapse.axis2.SynapseMessageReceiver;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.om.*;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.File;
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
*
*/

public class Axis2EnvSetup {
    public static MessageContext axis2Deployment(String testingRepository)
            throws AxisFault {
        final ConfigurationContext configCtx = ConfigurationContextFactory
                .createConfigurationContextFromFileSystem(testingRepository,
                        testingRepository + File.separator + "conf" + File.separator + "axis2.xml");
        MessageContext msgCtx = new MessageContext();
        msgCtx.setConfigurationContext(configCtx);
        msgCtx.setEnvelope(testEnvSetup());
        msgCtx.setServerSide(true);

        AxisConfiguration axisConfiguration = msgCtx.getConfigurationContext()
                .getAxisConfiguration();
        AxisService service = new AxisService("se");
        msgCtx.setAxisService(service);
        service.setClassLoader(axisConfiguration.getServiceClassLoader());
        AxisOperation axisOp = new InOutAxisOperation(
                new QName("op"));
        msgCtx.setAxisOperation(axisOp);
        axisOp.setMessageReceiver(new SynapseMessageReceiver());
        service.addOperation(axisOp);
        axisConfiguration.addService(service);
        msgCtx.setTo(
                new EndpointReference("/axis2/services/" + "se" + "/" + "op"));
        msgCtx.setSoapAction("op");
        return msgCtx;
    }

    private static SOAPEnvelope testEnvSetup() {
        OMFactory fac = OMAbstractFactory.getOMFactory();
        SOAPEnvelope env = OMAbstractFactory.getSOAP11Factory()
                .getDefaultEnvelope();
        OMDocument doc = fac.createOMDocument();
        doc.addChild(env);
        OMElement ele = fac.createOMElement("text", "urn:text-body", "ns");
        env.getBody().addChild(ele);
        return env;
    }

    public static OMElement getSynapseConfigElement(String synapseXml)
            throws XMLStreamException {
        XMLStreamReader parser = XMLInputFactory.newInstance()
                .createXMLStreamReader(
                        new ByteArrayInputStream(synapseXml.getBytes()));
        OMFactory fac = OMAbstractFactory.getOMFactory();
        StAXOMBuilder staxBuilder = new StAXOMBuilder(fac, parser);
        OMElement config = staxBuilder.getDocumentElement();
        return config;

    }

    public static OMElement payload() {
        OMFactory fac = OMAbstractFactory.getOMFactory();
        OMNamespace omNs = fac.createOMNamespace(
                "urn:text-body", "ns");
        OMElement method = fac.createOMElement("service", omNs);
        OMElement value = fac.createOMElement("text", omNs);
        value.addChild(
                fac.createOMText(value, "Synapse Testing String"));
        method.addChild(value);
        return method;
    }

    public static OMElement payloadNamedAdddressing() {
        OMFactory fac = OMAbstractFactory.getOMFactory();
        OMNamespace omNs = fac.createOMNamespace(
                "urn:text-body", "ns");
        OMElement method = fac.createOMElement("service", omNs);
        OMElement value = fac.createOMElement("text_addressing", omNs);
        value.addChild(
                fac.createOMText(value,
                        "Synapse Testing String Through Addressing"));
        method.addChild(value);
        return method;
    }

    public static OMElement payloadNamedPing() {
        OMFactory fac = OMAbstractFactory.getOMFactory();
        OMNamespace omNs = fac.createOMNamespace(
                "urn:text-body", "ns");
        OMElement method = fac.createOMElement("service", omNs);
        OMElement value = fac.createOMElement("text_ping", omNs);
        value.addChild(
                fac.createOMText(value, "Synapse Testing String for Ping"));
        method.addChild(value);
        return method;
    }

    public static ConfigurationContext createConfigurationContextFromFileSystem(
            String repository) throws AxisFault {

        return ConfigurationContextFactory
                .createConfigurationContextFromFileSystem(repository, null);
    }
}
