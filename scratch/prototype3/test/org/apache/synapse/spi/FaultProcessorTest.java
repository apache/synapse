package org.apache.synapse.spi;

import junit.framework.TestCase;
import org.apache.axis2.transport.http.SimpleHTTPServer;
import org.apache.axis2.context.MessageContext;
import org.apache.synapse.util.Axis2EvnSetup;
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
    private MessageContext mc;
    private SimpleHTTPServer synapseServer;
    public  void setUp() throws Exception {
        mc = Axis2EvnSetup.axis2Deployment("target/synapse-repository-fault");
        synapseServer = new SimpleHTTPServer(mc.getSystemContext(),5043);
        synapseServer.start();
    }
    protected void tearDown() throws Exception {
        synapseServer.stop();
    }
    public void testFaultPrcessor() throws Exception {

    }

}
