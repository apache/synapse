package org.apache.synapse.spi.resources;

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.transport.http.SimpleHTTPServer;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseMessage;
import org.apache.synapse.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.axis2.Axis2SynapseMessage;
import org.apache.synapse.util.Axis2EnvSetup;
import org.apache.axiom.om.OMElement;
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
 */

public class SimpleResourcesAquiringTest extends TestCase {
    private MessageContext msgCtx;
    private SynapseEnvironment env;
    private OMElement config;
    private String synapsexml =
            "<synapse xmlns=\"http://ws.apache.org/ns/synapse\">\n" +
                    "<resource type=\"org.apache.synapse.resources.http.SimpleGETResourceHandler\" uri-root=\"http://127.0.0.1:8090/axis2/services/npe/simple_resources\">\n" +
                    "<property name=\"http.username\">paul</property>" +
                    "<property name=\"http.password\">pass</property>" +
                    "</resource>\n" +
                    "<classmediator class=\"org.apache.synapse.mediators.SimpleGETResourceAquiringMediator\"/>\n" +
                    "</synapse>";

    private SimpleHTTPServer resources;

    public void setUp() throws Exception {
        msgCtx = Axis2EnvSetup.axis2Deployment("target/synapse-repository");
        config = Axis2EnvSetup.getSynapseConfigElement(synapsexml);
        env = new Axis2SynapseEnvironment(config,
                Thread.currentThread().getContextClassLoader());
        ConfigurationContext context = ConfigurationContextFactory
                .createConfigurationContextFromFileSystem(
                        "target/synapse-repository-resources", null);
        resources = new SimpleHTTPServer(context, 8090);
        resources.start();
    }

    protected void tearDown() throws Exception {
        resources.stop();
    }

    public void testSimpleResourcesHandler() throws Exception {
        SynapseMessage smc = new Axis2SynapseMessage(msgCtx);
        env.injectMessage(smc);

    }
}
