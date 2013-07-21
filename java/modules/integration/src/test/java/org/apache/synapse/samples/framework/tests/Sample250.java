package org.apache.synapse.samples.framework.tests;

import org.apache.synapse.samples.framework.Axis2BackEndServerController;
import org.apache.synapse.samples.framework.BackEndServerController;
import org.apache.synapse.samples.framework.SynapseTestCase;
import org.apache.synapse.samples.framework.clients.JMSSampleClient;

import java.util.List;

public class Sample250 extends SynapseTestCase {

    public Sample250() {
        super(250);
    }

    public void testPlaceOrder() throws Exception {
        List<BackEndServerController> servers = getBackendServerControllers();
        Axis2BackEndServerController axis2Server = null;
        for (BackEndServerController server : servers) {
            if (server instanceof Axis2BackEndServerController) {
                axis2Server = (Axis2BackEndServerController) server;
                break;
            }
        }
        if (axis2Server == null) {
            fail("Failed to load the Axis2BackEndServerController");
        }

        assertEquals(0, axis2Server.getMessageCount("SimpleStockQuoteService", "placeOrder"));
        JMSSampleClient client = new JMSSampleClient();
        client.connect("dynamicQueues/StockQuoteProxy");
        client.sendAsPox("IBM");
        Thread.sleep(2000);
        assertEquals(1, axis2Server.getMessageCount("SimpleStockQuoteService", "placeOrder"));
        client.shutdown();
    }
}
