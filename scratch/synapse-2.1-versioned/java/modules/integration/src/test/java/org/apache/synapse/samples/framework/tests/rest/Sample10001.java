package org.apache.synapse.samples.framework.tests.rest;

import org.apache.axiom.om.OMElement;
import org.apache.http.HttpStatus;
import org.apache.synapse.samples.framework.SynapseTestCase;
import org.apache.synapse.samples.framework.clients.BasicHttpClient;
import org.apache.synapse.samples.framework.clients.HttpResponse;

public class Sample10001 extends SynapseTestCase {

    public Sample10001() {
        super(10001);
    }

    public void testGetQuote() throws Exception {
        BasicHttpClient client = new BasicHttpClient();
        HttpResponse response = client.doGet("http://127.0.0.1:8280/stockquote/view/IBM");
        assertEquals(response.getStatus(), HttpStatus.SC_INTERNAL_SERVER_ERROR);
        OMElement body = response.getBodyAsXML();
        assertEquals(body.getLocalName(), "Exception");
        log.info("An exception was thrown as expected.");
    }
}
