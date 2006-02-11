package org.apache.synapse.spi.injection;

import junit.framework.TestCase;
import org.apache.axis2.transport.http.SimpleHTTPServer;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.client.Options;
import org.apache.axis2.om.OMElement;
import org.apache.synapse.util.Axis2EnvSetup;

/*
 */
public class SendNowProcessorWithRuleTest extends TestCase {
     private SimpleHTTPServer synapseServer;
    private SimpleHTTPServer axis2Server;
    private EndpointReference targetEpr = new EndpointReference(
            "http://127.0.0.1:5043/axis2/services/anonymous");

    public void setUp() throws Exception {
        synapseServer = new SimpleHTTPServer("target/synapse-repository-sendnow",
                5043);
        /**
         * axis2Server is the one who holds the actual service
         */
        axis2Server =
                new SimpleHTTPServer("target/synapse-repository-sendonAxis2",
                        8090);
        synapseServer.start();
        axis2Server.start();
    }

    protected void tearDown() throws Exception {
        synapseServer.stop();
        axis2Server.stop();
    }

    public void testSendNowProcessor() throws Exception {
        ServiceClient serviceClient = new ServiceClient(
                Axis2EnvSetup.createConfigurationContextFromFileSystem(
                        "target/synapse-repository-sendnow"), null);
        Options options = new Options();
        options.setTo(targetEpr);
        serviceClient.setOptions(options);
        OMElement response = serviceClient
                .sendReceive(Axis2EnvSetup.payloadNamedAdddressing());
        assertEquals("Synapse Testing String_Response_With_Addressing",
                response.getText());

    }

    public void testSendNowPingProcessor() throws Exception {
        ServiceClient serviceClient = new ServiceClient(
                Axis2EnvSetup.createConfigurationContextFromFileSystem(
                        "target/synapse-repository-sendnow"), null);
        Options options = new Options();
        options.setTo(targetEpr);
        serviceClient.setOptions(options);
        serviceClient.fireAndForget(Axis2EnvSetup.payloadNamedPing());

    }
}
