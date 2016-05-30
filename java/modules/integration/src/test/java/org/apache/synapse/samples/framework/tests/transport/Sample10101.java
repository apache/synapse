package org.apache.synapse.samples.framework.tests.transport;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.synapse.samples.framework.SynapseTestCase;
import org.apache.synapse.samples.framework.clients.BasicHttpClient;
import org.apache.synapse.samples.framework.clients.HttpResponse;

import java.util.List;
import java.util.Map;

public class Sample10101 extends SynapseTestCase {

    public static final byte[] TEST_PAYLOAD = "<test>foo</test>".getBytes();

    public Sample10101() {
        super(10101);
    }

    public void testChunking() throws Exception {
        BasicHttpClient client = new BasicHttpClient();
        HttpResponse response = client.doPost("http://127.0.0.1:8280/test/chunked",
                TEST_PAYLOAD, "application/xml");
        assertEquals(HttpStatus.SC_OK, response.getStatus());
        Map<String,List<String>> headers = response.getBodyAsMap();
        assertTrue(headers.containsKey(HttpHeaders.TRANSFER_ENCODING));
        assertFalse(headers.containsKey(HttpHeaders.CONTENT_LENGTH));
        assertEquals("chunked", headers.get(HttpHeaders.TRANSFER_ENCODING).get(0));
    }

    public void testDisableChunking() throws Exception {
        BasicHttpClient client = new BasicHttpClient();
        HttpResponse response = client.doPost("http://127.0.0.1:8280/test/content_length",
                TEST_PAYLOAD, "application/xml");
        assertEquals(HttpStatus.SC_OK, response.getStatus());
        Map<String,List<String>> headers = response.getBodyAsMap();
        assertFalse(headers.containsKey(HttpHeaders.TRANSFER_ENCODING));
        assertTrue(headers.containsKey(HttpHeaders.CONTENT_LENGTH));
        assertEquals(TEST_PAYLOAD.length, Integer.parseInt(headers.get(
                HttpHeaders.CONTENT_LENGTH).get(0)));
    }

}
