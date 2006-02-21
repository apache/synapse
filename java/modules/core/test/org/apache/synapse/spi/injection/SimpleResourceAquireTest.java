package org.apache.synapse.spi.injection;

import junit.framework.TestCase;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.om.OMElement;
import org.apache.axis2.transport.http.SimpleHTTPServer;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseMessage;
import org.apache.synapse.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.axis2.Axis2SynapseMessage;
import org.apache.synapse.util.Axis2EnvSetup;

/**
 *
 */
public class SimpleResourceAquireTest extends TestCase {
    private MessageContext msgCtx;
    private SynapseEnvironment env;
    private OMElement config;
    private String synapsexml =
                    "<synapse xmlns=\"http://ws.apache.org/ns/synapse\">\n" +
                    "    <classmediator class=\"org.apache.synapse.mediators.SimpleResourceAquireMediator\"/>\n" +
                    "</synapse>";

    private SimpleHTTPServer resources;

    public void setUp() throws Exception {
        msgCtx = Axis2EnvSetup.axis2Deployment("target/synapse-repository");
        config = Axis2EnvSetup.getSynapseConfigElement(synapsexml);
        env = new Axis2SynapseEnvironment(config,
                Thread.currentThread().getContextClassLoader());
        resources = new SimpleHTTPServer("target/synapse-repository-resources",8090);
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
