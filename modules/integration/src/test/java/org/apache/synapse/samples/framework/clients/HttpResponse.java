package org.apache.synapse.samples.framework.clients;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMXMLBuilderFactory;
import org.apache.axiom.om.OMXMLParserWrapper;
import org.apache.http.Header;
import org.apache.http.HttpEntity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class HttpResponse {

    private int status;
    private Map<String,String> headers = new HashMap<String, String>();
    private byte[] body;

    public HttpResponse(org.apache.http.HttpResponse response) throws IOException {
        this.status = response.getStatusLine().getStatusCode();
        Header[] headers = response.getAllHeaders();
        for (Header header : headers) {
            this.headers.put(header.getName(), header.getValue());
        }
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            InputStream in = entity.getContent();
            byte[] data = new byte[1024];
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int len;
            while ((len = in.read(data)) != -1) {
                out.write(data, 0, len);
            }
            this.body = out.toByteArray();
            out.close();
            in.close();
        }
    }

    public int getStatus() {
        return status;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public byte[] getBody() {
        return body;
    }

    public OMElement getBodyAsXML() {
        OMXMLParserWrapper builder = OMXMLBuilderFactory.createOMBuilder(
                new ByteArrayInputStream(this.body));
        return builder.getDocumentElement();
    }

    public String getBodyAsString() {
        return new String(this.body);
    }
}
