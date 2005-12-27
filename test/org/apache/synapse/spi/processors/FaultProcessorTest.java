package org.apache.synapse.spi.processors;

import junit.framework.TestCase;
import org.apache.axis2.transport.http.SimpleHTTPServer;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Call;
import org.apache.axis2.client.Options;
import org.apache.axis2.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.synapse.util.Axis2EvnSetup;

import javax.xml.namespace.QName;
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

public class FaultProcessorTest extends TestCase {

    private SimpleHTTPServer synapseServer;
    private EndpointReference targetEpr = new EndpointReference(
            "http://127.0.0.1:5043/axis2/services/anonymous");
    private QName operation = new QName("anonymous");

    public void setUp() throws Exception {
        synapseServer = new SimpleHTTPServer("target/synapse-repository-fault",
                5043);
        synapseServer.start();
    }

    protected void tearDown() throws Exception {
        synapseServer.stop();
    }

    public void testFaultProcessor() {
        try {
            Call call = new Call();
            Options options = new Options();
            options.setTo(targetEpr);
            call.setClientOptions(options);
            call.invokeBlocking(operation.getLocalPart(),
                    Axis2EvnSetup.payload());
            fail(null);
        } catch (AxisFault e) {
        }
    }

}
