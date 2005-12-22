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
*/package org.apache.synapse.spi.injection;

import junit.framework.TestCase;

import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.MessageSender;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.Call;
import org.apache.axis2.transport.http.SimpleHTTPServer;
import org.apache.axis2.om.OMElement;
import org.apache.synapse.util.Axis2EvnSetup;

import javax.xml.namespace.QName;


public class SendOnProcessorWithRuleTest extends TestCase {

    private SimpleHTTPServer synapseServer;
    private SimpleHTTPServer axis2Server;
    private EndpointReference targetEpr = new EndpointReference(
            "http://127.0.0.1:5043/axis2/services/anonymous");
    private QName operation = new QName("anonymous");

    public void setUp() throws Exception {
        synapseServer = new SimpleHTTPServer("target/synapse-repository-sendon",
                5043);
        /**
         * axis2Server is the one who holds the actual service
         */
        axis2Server = new SimpleHTTPServer("target/synapse-repository-sendonAxis2",
                8090);
        synapseServer.start();
        axis2Server.start();
    }

    protected void tearDown() throws Exception {
        synapseServer.stop();
        axis2Server.stop();
    }

    public void testSendProcessor() throws Exception {
        Call call = new Call();
        Options options = new Options();
        options.setTo(targetEpr);
        call.setClientOptions(options);
        OMElement response = call.invokeBlocking(operation.getLocalPart(), Axis2EvnSetup.payload());
        assertEquals("Synapse Testing String_Response",response.getText());

    }

}
