package org.apache.synapse.samples.framework.tests.rest;

import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.apache.synapse.samples.framework.SynapseTestCase;
import org.apache.synapse.samples.framework.clients.BasicHttpClient;
import org.apache.synapse.samples.framework.clients.HttpResponse;

public class Sample800 extends SynapseTestCase {

    private static final Log log = LogFactory.getLog(Sample800.class);

    public Sample800() {
        super(800);
    }

    public void testGetQuote() throws Exception {
        BasicHttpClient client = new BasicHttpClient();
        HttpResponse response = client.doGet("http://127.0.0.1:8280/stockquote/view/IBM");
        assertEquals(response.getStatus(), HttpStatus.SC_OK);
        OMElement body = response.getBodyAsXML();
        assertEquals(body.getLocalName(), "getQuoteResponse");
    }

    public void testPlaceOrder() throws Exception {
        BasicHttpClient client = new BasicHttpClient();
        String payload = "<placeOrder xmlns=\"http://services.samples\">\n" +
                "  <order>\n" +
                "     <price>50</price>\n" +
                "     <quantity>10</quantity>\n" +
                "     <symbol>IBM</symbol>\n" +
                "  </order>\n" +
                "</placeOrder>";
        HttpResponse response = client.doPost("http://127.0.0.1:8280/stockquote/order",
                payload.getBytes(), "application/xml");
        assertEquals(response.getStatus(), HttpStatus.SC_ACCEPTED);
    }
}
