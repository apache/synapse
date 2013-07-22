package org.apache.synapse.samples.framework.tests.transport;

import org.apache.synapse.samples.framework.*;
import org.apache.synapse.samples.framework.clients.StockQuoteSampleClient;

public class Sample251 extends SynapseTestCase {

    public Sample251() {
        super(251);
    }

    public void testPlaceOrder() throws Exception {
        Axis2BackEndServerController axis2Server = getAxis2Server();
        if (axis2Server == null) {
            fail("Failed to load the Axis2BackEndServerController");
        }

        assertEquals(0, axis2Server.getMessageCount("SimpleStockQuoteService", "placeOrder"));
        StockQuoteSampleClient client = getStockQuoteClient();
        String trpUrl = "http://localhost:8280/services/StockQuoteProxy";
        SampleClientResult result = client.placeOrder(null, trpUrl, null, "IBM");
        assertResponseReceived(result);
        Thread.sleep(2000);
        assertEquals(1, axis2Server.getMessageCount("SimpleStockQuoteService", "placeOrder"));
    }
}
