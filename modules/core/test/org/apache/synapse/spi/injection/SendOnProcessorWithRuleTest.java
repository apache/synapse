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
package org.apache.synapse.spi.injection;

import junit.framework.TestCase;

import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.transport.http.SimpleHTTPServer;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.synapse.util.Axis2EnvSetup;
import org.apache.axiom.om.OMElement;

import javax.xml.namespace.QName;


public class SendOnProcessorWithRuleTest extends TestCase {

    private SimpleHTTPServer synapseServer;
    private SimpleHTTPServer axis2Server;
    private EndpointReference targetEpr = new EndpointReference(
            "http://127.0.0.1:5043/axis2/services/anonymous");

    public void setUp() throws Exception {
        ConfigurationContext synapseServerContext = ConfigurationContextFactory
                .createConfigurationContextFromFileSystem(
                        "target/synapse-repository-sendon", null);
        ConfigurationContext serverContext = ConfigurationContextFactory
                .createConfigurationContextFromFileSystem(
                        "target/synapse-repository-sendonAxis2", null);
        synapseServer = new SimpleHTTPServer(synapseServerContext, 5043);
        /**
         * axis2Server is the one who holds the actual service
         */
        axis2Server =
                new SimpleHTTPServer(serverContext, 8090);
        synapseServer.start();
        axis2Server.start();
    }

    protected void tearDown() throws Exception {
        synapseServer.stop();
        axis2Server.stop();
    }

    public void testSendProcessor() throws Exception {
        ServiceClient serviceClient = new ServiceClient(
                Axis2EnvSetup.createConfigurationContextFromFileSystem(
                        "target/synapse-repository-sendon"), null);
        Options options = new Options();
        options.setTo(targetEpr);
        serviceClient.setOptions(options);
        serviceClient.disEngageModule(new QName("addressing"));
        OMElement response = serviceClient.sendReceive(Axis2EnvSetup.payload());
        assertEquals("Synapse Testing String_Response", response.getText());

    }

    public void testSendProcessorMultipleTimes() throws Exception {
        for (int i = 0; i < 10; i++) {
            ServiceClient serviceClient = new ServiceClient(
                    Axis2EnvSetup.createConfigurationContextFromFileSystem(
                            "target/synapse-repository-sendon"), null);
            Options options = new Options();
            options.setTo(targetEpr);
            serviceClient.setOptions(options);
            serviceClient.disEngageModule(new QName("addressing"));
            OMElement response =
                    serviceClient.sendReceive(Axis2EnvSetup.payload());
            assertEquals("Synapse Testing String_Response", response.getText());
        }
    }

}
