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
import org.apache.axis2.om.*;
import org.apache.axis2.om.impl.llom.builder.StAXOMBuilder;
import org.apache.axis2.soap.SOAPEnvelope;
import org.apache.synapse.axis2.SynapseMessageReceiver;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
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

public class Axis2EvnSetup {
    public static MessageContext axis2Deployment(String testingRepository)
            throws AxisFault {
        ConfigurationContextFactory conFac = new ConfigurationContextFactory();
        ConfigurationContext configCtx = conFac
                .buildConfigurationContext(testingRepository);
        MessageContext msgCtx = new MessageContext(configCtx);
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
                fac.createText(value, "Synapse Testing String"));
        method.addChild(value);
        return method;
    }
}
