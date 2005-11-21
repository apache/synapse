package org.apache.synapse.spi.engine;

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.soap.SOAPEnvelope;
import org.apache.axis2.om.OMAbstractFactory;
import org.apache.axis2.om.OMFactory;
import org.apache.axis2.om.OMDocument;
import org.apache.axis2.om.OMElement;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.InOutAxisOperation;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.synapse.axis2.SynapseMessageReceiver;

import javax.xml.namespace.QName;
import java.io.File;

import junit.framework.TestCase;
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

public class SynapseEngineRuleTest extends TestCase {
    private MessageContext msgCtx;
    private ConfigurationContext configCtx;

    public  void setUp() throws Exception {
        ConfigurationContextFactory conFac = new ConfigurationContextFactory();
        File path = new File("./repo");
        configCtx = conFac
                .buildClientConfigurationContext(path.getAbsolutePath());
        OMFactory fac = OMAbstractFactory.getOMFactory();
        SOAPEnvelope dumEnv = OMAbstractFactory.getSOAP11Factory()
                .getDefaultEnvelope();
        OMDocument doc = fac.createOMDocument();
        doc.addChild(dumEnv);

        OMElement ele = fac.createOMElement("text", "urn:text-body","t");
        dumEnv.getBody().addChild(ele);

        msgCtx = new MessageContext(configCtx);
        msgCtx.setEnvelope(dumEnv);
        msgCtx.setServerSide(true);

        //------------------
        AxisConfiguration axisConfiguration = msgCtx.getSystemContext()
                .getAxisConfiguration();
        AxisService service = new AxisService(
                new QName("se"));
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

    }
    public void testAllAndXpath() throws Exception {
        AxisEngine engine = new AxisEngine(configCtx);
        engine.receive(msgCtx);
    }
}
