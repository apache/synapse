package org.apache.synapse.spi;

import junit.framework.TestCase;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.MessageSender;
import org.apache.axis2.transport.http.SimpleHTTPServer;
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

public class SendOnProcessorTest extends TestCase {
    // todo: i have  to comment out this test case
    // todo: because SimpleHTTPServer does not allow
    // todo: to initiate multiple objects. :(
    private SimpleHTTPServer synapseServer;
    private SimpleHTTPServer axis2Server;
    private EndpointReference targetEpr = new EndpointReference(
            "http://127.0.0.1:5043/axis2/services/anonymous");
    private QName operation = new QName("anonymous");

    public void setUp() throws Exception {
//        synapseServer = new SimpleHTTPServer("target/synapse-repository-sendon",
//                5043);
//        /**
//         * axis2Server is the one who holds the actual service
//         */
//        axis2Server = new SimpleHTTPServer("synapse-repository-sendonAxis2",
//                8090);
//        synapseServer.start();
//        axis2Server.start();
    }

    protected void tearDown() throws Exception {
//        synapseServer.stop();
//        axis2Server.stop();
    }

    public void testSendPrcessor() throws Exception {
//        MessageSender msgSender = new MessageSender();
//        msgSender.setTo(targetEpr);
//        msgSender.setSenderTransport(Constants.TRANSPORT_HTTP);
//        msgSender.send(operation.getLocalPart(), Axis2EvnSetup.payload());

    }

}
