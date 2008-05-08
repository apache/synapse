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
package org.apache.synapse.transport.udp;

import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.InOutAxisOperation;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.receivers.RawXMLINOnlyMessageReceiver;
import org.apache.axis2.receivers.RawXMLINOutMessageReceiver;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.synapse.transport.Echo;

/**
 * Test case for {@link UDPListener} and {@link UDPSender}.
 */
public class UDPTest extends TestCase {
    public void testSoapOverUdpWithEchoService() throws Exception {
        // Create the configuration context. The test assumes that the repository is set up
        // to contain the addressing module. The UDP transport is enabled in axis2.xml.
        ConfigurationContext configurationContext
            = ConfigurationContextFactory.createConfigurationContextFromFileSystem("target/test_rep",
                    UDPTest.class.getResource("axis2.xml").getFile());
        
        //
        // Set up the echo service
        //
        // TODO: copy&paste from UtilsTransportServer#deployEchoService
        
        AxisService service = new AxisService("EchoService");
        service.setClassLoader(Thread.currentThread().getContextClassLoader());
        service.addParameter(new Parameter(Constants.SERVICE_CLASS, Echo.class.getName()));

        AxisOperation axisOp = new InOutAxisOperation(new QName("echoOMElement"));
        axisOp.setMessageReceiver(new RawXMLINOutMessageReceiver());
        axisOp.setStyle(WSDLConstants.STYLE_RPC);
        service.addOperation(axisOp);
        service.mapActionToOperation(Constants.AXIS2_NAMESPACE_URI + "/echoOMElement", axisOp);

        axisOp = new InOutAxisOperation(new QName("echoOMElementNoResponse"));
        axisOp.setMessageReceiver(new RawXMLINOnlyMessageReceiver());
        axisOp.setStyle(WSDLConstants.STYLE_RPC);
        service.addOperation(axisOp);
        service.mapActionToOperation(Constants.AXIS2_NAMESPACE_URI + "/echoOMElementNoResponse", axisOp);
        
        service.addParameter(UDPConstants.PORT_KEY, 3333);
        service.addParameter(UDPConstants.CONTENT_TYPE_KEY, "text/xml+soap");
        
        configurationContext.getAxisConfiguration().addService(service);
        
        //
        // Create echo message to send
        //
        // TODO: copy&paste from AbstractTransportTest#createPayload
        
        OMFactory fac = OMAbstractFactory.getOMFactory();
        OMNamespace omNs = fac.createOMNamespace("http://localhost/axis2/services/EchoXMLService", "my");
        OMElement method = fac.createOMElement("echoOMElement", omNs);
        OMElement value = fac.createOMElement("myValue", omNs);
        value.addChild(fac.createOMText(value, "omTextValue"));
        method.addChild(value);
        
        //
        // Call the echo service and check response
        //
        
        Options options = new Options();
        options.setTo(new EndpointReference("udp://127.0.0.1:3333?contentType=text/xml+soap"));
        options.setAction(Constants.AXIS2_NAMESPACE_URI + "/echoOMElement");
        options.setUseSeparateListener(true);
        options.setTimeOutInMilliSeconds(Long.MAX_VALUE);

        ServiceClient serviceClient = new ServiceClient(configurationContext, null);
        serviceClient.setOptions(options);
        // We need to set up the anonymous service Axis uses to get the response
        AxisService clientService = serviceClient.getServiceContext().getAxisService();
        clientService.addParameter(UDPConstants.PORT_KEY, 4444);
        clientService.addParameter(UDPConstants.CONTENT_TYPE_KEY, "text/xml+soap");
        OMElement response = serviceClient.sendReceive(method);
        
        assertEquals(new QName(omNs.getNamespaceURI(), "echoOMElementResponse"), response.getQName());
    }
}
